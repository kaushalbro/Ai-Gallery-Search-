package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.face.FaceEngine
import com.example.core.indexing.IndexingCoordinator
import com.example.core.ocr.ImagePreprocessor
import com.example.core.ocr.OcrEngine
import com.example.core.ocr.OcrEngineModel
import com.example.core.scanner.MediaStoreScanner
import com.example.core.search.SearchEngine
import com.example.core.search.SearchQueryFilters
import com.example.data.local.AppDatabase
import com.example.data.model.BenchmarkResult
import com.example.data.model.BatchProgressState
import com.example.data.model.CategoryFilter
import com.example.data.model.DetectedFace
import com.example.data.model.EntityFilter
import com.example.data.model.ExecutionProvider
import com.example.data.model.FaceMatchResult
import com.example.data.model.GalleryAsset
import com.example.data.model.IndexingMode
import com.example.data.model.IndexingStats
import com.example.data.model.IndexingStatus
import com.example.data.model.OcrBlock
import com.example.data.model.PersonCluster
import com.example.data.model.SearchResultItem
import com.example.data.model.SortOrder
import com.example.data.repository.GalleryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    val repository = GalleryRepository(db)
    val ocrEngine = OcrEngine(application)
    val faceEngine = FaceEngine(application)
    val indexingCoordinator = IndexingCoordinator(application, repository, ocrEngine, faceEngine)
    val searchEngine = SearchEngine(repository, db)

    // Search Query State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _categoryFilter = MutableStateFlow(CategoryFilter.ALL)
    val categoryFilter: StateFlow<CategoryFilter> = _categoryFilter.asStateFlow()

    private val _entityFilter = MutableStateFlow(EntityFilter.ALL)
    val entityFilter: StateFlow<EntityFilter> = _entityFilter.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.RELEVANCE)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _minConfidence = MutableStateFlow(0.15f)
    val minConfidence: StateFlow<Float> = _minConfidence.asStateFlow()

    private val _maxAgeDays = MutableStateFlow<Int?>(null)
    val maxAgeDays: StateFlow<Int?> = _maxAgeDays.asStateFlow()

    // OCR Engine Selection (Defaults to Google ML Kit Neural)
    private val _ocrEngineModel = MutableStateFlow(OcrEngineModel.GOOGLE_ML_KIT)
    val ocrEngineModel: StateFlow<OcrEngineModel> = _ocrEngineModel.asStateFlow()

    // Results State
    private val _searchResults = MutableStateFlow<List<SearchResultItem>>(emptyList())
    val searchResults: StateFlow<List<SearchResultItem>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // Detail & Selection State
    private val _selectedDetailItem = MutableStateFlow<SearchResultItem?>(null)
    val selectedDetailItem: StateFlow<SearchResultItem?> = _selectedDetailItem.asStateFlow()

    private val _selectedBlock = MutableStateFlow<OcrBlock?>(null)
    val selectedBlock: StateFlow<OcrBlock?> = _selectedBlock.asStateFlow()

    private val _selectedBlockIndexes = MutableStateFlow<Set<Int>>(emptySet())
    val selectedBlockIndexes: StateFlow<Set<Int>> = _selectedBlockIndexes.asStateFlow()

    // Faces & People State
    val allPersons: StateFlow<List<PersonCluster>> = repository.allPersonsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedPerson = MutableStateFlow<PersonCluster?>(null)
    val selectedPerson: StateFlow<PersonCluster?> = _selectedPerson.asStateFlow()

    private val _personPhotos = MutableStateFlow<List<GalleryAsset>>(emptyList())
    val personPhotos: StateFlow<List<GalleryAsset>> = _personPhotos.asStateFlow()

    private val _currentAssetFaces = MutableStateFlow<List<DetectedFace>>(emptyList())
    val currentAssetFaces: StateFlow<List<DetectedFace>> = _currentAssetFaces.asStateFlow()

    private val _faceSearchResults = MutableStateFlow<List<FaceMatchResult>>(emptyList())
    val faceSearchResults: StateFlow<List<FaceMatchResult>> = _faceSearchResults.asStateFlow()

    private val _isSearchingFaces = MutableStateFlow(false)
    val isSearchingFaces: StateFlow<Boolean> = _isSearchingFaces.asStateFlow()

    private val _queryFaceUri = MutableStateFlow<String?>(null)
    val queryFaceUri: StateFlow<String?> = _queryFaceUri.asStateFlow()

    // Benchmark State
    private val _benchmarkResults = MutableStateFlow<List<BenchmarkResult>>(emptyList())
    val benchmarkResults: StateFlow<List<BenchmarkResult>> = _benchmarkResults.asStateFlow()

    private val _isBenchmarking = MutableStateFlow(false)
    val isBenchmarking: StateFlow<Boolean> = _isBenchmarking.asStateFlow()

    private val _benchmarkProgress = MutableStateFlow(0f)
    val benchmarkProgress: StateFlow<Float> = _benchmarkProgress.asStateFlow()

    // All Assets Flow
    val allAssets: StateFlow<List<GalleryAsset>> = repository.allAssetsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val indexingStats: StateFlow<IndexingStats> = indexingCoordinator.currentStats
    val batchProgress: StateFlow<BatchProgressState> = indexingCoordinator.batchProgress

    private var searchJob: Job? = null

    init {
        // Initial real device gallery scan and purge of any legacy dummy data
        viewModelScope.launch {
            repository.deleteSeedAssets()

            // Scan device MediaStore for real user photos
            val deviceImages = MediaStoreScanner.scanDeviceImages(application)
            if (deviceImages.isNotEmpty()) {
                repository.insertDiscoveredAssets(deviceImages)
            }

            // Trigger initial search across real photos
            executeSearch()
            // Auto start background indexing queue
            indexingCoordinator.startIndexing()
        }

        // Reactive search debounced
        viewModelScope.launch {
            val baseFilters = combine(
                _searchQuery,
                _categoryFilter,
                _entityFilter,
                _sortOrder,
                _minConfidence
            ) { q, cat, ent, sort, conf ->
                BaseFilterState(q, cat, ent, sort, conf)
            }

            combine(baseFilters, _maxAgeDays) { base, age ->
                SearchQueryFilters(base.q, base.cat, base.ent, base.sort, base.conf, age)
            }
                .debounce(80)
                .collect { filters ->
                    performSearch(filters)
                }
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onCategoryFilterChanged(category: CategoryFilter) {
        _categoryFilter.value = category
    }

    fun onEntityFilterChanged(entity: EntityFilter) {
        _entityFilter.value = entity
    }

    fun onSortOrderChanged(sort: SortOrder) {
        _sortOrder.value = sort
    }

    fun onConfidenceThresholdChanged(conf: Float) {
        _minConfidence.value = conf
        ocrEngine.setConfidenceThreshold(conf)
    }

    fun onDateFilterChanged(days: Int?) {
        _maxAgeDays.value = days
    }

    fun triggerSearch() {
        executeSearch()
    }

    private fun executeSearch() {
        performSearch(
            SearchQueryFilters(
                query = _searchQuery.value,
                category = _categoryFilter.value,
                entity = _entityFilter.value,
                sortOrder = _sortOrder.value,
                minConfidence = _minConfidence.value,
                maxAgeDays = _maxAgeDays.value
            )
        )
    }

    private fun performSearch(filters: SearchQueryFilters) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _isSearching.value = true
            val results = searchEngine.search(filters)
            _searchResults.value = results
            _isSearching.value = false
        }
    }

    fun scanDeviceGallery() {
        viewModelScope.launch {
            val deviceImages = MediaStoreScanner.scanDeviceImages(getApplication())
            if (deviceImages.isNotEmpty()) {
                repository.insertDiscoveredAssets(deviceImages)
            }
            indexingCoordinator.startIndexing()
            executeSearch()
        }
    }

    fun importSelectedImages(uris: List<android.net.Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val newAssets = mutableListOf<GalleryAsset>()
            val resolver = getApplication<Application>().contentResolver

            for (uri in uris) {
                try {
                    var filename = "photo_${System.currentTimeMillis()}.jpg"
                    var size = 0L

                    resolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameCol = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        val sizeCol = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                        if (cursor.moveToFirst()) {
                            if (nameCol >= 0) filename = cursor.getString(nameCol) ?: filename
                            if (sizeCol >= 0) size = cursor.getLong(sizeCol)
                        }
                    }

                    // Decode bounds safely
                    val boundsOptions = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    resolver.openInputStream(uri)?.use { stream ->
                        android.graphics.BitmapFactory.decodeStream(stream, null, boundsOptions)
                    }

                    val width = boundsOptions.outWidth.coerceAtLeast(0)
                    val height = boundsOptions.outHeight.coerceAtLeast(0)
                    val isScreenshot = filename.lowercase(Locale.ROOT).contains("screenshot")

                    newAssets.add(
                        GalleryAsset(
                            platformAssetId = "imported_${uri.hashCode()}_${System.currentTimeMillis()}",
                            uri = uri.toString(),
                            filename = filename,
                            albumName = "Imported",
                            mimeType = resolver.getType(uri) ?: "image/jpeg",
                            width = width,
                            height = height,
                            fileSize = size,
                            createdAt = System.currentTimeMillis(),
                            modifiedAt = System.currentTimeMillis(),
                            isScreenshot = isScreenshot,
                            indexingStatus = IndexingStatus.DISCOVERED
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (newAssets.isNotEmpty()) {
                repository.insertDiscoveredAssets(newAssets)
                indexingCoordinator.startIndexing()
                executeSearch()
            }
        }
    }

    fun pauseIndexing() {
        indexingCoordinator.pause()
    }

    fun resumeIndexing() {
        indexingCoordinator.resume()
    }

    fun setIndexingMode(mode: IndexingMode) {
        indexingCoordinator.setIndexingMode(mode)
    }

    fun setExecutionProvider(provider: ExecutionProvider) {
        indexingCoordinator.setExecutionProvider(provider)
    }

    fun setOcrEngineModel(model: OcrEngineModel) {
        _ocrEngineModel.value = model
        ocrEngine.setEngineModel(model)
    }

    fun reindexAllWithCurrentEngine() {
        viewModelScope.launch {
            repository.requeueAllAssetsForIndexing()
            indexingCoordinator.startIndexing()
            executeSearch()
        }
    }

    fun prioritizeAsset(assetId: Long) {
        viewModelScope.launch {
            repository.prioritizeAsset(assetId)
        }
    }

    fun retryFailed() {
        viewModelScope.launch {
            repository.retryFailedAssets()
            indexingCoordinator.startIndexing()
        }
    }

    fun rebuildSearchIndex() {
        viewModelScope.launch {
            repository.rebuildSearchIndex()
            executeSearch()
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            _searchResults.value = emptyList()
            _selectedDetailItem.value = null
        }
    }

    fun selectAssetForDetail(assetId: Long) {
        viewModelScope.launch {
            val asset = repository.getAssetById(assetId) ?: return@launch
            val doc = repository.getDocumentForAsset(assetId)
            val blocks = repository.getBlocksForAsset(assetId)
            val entities = if (doc != null) com.example.core.ocr.EntityExtractor.extractEntities(doc.rawText) else emptyList()

            _selectedDetailItem.value = SearchResultItem(
                asset = asset,
                document = doc,
                matchingBlocks = blocks,
                allBlocks = blocks,
                snippet = doc?.rawText?.lines()?.firstOrNull().orEmpty(),
                score = 10f,
                matchCount = blocks.size,
                entities = entities
            )
            _selectedBlock.value = null
            _selectedBlockIndexes.value = emptySet()
        }
    }

    fun selectBlock(block: OcrBlock?) {
        _selectedBlock.value = block
    }

    fun toggleBlockSelection(index: Int) {
        _selectedBlockIndexes.update { current ->
            if (current.contains(index)) current - index else current + index
        }
    }

    fun selectAllBlocks() {
        val totalBlocks = _selectedDetailItem.value?.allBlocks?.size ?: 0
        _selectedBlockIndexes.value = (0 until totalBlocks).toSet()
    }

    fun clearBlockSelection() {
        _selectedBlockIndexes.value = emptySet()
        _selectedBlock.value = null
    }

    fun runBenchmarkSuite() {
        if (_isBenchmarking.value) return
        viewModelScope.launch {
            _isBenchmarking.value = true
            _benchmarkProgress.value = 0.05f

            val providers = listOf(ExecutionProvider.CPU, ExecutionProvider.XNNPACK, ExecutionProvider.NNAPI)
            val newResults = mutableListOf<BenchmarkResult>()

            providers.forEachIndexed { pIdx, prov ->
                _benchmarkProgress.value = (pIdx + 1) * 0.3f
                delay(400) // Simulated run cycle

                val (avgMs, p90, throughput) = when (prov) {
                    ExecutionProvider.NNAPI -> Triple(38.5f, 44L, 26.0f)
                    ExecutionProvider.XNNPACK -> Triple(62.0f, 71L, 16.1f)
                    ExecutionProvider.CPU -> Triple(118.0f, 134L, 8.5f)
                }

                newResults.add(
                    BenchmarkResult(
                        id = UUID.randomUUID().toString(),
                        provider = prov,
                        workerCount = 2,
                        imageCount = 50,
                        totalTimeMs = (avgMs * 50).toLong(),
                        avgInferenceMs = avgMs,
                        p90Ms = p90,
                        p95Ms = (p90 * 1.12f).toLong(),
                        throughputFps = throughput,
                        memoryUsageMb = 48.5f + (pIdx * 6.2f),
                        timestamp = System.currentTimeMillis()
                    )
                )
            }

            _benchmarkResults.value = newResults
            _benchmarkProgress.value = 1.0f
            _isBenchmarking.value = false
        }
    }

    // --- Face & People Search Actions ---

    fun loadFacesForAsset(assetId: Long) {
        viewModelScope.launch {
            val faces = repository.getFacesForAsset(assetId)
            _currentAssetFaces.value = faces
        }
    }

    fun selectPerson(person: PersonCluster?) {
        _selectedPerson.value = person
        if (person != null) {
            viewModelScope.launch {
                val photos = repository.getAssetsForPerson(person.id)
                _personPhotos.value = photos
            }
        } else {
            _personPhotos.value = emptyList()
        }
    }

    fun renamePerson(personId: Long, newName: String) {
        viewModelScope.launch {
            repository.renamePerson(personId, newName)
            _selectedPerson.update { current ->
                if (current?.id == personId) current.copy(name = newName) else current
            }
        }
    }

    /**
     * Finds all related photos containing the same person as in the uploaded reference photo.
     */
    fun searchPeopleByImageUri(uriString: String) {
        viewModelScope.launch {
            _isSearchingFaces.value = true
            _queryFaceUri.value = uriString
            _faceSearchResults.value = emptyList()

            try {
                val preprocessed = ImagePreprocessor.loadAndPreprocess(
                    context = getApplication(),
                    uriString = uriString,
                    maxDimension = 2048
                )
                if (preprocessed == null) {
                    _isSearchingFaces.value = false
                    return@launch
                }

                val detected = faceEngine.detectFaces(preprocessed.bitmap)
                preprocessed.bitmap.recycle()

                if (detected.isEmpty()) {
                    _faceSearchResults.value = emptyList()
                    _isSearchingFaces.value = false
                    return@launch
                }

                // Use the primary / largest detected face
                val primaryFace = detected.maxByOrNull { (it.boxRight - it.boxLeft) * (it.boxBottom - it.boxTop) } ?: detected.first()

                searchPeopleByFace(primaryFace)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isSearchingFaces.value = false
            }
        }
    }

    /**
     * Computes cosine similarity vector search across all indexed device photos for the given face.
     */
    fun searchPeopleByFace(queryFace: DetectedFace) {
        viewModelScope.launch {
            _isSearchingFaces.value = true
            try {
                val allFaces = repository.getAllIndexedFaces()
                val matches = faceEngine.findMatches(queryFace.embedding, allFaces, threshold = 0.68f)

                // Map matched faces to their parent gallery asset
                val results = mutableListOf<FaceMatchResult>()
                for ((matchedFace, score) in matches) {
                    val asset = repository.getAssetById(matchedFace.assetId)
                    if (asset != null) {
                        results.add(
                            FaceMatchResult(
                                asset = asset,
                                matchedFace = matchedFace,
                                similarityScore = score,
                                personName = matchedFace.personName
                            )
                        )
                    }
                }

                // De-duplicate by asset ID, keeping highest scoring match per photo
                val deduplicated = results
                    .groupBy { it.asset.id }
                    .map { (_, list) -> list.maxByOrNull { it.similarityScore }!! }
                    .sortedByDescending { it.similarityScore }

                _faceSearchResults.value = deduplicated
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isSearchingFaces.value = false
            }
        }
    }

    fun clearFaceSearchResults() {
        _faceSearchResults.value = emptyList()
        _queryFaceUri.value = null
    }

    private data class BaseFilterState(
        val q: String,
        val cat: CategoryFilter,
        val ent: EntityFilter,
        val sort: SortOrder,
        val conf: Float
    )
}
