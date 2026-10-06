package com.example.core.search

import com.example.core.ocr.EntityExtractor
import com.example.data.local.AppDatabase
import com.example.data.model.CategoryFilter
import com.example.data.model.EntityFilter
import com.example.data.model.ExtractedEntity
import com.example.data.model.GalleryAsset
import com.example.data.model.OcrBlock
import com.example.data.model.OcrDocument
import com.example.data.model.SearchResultItem
import com.example.data.model.SortOrder
import com.example.data.repository.GalleryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.max

data class SearchQueryFilters(
    val query: String = "",
    val category: CategoryFilter = CategoryFilter.ALL,
    val entity: EntityFilter = EntityFilter.ALL,
    val sortOrder: SortOrder = SortOrder.RELEVANCE,
    val minConfidence: Float = 0.15f,
    val maxAgeDays: Int? = null // null = all time, 1 = today, 7 = week, 30 = month, 365 = year
)

class SearchEngine(
    private val repository: GalleryRepository,
    private val db: AppDatabase
) {

    suspend fun search(
        filters: SearchQueryFilters,
        limit: Int = 100,
        offset: Int = 0
    ): List<SearchResultItem> = withContext(Dispatchers.IO) {
        val trimmedQuery = filters.query.trim().lowercase(Locale.ROOT)
        val ftsQuery = sanitizeFtsQuery(trimmedQuery)

        // 1. Fetch matching assets from DB
        val matchedEntities = if (trimmedQuery.isBlank()) {
            if (filters.category == CategoryFilter.SCREENSHOTS) {
                db.searchDao().searchScreenshotAssetsByText("", limit, offset)
            } else {
                db.searchDao().searchAssetsByText("", limit, offset)
            }
        } else {
            // Check FTS match safely
            val ftsAssetIds = if (ftsQuery.isNotBlank()) {
                runCatching {
                    db.searchDao().searchFtsAssetIds(ftsQuery)
                }.getOrDefault(emptyList())
            } else {
                emptyList()
            }

            val rawMatches = if (filters.category == CategoryFilter.SCREENSHOTS) {
                db.searchDao().searchScreenshotAssetsByText(trimmedQuery, limit, offset)
            } else {
                db.searchDao().searchAssetsByText(trimmedQuery, limit, offset)
            }

            // Merge FTS and LIKE matches uniquely
            val allIds = (ftsAssetIds + rawMatches.map { it.id }).distinct()
            allIds.mapNotNull { id -> db.assetDao().getAssetById(id) }
        }

        val resultItems = mutableListOf<SearchResultItem>()
        val terms = trimmedQuery.split(Regex("\\s+")).filter { it.isNotBlank() }

        val now = System.currentTimeMillis()
        val maxAgeMs = filters.maxAgeDays?.let { it.toLong() * 86400000L }

        for (assetEntity in matchedEntities) {
            val asset = repository.getAssetById(assetEntity.id) ?: continue

            // Filter: Date
            if (maxAgeMs != null && (now - asset.createdAt) > maxAgeMs) {
                continue
            }

            val doc = repository.getDocumentForAsset(asset.id)
            val blocks = repository.getBlocksForAsset(asset.id)
            val fullText = ((doc?.normalizedText ?: "") + " " + asset.filename + " " + asset.albumName).lowercase()

            // Filter: Category
            when (filters.category) {
                CategoryFilter.SCREENSHOTS -> if (!asset.isScreenshot) continue
                CategoryFilter.CAMERA -> if (asset.isScreenshot) continue
                CategoryFilter.RECEIPTS -> {
                    val isReceipt = fullText.contains("total") || fullText.contains("tax") ||
                            fullText.contains("invoice") || fullText.contains("receipt") ||
                            fullText.contains("amount") || fullText.contains("subtotal") ||
                            fullText.contains("$") || fullText.contains("₹") || fullText.contains("rs") ||
                            fullText.contains("bill")
                    if (!isReceipt) continue
                }
                CategoryFilter.CODE -> {
                    val isCodeOrError = fullText.contains("error") || fullText.contains("exception") ||
                            fullText.contains("nullpointer") || fullText.contains("trace") ||
                            fullText.contains("http") || fullText.contains("status") ||
                            fullText.contains("code") || fullText.contains("import") ||
                            fullText.contains("fatal")
                    if (!isCodeOrError) continue
                }
                CategoryFilter.CREDENTIALS -> {
                    val isCred = fullText.contains("wifi") || fullText.contains("password") ||
                            fullText.contains("wpa") || fullText.contains("ssid") ||
                            fullText.contains("passcode") || fullText.contains("pin")
                    if (!isCred) continue
                }
                CategoryFilter.DOCUMENTS -> {
                    val isDoc = asset.mimeType.contains("pdf") || asset.filename.contains("doc", true) ||
                            (doc != null && doc.wordCount > 12)
                    if (!isDoc) continue
                }
                else -> Unit
            }

            // Extract all entities in this document
            val allEntities = if (doc != null) EntityExtractor.extractEntities(doc.rawText) else emptyList()

            // Filter: Entity
            if (filters.entity != EntityFilter.ALL) {
                val hasMatchingEntity = allEntities.any { it.type == filters.entity }
                if (!hasMatchingEntity) continue
            }

            // Filter: Matching Blocks & Confidence
            val matchingBlocks = mutableListOf<OcrBlock>()
            var matchCount = 0

            for (block in blocks) {
                val blockLower = block.normalizedText
                val matchesTerm = terms.isEmpty() || terms.any { term -> blockLower.contains(term) }
                if (matchesTerm) {
                    matchingBlocks.add(block)
                    matchCount++
                }
            }

            // If a specific text query was typed and no blocks matched, check if document text contains it
            val docContainsTerm = doc != null && terms.any { term -> doc.normalizedText.contains(term) }
            if (terms.isNotEmpty() && matchCount == 0 && !docContainsTerm && !asset.filename.contains(trimmedQuery, true)) {
                continue
            }

            // Build best snippet
            val snippet = generateSnippet(doc?.rawText ?: "", matchingBlocks, trimmedQuery)

            // Calculate Ranking Score
            val score = calculateScore(asset, doc, matchingBlocks, terms, trimmedQuery)

            resultItems.add(
                SearchResultItem(
                    asset = asset,
                    document = doc,
                    matchingBlocks = matchingBlocks,
                    allBlocks = blocks,
                    snippet = snippet,
                    score = score,
                    matchCount = max(matchCount, 1),
                    entities = allEntities
                )
            )
        }

        // Sort results
        val sorted = when (filters.sortOrder) {
            SortOrder.RELEVANCE -> resultItems.sortedByDescending { it.score }
            SortOrder.NEWEST -> resultItems.sortedByDescending { it.asset.createdAt }
            SortOrder.OLDEST -> resultItems.sortedBy { it.asset.createdAt }
            SortOrder.CONFIDENCE -> resultItems.sortedByDescending { item ->
                item.matchingBlocks.map { it.confidence }.average().toFloat().takeIf { !it.isNaN() } ?: 0f
            }
        }

        sorted
    }

    private fun generateSnippet(
        rawText: String,
        matchingBlocks: List<OcrBlock>,
        query: String
    ): String {
        if (matchingBlocks.isNotEmpty()) {
            val bestMatches = matchingBlocks.take(3).joinToString(" • ") { it.text }
            if (bestMatches.isNotBlank()) return bestMatches
        }

        if (rawText.isBlank()) return "Indexed Asset (Ready to inspect)"

        val lines = rawText.lines().filter { it.isNotBlank() }
        val matchingLine = lines.firstOrNull { it.contains(query, ignoreCase = true) }
        return matchingLine ?: lines.take(2).joinToString(" • ")
    }

    private fun calculateScore(
        asset: GalleryAsset,
        doc: OcrDocument?,
        matchingBlocks: List<OcrBlock>,
        terms: List<String>,
        fullQuery: String
    ): Float {
        var score = 10.0f

        if (terms.isEmpty()) {
            // Prioritize recent screenshots and high quality scans
            if (asset.isScreenshot) score += 5f
            val recencyDays = (System.currentTimeMillis() - asset.createdAt) / 86400000f
            score += (100f / (recencyDays + 1f)).coerceAtMost(20f)
            return score
        }

        val docText = doc?.normalizedText.orEmpty()
        val filename = asset.filename.lowercase(Locale.ROOT)

        // Exact phrase match bonus
        if (fullQuery.isNotBlank() && docText.contains(fullQuery)) {
            score += 25.0f
        }

        // Term coverage score
        var termsMatched = 0
        for (term in terms) {
            if (docText.contains(term) || filename.contains(term)) {
                termsMatched++
            }
        }
        score += (termsMatched.toFloat() / terms.size.toFloat()) * 30.0f

        // Filename match bonus
        if (filename.contains(fullQuery)) {
            score += 15.0f
        }

        // Screenshot priority bonus
        if (asset.isScreenshot) {
            score += 6.0f
        }

        // OCR Confidence bonus
        val avgConfidence = matchingBlocks.map { it.confidence }.average().toFloat().takeIf { !it.isNaN() } ?: 0.5f
        score += avgConfidence * 10.0f

        // Recency bonus
        val ageDays = (System.currentTimeMillis() - asset.createdAt) / 86400000f
        score += (30f / (ageDays + 1f)).coerceAtMost(10f)

        return score
    }

    private fun sanitizeFtsQuery(raw: String): String {
        val clean = raw.replace(Regex("""[^\p{L}\p{N}\s]"""), " ").trim()
        val tokens = clean.split(Regex("""\s+""")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return ""
        return tokens.joinToString(" ") { "\"$it\"*" }
    }
}
