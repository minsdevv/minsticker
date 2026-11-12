package com.minslab.minsticker.segmentation

import android.content.Context
import android.graphics.Bitmap
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object SubjectSegmenter {

    private val options = SubjectSegmenterOptions.Builder()
        .enableForegroundBitmap()
        .build()

    private val segmenter by lazy {
        SubjectSegmentation.getClient(options)
    }

    /**
     * Tries to trigger on-demand download of the ML Kit module via Google Play Services.
     */
    suspend fun requestDownloadModule(context: Context): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val moduleInstallClient = ModuleInstall.getClient(context)
            val request = ModuleInstallRequest.newBuilder()
                .addApi(segmenter)
                .build()
            val response = moduleInstallClient.installModules(request).await()
            Result.success(response.areModulesAlreadyInstalled())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Segments the foreground subjects (person, pet, object) from the input bitmap.
     * Returns a transparent ARGB_8888 Bitmap containing only the foreground subject.
     */
    suspend fun segment(bitmap: Bitmap, context: Context? = null): Result<Bitmap> = withContext(Dispatchers.Default) {
        try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val result = segmenter.process(inputImage).await()
            val foreground = result.foregroundBitmap
            if (foreground != null) {
                Result.success(foreground)
            } else {
                Result.failure(IllegalStateException("Không tìm thấy chủ thể rõ ràng trong ảnh"))
            }
        } catch (e: Exception) {
            val msg = e.message ?: ""
            if (msg.contains("optional module", ignoreCase = true) || msg.contains("Waiting for", ignoreCase = true)) {
                if (context != null) {
                    try {
                        requestDownloadModule(context)
                    } catch (_: Exception) {}
                }
                Result.failure(
                    IllegalStateException(
                        "Gói AI tách nền đang được Google Play Services tải về lần đầu. Vui lòng kết nối Wi-Fi/4G và đợi khoảng 15-30 giây rồi thử lại. (Lưu ý: máy ảo Waydroid không có dịch vụ Google Play Services, cần kiểm tra trên điện thoại Android thật)."
                    )
                )
            } else {
                Result.failure(e)
            }
        }
    }
}
