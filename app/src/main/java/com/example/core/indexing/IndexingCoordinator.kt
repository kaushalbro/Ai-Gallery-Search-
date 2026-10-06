package com.example.core.indexing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.example.core.face.FaceEngine
import com.example.core.hashing.HashEngine
import com.example.core.notification.OcrNotificationManager
import com.example.core.ocr.ImagePreprocessor
import com.example.core.ocr.OcrEngine
import com.example.data.model.BatchProgressState
import com.example.data.model.ExecutionProvider
import com.example.data.model.GalleryAsset
import com.example.data.model.IndexingMode
import com.example.data.model.IndexingStats
import com.example.data.model.IndexingStatus
import com.example.data.repository.GalleryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File
import java.io.InputStream
import java.net.URI

class IndexingCoordinator(
    private val context: Context,
    private val repository: GalleryRepository,
    private val ocrEngine: OcrEngine,
    private val faceEngine: FaceEngine = FaceEngine(context)
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var indexingJob: Job? = null

    private val notificationManager = OcrNotificationManager(context)

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _currentStats = MutableStateFlow(IndexingStats())
    val currentStats: StateFlow<IndexingStats> = _currentStats.asStateFlow()

    private val _batchProgress = MutableStateFlow(BatchProgressState())
    val batchProgress: StateFlow<BatchProgressState> = _batchProgress.asStateFlow()

    private var activeMode = IndexingMode.FAST
    private var batteryPct = 85
    private var isDeviceCharging = true
    private var processedInWindow = 0
    private var windowStartTime = System.currentTimeMillis()
    private var batchTotalSnapshot = 0
    private var batchCompletedCount = 0

    init {
        registerBatteryMonitor()
        // Collect DB stats reactively
        scope.launch {
            repository.statsFlow.collect { dbStats ->
                _currentStats.update { current ->
                    current.copy(
                        totalAssets = dbStats.totalAssets,
                        indexedCount = dbStats.indexedCount,
                        queuedCount = dbStats.queuedCount,
                        skippedDuplicates = dbStats.skippedDuplicates,
                        failedCount = dbStats.failedCount,
                        activeWorkers = getEffectiveWorkerCount(),
                        ocrProvider = ocrEngine.getExecutionProvider(),
                        batteryLevel = batteryPct,
                        isCharging = isDeviceCharging,
                        isPaused = _isPaused.value
                    )
                }
            }
        }
    }

    fun setIndexingMode(mode: IndexingMode) {
        this.activeMode = mode
        _currentStats.update { it.copy(activeWorkers = getEffectiveWorkerCount()) }
    }

    fun getIndexingMode(): IndexingMode = activeMode

    fun setExecutionProvider(provider: ExecutionProvider) {
        ocrEngine.setExecutionProvider(provider)
        _currentStats.update { it.copy(ocrProvider = provider) }
    }

    fun pause() {
        _isPaused.value = true
        _currentStats.update { it.copy(isPaused = true, currentSpeedFps = 0f) }
    }

    fun resume() {
        _isPaused.value = false
        _currentStats.update { it.copy(isPaused = false) }
        startIndexing()
    }

    fun startIndexing() {
        if (indexingJob?.isActive == true) return

        indexingJob = scope.launch {
            // Recover any assets interrupted during prior app runs
            repository.resetStalledAssets()

            val semaphore = Semaphore(getEffectiveWorkerCount())

            while (isActive && !_isPaused.value) {
                var pendingJobs = repository.getPendingJobs(limit = 10)
                if (pendingJobs.isEmpty()) {
                    // Check if any unindexed assets need job creation
                    repository.enqueueUnindexedAssets()
                    pendingJobs = repository.getPendingJobs(limit = 10)
                }

                if (pendingJobs.isEmpty()) {
                    if (_batchProgress.value.isActive) {
                        val finishedCount = batchCompletedCount
                        _batchProgress.value = BatchProgressState(
                            isActive = false,
                            isComplete = true,
                            totalCount = finishedCount,
                            completedCount = finishedCount
                        )
                        if (finishedCount > 0) {
                            notificationManager.notifyBatchComplete(finishedCount)
                        }
                        batchTotalSnapshot = 0
                        batchCompletedCount = 0
                    }
                    _currentStats.update { it.copy(currentSpeedFps = 0f, thermalState = "Optimal (Idle / Up-to-date)") }
                    delay(2000)
                    continue
                }

                // Initialize batch snapshot if not active
                if (!_batchProgress.value.isActive) {
                    batchTotalSnapshot = (_currentStats.value.queuedCount + pendingJobs.size).coerceAtLeast(pendingJobs.size)
                    batchCompletedCount = 0
                    _batchProgress.value = BatchProgressState(
                        isActive = true,
                        totalCount = batchTotalSnapshot,
                        completedCount = 0,
                        currentFilename = "Processing...",
                        startedAt = System.currentTimeMillis()
                    )
                }

                // Process batch concurrently
                val workerJobs = pendingJobs.map { jobEntity ->
                    launch {
                        semaphore.withPermit {
                            if (!isActive || _isPaused.value) return@withPermit
                            try {
                                processAsset(jobEntity.assetId, jobEntity.id)
                            } catch (t: Throwable) {
                                t.printStackTrace()
                            }
                        }
                    }
                }

                workerJobs.forEach { it.join() }
                updateSpeedMetric()
            }
        }
    }

    private suspend fun processAsset(assetId: Long, jobId: Long) {
        val asset = repository.getAssetById(assetId) ?: return
        if (asset.indexingStatus == IndexingStatus.INDEXED || asset.indexingStatus == IndexingStatus.SKIPPED_DUPLICATE) {
            repository.markJobCompleted(jobId)
            return
        }

        // Prevent infinite loops on permanently corrupt images
        if (asset.retryCount >= 3) {
            repository.updateAssetStatus(assetId, IndexingStatus.FAILED_PERMANENT, "Exceeded 3 retries")
            repository.markJobCompleted(jobId, error = "Exceeded max retry attempts")
            return
        }

        try {
            repository.markJobStarted(jobId)
            repository.updateAssetStatus(assetId, IndexingStatus.HASHING)
            _batchProgress.update { it.copy(currentFilename = asset.filename) }

            // 1. Load image and compute exact hash
            val processedImage = ImagePreprocessor.loadAndPreprocess(context, asset.uri, maxDimension = 3200)
            if (processedImage == null) {
                repository.updateAssetStatus(assetId, IndexingStatus.FAILED_PERMANENT, "Could not decode bitmap")
                repository.markJobCompleted(jobId, error = "Decode failed")
                return
            }

            val exactHash = HashEngine.computeExactHash(processedImage.bitmap.toByteArray())
            val pHash = HashEngine.computePerceptualHash(processedImage.bitmap)

            // 2. Duplicate Detection
            val existingCanonical = repository.findCanonicalByHash(exactHash)
            if (existingCanonical != null && existingCanonical.id != asset.id && existingCanonical.canonicalDocumentId != null) {
                // Reuse existing OCR document without re-running OCR
                repository.markAssetDuplicate(
                    assetId = asset.id,
                    canonicalDocId = existingCanonical.canonicalDocumentId,
                    exactHash = exactHash,
                    pHash = pHash
                )
                repository.markJobCompleted(jobId)
                processedImage.bitmap.recycle()
                trackImageProcessed(asset.filename)
                return
            }

            // 3. Run on-device OCR
            repository.updateAssetStatus(assetId, IndexingStatus.OCR_RUNNING)
            val ocrResult = ocrEngine.processImage(asset.id, processedImage.bitmap)

            // 4. Run on-device Neural Face & People Detection
            val detectedFaces = faceEngine.detectFaces(processedImage.bitmap, asset.id)
            if (detectedFaces.isNotEmpty()) {
                repository.saveFacesForAsset(asset.id, detectedFaces, asset.uri)
            }

            // 5. Persist OCR result and update FTS
            repository.saveOcrResult(
                assetId = asset.id,
                document = ocrResult.document,
                blocks = ocrResult.blocks,
                exactHash = exactHash,
                pHash = pHash
            )

            repository.markJobCompleted(jobId)
            processedImage.bitmap.recycle()
            trackImageProcessed(asset.filename)

        } catch (e: Exception) {
            e.printStackTrace()
            repository.incrementRetryCount(assetId)
            val nextStatus = if (asset.retryCount + 1 >= 3) IndexingStatus.FAILED_PERMANENT else IndexingStatus.FAILED_RETRYABLE
            repository.updateAssetStatus(assetId, nextStatus, e.message)
            repository.markJobCompleted(jobId, error = e.message)
        }
    }

    private fun trackImageProcessed(filename: String) {
        processedInWindow++
        batchCompletedCount++
        _batchProgress.update { current ->
            val total = maxOf(current.totalCount, batchCompletedCount)
            current.copy(
                completedCount = batchCompletedCount,
                totalCount = total,
                currentFilename = filename
            )
        }
        val curBatch = _batchProgress.value
        notificationManager.updateProgress(
            completed = curBatch.completedCount,
            total = curBatch.totalCount,
            currentFilename = filename
        )
    }

    private fun updateSpeedMetric() {
        val now = System.currentTimeMillis()
        val elapsedSec = (now - windowStartTime) / 1000f
        if (elapsedSec >= 1.0f) {
            val speed = if (elapsedSec > 0.05f) (processedInWindow / elapsedSec).coerceAtLeast(0f) else 0f
            _currentStats.update { it.copy(currentSpeedFps = (speed * 10).toInt() / 10f) }
            processedInWindow = 0
            windowStartTime = now
        }
    }

    private fun getEffectiveWorkerCount(): Int {
        return 4 // Locked High Performance: 4 Parallel Workers
    }

    private fun registerBatteryMonitor() {
        try {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            intent?.let {
                val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level >= 0 && scale > 0) {
                    batteryPct = (level * 100) / scale
                }
                val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                isDeviceCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
            }
        } catch (e: Exception) {
            batteryPct = 85
            isDeviceCharging = true
        }
    }

    private fun android.graphics.Bitmap.toByteArray(): ByteArray {
        val stream = java.io.ByteArrayOutputStream()
        this.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, stream)
        return stream.toByteArray()
    }
}
