package com.example.data.model

enum class IndexingStatus {
    DISCOVERED,
    QUEUED,
    HASHING,
    PREPROCESSING,
    OCR_RUNNING,
    OCR_COMPLETE,
    INDEXED,
    SKIPPED_DUPLICATE,
    FAILED_RETRYABLE,
    FAILED_PERMANENT,
    REMOVED
}

enum class ExecutionProvider(val displayName: String, val chipLabel: String) {
    CPU("CPU (Default)", "CPU"),
    XNNPACK("XNNPACK Optimized", "XNNPACK"),
    NNAPI("NNAPI Neural Accelerator", "NNAPI")
}

enum class CategoryFilter(val label: String) {
    ALL("All Images"),
    SCREENSHOTS("Screenshots"),
    CAMERA("Camera"),
    DOCUMENTS("Documents"),
    RECEIPTS("Receipts / Bills"),
    CODE("Code / Errors"),
    CREDENTIALS("Wi-Fi / Keys")
}

enum class EntityFilter(val label: String) {
    ALL("All Entities"),
    URL("Web URLs"),
    EMAIL("Emails"),
    PHONE("Phone Numbers"),
    IP("IP Addresses"),
    INVOICE("Invoices / Totals"),
    CREDENTIALS("Passwords / Keys")
}

enum class SortOrder(val label: String) {
    RELEVANCE("Best Match"),
    NEWEST("Newest First"),
    OLDEST("Oldest First"),
    CONFIDENCE("Highest OCR Score")
}

enum class IndexingMode(val label: String, val maxWorkers: Int) {
    FAST("Fast (4 Workers)", 4),
    BALANCED("Balanced (2 Workers)", 2),
    BATTERY_SAVER("Battery Saver (1 Worker)", 1),
    CHARGING_ONLY("Charging Only", 2)
}

data class GalleryAsset(
    val id: Long = 0,
    val platformAssetId: String,
    val uri: String,
    val filename: String,
    val albumName: String,
    val mimeType: String,
    val width: Int,
    val height: Int,
    val fileSize: Long,
    val createdAt: Long,
    val modifiedAt: Long,
    val isScreenshot: Boolean = false,
    val exactHash: String? = null,
    val perceptualHash: String? = null,
    val canonicalDocumentId: Long? = null,
    val indexingStatus: IndexingStatus = IndexingStatus.DISCOVERED,
    val lastError: String? = null,
    val retryCount: Int = 0,
    val discoveredAt: Long = System.currentTimeMillis(),
    val indexedAt: Long? = null
)

data class OcrDocument(
    val id: Long = 0,
    val assetId: Long,
    val normalizedText: String,
    val rawText: String,
    val modelName: String = "PP-OCRv6-Tiny",
    val modelVersion: String = "1.0.0",
    val runtimeProvider: ExecutionProvider = ExecutionProvider.CPU,
    val inferenceMs: Long = 0,
    val blockCount: Int = 0,
    val wordCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

data class OcrBlock(
    val id: Long = 0,
    val documentId: Long,
    val assetId: Long,
    val blockIndex: Int,
    val text: String,
    val normalizedText: String,
    val confidence: Float,
    // Normalized coordinates [0.0 - 1.0] representing 4 corner polygon points
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val x3: Float,
    val y3: Float,
    val x4: Float,
    val y4: Float
)

data class ExtractedEntity(
    val type: EntityFilter,
    val value: String,
    val rawContext: String
)

data class SearchResultItem(
    val asset: GalleryAsset,
    val document: OcrDocument?,
    val matchingBlocks: List<OcrBlock> = emptyList(),
    val allBlocks: List<OcrBlock> = emptyList(),
    val snippet: String,
    val score: Float,
    val matchCount: Int,
    val entities: List<ExtractedEntity> = emptyList()
)

data class IndexingStats(
    val totalAssets: Int = 0,
    val indexedCount: Int = 0,
    val queuedCount: Int = 0,
    val processingCount: Int = 0,
    val skippedDuplicates: Int = 0,
    val failedCount: Int = 0,
    val currentSpeedFps: Float = 0f,
    val activeWorkers: Int = 2,
    val ocrProvider: ExecutionProvider = ExecutionProvider.CPU,
    val thermalState: String = "Normal (Optimal)",
    val batteryLevel: Int = 85,
    val isCharging: Boolean = true,
    val isPaused: Boolean = false,
    val estimatedStorageBytes: Long = 0,
    val isScanning: Boolean = false
) {
    val percentIndexed: Float
        get() = if (totalAssets > 0) (indexedCount.toFloat() / totalAssets.toFloat()) * 100f else 0f
}

data class BatchProgressState(
    val isActive: Boolean = false,
    val totalCount: Int = 0,
    val completedCount: Int = 0,
    val currentFilename: String = "",
    val isComplete: Boolean = false,
    val startedAt: Long = 0L
) {
    val progressFraction: Float
        get() = if (totalCount > 0) (completedCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f) else 0f

    val percentText: String
        get() = "${(progressFraction * 100).toInt()}%"
}

data class BenchmarkResult(
    val id: String,
    val provider: ExecutionProvider,
    val workerCount: Int,
    val imageCount: Int,
    val totalTimeMs: Long,
    val avgInferenceMs: Float,
    val p90Ms: Long,
    val p95Ms: Long,
    val throughputFps: Float,
    val memoryUsageMb: Float,
    val timestamp: Long = System.currentTimeMillis()
)

data class DetectedFace(
    val id: Long = 0,
    val assetId: Long = 0,
    val personId: Long? = null,
    val boxLeft: Float,
    val boxTop: Float,
    val boxRight: Float,
    val boxBottom: Float,
    val confidence: Float = 0.95f,
    val yaw: Float = 0f,
    val pitch: Float = 0f,
    val roll: Float = 0f,
    val embedding: FloatArray = FloatArray(128),
    val personName: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as DetectedFace
        return id == other.id && assetId == other.assetId
    }

    override fun hashCode(): Int = (31 * id + assetId).toInt()
}

data class PersonCluster(
    val id: Long,
    val name: String,
    val coverFaceId: Long?,
    val coverAssetUri: String?,
    val coverBoxLeft: Float = 0f,
    val coverBoxTop: Float = 0f,
    val coverBoxRight: Float = 1f,
    val coverBoxBottom: Float = 1f,
    val faceCount: Int,
    val photosCount: Int = faceCount,
    val lastSeenAt: Long = System.currentTimeMillis()
)

data class FaceMatchResult(
    val asset: GalleryAsset,
    val matchedFace: DetectedFace,
    val similarityScore: Float,
    val personName: String? = null
) {
    val similarityPercent: Int
        get() = (similarityScore * 100).toInt().coerceIn(0, 100)
}

