package com.example.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.screens.benchmark.BenchmarkScreen
import com.example.ui.screens.detail.ImageDetailScreen
import com.example.ui.screens.gallery.GalleryScreen
import com.example.ui.screens.indexing.IndexingDashboardScreen
import com.example.ui.screens.people.PeopleScreen
import com.example.ui.screens.search.SearchScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianCardBorder
import com.example.ui.viewmodel.MainViewModel

enum class NavTab(val title: String, val icon: ImageVector, val tag: String) {
    SEARCH("Search", Icons.Default.Search, "tab_search"),
    PEOPLE("People", Icons.Default.Face, "tab_people"),
    GALLERY("Gallery", Icons.Default.PhotoLibrary, "tab_gallery"),
    DASHBOARD("Dashboard", Icons.Default.Dashboard, "tab_dashboard"),
    BENCHMARK("Benchmark", Icons.Default.Assessment, "tab_benchmark"),
    SETTINGS("Settings", Icons.Default.Settings, "tab_settings")
}

@Composable
fun NavigationRoot(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var currentTab by remember { mutableStateOf(NavTab.SEARCH) }
    var detailAssetId by remember { mutableStateOf<Long?>(null) }

    if (detailAssetId != null) {
        ImageDetailScreen(
            viewModel = viewModel,
            onBack = { detailAssetId = null }
        )
    } else {
        Scaffold(
            containerColor = ObsidianBackground,
            bottomBar = {
                NavigationBar(
                    containerColor = ObsidianCard,
                    contentColor = Color.White,
                    tonalElevation = 8.dp,
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    NavTab.values().forEach { tab ->
                        val isSelected = currentTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentTab = tab },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.Black,
                                selectedTextColor = CyanAccent,
                                indicatorColor = CyanAccent,
                                unselectedIconColor = Color(0xFF94A3B8),
                                unselectedTextColor = Color(0xFF94A3B8)
                            ),
                            modifier = Modifier.testTag(tab.tag)
                        )
                    }
                }
            },
            modifier = modifier
        ) { innerPadding ->
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) { tab ->
                when (tab) {
                    NavTab.SEARCH -> SearchScreen(
                        viewModel = viewModel,
                        onOpenDetail = { assetId ->
                            viewModel.selectAssetForDetail(assetId)
                            detailAssetId = assetId
                        },
                        onNavigateToDashboard = { currentTab = NavTab.DASHBOARD }
                    )
                    NavTab.PEOPLE -> PeopleScreen(
                        viewModel = viewModel,
                        onOpenDetail = { assetId ->
                            viewModel.selectAssetForDetail(assetId)
                            detailAssetId = assetId
                        }
                    )
                    NavTab.GALLERY -> GalleryScreen(
                        viewModel = viewModel,
                        onOpenDetail = { assetId ->
                            viewModel.selectAssetForDetail(assetId)
                            detailAssetId = assetId
                        }
                    )
                    NavTab.DASHBOARD -> IndexingDashboardScreen(
                        viewModel = viewModel
                    )
                    NavTab.BENCHMARK -> BenchmarkScreen(
                        viewModel = viewModel
                    )
                    NavTab.SETTINGS -> SettingsScreen(
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}
