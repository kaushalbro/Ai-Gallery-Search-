package com.example.ui.screens.detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.OcrBlock
import com.example.ui.components.BoundingBoxOverlay
import com.example.ui.screens.people.FaceSearchResultsSheet
import com.example.ui.theme.BoxHighlightYellow
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianCardBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.SuccessEmerald
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ImageDetailScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val item by viewModel.selectedDetailItem.collectAsStateWithLifecycle()
    val selectedBlock by viewModel.selectedBlock.collectAsStateWithLifecycle()
    val selectedIndexes by viewModel.selectedBlockIndexes.collectAsStateWithLifecycle()
    val currentFaces by viewModel.currentAssetFaces.collectAsStateWithLifecycle()
    val faceSearchResults by viewModel.faceSearchResults.collectAsStateWithLifecycle()
    val isSearchingFaces by viewModel.isSearchingFaces.collectAsStateWithLifecycle()
    val queryFaceUri by viewModel.queryFaceUri.collectAsStateWithLifecycle()

    var showBoxes by remember { mutableStateOf(true) }
    var showMetadata by remember { mutableStateOf(false) }
    var showFaceSearchSheet by remember { mutableStateOf(false) }

    LaunchedEffect(item?.asset?.id) {
        item?.asset?.id?.let { viewModel.loadFacesForAsset(it) }
    }

    if (item == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianBackground),
            contentAlignment = Alignment.Center
        ) {
            Text("Asset Not Found", color = Color.White)
        }
        return
    }

    val currentItem = item!!
    val scrollState = rememberScrollState()

    fun copyToClipboard(text: String, label: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied $label to clipboard", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        containerColor = ObsidianBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = currentItem.asset.filename,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("detail_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, currentItem.document?.rawText.orEmpty())
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Extracted Text"))
                        },
                        modifier = Modifier.testTag("share_text_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = CyanAccent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ObsidianBackground)
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .background(ObsidianBackground)
        ) {
            // Main Image with Bounding Box Overlay Canvas
            val safeRatio = remember(currentItem.asset.width, currentItem.asset.height) {
                if (currentItem.asset.height > 0 && currentItem.asset.width > 0) {
                    (currentItem.asset.width.toFloat() / currentItem.asset.height.toFloat()).coerceIn(0.5f, 2.0f)
                } else {
                    1.0f
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(safeRatio)
                    .background(Color(0xFF0F172A))
                    .testTag("detail_image_container")
            ) {
                AsyncImage(
                    model = currentItem.asset.uri,
                    contentDescription = currentItem.asset.filename,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )

                if (showBoxes) {
                    BoundingBoxOverlay(
                        blocks = currentItem.allBlocks,
                        matchingBlocks = currentItem.matchingBlocks,
                        selectedBlock = selectedBlock,
                        selectedIndexes = selectedIndexes,
                        onTapBlock = { viewModel.selectBlock(it) },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Overlay Controls Overlay
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        onClick = { showBoxes = !showBoxes },
                        color = Color.Black.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianCardBorder)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                if (showBoxes) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = if (showBoxes) CyanAccent else Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (showBoxes) "OCR Boxes On" else "Boxes Off",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {

                // Selected Box Inspection Card
                if (selectedBlock != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, BoxHighlightYellow),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("selected_box_card")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(modifier = Modifier.size(10.dp).background(BoxHighlightYellow, CircleShape))
                                    Text(
                                        text = "Selected Text Block #${selectedBlock!!.blockIndex + 1}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = BoxHighlightYellow,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "Confidence: ${(selectedBlock!!.confidence * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SuccessEmerald,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = selectedBlock!!.text,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { copyToClipboard(selectedBlock!!.text, "Selected Block") },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Copy Text", fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { viewModel.selectBlock(null) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8))
                                ) {
                                    Text("Deselect")
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Extracted Entities (URLs, Phone, Wi-Fi, Invoice)
                if (currentItem.entities.isNotEmpty()) {
                    Text(
                        text = "Detected Smart Entities",
                        style = MaterialTheme.typography.labelLarge,
                        color = CyanAccent,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        currentItem.entities.forEach { entity ->
                            Surface(
                                onClick = { copyToClipboard(entity.value, entity.type.label) },
                                color = ObsidianCard,
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x6600E5FF))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "${entity.type.name}:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = IndigoAccent,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = entity.value,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = "Copy",
                                        tint = CyanAccent,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Detected People & Faces in this Image
                if (currentFaces.isNotEmpty()) {
                    Text(
                        text = "Identified People (${currentFaces.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = CyanAccent,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        currentFaces.forEachIndexed { fIdx, face ->
                            Surface(
                                onClick = {
                                    viewModel.searchPeopleByFace(face)
                                    showFaceSearchSheet = true
                                },
                                color = Color(0xFF1E293B),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Face,
                                        contentDescription = null,
                                        tint = CyanAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = face.personName ?: "Person #${fIdx + 1}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "• Find all photos",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CyanAccent
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Full Extracted Text Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Extracted Text (${currentItem.allBlocks.size} blocks)",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                onClick = {
                                    copyToClipboard(
                                        currentItem.document?.rawText.orEmpty(),
                                        "All Image Text"
                                    )
                                },
                                color = Color(0x3300E5FF),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(14.dp))
                                    Text("Copy All", style = MaterialTheme.typography.labelSmall, color = CyanAccent, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = currentItem.document?.rawText ?: "No text recognized in this image.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1),
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 20.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Technical Metadata Collapsible Card
                Card(
                    onClick = { showMetadata = !showMetadata },
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = IndigoAccent)
                                Text(
                                    text = "Technical & Model Details",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = if (showMetadata) "Hide" else "Show",
                                style = MaterialTheme.typography.labelSmall,
                                color = IndigoAccent
                            )
                        }

                        AnimatedVisibility(visible = showMetadata) {
                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                MetadataRow("OCR Engine", "PP-OCRv6-Tiny (ONNX Mobile)")
                                MetadataRow("Execution Provider", currentItem.document?.runtimeProvider?.displayName ?: "CPU")
                                MetadataRow("Inference Latency", "${currentItem.document?.inferenceMs ?: 0} ms")
                                MetadataRow("Dimensions", "${currentItem.asset.width} × ${currentItem.asset.height} px")
                                MetadataRow("File Size", "${currentItem.asset.fileSize / 1024} KB")
                                MetadataRow("Is Screenshot", if (currentItem.asset.isScreenshot) "Yes (Heuristic/Path Match)" else "No")
                                MetadataRow("Exact Hash (xxHash3)", currentItem.asset.exactHash?.take(16)?.plus("...") ?: "N/A")
                                MetadataRow("Storage Location", currentItem.asset.uri)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showFaceSearchSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showFaceSearchSheet = false
                viewModel.clearFaceSearchResults()
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = ObsidianCard
        ) {
            FaceSearchResultsSheet(
                queryUri = currentItem.asset.uri,
                isLoading = isSearchingFaces,
                results = faceSearchResults,
                onPhotoClick = { assetId ->
                    showFaceSearchSheet = false
                    viewModel.clearFaceSearchResults()
                    viewModel.selectAssetForDetail(assetId)
                },
                onPickAnother = {
                    showFaceSearchSheet = false
                },
                onClose = {
                    showFaceSearchSheet = false
                    viewModel.clearFaceSearchResults()
                }
            )
        }
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = Color(0xFF94A3B8))
        Text(text = value, style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Medium)
    }
}
