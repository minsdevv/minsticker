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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
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
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF181825),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF181825)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Preview Canvas (Checkerboard/Dark Container)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF313244), RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFF45475A), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (isProcessing) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFF89B4FA))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Đang tách nền AI...", color = Color.White, fontSize = 14.sp)
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
                                .background(Color(0xFF89B4FA), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.AddPhotoAlternate,
                                contentDescription = "Chọn ảnh",
                                tint = Color.Black,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Chạm để chọn ảnh từ bộ sưu tập",
                            color = Color(0xFFA6ADC8),
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
                            color = Color(0xFFF38BA8),
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
                                containerColor = Color(0xFF89B4FA),
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp)
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
                        color = Color.White,
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
                                    selectedContainerColor = Color(0xFF89B4FA),
                                    selectedLabelColor = Color.Black,
                                    containerColor = Color(0xFF313244),
                                    labelColor = Color.White
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
                        Text("Đổ bóng nổi (Drop Shadow)", color = Color.White, fontSize = 13.sp)
                        Switch(
                            checked = enableShadow,
                            onCheckedChange = { enableShadow = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF89B4FA)
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
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF45475A))
                        ) {
                            Text("Đổi ảnh", color = Color.White)
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
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA6E3A1))
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Lưu Sticker", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
