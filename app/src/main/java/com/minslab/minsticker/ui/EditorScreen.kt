package com.minslab.minsticker.ui

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterDrama
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minslab.minsticker.data.AppLanguage
import com.minslab.minsticker.data.PreferencesManager
import com.minslab.minsticker.data.StickerRepository
import com.minslab.minsticker.segmentation.EdgeRefiner
import com.minslab.minsticker.segmentation.OutlineRenderer
import com.minslab.minsticker.segmentation.StickerEffect
import com.minslab.minsticker.segmentation.StickerOptimizer
import com.minslab.minsticker.segmentation.SubjectSegmenter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    initialUri: Uri? = null,
    onNavigateBack: () -> Unit,
    onSavedSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = remember { PreferencesManager.getInstance(context) }
    val repository = remember { StickerRepository(context) }
    val language by preferences.language.collectAsState()
    val isVi = language == AppLanguage.VI

    var selectedUri by remember { mutableStateOf<Uri?>(initialUri) }
    var rawBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var segmentedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var currentEffect by remember { mutableStateOf(StickerEffect.OUTLINE) }
    var enableShadow by remember { mutableStateOf(true) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
        }
    }

    // Load and segment photo when selected
    LaunchedEffect(selectedUri) {
        val uri = selectedUri ?: return@LaunchedEffect
        isProcessing = true
        errorMessage = null
        try {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, _, _ ->
                    decoder.isMutableRequired = true
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
            rawBitmap = bitmap

            // Segment foreground
            val result = SubjectSegmenter.segment(bitmap, context)
            result.onSuccess { fg ->
                val refined = EdgeRefiner.refineEdges(fg)
                segmentedBitmap = refined
                previewBitmap = OutlineRenderer.renderEffect(refined, currentEffect, enableShadow)
            }.onFailure { err ->
                errorMessage = err.message ?: if (isVi) "Không thể tách chủ thể" else "Could not segment subject"
            }
        } catch (e: Exception) {
            errorMessage = (if (isVi) "Lỗi đọc tệp hình ảnh: " else "Failed to read image: ") + "${e.message}"
        } finally {
            isProcessing = false
        }
    }

    // Update styled preview when effect or shadow toggles
    LaunchedEffect(currentEffect, enableShadow, segmentedBitmap) {
        val seg = segmentedBitmap ?: return@LaunchedEffect
        previewBitmap = OutlineRenderer.renderEffect(seg, currentEffect, enableShadow)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isVi) "Biên tập nhãn dán" else "Sticker Editor", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = if (isVi) "Quay lại" else "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Preview Canvas (Material 3 Container)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(24.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (isProcessing) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            if (isVi) "Đang tự động tách nền..." else "Segmenting subject...",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp
                        )
                    }
                } else if (previewBitmap != null) {
                    Image(
                        bitmap = previewBitmap!!.asImageBitmap(),
                        contentDescription = if (isVi) "Xem trước nhãn dán" else "Sticker preview",
                        modifier = Modifier
                            .fillMaxSize(0.85f)
                            .padding(16.dp)
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        IconButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .size(64.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        ) {
                            Icon(
                                Icons.Default.AddPhotoAlternate,
                                contentDescription = if (isVi) "Chọn hình ảnh" else "Choose photo",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            if (isVi) "Chọn hình ảnh từ thư viện để tạo nhãn dán" else "Select an image from gallery to create sticker",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }

                if (errorMessage != null) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (isVi) "Chọn ảnh khác từ Thư viện" else "Choose another photo", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Editing Options (Visible when photo is segmented)
            if (segmentedBitmap != null && !isProcessing) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        if (isVi) "Hiệu ứng nhãn dán (Phong cách iOS 17):" else "Sticker Effects (iOS 17 Style):",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(StickerEffect.entries) { effect ->
                            val isSelected = currentEffect == effect
                            val icon = when (effect) {
                                StickerEffect.ORIGINAL -> Icons.Default.Image
                                StickerEffect.OUTLINE -> Icons.Default.Palette
                                StickerEffect.COMIC -> Icons.Default.FilterDrama
                                StickerEffect.PUFFY -> Icons.Default.Grain
                                StickerEffect.SHINY -> Icons.Default.AutoAwesome
                            }

                            FilterChip(
                                selected = isSelected,
                                onClick = { currentEffect = effect },
                                leadingIcon = {
                                    Icon(
                                        icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        if (isVi) effect.title else effect.titleEn,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    labelColor = MaterialTheme.colorScheme.onSurface,
                                    iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            if (isVi) "Bóng đổ tự nhiên (Drop Shadow)" else "Drop Shadow",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp
                        )
                        Switch(
                            checked = enableShadow,
                            onCheckedChange = { enableShadow = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                uncheckedThumbColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Text(if (isVi) "Chọn ảnh khác" else "Change Photo", color = MaterialTheme.colorScheme.onSurface)
                        }

                        Button(
                            onClick = {
                                val currentPreview = previewBitmap ?: return@Button
                                scope.launch {
                                    isProcessing = true
                                    val trimmed = StickerOptimizer.trimAlpha(currentPreview)
                                    val optimized = StickerOptimizer.resizeForSticker(trimmed)
                                    val (png, webp) = StickerOptimizer.saveSticker(context, optimized)

                                    val entity = com.minslab.minsticker.data.StickerEntity(
                                        id = java.util.UUID.randomUUID().toString(),
                                        filePath = png.absolutePath,
                                        webpPath = webp.absolutePath,
                                        width = optimized.width,
                                        height = optimized.height
                                    )
                                    repository.insertSticker(entity)

                                    isProcessing = false
                                    onSavedSuccess()
                                }
                            },
                            modifier = Modifier.weight(1.5f),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (isVi) "Lưu nhãn dán" else "Save Sticker",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
