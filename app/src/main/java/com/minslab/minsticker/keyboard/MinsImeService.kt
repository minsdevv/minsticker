package com.minslab.minsticker.keyboard

import android.content.Context
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import coil.compose.rememberAsyncImagePainter
import com.minslab.minsticker.MainActivity
import com.minslab.minsticker.data.StickerEntity
import com.minslab.minsticker.data.StickerRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class MinsImeService : InputMethodService(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    companion object {
        private const val TAG = "Minsticker-IME"
    }

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private var currentPackageName by mutableStateOf("Chưa xác định")
    private var samplePngFile: File? = null
    private var sampleWebpFile: File? = null

    private val repository by lazy { StickerRepository(this) }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        // Ensure sample stickers exist
        try {
            val (png, webp) = SampleStickerGenerator.ensureSampleStickers(this)
            samplePngFile = png
            sampleWebpFile = webp
        } catch (e: Exception) {
            Log.e(TAG, "Error generating sample sticker", e)
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        currentPackageName = info?.packageName ?: "Không rõ"
        val mimes = StickerSender.getSupportedMimeTypes(info)
        Log.i(TAG, "=== Focus Input: Package=$currentPackageName, Mimes=$mimes ===")
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }

    override fun onCreateInputView(): View {
        val window = window?.window
        if (window != null) {
            val decorView = window.decorView
            decorView.setViewTreeLifecycleOwner(this)
            decorView.setViewTreeViewModelStoreOwner(this)
            decorView.setViewTreeSavedStateRegistryOwner(this)
        }

        return ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@MinsImeService)
            setViewTreeViewModelStoreOwner(this@MinsImeService)
            setViewTreeSavedStateRegistryOwner(this@MinsImeService)

            setContent {
                MaterialTheme {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(295.dp),
                        color = Color(0xFF13131D)
                    ) {
                        StickerKeyboardContent()
                    }
                }
            }
        }
    }

    @Composable
    private fun StickerKeyboardContent() {
        val view = LocalView.current
        val scope = rememberCoroutineScope()
        var selectedTabIndex by remember { mutableStateOf(0) }

        val allStickers by repository.allStickersFlow.collectAsState(initial = emptyList())
        val favStickers by repository.favoriteStickersFlow.collectAsState(initial = emptyList())
        val recentStickers by repository.recentStickersFlow.collectAsState(initial = emptyList())

        val currentStickers = when (selectedTabIndex) {
            1 -> favStickers
            2 -> recentStickers
            else -> allStickers
        }

        val appBadgeInfo = remember(currentPackageName) {
            when {
                currentPackageName.contains("zalo") -> "Zalo" to Color(0xFF0068FF)
                currentPackageName.contains("whatsapp") -> "WhatsApp" to Color(0xFF25D366)
                currentPackageName.contains("telegram") -> "Telegram" to Color(0xFF2AABEE)
                currentPackageName.contains("messenger") -> "Messenger" to Color(0xFF0099FF)
                currentPackageName.contains("instagram") -> "Instagram" to Color(0xFFE1306C)
                currentPackageName.contains("viber") -> "Viber" to Color(0xFF7360F2)
                currentPackageName.contains("messages") -> "Messages" to Color(0xFF89B4FA)
                else -> "Sticker" to Color(0xFFA6ADC8)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF13131D))
        ) {
            // Subtle top divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0x18FFFFFF))
            )

            // Top Control Bar (iOS 17 Sticker Drawer Style)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Switch Keyboard Button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF20202F))
                        .clickable {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            switchToNextInputMethod(false)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Keyboard,
                        contentDescription = "Đổi phím",
                        tint = Color(0xFFA6ADC8),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Target App Badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1C1C2A))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(appBadgeInfo.second)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = appBadgeInfo.first,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // iOS-Style Segmented Category Tabs
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1E1E2E))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val tabs = listOf("Tất cả", "Yêu thích", "Gần đây")
                    tabs.forEachIndexed { index, title ->
                        val isSelected = selectedTabIndex == index
                        val bg by animateColorAsState(
                            targetValue = if (isSelected) Color(0xFF35364C) else Color.Transparent,
                            label = "tabBg"
                        )
                        val textColor by animateColorAsState(
                            targetValue = if (isSelected) Color.White else Color(0xFF8E90A6),
                            label = "tabText"
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(bg)
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    selectedTabIndex = index
                                }
                                .padding(vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = textColor,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Fast Add Sticker Button ("+")
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF20202F))
                        .clickable {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            val intent = Intent(this@MinsImeService, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                putExtra("create_sticker", true)
                            }
                            startActivity(intent)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Tạo sticker mới",
                        tint = Color(0xFF89B4FA),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Backspace Button (supports tap + long-press repeat)
                var isBackspaceHeld by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isBackspaceHeld) Color(0xFF35364C) else Color(0xFF20202F))
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    isBackspaceHeld = true
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)

                                    val job = scope.launch {
                                        delay(400) // Initial hold delay
                                        while (isActive) {
                                            sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                            delay(50) // Fast repeat
                                        }
                                    }
                                    tryAwaitRelease()
                                    job.cancel()
                                    isBackspaceHeld = false
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Xóa",
                        tint = Color(0xFFA6ADC8),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Sticker Gallery / Empty State Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                if (currentStickers.isEmpty()) {
                    // Contextual Empty States
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        when (selectedTabIndex) {
                            1 -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("❤️", fontSize = 28.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Chưa có sticker yêu thích",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        "Mở ứng dụng Minsticker để gắn tim sticker bạn thích",
                                        color = Color(0xFFA6ADC8),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            2 -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🕒", fontSize = 28.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Chưa có sticker gần đây",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        "Chạm vào sticker ở tab Tất cả để gửi ngay",
                                        color = Color(0xFFA6ADC8),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            else -> {
                                // All stickers empty: Show sample sticker + invite to create
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    samplePngFile?.let { file ->
                                        if (file.exists()) {
                                            StickerItemView(
                                                file = file,
                                                onClick = {
                                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                                    StickerSender.sendSticker(
                                                        this@MinsImeService,
                                                        currentInputConnection,
                                                        currentInputEditorInfo,
                                                        file
                                                    )
                                                },
                                                modifier = Modifier.size(76.dp)
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                "Chạm sticker mẫu ở trên để gửi thử!",
                                                color = Color(0xFF89B4FA),
                                                fontSize = 11.sp
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                            val intent = Intent(this@MinsImeService, MainActivity::class.java).apply {
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                                putExtra("create_sticker", true)
                                            }
                                            startActivity(intent)
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF89B4FA),
                                            contentColor = Color.Black
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Tạo Sticker Từ Ảnh", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Floating Stickers Grid (iOS 17 Floating Style)
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(currentStickers, key = { it.id }) { sticker ->
                            val file = File(sticker.filePath)
                            StickerItemView(
                                file = file,
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    if (file.exists()) {
                                        StickerSender.sendSticker(
                                            this@MinsImeService,
                                            currentInputConnection,
                                            currentInputEditorInfo,
                                            file
                                        )
                                        scope.launch {
                                            repository.markUsed(sticker.id)
                                        }
                                    }
                                },
                                modifier = Modifier.aspectRatio(1f)
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Floating Sticker Item with smooth iOS-style press bounce animation.
     */
    @Composable
    private fun StickerItemView(
        file: File,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()

        val scale by animateFloatAsState(
            targetValue = if (isPressed) 0.88f else 1.0f,
            animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
            label = "stickerScale"
        )

        val bgAlpha by animateFloatAsState(
            targetValue = if (isPressed) 0.18f else 0.05f,
            label = "stickerBgAlpha"
        )

        Box(
            modifier = modifier
                .scale(scale)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = bgAlpha))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = rememberAsyncImagePainter(file),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize(0.88f)
                    .padding(2.dp)
            )
        }
    }
}
