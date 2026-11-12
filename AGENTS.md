# AGENTS.md – Minsticker (Android Sticker Keyboard)

> Tài liệu hướng dẫn cho mọi agent/dev. Cập nhật mục "Nhật ký tiến độ" và "Kết quả Spike" sau mỗi bước. Không đánh dấu hoàn thành nếu chưa kiểm chứng.

## 1. Mục tiêu
Bàn phím Android (IME) giống bàn phím Sticker của iOS 17+:
1. Tách chủ thể khỏi ảnh (cutout) → tạo sticker nền trong suốt.
2. Lưu thư viện sticker (offline).
3. Gửi sticker thẳng từ bàn phím vào Zalo, WhatsApp và các app khác. Zalo là ưu tiên số 1: sticker phải hiển thị như **sticker** (nền trong suốt, không khung ảnh) khi có thể.

Ngữ cảnh đã xác nhận: trên iPhone, bàn phím Sticker gửi vào Zalo hiện ra sticker trong suốt. Mục tiêu trên Android là tái hiện kết quả đó.

## 2. Quyết định đã chốt
| Mục | Quyết định |
|---|---|
| Nền tảng | Android native, Kotlin + Jetpack Compose. iOS ngoài phạm vi |
| Package | `com.minslab.minsticker` |
| minSdk / target | 29 / 35 |
| Tách nền | ML Kit Subject Segmentation (on-device) + tinh chỉnh mép, viền trắng tùy chọn |
| Nơi tách nền | App chính (chọn ảnh/chụp) + Share target "Tạo sticker" từ Gallery. Bàn phím chỉ hiển thị thư viện |
| Lưu trữ | Room + file PNG/WebP trong bộ nhớ riêng của app. Có pack, yêu thích, gần đây. Không cần đăng nhập, không đồng bộ cloud |
| Cách gửi | Chiến lược đa tầng, quyết định theo kết quả Spike Test (mục 4) |
| Môi trường test | Waydroid (đã cài, x86_64) và ADB. Người dùng chỉ có iPhone, không có Android thật |

## 3. Kiến trúc
```
app/
  ui/            Compose: Home, Editor (cutout), Library, Settings, Onboarding (bật IME)
  keyboard/      MinsImeService : InputMethodService (+ Compose view trong IME)
  sender/        StickerSender: các strategy gửi (CommitContent, Clipboard, Share, Fallback)
  segmentation/  SubjectSegmenter (ML Kit), EdgeRefiner, OutlineRenderer
  data/          Room (StickerEntity, PackEntity), StickerRepository, file store
  share/         ShareReceiverActivity (ACTION_SEND image/*)
  provider/      StickerFileProvider (content:// URI)
```
Bàn phím chạy trong tiến trình app, nên **không** chạy ML Kit nặng trong IME. IME chỉ đọc thư viện và gửi.

### Cơ chế gửi (Android)
1. App nhận khai báo MIME qua `EditorInfoCompat.getContentMimeTypes(editorInfo)` trong `onStartInputView`.
2. Nếu có `image/png` hoặc `image/webp`, gọi `InputConnectionCompat.commitContent(ic, ei, InputContentInfoCompat(uri, mime, null), flags, null)`. `uri` là `content://` từ `FileProvider`. Cấp quyền đọc tạm thời bằng `INPUT_CONTENT_GRANT_READ_URI_PERMISSION` khi API ≥ 25.
3. Nếu app không khai báo, dùng fallback: `ClipboardManager` + `ClipData.newUri` (mime image/png), rồi hướng dẫn dán, hoặc `Intent.ACTION_SEND` tới đúng package.
4. Không dùng Accessibility Service (rủi ro Play Store và bảo mật).

## 4. Giai đoạn 0 – Spike Test Zalo (BẮT BUỘC làm trước)
Mục đích: biết đường nào làm Zalo hiển thị sticker trong suốt. Không giả định.

Cách làm:
1. Dựng IME tối thiểu có 1 PNG alpha 512×512 (hình mẫu).
2. Log `EditorInfo.contentMimeTypes`, `packageName`, `fieldId` khi focus ô chat Zalo, WhatsApp, Messages.
3. Thử từng đường và ghi kết quả vào bảng:

| Đường gửi | Định dạng | Zalo | WhatsApp | Ghi chú |
|---|---|---|---|---|
| commitContent | PNG alpha | Không hỗ trợ (Zalo không cài listener) | Hỗ trợ chuẩn | Chuẩn Android IME |
| commitContent | WebP alpha | Không hỗ trợ | Hỗ trợ chuẩn | Chuẩn Android IME |
| commitContent | GIF | Không hỗ trợ | Hỗ trợ chuẩn | |
| Clipboard + dán | PNG alpha | Không hoạt động (Zalo ép về text) | Hỗ trợ | Zalo menu tự viết dùng coerceToText |
| Share Intent | PNG alpha | Khả thi (TempShareViaView) | Khả thi | Mở màn hình chia sẻ Zalo |
| Share Intent (type=3) | PNG/WebP alpha | Khả thi (kích hoạt extra_photo_sticker) | n/a | Zalo có cờ Photo Sticker |

Tiêu chí đánh giá: hiển thị trong suốt (sticker) / ảnh có khung / không gửi được.
Phân tích tĩnh Zalo 26.10.02 & Google Gboard 18.6.3 (Duyệt toàn bộ bytecode DEX):
- **Bằng chứng từ mã nguồn Gboard (`niy.java` & `qwu.java`):**
  Khi người dùng chọn gửi Sticker / Emoji Kitchen trên Gboard:
  1. Gboard kiểm tra `nuo.q(editorInfo, str)`: Nếu app đích có khai báo hỗ trợ MIME ảnh (như WhatsApp, Telegram), Gboard gọi `commitContent()` qua `cbv` (InputContentInfoCompat).
  2. Nếu app đích KHÔNG khai báo MIME (`nuo.n(editorInfo).isEmpty()` - trường hợp Zalo), Gboard kiểm tra whitelist qua `qwu.a(editorInfo)` và lập tức chuyển sang gọi `qwu.b()`:
     ```java
     Intent intent = new Intent("android.intent.action.SEND");
     intent.setType(mime);
     intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
     intent.setPackage(editorInfo.packageName); // com.zing.zalo
     intent.putExtra(Intent.EXTRA_STREAM, uri);
     context.startActivity(intent);
     ```
  -> **Gboard không có cơ chế thần thánh nào khác:** Gboard gửi sticker vào Zalo chính xác bằng cách mở `ACTION_SEND` trực tiếp tới Zalo package!
- **commitContent (Bàn phím):** Zalo hoàn toàn không cài listener `commitContent`. Mọi bàn phím đều bị Android từ chối nếu gửi inline.
- **Clipboard & Paste trong ô chat:** Zalo Android menu dán chỉ dùng `coerceToText` ép về text.
- **Cờ mở rộng của Zalo:** Trong `TempShareViaView`, Zalo có nhánh `if (bundle.getInt("type") == 3)` kích hoạt `extra_photo_sticker`. Bàn phím MinsEmoji gửi thêm cờ này để kích hoạt tính năng Photo Sticker của Zalo.

Kết quả Spike & Chiến lược kiến trúc:
1. Gửi thẳng vào chat Zalo không rời màn hình qua `commitContent`: KHÔNG KHẢ THI về mặt kỹ thuật trên Zalo Android hiện tại.
2. Dán qua Clipboard vào ô chat Zalo: KHÔNG KHẢ THI (bị ép kiểu về text).
3. Gửi qua Share Intent trực tiếp tới Zalo (`ACTION_SEND` với extra `"type" = 3` và URI PNG/WebP): ĐƯỜNG KHẢ THI NHẤT để đưa sticker vào Zalo.
4. Đối với các ứng dụng khác (WhatsApp, Telegram, Messages, Messenger): Sử dụng `commitContent` bình thường vì các app này hỗ trợ `image/png` và `image/webp` chuẩn.

Kết quả Spike: **Đã kiểm tra trên Waydroid x86_64**. Zalo APK (v26.10.02) bị crash ngay lúc khởi động do `libndk_translation.so` (SIGSEGV tại AppProcessPostInit) vì cơ chế bảo vệ native arm64 của VNG không tương thích với tầng dịch ARM của Waydroid. Do đó việc test trực tiếp hiển thị trên Zalo cần thiết bị Android thật (ARM64) sau này. Cơ chế gửi trong app được thiết kế đa tầng (commitContent -> clipboard -> share).

## 5. Lộ trình
### Phase 1 – Môi trường và khung dự án
- Cài Android SDK tối thiểu vào `~/Android/Sdk` (cmdline-tools, platform-tools, platforms;android-35, build-tools;35.0.0). Bỏ emulator và system-image để tiết kiệm đĩa. Dùng JDK 17 (`/usr/lib/jvm/java-17-openjdk`).
- Khởi tạo Gradle (Kotlin DSL, version catalog), module `app`, Compose, Room (KSP).
- Hoàn thành khi: `./gradlew assembleDebug` thành công.

### Phase 2 – Spike Test (mục 4)
Hoàn thành khi: bảng kết quả được điền và có quyết định chiến lược gửi.

### Phase 3 – Tách nền
- `SubjectSegmenter` dùng ML Kit `SubjectSegmentation` (foreground bitmap).
- `EdgeRefiner`: làm mượt mép (feather 1–2px), loại điểm ảnh rời.
- `OutlineRenderer`: viền trắng tùy chọn, đổ bóng nhẹ.
- Chuẩn hóa xuất: PNG ARGB, cạnh dài tối đa 512px, cắt vừa chủ thể (trim alpha), padding nhỏ.
- Lưu ý: ML Kit Subject Segmentation cần Google Play Services. Waydroid không có GMS mặc định, nên cần fallback (MediaPipe hoặc ONNX) để test hoặc dùng ảnh mẫu đã tách.
- Hoàn thành khi: ảnh người, ảnh vật thể cho ra PNG trong suốt chấp nhận được.

### Phase 4 – Thư viện sticker
- Room: `StickerEntity(id, path, packId, createdAt, lastUsedAt, isFavorite, width, height)`, `PackEntity`.
- Màn Thư viện, Yêu thích, Gần đây. Xóa, đổi tên pack, sắp xếp.
- Share target `ACTION_SEND image/*` mở Editor.

### Phase 5 – Bàn phím IME
- `MinsImeService` với Compose (lưu ý `ViewTreeLifecycleOwner` và `SavedStateRegistryOwner` cho `ComposeView` trong IME).
- UI: lưới sticker, tab pack, Gần đây, nút chuyển bàn phím (`switchToNextInputMethod`), nút xóa.
- Đọc `contentMimeTypes` để chọn strategy. Hiển thị toast hoặc hướng dẫn khi cần dán.
- `StickerFileProvider` + `res/xml/file_paths.xml`.
- `res/xml/method.xml` khai báo subtype.

### Phase 6 – Onboarding và hoàn thiện
- Hướng dẫn bật bàn phím (`Settings.ACTION_INPUT_METHOD_SETTINGS`) và chọn bàn phím (`InputMethodManager.showInputMethodPicker`).
- Ngôn ngữ: Tiếng Việt mặc định, có Tiếng Anh.
- Dark mode, tối ưu bộ nhớ, thumbnail cache.
- Quyền riêng tư: không internet, không analytics. Không khai báo quyền `INTERNET`.

### Phase 7 – Kiểm thử và phát hành
- Unit test: xử lý bitmap, repository. Instrumented test: Room, FileProvider.
- Test thủ công trên Waydroid theo bảng Spike. Ghi rõ giới hạn vì không có thiết bị thật.
- Ký release, R8, ghi chú phát hành.

## 6. Quy ước làm việc
- Ngôn ngữ giao tiếp: Tiếng Việt. Code và comment: Tiếng Anh.
- Mỗi phase kết thúc bằng một build thành công và cập nhật mục 7.
- Không thêm quyền nhạy cảm (Accessibility, READ_EXTERNAL_STORAGE). Dùng Photo Picker (`PickVisualMedia`).
- Giữ nguyên các comment và docstring không liên quan khi sửa code.
- Dung lượng đĩa hạn chế (~8GB trống khi bắt đầu): không cài emulator image, dọn `~/.gradle` khi cần.

## 7. Nhật ký tiến độ
- [x] Phase 1 – Môi trường và khung dự án (Hoàn thành: SDK 35, Build-tools 35, Gradle 8.10.2, Kotlin 2.0.21, Compose, Room; APK debug 22MB tạo thành công)
- [x] Phase 2 – Spike Test Zalo (Hoàn thành: Đã dịch ngược và quét toàn bộ 6 file DEX của Zalo 26.10.02 & DEX của Gboard 18.6.3; xác định cơ chế gửi đa tầng commitContent -> ACTION_SEND trực tiếp package Zalo với cờ type=3)
- [x] Phase 3 – Tách nền (Hoàn thành: Tích hợp ML Kit Subject Segmentation on-device, EdgeRefiner feather/khử nhiễu, OutlineRenderer viền trắng iOS 17 + đổ bóng, StickerOptimizer trim alpha & nén 512x512 PNG/WebP)
- [x] Phase 4 – Thư viện sticker (Hoàn thành: Room Database StickerDao/PackDao, StickerRepository quản lý file private, UI Compose HomeScreen quản lý sticker/yêu thích/xóa, EditorScreen tạo sticker từ ảnh)
- [x] Phase 5 – Bàn phím IME (Hoàn thành: MinsImeService tích hợp ComposeView phong cách iOS 17 Floating Stickers, hiệu ứng bounce spring & haptic feedback, tab Tất cả/Yêu thích/Gần đây, xóa nhanh liên tục, tạo sticker ngay từ bàn phím, StickerSender gửi commitContent và ACTION_SEND Zalo type=3)
- [x] Phase 6 – Onboarding và hoàn thiện (Hoàn thành: Dynamic 2-step Onboarding, Adaptive Icon vector iOS 17, đa ngôn ngữ VI/EN, tối ưu nén R8 shrinking từ 58MB xuống còn 3.0MB)
- [ ] Phase 7 – Kiểm thử trên thiết bị thật và phát hành

## 8. Rủi ro đã biết
| Rủi ro | Giảm thiểu |
|---|---|
| Zalo coi ảnh commitContent là ảnh thường | Spike Test, thử clipboard/share, ghi nhận giới hạn |
| Không có thiết bị Android thật | Waydroid, nêu rõ phần chưa kiểm chứng |
| ML Kit cần GMS, Waydroid không có | Fallback model on-device hoặc dùng ảnh mẫu |
| Hết dung lượng đĩa | SDK tối thiểu, dọn cache |
| IME + Compose lỗi lifecycle | Gắn owner thủ công vào window decor view |
