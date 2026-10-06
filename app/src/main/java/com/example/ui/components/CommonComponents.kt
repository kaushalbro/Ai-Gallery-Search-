package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.CategoryFilter
import com.example.data.model.EntityFilter
import com.example.data.model.ExtractedEntity
import com.example.data.model.GalleryAsset
import com.example.data.model.IndexingStats
import com.example.data.model.OcrBlock
import com.example.data.model.SearchResultItem
import com.example.ui.theme.BoxHighlightCyan
import com.example.ui.theme.BoxHighlightCyanFill
import com.example.ui.theme.BoxHighlightYellow
import com.example.ui.theme.BoxHighlightYellowFill
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianCardBorder
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.SuccessEmerald

@Composable
fun CategoryPills(
    selectedCategory: CategoryFilter,
    onSelectCategory: (CategoryFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CategoryFilter.values().forEach { category ->
            val isSelected = selectedCategory == category
            FilterChip(
                selected = isSelected,
                onClick = { onSelectCategory(category) },
                label = {
                    Text(
                        text = category.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CyanAccent,
                    selectedLabelColor = Color.Black,
                    containerColor = ObsidianCard,
                    labelColor = Color.White
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = if (isSelected) CyanAccent else ObsidianCardBorder,
                    selectedBorderColor = CyanAccent,
                    borderWidth = 1.dp
                ),
                modifier = Modifier.testTag("filter_chip_${category.name.lowercase()}")
            )
        }
    }
}

@Composable
fun IndexingStatusBar(
    stats: IndexingStats,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onTap,
        color = ObsidianCard,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianCardBorder),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .testTag("indexing_status_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(if (stats.isPaused) Color(0xFFF59E0B) else SuccessEmerald, CircleShape)
                )
                Text(
                    text = if (stats.totalAssets > 0) {
                        "${stats.indexedCount} / ${stats.totalAssets} Indexed (${stats.percentIndexed.toInt()}%)"
                    } else {
                        "Indexing Ready • Tap to Scan"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFE2E8F0),
                    fontWeight = FontWeight.Medium
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (stats.currentSpeedFps > 0f) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        Icon(
                            Icons.Default.Bolt,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${stats.currentSpeedFps} img/s",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = stats.ocrProvider.chipLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = IndigoAccent,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(Color(0x22818CF8), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun SearchResultCard(
    item: SearchResultItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianCardBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("search_result_card_${item.asset.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(Color(0xFF0F172A))
            ) {
                AsyncImage(
                    model = item.asset.uri,
                    contentDescription = item.asset.filename,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Top badges
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.asset.isScreenshot) {
                        Surface(
                            color = Color(0xCC0284C7),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Screenshot",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Surface(
                            color = Color(0xCC1E293B),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = item.asset.albumName,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFCBD5E1),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (item.matchCount > 0) {
                        Surface(
                            color = Color(0xDD00E5FF),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "${item.matchCount} text matches",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = item.asset.filename,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.snippet,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (item.entities.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item.entities.take(2).forEach { entity ->
                            Surface(
                                color = Color(0x2238BDF8),
                                shape = RoundedCornerShape(4.dp),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0x4438BDF8))
                            ) {
                                Text(
                                    text = entity.value,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyanAccent,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Interactive canvas overlay that paints OCR bounding boxes over the image.
 * Responds to tap gestures to select individual text boxes.
 */
@Composable
fun BoundingBoxOverlay(
    blocks: List<OcrBlock>,
    matchingBlocks: List<OcrBlock>,
    selectedBlock: OcrBlock?,
    selectedIndexes: Set<Int>,
    onTapBlock: (OcrBlock?) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(blocks) {
                detectTapGestures { offset ->
                    if (size.width <= 0 || size.height <= 0) return@detectTapGestures
                    val normX = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    val normY = (offset.y / size.height.toFloat()).coerceIn(0f, 1f)

                    val tapped = blocks.firstOrNull { b ->
                        val minX = minOf(b.x1, b.x2, b.x3, b.x4)
                        val maxX = maxOf(b.x1, b.x2, b.x3, b.x4)
                        val minY = minOf(b.y1, b.y2, b.y3, b.y4)
                        val maxY = maxOf(b.y1, b.y2, b.y3, b.y4)
                        normX in minX..maxX && normY in minY..maxY
                    }
                    onTapBlock(tapped)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height

            blocks.forEachIndexed { idx, block ->
                val isMatching = matchingBlocks.any { it.blockIndex == block.blockIndex }
                val isSelected = selectedBlock?.blockIndex == block.blockIndex || selectedIndexes.contains(idx)

                val path = Path().apply {
                    moveTo(block.x1 * canvasW, block.y1 * canvasH)
                    lineTo(block.x2 * canvasW, block.y2 * canvasH)
                    lineTo(block.x3 * canvasW, block.y3 * canvasH)
                    lineTo(block.x4 * canvasW, block.y4 * canvasH)
                    close()
                }

                val fillColor = when {
                    isSelected -> BoxHighlightYellowFill
                    isMatching -> BoxHighlightCyanFill
                    else -> Color(0x1538BDF8)
                }

                val strokeColor = when {
                    isSelected -> BoxHighlightYellow
                    isMatching -> BoxHighlightCyan
                    else -> Color(0x4038BDF8)
                }

                val strokeWidth = when {
                    isSelected -> 3.dp.toPx()
                    isMatching -> 2.5.dp.toPx()
                    else -> 1.dp.toPx()
                }

                drawPath(path, fillColor)
                drawPath(path, strokeColor, style = Stroke(width = strokeWidth))
            }
        }
    }
}
