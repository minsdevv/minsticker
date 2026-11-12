package com.minslab.minsticker.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.minslab.minsticker.data.StickerEntity
import com.minslab.minsticker.data.StickerRepository
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onCreateStickerClick: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val repository = remember { StickerRepository(context) }
    val stickers by repository.allStickersFlow.collectAsState(initial = emptyList())

    var selectedStickerForAction by remember { mutableStateOf<StickerEntity?>(null) }

    fun checkImeStatus(): Pair<Boolean, Boolean> {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val isEnabled = imm.enabledInputMethodList.any { it.packageName == context.packageName }
        val currentIme = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        val isSelected = currentIme?.contains(context.packageName) == true
        return Pair(isEnabled, isSelected)
    }

    var imeState by remember { mutableStateOf(checkImeStatus()) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                imeState = checkImeStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val (isImeEnabled, isImeSelected) = imeState

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Minsticker", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("Bàn phím Sticker iOS cho Android", fontSize = 12.sp, color = Color(0xFFA6ADC8))
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                            imm.showInputMethodPicker()
                        }
                    ) {
                        Icon(Icons.Default.Keyboard, contentDescription = "Chọn bàn phím", tint = Color(0xFF89B4FA))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF181825),
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateStickerClick,
                containerColor = Color(0xFF89B4FA),
                contentColor = Color.Black,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tạo Sticker mới", modifier = Modifier.size(28.dp))
            }
        },
        containerColor = Color(0xFF181825)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Onboarding & IME Activation Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        !isImeEnabled -> Color(0xFF313244)
                        !isImeSelected -> Color(0xFF2A2B3C)
                        else -> Color(0xFF1E2D2F)
                    }
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    when {
                        !isImeEnabled -> {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Bước 1: Bật bàn phím Minsticker", fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 14.sp)
                                Text("Vào Cài đặt hệ thống để kích hoạt bàn phím", fontSize = 12.sp, color = Color(0xFFA6ADC8))
                            }
                            Button(
                                onClick = {
                                    context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA6E3A1)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Bật ngay", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        !isImeSelected -> {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Bước 2: Chọn Minsticker làm bàn phím", fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 14.sp)
                                Text("Chuyển bàn phím hiện tại sang Minsticker", fontSize = 12.sp, color = Color(0xFF89B4FA))
                            }
                            Button(
                                onClick = {
                                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                                    imm.showInputMethodPicker()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF89B4FA)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Chọn phím", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        else -> {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("✓ Bàn phím Minsticker đã sẵn sàng!", fontWeight = FontWeight.SemiBold, color = Color(0xFFA6E3A1), fontSize = 14.sp)
                                Text("Mở Zalo hoặc WhatsApp để gửi sticker ngay", fontSize = 12.sp, color = Color(0xFFA6ADC8))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sticker Gallery Grid
            if (stickers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Chưa có sticker nào", color = Color(0xFFA6ADC8), fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Nhấn nút + bên dưới để tách ảnh thành sticker!", color = Color(0xFF6C7086), fontSize = 13.sp)
                    }
                }
            } else {
                Text(
                    text = "Bộ sưu tập (${stickers.size} sticker)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(stickers, key = { it.id }) { sticker ->
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .background(Color(0xFF313244), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFF45475A), RoundedCornerShape(12.dp))
                                .combinedClickable(
                                    onClick = { selectedStickerForAction = sticker },
                                    onLongClick = { selectedStickerForAction = sticker }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(File(sticker.filePath)),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize(0.82f)
                                    .padding(6.dp)
                            )

                            if (sticker.isFavorite) {
                                Icon(
                                    Icons.Default.Favorite,
                                    contentDescription = "Yêu thích",
                                    tint = Color(0xFFF38BA8),
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Sticker Details & Actions Dialog
    selectedStickerForAction?.let { sticker ->
        AlertDialog(
            onDismissRequest = { selectedStickerForAction = null },
            title = { Text("Quản lý Sticker", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .background(Color(0xFF313244), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(File(sticker.filePath)),
                            contentDescription = null,
                            modifier = Modifier.size(120.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Định dạng: PNG + WebP trong suốt (512x512)", color = Color(0xFFA6ADC8), fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            repository.toggleFavorite(sticker.id, sticker.isFavorite)
                            selectedStickerForAction = null
                        }
                    }
                ) {
                    Icon(
                        if (sticker.isFavorite) Icons.Default.FavoriteBorder else Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color(0xFFFAB387)
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(if (sticker.isFavorite) "Bỏ thích" else "Yêu thích", color = Color(0xFFFAB387))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            repository.deleteSticker(sticker)
                            selectedStickerForAction = null
                        }
                    }
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFF38BA8))
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("Xóa", color = Color(0xFFF38BA8))
                }
            },
            containerColor = Color(0xFF1E1E2E)
        )
    }
}
