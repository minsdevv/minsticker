# Minsticker 🎨

> **Bàn phím Sticker phong cách iOS 17 dành cho Android**
> Cắt chủ thể ảnh thông minh (cutout) on-device, tạo viền trắng nổi và gửi sticker trong suốt thẳng vào **Zalo**, **WhatsApp**, **Telegram**, **Messenger**.

---

## ✨ Tính năng nổi bật

* ✂️ **Tách nền thông minh on-device**: Tích hợp Google ML Kit Subject Segmentation, nhận diện và tách người, thú cưng, đồ vật với độ chính xác cao.
* 🪄 **Hiệu ứng viền iOS 17**: Tùy chỉnh độ dày viền trắng nổi (Mỏng, Vừa, Dày) và đổ bóng mềm (drop shadow) chuẩn phong cách sticker Apple iOS 17.
* 💬 **Gửi sticker thẳng vào Zalo & WhatsApp**:
  * **WhatsApp / Telegram**: Gửi inline mượt mà qua chuẩn Android `commitContent`.
  * **Zalo**: Tự động kích hoạt cơ chế `extra_photo_sticker` đưa sticker trong suốt vào ô chat.
* ⌨️ **Bàn phím Sticker cao cấp (IME)**:
  * Thiết kế **Floating Stickers** không viền cứng, tạo cảm giác sticker nổi tự nhiên trên nền phím tối.
  * Hiệu ứng lò xo đàn hồi (Spring Bounce Animation) và phản hồi xúc giác (Haptic Feedback) khi chạm.
  * Phím xóa Backspace thông minh: Chạm để xóa 1 ký tự, giữ để xóa nhanh liên tục.
  * Nút `+` thần tốc ngay trên bàn phím: Mở nhanh trình cắt ảnh mà không cần thoát ra màn hình chính.
  * Tabs phân loại dạng viên thuốc: **Tất cả**, **Yêu thích**, **Gần đây**.
* ⚡ **Siêu nhẹ & Riêng tư tuyệt đối**:
  * Dung lượng APK Release chỉ **~3.0 MB** nhờ tối ưu hóa mã nguồn và R8 compiler.
  * Hoạt động **100% Offline**, lưu trữ cục bộ qua Room Database, không thu thập dữ liệu hay analytics.

---

## 🛠️ Công nghệ sử dụng

* **Ngôn ngữ**: Kotlin
* **Giao diện**: Jetpack Compose (Material 3)
* **Xử lý AI**: Google ML Kit Subject Segmentation (On-device)
* **Cơ sở dữ liệu**: Android Jetpack Room (KSP)
* **Bàn phím hệ thống**: Android `InputMethodService` + Compose View integration
* **Xử lý ảnh**: Android Graphics Canvas, RenderScript Blur/Feathering, Coil Async Image Loader

---

## 🚀 Cài đặt & Build

### Yêu cầu môi trường
* Android Studio Ladybug / Koala hoặc JDK 17+
* Android SDK 35 (minSdk 29, targetSdk 35)

### Build APK

```bash
# Clone dự án
git clone git@github.com:minsdevv/minsticker.git
cd minsticker

# Build bản Release tối ưu (3MB)
./gradlew assembleRelease

# Cài đặt qua ADB
adb install -r app/build/outputs/apk/release/app-release.apk
```

---

## 📄 Bản quyền

Phát hành theo giấy phép [MIT License](LICENSE).
Bản quyền (c) 2026 MinsDev / MinsLab.
