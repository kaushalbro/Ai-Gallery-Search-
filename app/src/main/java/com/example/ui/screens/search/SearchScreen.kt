package com.example.ui.screens.search

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CategoryFilter
import com.example.data.model.EntityFilter
import com.example.data.model.SortOrder
import com.example.ui.components.CategoryPills
import com.example.ui.components.IndexingStatusBar
import com.example.ui.components.SearchResultCard
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianCardBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: MainViewModel,
    onOpenDetail: (Long) -> Unit,
    onNavigateToDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val category by viewModel.categoryFilter.collectAsStateWithLifecycle()
    val entity by viewModel.entityFilter.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val minConfidence by viewModel.minConfidence.collectAsStateWithLifecycle()
    val results by viewModel.searchResults.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val stats by viewModel.indexingStats.collectAsStateWithLifecycle()

    var showFilterSheet by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importSelectedImages(uris)
        }
    }

    Scaffold(
        containerColor = ObsidianBackground,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianBackground)
                    .padding(top = 8.dp)
            ) {
                // Title and Privacy Lock
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "LensVault",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Surface(
                            color = Color(0x3300E5FF),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x6600E5FF))
                        ) {
                            Text(
                                text = "PP-OCRv6",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanAccent,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .background(Color(0x2210B981), RoundedCornerShape(20.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "100% On-Device",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "100% Offline",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Search Bar and Quick Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        placeholder = {
                            Text(
                                "Search text, receipts, wifi, errors...",
                                color = Color(0xFF64748B),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
                                tint = CyanAccent
                            )
                        },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(
                                    onClick = { viewModel.onSearchQueryChanged("") },
                                    modifier = Modifier.testTag("clear_search_button")
                                ) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color(0xFF94A3B8))
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ObsidianCard,
                            unfocusedContainerColor = ObsidianCard,
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = ObsidianCardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = CyanAccent
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("search_input_field")
                    )

                    IconButton(
                        onClick = { viewModel.scanDeviceGallery() },
                        modifier = Modifier
                            .size(52.dp)
                            .background(ObsidianCard, RoundedCornerShape(16.dp))
                            .border(1.dp, ObsidianCardBorder, RoundedCornerShape(16.dp))
                            .testTag("home_scan_gallery_button")
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Scan Device Images",
                            tint = CyanAccent
                        )
                    }

                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .background(ObsidianCard, RoundedCornerShape(16.dp))
                            .border(1.dp, ObsidianCardBorder, RoundedCornerShape(16.dp))
                            .testTag("home_pick_photos_button")
                    ) {
                        Icon(
                            Icons.Default.AddPhotoAlternate,
                            contentDescription = "Pick Photos",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = { showFilterSheet = true },
                        modifier = Modifier
                            .size(52.dp)
                            .background(ObsidianCard, RoundedCornerShape(16.dp))
                            .border(1.dp, ObsidianCardBorder, RoundedCornerShape(16.dp))
                            .testTag("filter_button")
                    ) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = "Filters",
                            tint = if (entity != EntityFilter.ALL || sortOrder != SortOrder.RELEVANCE) CyanAccent else Color(0xFF94A3B8)
                        )
                    }
                }

                // Quick Category Filters
                CategoryPills(
                    selectedCategory = category,
                    onSelectCategory = { viewModel.onCategoryFilterChanged(it) }
                )

                // Indexing Status Banner
                IndexingStatusBar(
                    stats = stats,
                    onTap = onNavigateToDashboard
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ObsidianBackground)
        ) {
            if (isSearching) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CyanAccent)
                }
            } else if (results.isEmpty()) {
                EmptySearchState(
                    query = query,
                    onSampleQuery = { viewModel.onSearchQueryChanged(it) },
                    onScanClick = { viewModel.scanDeviceGallery() },
                    onPickClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize().testTag("search_results_grid")
                ) {
                    items(results, key = { it.asset.id }) { item ->
                        SearchResultCard(
                            item = item,
                            onClick = { onOpenDetail(item.asset.id) }
                        )
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            containerColor = ObsidianSurface,
            sheetState = rememberModalBottomSheetState()
        ) {
            FilterBottomSheetContent(
                selectedEntity = entity,
                selectedSort = sortOrder,
                confidence = minConfidence,
                onSelectEntity = { viewModel.onEntityFilterChanged(it) },
                onSelectSort = { viewModel.onSortOrderChanged(it) },
                onChangeConfidence = { viewModel.onConfidenceThresholdChanged(it) },
                onDismiss = { showFilterSheet = false }
            )
        }
    }
}

@Composable
private fun EmptySearchState(
    query: String,
    onSampleQuery: (String) -> Unit,
    onScanClick: () -> Unit,
    onPickClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            color = Color(0x1500E5FF),
            shape = CircleShape,
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = CyanAccent,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (query.isBlank()) "Instant On-Device Search" else "No matching OCR text found",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (query.isBlank()) {
                "Scan device photos to index all screenshots, receipts, error logs, and Wi-Fi stickers:"
            } else {
                "Try a different word or scan recent gallery photos."
            },
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF94A3B8),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Scan & Pick Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onScanClick,
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).testTag("empty_state_scan_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Scan Gallery", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            }

            OutlinedButton(
                onClick = onPickClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                modifier = Modifier.weight(1f).testTag("empty_state_pick_button")
            ) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Pick Photos", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Sample Query Chips
        val sampleQueries = listOf("14.85", "wifi password", "INV-2026", "500 error", "DL402", "32.50", "pytorch")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    sampleQueries.take(3).forEach { sq ->
                        Surface(
                            onClick = { onSampleQuery(sq) },
                            color = ObsidianCard,
                            shape = RoundedCornerShape(20.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianCardBorder)
                        ) {
                            Text(
                                text = "🔍 $sq",
                                style = MaterialTheme.typography.labelMedium,
                                color = CyanAccent,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    sampleQueries.drop(3).forEach { sq ->
                        Surface(
                            onClick = { onSampleQuery(sq) },
                            color = ObsidianCard,
                            shape = RoundedCornerShape(20.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianCardBorder)
                        ) {
                            Text(
                                text = "🔍 $sq",
                                style = MaterialTheme.typography.labelMedium,
                                color = CyanAccent,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterBottomSheetContent(
    selectedEntity: EntityFilter,
    selectedSort: SortOrder,
    confidence: Float,
    onSelectEntity: (EntityFilter) -> Unit,
    onSelectSort: (SortOrder) -> Unit,
    onChangeConfidence: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(
            text = "Search & OCR Filters",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Entity Type",
            style = MaterialTheme.typography.labelLarge,
            color = CyanAccent,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        EntityFilter.values().forEach { ent ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectEntity(ent) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedEntity == ent,
                    onClick = { onSelectEntity(ent) },
                    colors = RadioButtonDefaults.colors(selectedColor = CyanAccent)
                )
                Text(
                    text = ent.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Sort Order",
            style = MaterialTheme.typography.labelLarge,
            color = CyanAccent,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        SortOrder.values().forEach { sort ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectSort(sort) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedSort == sort,
                    onClick = { onSelectSort(sort) },
                    colors = RadioButtonDefaults.colors(selectedColor = CyanAccent)
                )
                Text(
                    text = sort.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Min OCR Confidence (${(confidence * 100).toInt()}%)",
            style = MaterialTheme.typography.labelLarge,
            color = CyanAccent,
            fontWeight = FontWeight.SemiBold
        )
        Slider(
            value = confidence,
            onValueChange = onChangeConfidence,
            valueRange = 0.2f..0.95f,
            colors = SliderDefaults.colors(
                thumbColor = CyanAccent,
                activeTrackColor = CyanAccent,
                inactiveTrackColor = ObsidianCardBorder
            )
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}
