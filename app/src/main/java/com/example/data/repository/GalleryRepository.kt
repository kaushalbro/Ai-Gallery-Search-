package com.example.data.repository

import com.example.core.face.FaceEngine
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AssetEntity
import com.example.data.local.entity.FaceEntity
import com.example.data.local.entity.IndexingJobEntity
import com.example.data.local.entity.OcrBlockEntity
import com.example.data.local.entity.OcrDocumentEntity
import com.example.data.local.entity.OcrSearchFtsEntity
import com.example.data.local.entity.PersonEntity
import com.example.data.model.CategoryFilter
import com.example.data.model.DetectedFace
import com.example.data.model.EntityFilter
import com.example.data.model.ExecutionProvider
import com.example.data.model.ExtractedEntity
import com.example.data.model.GalleryAsset
import com.example.data.model.IndexingStats
import com.example.data.model.IndexingStatus
import com.example.data.model.OcrBlock
import com.example.data.model.OcrDocument
import com.example.data.model.PersonCluster
import com.example.data.model.SearchResultItem
import com.example.data.model.SortOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class GalleryRepository(private val db: AppDatabase) {
    private val assetDao = db.assetDao()
    private val ocrDao = db.ocrDao()
    private val jobDao = db.jobDao()
    private val searchDao = db.searchDao()
    private val faceDao = db.faceDao()
    private val personDao = db.personDao()

    val allPersonsFlow: Flow<List<PersonCluster>> = personDao.getAllPersonsFlow().map { list ->
        list.map { it.toDomain() }
    }

    val allAssetsFlow: Flow<List<GalleryAsset>> = assetDao.getAllAssetsFlow().map { list ->
        list.map { it.toDomain() }
    }

    val screenshotsFlow: Flow<List<GalleryAsset>> = assetDao.getScreenshotsFlow().map { list ->
        list.map { it.toDomain() }
    }

    val statsFlow: Flow<IndexingStats> = combine(
        assetDao.getTotalAssetCountFlow(),
        assetDao.getIndexedCountFlow(),
        assetDao.getQueuedCountFlow(),
        assetDao.getDuplicateCountFlow(),
        assetDao.getFailedCountFlow()
    ) { total, indexed, queued, duplicates, failed ->
        IndexingStats(
            totalAssets = total,
            indexedCount = indexed,
            queuedCount = queued,
            skippedDuplicates = duplicates,
            failedCount = failed
        )
    }

    suspend fun getAssetById(id: Long): GalleryAsset? = withContext(Dispatchers.IO) {
        assetDao.getAssetById(id)?.toDomain()
    }

    suspend fun getDocumentForAsset(assetId: Long): OcrDocument? = withContext(Dispatchers.IO) {
        ocrDao.getDocumentForAsset(assetId)?.toDomain()
    }

    suspend fun getBlocksForAsset(assetId: Long): List<OcrBlock> = withContext(Dispatchers.IO) {
        ocrDao.getBlocksForAsset(assetId).map { it.toDomain() }
    }

    suspend fun insertDiscoveredAssets(assets: List<GalleryAsset>): Int = withContext(Dispatchers.IO) {
        val entities = assets.map { it.toEntity() }
        val ids = assetDao.insertAssets(entities)
        // Also queue indexing jobs for new assets
        val jobs = assets.zip(ids).filter { it.second > 0 }.map { (asset, id) ->
            val priority = when {
                asset.isScreenshot -> 80
                System.currentTimeMillis() - asset.createdAt < 7 * 86400000L -> 60
                else -> 20
            }
            IndexingJobEntity(
                assetId = id,
                priority = priority,
                state = "PENDING",
                createdAt = System.currentTimeMillis()
            )
        }
        if (jobs.isNotEmpty()) {
            jobDao.insertJobs(jobs)
        }
        ids.count { it > 0 }
    }

    suspend fun enqueueUnindexedAssets() = withContext(Dispatchers.IO) {
        val unindexed = assetDao.getUnindexedAssets(1000)
        val jobs = unindexed.map { asset ->
            val priority = if (asset.isScreenshot) 80 else 50
            IndexingJobEntity(
                assetId = asset.id,
                priority = priority,
                state = "PENDING",
                createdAt = System.currentTimeMillis()
            )
        }
        if (jobs.isNotEmpty()) {
            jobDao.insertJobs(jobs)
        }
    }

    suspend fun findCanonicalByHash(exactHash: String): GalleryAsset? = withContext(Dispatchers.IO) {
        assetDao.findCanonicalByExactHash(exactHash)?.toDomain()
    }

    suspend fun getAssetByPlatformId(platformAssetId: String): GalleryAsset? = withContext(Dispatchers.IO) {
        assetDao.getAssetByPlatformId(platformAssetId)?.toDomain()
    }

    suspend fun getTotalAssetCount(): Int = withContext(Dispatchers.IO) {
        assetDao.getTotalAssetCount()
    }

    suspend fun saveOcrResult(
        assetId: Long,
        document: OcrDocument,
        blocks: List<OcrBlock>,
        exactHash: String?,
        pHash: String?
    ) = withContext(Dispatchers.IO) {
        val docId = ocrDao.insertDocument(document.toEntity(assetId))
        val blockEntities = blocks.mapIndexed { idx, b ->
            b.copy(documentId = docId, assetId = assetId, blockIndex = idx).toEntity()
        }
        // Insert in safe batches of 50 to prevent SQLite variable limits on large documents
        blockEntities.chunked(50).forEach { chunk ->
            ocrDao.insertBlocks(chunk)
        }

        // Save FTS record
        searchDao.insertSearchDoc(
            OcrSearchFtsEntity(
                rowid = docId,
                assetId = assetId,
                text = document.normalizedText,
                filename = "",
                albumName = ""
            )
        )

        // Mark asset indexed
        assetDao.markAssetIndexed(
            id = assetId,
            status = IndexingStatus.INDEXED.name,
            indexedAt = System.currentTimeMillis(),
            canonicalDocId = docId,
            exactHash = exactHash,
            pHash = pHash
        )
    }

    suspend fun markAssetDuplicate(assetId: Long, canonicalDocId: Long, exactHash: String, pHash: String?) =
        withContext(Dispatchers.IO) {
            assetDao.markAssetIndexed(
                id = assetId,
                status = IndexingStatus.SKIPPED_DUPLICATE.name,
                indexedAt = System.currentTimeMillis(),
                canonicalDocId = canonicalDocId,
                exactHash = exactHash,
                pHash = pHash
            )
        }

    suspend fun updateAssetStatus(assetId: Long, status: IndexingStatus, error: String? = null) =
        withContext(Dispatchers.IO) {
            assetDao.updateAssetStatus(assetId, status.name, error)
        }

    suspend fun resetStalledAssets() = withContext(Dispatchers.IO) {
        assetDao.resetStalledAssets()
    }

    suspend fun incrementRetryCount(assetId: Long) = withContext(Dispatchers.IO) {
        assetDao.incrementRetryCount(assetId)
    }

    suspend fun prioritizeAsset(assetId: Long) = withContext(Dispatchers.IO) {
        jobDao.updateAssetPriority(assetId, 100)
    }

    suspend fun retryFailedAssets() = withContext(Dispatchers.IO) {
        assetDao.retryAllFailedAssets()
    }

    suspend fun requeueAllAssetsForIndexing() = withContext(Dispatchers.IO) {
        jobDao.deleteAllJobs()
        ocrDao.deleteAllDocuments()
        searchDao.clearFtsIndex()
        assetDao.requeueAllAssets()
        enqueueUnindexedAssets()
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        assetDao.deleteAllAssets()
        ocrDao.deleteAllDocuments()
        jobDao.deleteAllJobs()
        searchDao.clearFtsIndex()
    }

    suspend fun deleteSeedAssets() = withContext(Dispatchers.IO) {
        assetDao.deleteSeedAssets()
    }

    suspend fun rebuildSearchIndex() = withContext(Dispatchers.IO) {
        searchDao.clearFtsIndex()
        val allAssets = assetDao.getAssetsPaged(10000, 0)
        for (asset in allAssets) {
            val doc = ocrDao.getDocumentForAsset(asset.id)
            if (doc != null) {
                searchDao.insertSearchDoc(
                    OcrSearchFtsEntity(
                        rowid = doc.id,
                        assetId = asset.id,
                        text = doc.normalizedText,
                        filename = asset.filename,
                        albumName = asset.albumName
                    )
                )
            }
        }
    }

    suspend fun getPendingJobs(limit: Int): List<IndexingJobEntity> = withContext(Dispatchers.IO) {
        jobDao.getNextPendingJobs(limit)
    }

    suspend fun markJobStarted(jobId: Long) = withContext(Dispatchers.IO) {
        jobDao.markJobStarted(jobId, "RUNNING", System.currentTimeMillis())
    }

    suspend fun markJobCompleted(jobId: Long, error: String? = null) = withContext(Dispatchers.IO) {
        val state = if (error == null) "COMPLETED" else "FAILED"
        jobDao.markJobCompleted(jobId, state, System.currentTimeMillis(), error)
    }

    // Mapping helper extensions
    private fun AssetEntity.toDomain() = GalleryAsset(
        id = id,
        platformAssetId = platformAssetId,
        uri = uri,
        filename = filename,
        albumName = albumName,
        mimeType = mimeType,
        width = width,
        height = height,
        fileSize = fileSize,
        createdAt = createdAt,
        modifiedAt = modifiedAt,
        isScreenshot = isScreenshot,
        exactHash = exactHash,
        perceptualHash = perceptualHash,
        canonicalDocumentId = canonicalDocumentId,
        indexingStatus = runCatching { IndexingStatus.valueOf(indexingStatus) }.getOrDefault(IndexingStatus.DISCOVERED),
        lastError = lastError,
        retryCount = retryCount,
        discoveredAt = discoveredAt,
        indexedAt = indexedAt
    )

    private fun GalleryAsset.toEntity() = AssetEntity(
        id = id,
        platformAssetId = platformAssetId,
        uri = uri,
        filename = filename,
        albumName = albumName,
        mimeType = mimeType,
        width = width,
        height = height,
        fileSize = fileSize,
        createdAt = createdAt,
        modifiedAt = modifiedAt,
        isScreenshot = isScreenshot,
        exactHash = exactHash,
        perceptualHash = perceptualHash,
        canonicalDocumentId = canonicalDocumentId,
        indexingStatus = indexingStatus.name,
        lastError = lastError,
        retryCount = retryCount,
        discoveredAt = discoveredAt,
        indexedAt = indexedAt
    )

    private fun OcrDocumentEntity.toDomain() = OcrDocument(
        id = id,
        assetId = assetId,
        normalizedText = normalizedText,
        rawText = rawText,
        modelName = modelName,
        modelVersion = modelVersion,
        runtimeProvider = runCatching { ExecutionProvider.valueOf(runtimeProvider) }.getOrDefault(ExecutionProvider.CPU),
        inferenceMs = inferenceMs,
        blockCount = blockCount,
        wordCount = wordCount,
        createdAt = createdAt
    )

    private fun OcrDocument.toEntity(overrideAssetId: Long? = null) = OcrDocumentEntity(
        id = id,
        assetId = overrideAssetId ?: assetId,
        normalizedText = normalizedText,
        rawText = rawText,
        modelName = modelName,
        modelVersion = modelVersion,
        runtimeProvider = runtimeProvider.name,
        inferenceMs = inferenceMs,
        blockCount = blockCount,
        wordCount = wordCount,
        createdAt = createdAt
    )

    private fun OcrBlockEntity.toDomain() = OcrBlock(
        id = id,
        documentId = documentId,
        assetId = assetId,
        blockIndex = blockIndex,
        text = text,
        normalizedText = normalizedText,
        confidence = confidence,
        x1 = x1,
        y1 = y1,
        x2 = x2,
        y2 = y2,
        x3 = x3,
        y3 = y3,
        x4 = x4,
        y4 = y4
    )

    private fun OcrBlock.toEntity() = OcrBlockEntity(
        id = id,
        documentId = documentId,
        assetId = assetId,
        blockIndex = blockIndex,
        text = text,
        normalizedText = normalizedText,
        confidence = confidence,
        x1 = x1,
        y1 = y1,
        x2 = x2,
        y2 = y2,
        x3 = x3,
        y3 = y3,
        x4 = x4,
        y4 = y4
    )

    suspend fun getFacesForAsset(assetId: Long): List<DetectedFace> = withContext(Dispatchers.IO) {
        val faceEntities = faceDao.getFacesForAsset(assetId)
        val personsMap = personDao.getAllPersons().associateBy { it.id }
        faceEntities.map { entity ->
            entity.toDomain(personsMap[entity.personId]?.name)
        }
    }

    suspend fun saveFacesForAsset(
        assetId: Long,
        faces: List<DetectedFace>,
        assetUri: String
    ): List<Long> = withContext(Dispatchers.IO) {
        faceDao.deleteFacesForAsset(assetId)
        if (faces.isEmpty()) return@withContext emptyList()

        val entities = faces.map { face ->
            FaceEntity(
                id = 0,
                assetId = assetId,
                personId = face.personId,
                boxLeft = face.boxLeft,
                boxTop = face.boxTop,
                boxRight = face.boxRight,
                boxBottom = face.boxBottom,
                confidence = face.confidence,
                yaw = face.yaw,
                pitch = face.pitch,
                roll = face.roll,
                embeddingJson = FaceEngine.embeddingToJson(face.embedding),
                createdAt = System.currentTimeMillis()
            )
        }
        val insertedIds = faceDao.insertFaces(entities)

        // Cluster and match against existing known persons
        autoAssignPersons(assetId, assetUri)

        insertedIds
    }

    suspend fun getAllIndexedFaces(): List<DetectedFace> = withContext(Dispatchers.IO) {
        val faces = faceDao.getAllFaces()
        val personsMap = personDao.getAllPersons().associateBy { it.id }
        faces.map { it.toDomain(personsMap[it.personId]?.name) }
    }

    suspend fun getAssetsForPerson(personId: Long): List<GalleryAsset> = withContext(Dispatchers.IO) {
        val faces = faceDao.getFacesForPerson(personId)
        val assetIds = faces.map { it.assetId }.distinct()
        val assets = mutableListOf<GalleryAsset>()
        for (aId in assetIds) {
            assetDao.getAssetById(aId)?.let { assets.add(it.toDomain()) }
        }
        assets
    }

    suspend fun renamePerson(personId: Long, newName: String) = withContext(Dispatchers.IO) {
        personDao.renamePerson(personId, newName.trim())
    }

    suspend fun getPersonById(personId: Long): PersonCluster? = withContext(Dispatchers.IO) {
        personDao.getPersonById(personId)?.toDomain()
    }

    /**
     * Automatic face clustering: Groups unclustered faces and creates or attaches to Person records.
     */
    suspend fun autoAssignPersons(assetId: Long, assetUri: String) = withContext(Dispatchers.IO) {
        val allFaces = faceDao.getAllFaces()
        if (allFaces.isEmpty()) return@withContext

        val domainFaces = allFaces.map { it.toDomain(null) }
        val existingPersons = personDao.getAllPersons().toMutableList()

        for (face in domainFaces) {
            if (face.personId != null) continue

            var matchedPerson: PersonEntity? = null
            var bestSimilarity = 0.72f // Match threshold

            for (person in existingPersons) {
                val personFaces = faceDao.getFacesForPerson(person.id).map { it.toDomain(null) }
                for (pFace in personFaces.take(5)) {
                    val sim = computeFastSim(face.embedding, pFace.embedding)
                    if (sim > bestSimilarity) {
                        bestSimilarity = sim
                        matchedPerson = person
                    }
                }
            }

            if (matchedPerson != null) {
                faceDao.updateFacePersonId(face.id, matchedPerson.id)
                val newCount = faceDao.getDistinctAssetCountForPerson(matchedPerson.id)
                personDao.updateFaceCount(matchedPerson.id, newCount, System.currentTimeMillis())
            } else {
                // Create a new person cluster
                val newPersonIndex = existingPersons.size + 1
                val newPerson = PersonEntity(
                    id = 0,
                    name = "Person #$newPersonIndex",
                    coverFaceId = face.id,
                    coverAssetUri = assetUri,
                    coverBoxLeft = face.boxLeft,
                    coverBoxTop = face.boxTop,
                    coverBoxRight = face.boxRight,
                    coverBoxBottom = face.boxBottom,
                    faceCount = 1,
                    createdAt = System.currentTimeMillis(),
                    lastSeenAt = System.currentTimeMillis()
                )
                val pId = personDao.insertPerson(newPerson)
                if (pId > 0) {
                    faceDao.updateFacePersonId(face.id, pId)
                    existingPersons.add(newPerson.copy(id = pId))
                }
            }
        }
    }

    private fun computeFastSim(vec1: FloatArray, vec2: FloatArray): Float {
        if (vec1.size != 128 || vec2.size != 128) return 0f
        var dot = 0f
        for (i in 0 until 128) {
            dot += vec1[i] * vec2[i]
        }
        return dot.coerceIn(0f, 1f)
    }

    private fun FaceEntity.toDomain(personName: String?) = DetectedFace(
        id = id,
        assetId = assetId,
        personId = personId,
        boxLeft = boxLeft,
        boxTop = boxTop,
        boxRight = boxRight,
        boxBottom = boxBottom,
        confidence = confidence,
        yaw = yaw,
        pitch = pitch,
        roll = roll,
        embedding = FaceEngine.jsonToEmbedding(embeddingJson),
        personName = personName
    )

    private fun PersonEntity.toDomain() = PersonCluster(
        id = id,
        name = name,
        coverFaceId = coverFaceId,
        coverAssetUri = coverAssetUri,
        coverBoxLeft = coverBoxLeft,
        coverBoxTop = coverBoxTop,
        coverBoxRight = coverBoxRight,
        coverBoxBottom = coverBoxBottom,
        faceCount = faceCount,
        photosCount = faceCount,
        lastSeenAt = lastSeenAt
    )
}
