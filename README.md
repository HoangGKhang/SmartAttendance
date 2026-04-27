# SmartAttendance

SmartAttendance là ứng dụng Android hỗ trợ quản lý điểm danh lớp học. Ứng dụng có luồng sử dụng cho giảng viên và sinh viên, kết hợp mã QR, kiểm tra vị trí và xác thực khuôn mặt để giảm thao tác điểm danh thủ công.

## Tính năng

- Đăng nhập và đăng ký tài khoản bằng Firebase Authentication.
- Màn hình giảng viên để quản lý lớp học và phiên điểm danh.
- Tạo mã QR cho từng phiên điểm danh.
- Sinh viên quét mã QR để tham gia phiên điểm danh.
- Đăng ký khuôn mặt và xác thực khuôn mặt bằng CameraX, ML Kit Face Detection và TensorFlow Lite.
- Lưu dữ liệu điểm danh bằng Cloud Firestore.
- Hỗ trợ quyền vị trí để phục vụ kiểm tra điểm danh theo vị trí.

## Công nghệ sử dụng

- Kotlin
- Android Gradle Plugin 8.13.2
- Gradle Kotlin DSL
- Firebase Authentication
- Cloud Firestore
- ZXing QR scanner/generator
- CameraX
- ML Kit Face Detection
- TensorFlow Lite
- AndroidX, Material Components, RecyclerView, ConstraintLayout

## Cấu trúc project

```text
.
+-- app/
|   +-- src/main/java/com/example/smartattendance/
|   |   +-- ai/                 # Xử lý nhận diện khuôn mặt
|   |   +-- data/model/         # Các model dữ liệu
|   |   +-- data/repository/    # Repository làm việc với Firebase
|   |   +-- ui/auth/            # Màn hình đăng nhập, đăng ký, splash
|   |   +-- ui/lecturer/        # Các màn hình cho giảng viên
|   |   +-- ui/student/         # Các màn hình cho sinh viên
|   +-- src/main/assets/        # Model TensorFlow Lite
|   +-- build.gradle.kts
+-- gradle/libs.versions.toml
+-- build.gradle.kts
+-- settings.gradle.kts
```

## Yêu cầu

- Android Studio
- JDK 17
- Android SDK API 36
- Firebase project đã bật Authentication và Firestore

## Cài đặt

1. Clone repository.

```bash
git clone <repository-url>
cd smartattendance
```

2. Mở project bằng Android Studio.

3. Tạo hoặc chọn một Firebase project.

4. Đăng ký Android app trong Firebase với package name:

```text
com.example.smartattendance
```

5. Tải file `google-services.json` từ Firebase Console và đặt vào:

```text
app/google-services.json
```

6. Bật Firebase Authentication và Cloud Firestore trong Firebase Console.

7. Sync Gradle rồi chạy app trên máy ảo hoặc thiết bị Android thật.

## Build

Có thể build bằng Android Studio hoặc chạy Gradle ở thư mục gốc của project:

```bash
./gradlew assembleDebug
```

Trên Windows:

```bash
gradlew.bat assembleDebug
```

## Quyền Android

Ứng dụng sử dụng các quyền sau:

- `INTERNET`: kết nối Firebase.
- `CAMERA`: quét QR và đăng ký/xác thực khuôn mặt.
- `ACCESS_FINE_LOCATION`: kiểm tra vị trí khi điểm danh.
- `ACCESS_COARSE_LOCATION`: quyền vị trí tương đối khi cần.

## Lưu ý khi up GitHub

Không nên commit các file cấu hình theo máy, file sinh ra khi build hoặc file chứa thông tin nhạy cảm. Các file/thư mục cần ignore gồm:

- `.gradle/`, `.kotlin/`, `build/`, `app/build/`: file build tự sinh, có thể tạo lại.
- `local.properties`: chứa đường dẫn Android SDK trên máy cá nhân.
- `.idea/workspace.xml` và các file cache IDE: trạng thái cá nhân của Android Studio.
- `*.jks`, `*.keystore`: khóa ký app, không được để lộ.
- `.env`, `.env.*`: biến môi trường hoặc secret local.
- `app/google-services.json`: cấu hình Firebase thật, nên ignore nếu repository public.

Nếu ignore `google-services.json`, mỗi người clone project cần tự tải file này từ Firebase Console và đặt vào đúng vị trí.

## Chạy test

Chạy unit test:

```bash
./gradlew test
```

Chạy Android instrumented test:

```bash
./gradlew connectedAndroidTest
```

## Ghi chú

- Model TensorFlow Lite đang nằm tại `app/src/main/assets/face_model.tflite`.
- Project đã cấu hình không nén file `.tflite` để model có thể được load đúng khi chạy app.
- Package name trong Firebase phải khớp với `applicationId` trong `app/build.gradle.kts`.
