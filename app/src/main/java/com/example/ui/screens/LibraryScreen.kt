package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import com.example.data.model.MediaCollection
import com.example.data.model.MediaItem
import com.example.data.model.MediaType
import com.example.ui.components.AiGroupingConfirmationSheet
import com.example.ui.components.CollectionBottomSheet
import com.example.ui.components.CollectionCard
import com.example.ui.components.DetailBottomSheet
import com.example.ui.components.FileRecategorizationSheet
import com.example.ui.components.FilterChips
import com.example.ui.components.FoldersView
import com.example.ui.components.MediaCard
import com.example.ui.components.MediaSearchBar
import com.example.ui.components.SettingsView
import com.example.ui.components.SmartAiView
import com.example.ui.viewmodel.MediaViewModel
import androidx.compose.material3.MaterialTheme
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.cardSurface
import com.example.ui.theme.textMuted
import com.example.ui.theme.textPrimary
import com.example.ui.theme.textSecondary
import com.example.ui.util.AppLanguage
import com.example.ui.util.UiStrings
import androidx.compose.foundation.border

enum class NavigationTab {
    LIBRARY,
    FOLDERS,
    SMART_AI,
    SETTINGS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MediaViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(NavigationTab.LIBRARY) }

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val mediaItems by viewModel.mediaItems.collectAsStateWithLifecycle()
    val mediaCollections by viewModel.mediaCollections.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val isGroupingWithAi by viewModel.isGroupingWithAi.collectAsStateWithLifecycle()
    val scanProgress by viewModel.scanProgress.collectAsStateWithLifecycle()

    val selectedMedia by viewModel.selectedMedia.collectAsStateWithLifecycle()
    val selectedCollection by viewModel.selectedCollection.collectAsStateWithLifecycle()
    val recommendations by viewModel.recommendations.collectAsStateWithLifecycle()
    val isLoadingRecs by viewModel.isLoadingRecommendations.collectAsStateWithLifecycle()
    val selectedRecommendationSource by viewModel.selectedRecommendationSource.collectAsStateWithLifecycle()
    val targetLanguage by viewModel.targetLanguage.collectAsStateWithLifecycle()
    val uiLanguage by viewModel.uiLanguage.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val strings = UiStrings(uiLanguage)

    val geminiApiKey by viewModel.geminiApiKey.collectAsStateWithLifecycle()
    val keyTestStatus by viewModel.keyTestStatus.collectAsStateWithLifecycle()
    val tmdbApiKey by viewModel.tmdbApiKey.collectAsStateWithLifecycle()
    val tmdbKeyTestStatus by viewModel.tmdbKeyTestStatus.collectAsStateWithLifecycle()
    val omdbApiKey by viewModel.omdbApiKey.collectAsStateWithLifecycle()
    val omdbKeyTestStatus by viewModel.omdbKeyTestStatus.collectAsStateWithLifecycle()

    val proposedClusters by viewModel.proposedClusters.collectAsStateWithLifecycle()
    val showAiConfirmationSheet by viewModel.showAiConfirmationSheet.collectAsStateWithLifecycle()

    val selectedFileIds by viewModel.selectedFileIds.collectAsStateWithLifecycle()
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val isAnalyzingSelection by viewModel.isAnalyzingSelection.collectAsStateWithLifecycle()
    val recategorizationResults by viewModel.recategorizationResults.collectAsStateWithLifecycle()
    val showRecategorizationSheet by viewModel.showRecategorizationSheet.collectAsStateWithLifecycle()
    val showApiKeyOnboardingSheet by viewModel.showApiKeyOnboardingSheet.collectAsStateWithLifecycle()

    val showCollectionAiEditSheet by viewModel.showCollectionAiEditSheet.collectAsStateWithLifecycle()
    val isAnalyzingCollection by viewModel.isAnalyzingCollection.collectAsStateWithLifecycle()
    val proposedCollectionMetadata by viewModel.proposedCollectionMetadata.collectAsStateWithLifecycle()

    val showManualAddSheet by viewModel.showManualAddSheet.collectAsStateWithLifecycle()

    val candidateReviewCollection by viewModel.candidateReviewCollection.collectAsStateWithLifecycle()
    val isFetchingCandidates by viewModel.isFetchingCandidates.collectAsStateWithLifecycle()
    val candidatesForReview by viewModel.candidatesForReview.collectAsStateWithLifecycle()

    // Permission launcher for Android Storage
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        if (granted) {
            Toast.makeText(context, "Storage access granted! Scanning video files...", Toast.LENGTH_SHORT).show()
            viewModel.scanLocalFiles(includeDemoFallback = false)
        } else {
            Toast.makeText(context, "Storage permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    // Folder picker launcher using SAF
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            Toast.makeText(
                context,
                if (strings.isFa) "پوشه انتخاب شد: ${uri.lastPathSegment}" else "Folder selected: ${uri.lastPathSegment}",
                Toast.LENGTH_SHORT
            ).show()
            viewModel.scanLocalFiles(customFolder = uri.toString(), includeDemoFallback = false)
        }
    }

    val hasStoragePermission = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.cardSurface,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .border(androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline), shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = currentTab == NavigationTab.LIBRARY,
                    onClick = { currentTab = NavigationTab.LIBRARY },
                    icon = { Icon(imageVector = Icons.Default.Home, contentDescription = strings.libraryTab) },
                    label = { Text(strings.libraryTab, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = PrimaryIndigo,
                        unselectedIconColor = MaterialTheme.colorScheme.textMuted,
                        unselectedTextColor = MaterialTheme.colorScheme.textMuted,
                        indicatorColor = PrimaryIndigo
                    ),
                    modifier = Modifier.testTag("nav_library")
                )

                NavigationBarItem(
                    selected = currentTab == NavigationTab.FOLDERS,
                    onClick = { currentTab = NavigationTab.FOLDERS },
                    icon = { Icon(imageVector = Icons.Default.Folder, contentDescription = strings.foldersTab) },
                    label = { Text(strings.foldersTab, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = PrimaryIndigo,
                        unselectedIconColor = MaterialTheme.colorScheme.textMuted,
                        unselectedTextColor = MaterialTheme.colorScheme.textMuted,
                        indicatorColor = PrimaryIndigo
                    ),
                    modifier = Modifier.testTag("nav_folders")
                )

                NavigationBarItem(
                    selected = currentTab == NavigationTab.SMART_AI,
                    onClick = { currentTab = NavigationTab.SMART_AI },
                    icon = { Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = strings.smartAiTab) },
                    label = { Text(strings.smartAiTab, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = PrimaryIndigo,
                        unselectedIconColor = MaterialTheme.colorScheme.textMuted,
                        unselectedTextColor = MaterialTheme.colorScheme.textMuted,
                        indicatorColor = PrimaryIndigo
                    ),
                    modifier = Modifier.testTag("nav_smart_ai")
                )

                NavigationBarItem(
                    selected = currentTab == NavigationTab.SETTINGS,
                    onClick = { currentTab = NavigationTab.SETTINGS },
                    icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = strings.settingsTab) },
                    label = { Text(strings.settingsTab, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = PrimaryIndigo,
                        unselectedIconColor = MaterialTheme.colorScheme.textMuted,
                        unselectedTextColor = MaterialTheme.colorScheme.textMuted,
                        indicatorColor = PrimaryIndigo
                    ),
                    modifier = Modifier.testTag("nav_settings")
                )
            }
        },
        floatingActionButton = {
            if (currentTab == NavigationTab.LIBRARY) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SmallFloatingActionButton(
                        onClick = { viewModel.openManualAddSheet() },
                        containerColor = MaterialTheme.colorScheme.cardSurface,
                        contentColor = PrimaryIndigo,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("floating_add_manual_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "افزودن مجموعه دستی",
                            tint = PrimaryIndigo
                        )
                    }

                    FloatingActionButton(
                        onClick = { viewModel.scanLocalFiles(includeDemoFallback = false) },
                        containerColor = PrimaryIndigo,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.testTag("floating_scan_button")
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "اسکن", tint = Color.White)
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                NavigationTab.LIBRARY -> {
                    if (mediaCollections.isEmpty() && !isScanning) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            MediaSearchBar(
                                query = searchQuery,
                                onQueryChange = { viewModel.searchQuery.value = it },
                                uiLanguage = uiLanguage,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )

                            FilterChips(
                                selectedCategory = selectedCategory,
                                onCategorySelected = { viewModel.selectedCategory.value = it },
                                uiLanguage = uiLanguage
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FolderOpen,
                                        contentDescription = null,
                                        tint = PrimaryIndigo,
                                        modifier = Modifier.size(56.dp)
                                    )
                                    
                                    Text(
                                        text = strings.noVideoFound,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.textPrimary,
                                        textAlign = TextAlign.Center
                                    )
                                    
                                    Text(
                                        text = strings.scanNotice,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.textMuted,
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Button(
                                        onClick = {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                permissionLauncher.launch(
                                                    arrayOf(
                                                        Manifest.permission.READ_MEDIA_VIDEO,
                                                        Manifest.permission.READ_MEDIA_IMAGES
                                                    )
                                                )
                                            } else {
                                                permissionLauncher.launch(
                                                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                                                )
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = PrimaryIndigo
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth(0.85f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Security,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.size(8.dp))
                                        Text(strings.scanStorage, fontSize = 13.sp)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            folderPickerLauncher.launch(null)
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                        modifier = Modifier.fillMaxWidth(0.85f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Folder,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.textSecondary
                                        )
                                        Spacer(modifier = Modifier.size(8.dp))
                                        Text(strings.selectFolder, fontSize = 13.sp, color = MaterialTheme.colorScheme.textPrimary)
                                    }
                                }
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // 1. Compact Header Search Bar
                            item(span = { GridItemSpan(2) }) {
                                MediaSearchBar(
                                    query = searchQuery,
                                    onQueryChange = { viewModel.searchQuery.value = it },
                                    uiLanguage = uiLanguage,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            if (geminiApiKey.isBlank()) {
                                item(span = { GridItemSpan(2) }) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.openApiKeyOnboarding() },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF3B2A10)),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f))
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
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Warning,
                                                    contentDescription = null,
                                                    tint = Color(0xFFF59E0B),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    text = "کلید هوش مصنوعی Gemini تنظیم نشده است (کاهش دقت انیمه/فیلم)",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFFDE68A)
                                                )
                                            }
                                            Text(
                                                text = "تنظیم 🔑",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFF59E0B)
                                            )
                                        }
                                    }
                                }
                            }

                            // 2. Compact Category Chips
                            item(span = { GridItemSpan(2) }) {
                                FilterChips(
                                    selectedCategory = selectedCategory,
                                    onCategorySelected = { viewModel.selectedCategory.value = it },
                                    uiLanguage = uiLanguage,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }

                            // 3. Compact AI Smart Grouping Bar
                            item(span = { GridItemSpan(2) }) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.cardSurface)
                                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                        .clickable {
                                            if (!isGroupingWithAi) {
                                                viewModel.runGeminiSmartGrouping { total ->
                                                    Toast.makeText(context, if (strings.isFa) "مجموعه‌ها با موفقیت ادغام شدند!" else "Collections merged successfully!", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 7.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = PrimaryIndigo,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = strings.smartGroupingBannerTitle,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.textPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        if (isGroupingWithAi) {
                                            CircularProgressIndicator(
                                                color = PrimaryIndigo,
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        } else {
                                            Text(
                                                text = strings.smartGroupingBtn,
                                                color = PrimaryIndigo,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            // 4. Compact Section Header
                            item(span = { GridItemSpan(2) }) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 2.dp, bottom = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = strings.collectionsAndTitles,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.textPrimary
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { viewModel.toggleSelectionMode() },
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.height(28.dp).testTag("toggle_selection_mode_button"),
                                            contentPadding = PaddingValues(horizontal = 8.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Checklist,
                                                contentDescription = null,
                                                modifier = Modifier.size(12.dp),
                                                tint = if (isSelectionMode) PrimaryIndigo else MaterialTheme.colorScheme.textMuted
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (isSelectionMode) strings.cancel else strings.batchSelect,
                                                fontSize = 10.sp,
                                                color = if (isSelectionMode) PrimaryIndigo else MaterialTheme.colorScheme.textSecondary
                                            )
                                        }

                                        if (isScanning) {
                                            Text(
                                                text = "${strings.analyzing} ${scanProgress.first}/${scanProgress.second}",
                                                fontSize = 10.sp,
                                                color = PrimaryIndigo,
                                                fontWeight = FontWeight.Bold
                                            )
                                        } else {
                                            Text(
                                                text = if (strings.isFa) "${mediaCollections.size} مجموعه (${mediaItems.size} فایل)" else "${mediaCollections.size} collections (${mediaItems.size} files)",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.textMuted,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }

                            // 5. Poster Collection Cards
                            items(mediaCollections, key = { it.collectionKey }) { collection ->
                                CollectionCard(
                                    collection = collection,
                                    onClick = { viewModel.selectCollection(collection) }
                                )
                            }
                        }
                    }
                }

                NavigationTab.FOLDERS -> {
                    FoldersView(
                        mediaItems = mediaItems,
                        uiLanguage = uiLanguage,
                        isSelectionMode = isSelectionMode,
                        selectedFileIds = selectedFileIds,
                        onToggleFileSelection = { viewModel.toggleFileSelection(it) },
                        onRescanFolder = { viewModel.scanLocalFiles() },
                        onSelectFolderClick = { folderPickerLauncher.launch(null) },
                        onItemClick = { viewModel.selectMediaItem(it) }
                    )
                }

                NavigationTab.SMART_AI -> {
                    SmartAiView(
                        targetLanguage = targetLanguage,
                        uiLanguage = uiLanguage,
                        onRunAiGrouping = { viewModel.runGeminiSmartGrouping() }
                    )
                }

                NavigationTab.SETTINGS -> {
                    SettingsView(
                        targetLanguage = targetLanguage,
                        uiLanguage = uiLanguage,
                        themeMode = themeMode,
                        geminiApiKey = geminiApiKey,
                        keyTestStatus = keyTestStatus,
                        tmdbApiKey = tmdbApiKey,
                        tmdbKeyTestStatus = tmdbKeyTestStatus,
                        omdbApiKey = omdbApiKey,
                        omdbKeyTestStatus = omdbKeyTestStatus,
                        onLanguageSelected = { viewModel.setLanguage(it) },
                        onUiLanguageSelected = { viewModel.setUiLanguage(it) },
                        onThemeModeSelected = { viewModel.setThemeMode(it) },
                        onSaveAndTestKey = { viewModel.saveAndTestGeminiApiKey(it) },
                        onClearKey = { viewModel.clearGeminiApiKey() },
                        onSaveAndTestTmdbKey = { viewModel.saveAndTestTmdbApiKey(it) },
                        onClearTmdbKey = { viewModel.clearTmdbApiKey() },
                        onSaveAndTestOmdbKey = { viewModel.saveAndTestOmdbApiKey(it) },
                        onClearOmdbKey = { viewModel.clearOmdbApiKey() },
                        onRescanClick = {
                            viewModel.scanLocalFiles()
                            Toast.makeText(context, if (strings.isFa) "در حال اسکن مجدد..." else "Rescanning local folders...", Toast.LENGTH_SHORT).show()
                        },
                        onClearCacheClick = {
                            viewModel.clearDatabase()
                            Toast.makeText(context, if (strings.isFa) "حافظه کش پاکسازی شد" else "Library database cleared", Toast.LENGTH_SHORT).show()
                        },
                        onRunAiGrouping = { viewModel.runGeminiSmartGrouping() }
                    )
                }
            }

            // Multi-Selection Floating Action Bar Overlay
            if (isSelectionMode || selectedFileIds.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp, start = 12.dp, end = 12.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.cardSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryIndigo),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.clearFileSelection() }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = strings.cancel,
                                    tint = MaterialTheme.colorScheme.textPrimary
                                )
                            }
                            Text(
                                text = strings.selectedCount(selectedFileIds.size),
                                color = MaterialTheme.colorScheme.textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val allIds = mediaItems.map { it.id }
                                    viewModel.selectAllFiles(allIds)
                                },
                                shape = RoundedCornerShape(20.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.textPrimary),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(imageVector = Icons.Default.SelectAll, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.textSecondary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = strings.selectAll, fontSize = 11.sp, color = MaterialTheme.colorScheme.textPrimary)
                            }

                            Button(
                                onClick = { viewModel.reCategorizeSelectedFiles() },
                                enabled = selectedFileIds.isNotEmpty() && !isAnalyzingSelection,
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("ai_recategorize_selection_button")
                            ) {
                                if (isAnalyzingSelection) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = strings.analyzing, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = strings.fixWithGemini, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // File Re-categorization Review Sheet
            if (showRecategorizationSheet) {
                FileRecategorizationSheet(
                    results = recategorizationResults,
                    onConfirmApply = {
                        viewModel.applyRecategorizationResults()
                        Toast.makeText(context, "اصلاحات و دسته‌بندی فایل‌ها با موفقیت اعمال شد!", Toast.LENGTH_SHORT).show()
                    },
                    onDismiss = {
                        viewModel.dismissRecategorizationSheet()
                    }
                )
            }

            // AI Confirmation Sheet
            if (showAiConfirmationSheet) {
                AiGroupingConfirmationSheet(
                    proposedClusters = proposedClusters,
                    onConfirm = {
                        viewModel.applyProposedAiGrouping()
                        Toast.makeText(context, "دسته‌بندی‌های هوشمند با موفقیت اعمال شدند!", Toast.LENGTH_SHORT).show()
                    },
                    onReAnalyze = {
                        viewModel.runGeminiSmartGrouping()
                    },
                    onDismiss = {
                        viewModel.dismissAiConfirmationSheet()
                    }
                )
            }

            // Collection Detail Bottom Sheet (For Grouped Franchises / Shows)
            selectedCollection?.let { collection ->
                if (collection.totalCount > 1) {
                    CollectionBottomSheet(
                        collection = collection,
                        recommendations = recommendations,
                        isLoadingRecommendations = isLoadingRecs,
                        selectedRecommendationSource = selectedRecommendationSource,
                        onSourceChange = { viewModel.setRecommendationSource(it) },
                        onDismiss = { viewModel.selectCollection(null) },
                        onPlayClick = { filePath, targetPackage ->
                            try {
                                val playIntent = viewModel.getPlayIntent(filePath, targetPackage)
                                context.startActivity(playIntent)
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    "Player unavailable. Launching system chooser...",
                                    Toast.LENGTH_SHORT
                                ).show()
                                try {
                                    val chooserIntent = viewModel.getPlayIntent(filePath, null)
                                    context.startActivity(chooserIntent)
                                } catch (ex: Exception) {
                                    Toast.makeText(context, "Unable to play video file: ${ex.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        onToggleWatched = { viewModel.toggleWatchStatus(it) },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onMarkAllWatched = { isWatched -> viewModel.markAllInCollectionAsWatched(collection, isWatched) },
                        onReAnalyzeCollection = { viewModel.reAnalyzeCollectionWithAi(collection) },
                        onOpenCandidateReview = { viewModel.openCandidateReviewSheet(collection) },
                        onDeleteCollection = { viewModel.deleteCollection(collection) },
                        onRecommendationClick = { rec ->
                            Toast.makeText(context, "عنوان پیشنهادی: ${rec.title}", Toast.LENGTH_SHORT).show()
                        }
                    )
                } else if (collection.totalCount == 1) {
                    val singleItem = collection.items.first()
                    DetailBottomSheet(
                        mediaItem = singleItem,
                        recommendations = recommendations,
                        isLoadingRecommendations = isLoadingRecs,
                        selectedRecommendationSource = selectedRecommendationSource,
                        onSourceChange = { viewModel.setRecommendationSource(it) },
                        onDismiss = { viewModel.selectCollection(null) },
                        onPlayClick = { filePath, targetPackage ->
                            try {
                                val playIntent = viewModel.getPlayIntent(filePath, targetPackage)
                                context.startActivity(playIntent)
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    "Player unavailable. Launching system chooser...",
                                    Toast.LENGTH_SHORT
                                ).show()
                                try {
                                    val chooserIntent = viewModel.getPlayIntent(filePath, null)
                                    context.startActivity(chooserIntent)
                                } catch (ex: Exception) {
                                    Toast.makeText(context, "Unable to play video file: ${ex.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        onToggleWatched = { viewModel.toggleWatchStatus(it) },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onRecommendationClick = { rec ->
                            Toast.makeText(context, "عنوان پیشنهادی: ${rec.title}", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            // Standalone Single Item Detail Sheet (Fallback)
            if (selectedCollection == null) {
                selectedMedia?.let { media ->
                    DetailBottomSheet(
                        mediaItem = media,
                        recommendations = recommendations,
                        isLoadingRecommendations = isLoadingRecs,
                        selectedRecommendationSource = selectedRecommendationSource,
                        onSourceChange = { viewModel.setRecommendationSource(it) },
                        onDismiss = { viewModel.selectMediaItem(null) },
                        onPlayClick = { filePath, targetPackage ->
                            try {
                                val playIntent = viewModel.getPlayIntent(filePath, targetPackage)
                                context.startActivity(playIntent)
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    "Player unavailable. Launching system chooser...",
                                    Toast.LENGTH_SHORT
                                ).show()
                                try {
                                    val chooserIntent = viewModel.getPlayIntent(filePath, null)
                                    context.startActivity(chooserIntent)
                                } catch (ex: Exception) {
                                    Toast.makeText(context, "Unable to play video file: ${ex.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        onToggleWatched = { viewModel.toggleWatchStatus(it) },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onRecommendationClick = { rec ->
                            Toast.makeText(context, "Recommendation: ${rec.title}", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            if (showCollectionAiEditSheet) {
                selectedCollection?.let { col ->
                    com.example.ui.components.CollectionAiEditSheet(
                        collection = col,
                        proposedMetadata = proposedCollectionMetadata,
                        isAnalyzing = isAnalyzingCollection,
                        onConfirmUpdate = { newTitle, newType, newSynopsis, newPoster ->
                            viewModel.applyCollectionUpdate(col, newTitle, newType, newSynopsis, newPoster)
                        },
                        onReFetchAi = { viewModel.reAnalyzeCollectionWithAi(col) },
                        onDismiss = { viewModel.dismissCollectionAiEditSheet() }
                    )
                }
            }

            if (showApiKeyOnboardingSheet) {
                com.example.ui.components.ApiKeyOnboardingSheet(
                    keyTestStatus = keyTestStatus,
                    onSaveAndTestKey = { key -> viewModel.saveAndTestGeminiApiKey(key) },
                    onDismiss = { viewModel.dismissApiKeyOnboarding() }
                )
            }

            if (showManualAddSheet) {
                com.example.ui.components.ManualAddCollectionSheet(
                    onConfirm = { input ->
                        viewModel.addManualCollection(input)
                        Toast.makeText(
                            context,
                            if (strings.isFa) "مجموعه «${input.title}» به کتابخانه اضافه شد" else "Collection \"${input.title}\" added",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onDismiss = { viewModel.dismissManualAddSheet() }
                )
            }

            candidateReviewCollection?.let { col ->
                com.example.ui.components.CollectionCandidateReviewSheet(
                    collection = col,
                    candidates = candidatesForReview,
                    isFetchingCandidates = isFetchingCandidates,
                    onSelectCandidate = { candidate ->
                        viewModel.confirmCandidateForCollection(col, candidate)
                        Toast.makeText(context, "مجموعه با موفقیت به «${candidate.title}» تغییر یافت", Toast.LENGTH_SHORT).show()
                    },
                    onConfirmManualEntry = { customTitle, mediaType, synopsis, posterUrl ->
                        viewModel.confirmManualEntryForCollection(col, customTitle, mediaType, synopsis, posterUrl)
                        Toast.makeText(context, "اطلاعات دستی با موفقیت ثبت شد", Toast.LENGTH_SHORT).show()
                    },
                    onDismiss = {
                        viewModel.dismissCandidateReviewSheet()
                    }
                )
            }
        }
    }
}
