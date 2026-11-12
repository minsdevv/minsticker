package com.minslab.minsticker.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.provider.Settings
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ContentInfoCompat
import androidx.core.view.OnReceiveContentListener
import androidx.core.view.ViewCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.rememberAsyncImagePainter
import com.minslab.minsticker.data.StickerEntity
import com.minslab.minsticker.data.StickerRepository
import com.minslab.minsticker.ui.theme.M3Background
import com.minslab.minsticker.ui.theme.M3OnBackground
import com.minslab.minsticker.ui.theme.M3OnPrimary
import com.minslab.minsticker.ui.theme.M3OnPrimaryContainer
import com.minslab.minsticker.ui.theme.M3OnSecondary
import com.minslab.minsticker.ui.theme.M3OnSurface
import com.minslab.minsticker.ui.theme.M3OnSurfaceVariant
import com.minslab.minsticker.ui.theme.M3Outline
import com.minslab.minsticker.ui.theme.M3OutlineVariant
import com.minslab.minsticker.ui.theme.M3Primary
import com.minslab.minsticker.ui.theme.M3PrimaryContainer
import com.minslab.minsticker.ui.theme.M3Secondary
import com.minslab.minsticker.ui.theme.M3SecondaryContainer
import com.minslab.minsticker.ui.theme.M3Surface
import com.minslab.minsticker.ui.theme.M3SurfaceContainer
import com.minslab.minsticker.ui.theme.M3SurfaceContainerHigh
import com.minslab.minsticker.ui.theme.M3SurfaceContainerHighest
import com.minslab.minsticker.ui.theme.M3SurfaceContainerLowest
import com.minslab.minsticker.ui.theme.M3Tertiary
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
    val favoriteStickers by repository.favoriteStickersFlow.collectAsState(initial = emptyList())

    var activeTab by remember { mutableStateOf(0) }
    var selectedStickerForAction by remember { mutableStateOf<StickerEntity?>(null) }
    val testStickerList = remember { mutableStateListOf<Uri>() }

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
    val displayedStickers = if (activeTab == 1) favoriteStickers else stickers

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(M3Primary, M3Tertiary)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🎨", fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Minsticker",
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp,
                                color = M3OnBackground
                            )
                            Text(
                                "Google Material You • Sticker Keyboard",
                                fontSize = 11.sp,
                                color = M3OnSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                            imm.showInputMethodPicker()
                        }
                    ) {
                        Icon(
                            Icons.Default.Keyboard,
                            contentDescription = "Chọn bàn phím",
                            tint = M3Primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = M3Surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateStickerClick,
                icon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(22.dp)) },
                text = { Text("Tạo Sticker", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                containerColor = M3Primary,
                contentColor = M3OnPrimary,
                shape = RoundedCornerShape(20.dp)
            )
        },
        containerColor = M3Background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // Section 1: Dynamic Material You Onboarding Card
            item {
                MaterialOnboardingBanner(
                    isImeEnabled = isImeEnabled,
                    isImeSelected = isImeSelected,
                    onEnableClick = {
                        context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                    },
                    onSelectClick = {
                        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.showInputMethodPicker()
                    }
                )
            }

            // Section 2: In-App Keyboard Interactive Test Sandbox
            item {
                Spacer(modifier = Modifier.height(10.dp))
                KeyboardPreviewSandbox(
                    receivedStickers = testStickerList,
                    onStickerReceived = { uri ->
                        testStickerList.add(uri)
                    },
                    onClearHistory = {
                        testStickerList.clear()
                    }
                )
            }

            // Section 3: Sticker Library Header & Tabs
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Bộ sưu tập",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = M3OnSurface
                    )

                    // Tab Selector Pills (Google Pixel Material 3 Style)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(M3SurfaceContainerHigh)
                            .padding(2.dp)
                    ) {
                        val tabs = listOf("Tất cả (${stickers.size})", "❤️ Yêu thích (${favoriteStickers.size})")
                        tabs.forEachIndexed { index, title ->
                            val isSelected = activeTab == index
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isSelected) M3PrimaryContainer else Color.Transparent)
                                    .clickable { activeTab = index }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = title,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) M3OnPrimaryContainer else M3OnSurfaceVariant
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Section 4: Sticker Grid
            if (displayedStickers.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = M3SurfaceContainer),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                if (activeTab == 1) "❤️" else "✨",
                                fontSize = 42.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                if (activeTab == 1) "Chưa có sticker yêu thích" else "Chưa có sticker nào",
                                color = M3OnSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                if (activeTab == 1)
                                    "Chạm vào hình trái tim trên sticker bất kỳ để thêm vào mục yêu thích."
                                else
                                    "Bấm nút Tạo Sticker bên dưới để tách ảnh và tạo sticker đầu tiên!",
                                color = M3OnSurfaceVariant,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                val rows = displayedStickers.chunked(3)
                items(rows) { rowStickers ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (sticker in rowStickers) {
                            Box(modifier = Modifier.weight(1f)) {
                                MaterialStickerCard(
                                    sticker = sticker,
                                    onClick = {
                                        selectedStickerForAction = sticker
                                    },
                                    onToggleFavorite = {
                                        scope.launch {
                                            repository.toggleFavorite(sticker.id, !sticker.isFavorite)
                                        }
                                    }
                                )
                            }
                        }
                        val remaining = 3 - rowStickers.size
                        for (i in 0 until remaining) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // Sticker Action Modal Bottom Sheet
        selectedStickerForAction?.let { sticker ->
            val sheetState = rememberModalBottomSheetState()
            ModalBottomSheet(
                onDismissRequest = { selectedStickerForAction = null },
                sheetState = sheetState,
                containerColor = M3SurfaceContainerHigh,
                scrimColor = Color.Black.copy(alpha = 0.6f)
            ) {
                StickerDetailSheetContent(
                    sticker = sticker,
                    onToggleFavorite = {
                        scope.launch {
                            repository.toggleFavorite(sticker.id, !sticker.isFavorite)
                            selectedStickerForAction = null
                        }
                    },
                    onDelete = {
                        scope.launch {
                            repository.deleteSticker(sticker)
                            selectedStickerForAction = null
                        }
                    },
                    onClose = {
                        selectedStickerForAction = null
                    }
                )
            }
        }
    }
}

/**
 * Modern Material You Interactive Keyboard Preview Sandbox
 */
@Composable
private fun KeyboardPreviewSandbox(
    receivedStickers: List<Uri>,
    onStickerReceived: (Uri) -> Unit,
    onClearHistory: () -> Unit
) {
    val context = LocalContext.current
    var focusedEditText by remember { mutableStateOf<EditText?>(null) }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = M3SurfaceContainer),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Sandbox Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(M3Primary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Phòng Thử Nghiệm Bàn Phím",
                        fontWeight = FontWeight.Bold,
                        color = M3OnSurface,
                        fontSize = 14.sp
                    )
                }

                if (receivedStickers.isNotEmpty()) {
                    Text(
                        "Xóa (${receivedStickers.size})",
                        color = M3Primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onClearHistory() }
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Chạm vào ô bên dưới để mở bàn phím Minsticker và chạm gửi sticker để xem hiệu ứng!",
                color = M3OnSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Chat / Sticker Preview Feed
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(M3SurfaceContainerLowest)
                    .border(1.dp, M3OutlineVariant.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (receivedStickers.isEmpty()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("💬", fontSize = 24.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Chưa có sticker gửi thử nào",
                            color = M3OnSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Bật bàn phím & chạm gửi 1 sticker từ tab Tất cả!",
                            color = M3OnSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                } else {
                    LazyRow(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(receivedStickers) { uri ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(M3SurfaceContainerHigh)
                                    .padding(8.dp)
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(uri),
                                    contentDescription = "Test Sticker",
                                    modifier = Modifier.size(76.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Đã nhận ✓✓",
                                    color = M3Primary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Interactive Input Box with Rich Content Listener
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(M3SurfaceContainerHigh)
                    .border(1.dp, M3Outline.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                    .clickable {
                        focusedEditText?.let { et ->
                            et.requestFocus()
                            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                            imm.showSoftInput(et, InputMethodManager.SHOW_FORCED)
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Keyboard,
                    contentDescription = null,
                    tint = M3Primary,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                AndroidView(
                    factory = { ctx ->
                        EditText(ctx).apply {
                            focusedEditText = this
                            hint = "Chạm vào đây để mở bàn phím..."
                            textSize = 13f
                            setTextColor(AndroidColor.parseColor("#EEDFE2"))
                            setHintTextColor(AndroidColor.parseColor("#9D8C91"))
                            setBackgroundColor(AndroidColor.TRANSPARENT)
                            isSingleLine = true
                            isFocusable = true
                            isFocusableInTouchMode = true
                            setOnClickListener {
                                requestFocus()
                                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                                imm.showSoftInput(this, InputMethodManager.SHOW_FORCED)
                            }

                            val mimeTypes = arrayOf("image/png", "image/webp", "image/jpeg", "image/*")
                            ViewCompat.setOnReceiveContentListener(this, mimeTypes, object : OnReceiveContentListener {
                                override fun onReceiveContent(view: View, payload: ContentInfoCompat): ContentInfoCompat? {
                                    val clip = payload.clip
                                    for (i in 0 until clip.itemCount) {
                                        val uri = clip.getItemAt(i).uri
                                        if (uri != null) {
                                            onStickerReceived(uri)
                                        }
                                    }
                                    return payload
                                }
                            })
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = {
                        focusedEditText?.let { et ->
                            et.requestFocus()
                            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                            imm.showSoftInput(et, InputMethodManager.SHOW_FORCED)
                        }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Bật phím",
                        tint = M3Primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Material You Dynamic Onboarding Banner
 */
@Composable
private fun MaterialOnboardingBanner(
    isImeEnabled: Boolean,
    isImeSelected: Boolean,
    onEnableClick: () -> Unit,
    onSelectClick: () -> Unit
) {
    val cardBg = when {
        !isImeEnabled -> M3SurfaceContainerHigh
        !isImeSelected -> M3SurfaceContainerHighest
        else -> M3SurfaceContainer
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(22.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            when {
                !isImeEnabled -> {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("1", fontWeight = FontWeight.Black, color = M3Primary, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Bật bàn phím Minsticker", fontWeight = FontWeight.Bold, color = M3OnSurface, fontSize = 14.sp)
                        }
                        Text("Vào Cài đặt hệ thống để kích hoạt bàn phím", fontSize = 12.sp, color = M3OnSurfaceVariant)
                    }
                    Button(
                        onClick = onEnableClick,
                        colors = ButtonDefaults.buttonColors(containerColor = M3Primary, contentColor = M3OnPrimary),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("Bật ngay", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                !isImeSelected -> {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("2", fontWeight = FontWeight.Black, color = M3Secondary, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Chọn Minsticker làm bàn phím", fontWeight = FontWeight.Bold, color = M3OnSurface, fontSize = 14.sp)
                        }
                        Text("Chuyển bàn phím hiện tại sang Minsticker", fontSize = 12.sp, color = M3OnSurfaceVariant)
                    }
                    Button(
                        onClick = onSelectClick,
                        colors = ButtonDefaults.buttonColors(containerColor = M3Secondary, contentColor = M3OnSecondary),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("Chọn phím", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = M3Primary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Bàn phím Minsticker đã sẵn sàng!", fontWeight = FontWeight.Bold, color = M3Primary, fontSize = 14.sp)
                            Text("Hoạt động trong Zalo, WhatsApp, Telegram và phòng test bên dưới", fontSize = 11.sp, color = M3OnSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Material 3 Sticker Grid Item Card
 */
@Composable
private fun MaterialStickerCard(
    sticker: StickerEntity,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = M3SurfaceContainer),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = rememberAsyncImagePainter(File(sticker.filePath)),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize(0.85f)
                    .padding(4.dp)
            )

            // Heart / Favorite Icon Button
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(32.dp)
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = if (sticker.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Yêu thích",
                    tint = if (sticker.isFavorite) M3Primary else M3OnSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}

/**
 * Sticker Bottom Sheet Actions
 */
@Composable
private fun StickerDetailSheetContent(
    sticker: StickerEntity,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Large Preview
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(M3SurfaceContainerLowest)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = rememberAsyncImagePainter(File(sticker.filePath)),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onToggleFavorite,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    if (sticker.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = if (sticker.isFavorite) M3Primary else M3OnSurface,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (sticker.isFavorite) "Bỏ thích" else "Yêu thích", color = M3OnSurface, fontSize = 13.sp)
            }

            Button(
                onClick = {
                    val file = File(sticker.filePath)
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "com.minslab.minsticker.fileprovider",
                        file
                    )
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Chia sẻ sticker"))
                    onClose()
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = M3Primary, contentColor = M3OnPrimary),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, tint = M3OnPrimary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Chia sẻ", color = M3OnPrimary, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = onDelete,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = M3Primary)
        ) {
            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Xóa Sticker này", fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
