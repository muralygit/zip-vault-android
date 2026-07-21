package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ZipDatabase
import com.example.data.ZipFileEntity
import com.example.data.ZipRepository
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.ZipSortType
import com.example.ui.ZipViewModel
import com.example.ui.ZipViewModelFactory
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: ZipViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize local database and repository
        val database = ZipDatabase.getDatabase(applicationContext)
        val repository = ZipRepository(database.zipDao())
        viewModel = ViewModelProvider(this, ZipViewModelFactory(repository))[ZipViewModel::class.java]

        // Handle incoming shared files on cold start
        handleIncomingIntent(intent)

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    ZipVaultDashboard(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        if (Intent.ACTION_SEND == intent.action && intent.type != null) {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            }
            if (uri != null) {
                viewModel.importZip(applicationContext, uri)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZipVaultDashboard(
    viewModel: ZipViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Dialog States
    var showEditTagsDialogForFile by remember { mutableStateOf<ZipFileEntity?>(null) }
    var showDeleteConfirmDialogForFile by remember { mutableStateOf<ZipFileEntity?>(null) }
    
    // Sort Menu State
    var showSortMenu by remember { mutableStateOf(false) }

    // Launcher for manually importing ZIP files
    val zipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.importZip(context, it)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header Hero Banner Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Zip Vault",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            letterSpacing = (-0.5).sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Offline-First Secure Storage",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    // Stats pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "${uiState.zipFiles.size} Files",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Search Bar & Filter Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search files or tags...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search"
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("search_bar")
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Sort Dropdown Button
                Box {
                    IconButton(
                        onClick = { showSortMenu = true },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .testTag("sort_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Sort Archives",
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Date added (Newest first)") },
                            leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                            onClick = {
                                viewModel.setSortType(ZipSortType.DATE_DESC)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Date added (Oldest first)") },
                            leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                            onClick = {
                                viewModel.setSortType(ZipSortType.DATE_ASC)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Name (A to Z)") },
                            leadingIcon = { Icon(Icons.Outlined.SortByAlpha, contentDescription = null) },
                            onClick = {
                                viewModel.setSortType(ZipSortType.NAME_ASC)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Name (Z to A)") },
                            leadingIcon = { Icon(Icons.Outlined.SortByAlpha, contentDescription = null) },
                            onClick = {
                                viewModel.setSortType(ZipSortType.NAME_DESC)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Size (Largest first)") },
                            leadingIcon = { Icon(Icons.Default.Save, contentDescription = null) },
                            onClick = {
                                viewModel.setSortType(ZipSortType.SIZE_DESC)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Size (Smallest first)") },
                            leadingIcon = { Icon(Icons.Default.Save, contentDescription = null) },
                            onClick = {
                                viewModel.setSortType(ZipSortType.SIZE_ASC)
                                showSortMenu = false
                            }
                        )
                    }
                }
            }

            // Status alerts (Success/Error banners with auto dismiss or dismiss button)
            AnimatedVisibility(
                visible = uiState.statusMessage != null || uiState.errorMessage != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                val isError = uiState.errorMessage != null
                val message = uiState.errorMessage ?: uiState.statusMessage ?: ""
                
                Surface(
                    color = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                    contentColor = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = if (isError) Icons.Default.Error else Icons.Default.CheckCircle,
                            contentDescription = if (isError) "Error" else "Success",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = message,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                if (isError) viewModel.clearErrorMessage() else viewModel.clearStatusMessage()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                
                // Optional auto-dismiss timer
                LaunchedEffect(uiState.statusMessage, uiState.errorMessage) {
                    delay(4000)
                    viewModel.clearStatusMessage()
                    viewModel.clearErrorMessage()
                }
            }

            // Main Content Area
            if (uiState.isLoading && uiState.zipFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (uiState.zipFiles.isEmpty()) {
                // Empty State Layout
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Clean, high-fidelity custom Canvas/Icon visual centerpiece for Empty State
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(140.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderZip,
                                contentDescription = "Empty Vault",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                modifier = Modifier.size(72.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = "Your Vault is Empty",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Text(
                            text = "Tap the + button to import ZIP archives, or share files directly from Google Files or other apps to save them securely inside the local vault.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            } else {
                // List of ZIP files
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 88.dp, start = 16.dp, end = 16.dp, top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(uiState.zipFiles, key = { it.id }) { zipFile ->
                        ZipFileCard(
                            zipFile = zipFile,
                            onShare = { shareZipOut(context, zipFile) },
                            onDelete = { showDeleteConfirmDialogForFile = zipFile },
                            onEditTags = { showEditTagsDialogForFile = zipFile }
                        )
                    }
                }
            }
        }

        // Floating Action Button to Import Manually
        FloatingActionButton(
            onClick = {
                try {
                    zipPickerLauncher.launch(
                        arrayOf(
                            "application/zip",
                            "application/x-zip-compressed"
                        )
                    )
                } catch (e: Exception) {
                    // Fallback launcher if specific mime types cause system Picker exceptions
                    zipPickerLauncher.launch(arrayOf("*/*"))
                }
            },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_zip_button")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Import manual ZIP",
                modifier = Modifier.size(28.dp)
            )
        }
    }

    // 1. Edit Tags Dialog
    showEditTagsDialogForFile?.let { zipFile ->
        var tempTags by remember { mutableStateOf(zipFile.tags) }
        
        AlertDialog(
            onDismissRequest = { showEditTagsDialogForFile = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Label,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Customize Tags", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Set labels/categories for \"${zipFile.name}\" separated by commas.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = tempTags,
                        onValueChange = { tempTags = it },
                        placeholder = { Text("e.g. Work, Invoice, Receipts") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tags_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateTags(zipFile, tempTags)
                        showEditTagsDialogForFile = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEditTagsDialogForFile = null }
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.outline)
                }
            }
        )
    }

    // 2. Delete Confirmation Dialog
    showDeleteConfirmDialogForFile?.let { zipFile ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialogForFile = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Delete ZIP File?", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete \"${zipFile.name}\" from local vault storage? This action cannot be undone.",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteZip(zipFile)
                        showDeleteConfirmDialogForFile = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmDialogForFile = null }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ZipFileCard(
    zipFile: ZipFileEntity,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onEditTags: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("zip_item_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                // ZIP File Icon Box
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderZip,
                        contentDescription = "ZIP File",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = zipFile.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatBytes(zipFile.size),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = " • ",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                        Text(
                            text = formatDate(zipFile.dateAdded),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Tag list or empty tag button
            Spacer(modifier = Modifier.height(12.dp))
            val tagList = remember(zipFile.tags) {
                zipFile.tags.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
            }

            if (tagList.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(tagList) { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onEditTags() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = tag,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    item {
                        IconButton(
                            onClick = onEditTags,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Tags",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            } else {
                TextButton(
                    onClick = onEditTags,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Label,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Add label / category",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Bottom Actions (Share, Delete)
            Divider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                modifier = Modifier.padding(vertical = 12.dp)
            )

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = onShare,
                    modifier = Modifier.testTag("share_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Share / Export ZIP",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete ZIP",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

// Helper: safe sharing out of file
private fun shareZipOut(context: Context, zipFile: ZipFileEntity) {
    try {
        val file = File(zipFile.filePath)
        if (!file.exists()) {
            return
        }

        val authority = "com.aistudio.zipvault.qkzpqr.fileprovider"
        val contentUri = FileProvider.getUriForFile(context, authority, file)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Share \"${zipFile.name}\" via"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

// Formats file sizes elegantly
private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    val groupVal = if (digitGroups < units.size) digitGroups else units.size - 1
    return String.format(Locale.getDefault(), "%.1f %s", bytes / Math.pow(1024.0, groupVal.toDouble()), units[groupVal])
}

// Formats dates elegantly
private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

// Simple non-blocking delay implementation using Coroutines
private suspend fun delay(timeMs: Long) {
    kotlinx.coroutines.delay(timeMs)
}
