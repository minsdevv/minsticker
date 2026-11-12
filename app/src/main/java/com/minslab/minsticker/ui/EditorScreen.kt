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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minslab.minsticker.data.StickerRepository
import com.minslab.minsticker.keyboard.SampleStickerGenerator
import com.minslab.minsticker.segmentation.EdgeRefiner
import com.minslab.minsticker.segmentation.OutlineRenderer
import com.minslab.minsticker.segmentation.OutlineStyle
import com.minslab.minsticker.segmentation.StickerOptimizer
import com.minslab.minsticker.segmentation.SubjectSegmenter
import com.minslab.minsticker.ui.theme.M3Background
import com.minslab.minsticker.ui.theme.M3Error
import com.minslab.minsticker.ui.theme.M3ErrorContainer
import com.minslab.minsticker.ui.theme.M3OnBackground
import com.minslab.minsticker.ui.theme.M3OnErrorContainer
import com.minslab.minsticker.ui.theme.M3OnPrimary
import com.minslab.minsticker.ui.theme.M3OnPrimaryContainer
import com.minslab.minsticker.ui.theme.M3OnSurface
import com.minslab.minsticker.ui.theme.M3OnSurfaceVariant
import com.minslab.minsticker.ui.theme.M3Outline
import com.minslab.minsticker.ui.theme.M3OutlineVariant
import com.minslab.minsticker.ui.theme.M3Primary
import com.minslab.minsticker.ui.theme.M3PrimaryContainer
import com.minslab.minsticker.ui.theme.M3SurfaceContainer
import com.minslab.minsticker.ui.theme.M3SurfaceContainerHigh
import com.minslab.minsticker.ui.theme.M3SurfaceContainerHighest
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
    val repository = remember { StickerRepository(context) }

    var selectedUri by remember { mutableStateOf<Uri?>(initialUri) }
    var rawBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var segmentedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var currentOutlineStyle by remember { mutableStateOf(OutlineStyle.REGULAR) }
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
                previewBitmap = OutlineRenderer.renderOutline(refined, currentOutlineStyle, enableShadow)
            }.onFailure { err ->
                errorMessage = err.message ?: "Không thể tách chủ thể"
            }
        } catch (e: Exception) {
            errorMessage = "Lỗi đọc ảnh: ${e.message}"
        } finally {
            isProcessing = false
        }
    }

    // Update styled preview when outline or shadow toggles
    LaunchedEffect(currentOutlineStyle, enableShadow, segmentedBitmap) {
        val seg = segmentedBitmap ?: return@LaunchedEffect
        previewBitmap = OutlineRenderer.renderOutline(seg, currentOutlineStyle, enableShadow)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tạo Sticker", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = M3Background,
                    titleContentColor = M3OnBackground,
                    navigationIconContentColor = M3OnBackground
                )
            )
        },
        containerColor = M3Background
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
                    .background(M3SurfaceContainer, RoundedCornerShape(24.dp))
                    .border(1.dp, M3OutlineVariant.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (isProcessing) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = M3Primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Đang tách nền AI...", color = M3OnSurface, fontSize = 14.sp)
                    }
                } else if (previewBitmap != null) {
                    Image(
                        bitmap = previewBitmap!!.asImageBitmap(),
                        contentDescription = "Sticker Preview",
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
                                .background(M3Primary, CircleShape)
                        ) {
                            Icon(
                                Icons.Default.AddPhotoAlternate,
                                contentDescription = "Chọn ảnh",
                                tint = M3OnPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Chạm để chọn ảnh từ bộ sưu tập",
                            color = M3OnSurfaceVariant,
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
                            color = M3Error,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val sample = SampleStickerGenerator.createSampleBitmap()
                                segmentedBitmap = sample
                                errorMessage = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = M3PrimaryContainer,
                                contentColor = M3OnPrimaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Dùng hình mẫu thử nghiệm (Waydroid)", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Editing Options (Visible when photo is segmented)
            if (segmentedBitmap != null && !isProcessing) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Kiểu viền sticker (phong cách iOS 17):",
                        color = M3OnSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlineStyle.values().forEach { style ->
                            FilterChip(
                                selected = currentOutlineStyle == style,
                                onClick = { currentOutlineStyle = style },
                                label = { Text(style.title, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = M3Primary,
                                    selectedLabelColor = M3OnPrimary,
                                    containerColor = M3SurfaceContainerHigh,
                                    labelColor = M3OnSurface
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
                        Text("Đổ bóng nổi (Drop Shadow)", color = M3OnSurface, fontSize = 13.sp)
                        Switch(
                            checked = enableShadow,
                            onCheckedChange = { enableShadow = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = M3OnPrimary,
                                checkedTrackColor = M3Primary,
                                uncheckedTrackColor = M3SurfaceContainerHighest,
                                uncheckedThumbColor = M3Outline
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
                                containerColor = M3SurfaceContainerHigh,
                                contentColor = M3OnSurface
                            )
                        ) {
                            Text("Đổi ảnh", color = M3OnSurface)
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
                                    repository.ensureDefaultPack()
                                    com.minslab.minsticker.data.MinsDatabase.getInstance(context)
                                        .stickerDao().insertSticker(entity)

                                    isProcessing = false
                                    onSavedSuccess()
                                }
                            },
                            modifier = Modifier.weight(1.5f),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = M3Primary,
                                contentColor = M3OnPrimary
                            )
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = M3OnPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Lưu Sticker", color = M3OnPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
