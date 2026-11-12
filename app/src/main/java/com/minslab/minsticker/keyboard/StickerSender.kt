package com.minslab.minsticker.keyboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat
import java.io.File

object StickerSender {
    private const val TAG = "Minsticker-Sender"
    private const val FILE_PROVIDER_AUTHORITY = "com.minslab.minsticker.fileprovider"
    private const val ZALO_PACKAGE = "com.zing.zalo"

    fun getSupportedMimeTypes(editorInfo: EditorInfo?): List<String> {
        if (editorInfo == null) return emptyList()
        val mimeTypes = EditorInfoCompat.getContentMimeTypes(editorInfo)
        return mimeTypes.toList()
    }

    fun getFileUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, file)
    }

    /**
     * Smart Send Strategy:
     * - If target app is Zalo: Use Direct Share Intent with extra 'type' = 3 (Zalo Photo Sticker).
     * - If target app supports image MIME types: Use commitContent (instant in-chat send).
     * - Fallback: Copy to clipboard or generic Share.
     */
    fun sendSticker(
        context: Context,
        inputConnection: InputConnection?,
        editorInfo: EditorInfo?,
        file: File,
        preferredMime: String = "image/png"
    ) {
        val targetPkg = editorInfo?.packageName

        // 1. Specialized Handler for Zalo (discovered via Dex Reverse Engineering)
        if (targetPkg == ZALO_PACKAGE) {
            Log.i(TAG, "Routing to Zalo Photo Sticker Share Intent (type = 3)")
            sendViaZaloPhotoSticker(context, file, preferredMime)
            return
        }

        // 2. Standard Android IME commitContent for apps that declare support (WhatsApp, Telegram, etc.)
        val supportedMimes = getSupportedMimeTypes(editorInfo)
        val supportsTargetMime = supportedMimes.any {
            it.equals(preferredMime, ignoreCase = true) || it.equals("image/*", ignoreCase = true)
        }

        if (supportsTargetMime && inputConnection != null && editorInfo != null) {
            val sent = sendViaCommitContent(context, inputConnection, editorInfo, file, preferredMime)
            if (sent) return
        }

        // 3. Fallback: Copy to clipboard + Direct share
        Log.i(TAG, "Fallback to Clipboard & Share for package: $targetPkg")
        copyToClipboard(context, file, preferredMime)
    }

    /**
     * Send via Zalo Direct Share with extra "type" = 3 (activates Zalo's extra_photo_sticker).
     */
    fun sendViaZaloPhotoSticker(context: Context, file: File, mimeType: String) {
        try {
            val contentUri = getFileUri(context, file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, contentUri)
                // Discovered in Zalo TempShareViaView: bundle.getInt("type") == 3 -> extra_photo_sticker = true
                putExtra("type", 3)
                putExtra("STR_EXTRA_SHARED_FROM_OTHER_APP", true)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                setPackage(ZALO_PACKAGE)
            }
            context.grantUriPermission(ZALO_PACKAGE, contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(intent)
            Toast.makeText(context, "Đang mở Zalo Sticker...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch Zalo Photo Sticker Intent", e)
            // Fallback to regular share intent
            sendViaShareIntent(context, file, mimeType, ZALO_PACKAGE)
        }
    }

    /**
     * Standard IME commitContent API.
     */
    fun sendViaCommitContent(
        context: Context,
        inputConnection: InputConnection,
        editorInfo: EditorInfo,
        file: File,
        mimeType: String
    ): Boolean {
        return try {
            val contentUri = getFileUri(context, file)
            val contentInfo = InputContentInfoCompat(
                contentUri,
                android.content.ClipDescription("Sticker", arrayOf(mimeType)),
                null
            )

            var flags = 0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                flags = flags or InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION
            }

            val success = InputConnectionCompat.commitContent(
                inputConnection,
                editorInfo,
                contentInfo,
                flags,
                null
            )
            Log.i(TAG, "commitContent result: $success for $mimeType")
            if (success) {
                Toast.makeText(context, "Đã gửi sticker", Toast.LENGTH_SHORT).show()
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "Exception during commitContent", e)
            false
        }
    }

    /**
     * Copy sticker URI to clipboard.
     */
    fun copyToClipboard(context: Context, file: File, mimeType: String): Boolean {
        return try {
            val contentUri = getFileUri(context, file)
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newUri(context.contentResolver, "Sticker", contentUri)
            clipboard.setPrimaryClip(clipData)
            Toast.makeText(context, "Đã sao chép sticker vào Clipboard!", Toast.LENGTH_SHORT).show()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy to clipboard", e)
            false
        }
    }

    /**
     * Generic Share Intent.
     */
    fun sendViaShareIntent(context: Context, file: File, mimeType: String, targetPackage: String? = null) {
        try {
            val contentUri = getFileUri(context, file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (!targetPackage.isNullOrBlank()) {
                    setPackage(targetPackage)
                }
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch Share Intent", e)
            Toast.makeText(context, "Lỗi mở chia sẻ: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
