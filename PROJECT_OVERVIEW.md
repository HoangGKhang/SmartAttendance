# SmartAttendance — Thông tin chi tiết về project hiện tại

> Tài liệu này mô tả **hiện trạng repository tại ngày 10/10/2026 (Asia/Bangkok)**.
> Nội dung được tổng hợp từ source code, cấu hình Gradle, resource Android, lịch sử Git và tài liệu database trên nhánh `origin/docs`.
> Không có giá trị credential nào được ghi vào tài liệu này.

---

## 1. Tóm tắt điều hành

SmartAttendance là ứng dụng Android phục vụ bài toán điểm danh lớp học. Tầm nhìn của hệ thống là hỗ trợ:

- Xác thực người dùng bằng Supabase Auth.
- Phân quyền theo vai trò `STUDENT`, `LECTURER`, `ADMIN`.
- Quản lý lớp học, buổi học và phiên điểm danh.
- Điểm danh bằng Dynamic QR, GPS và nhận diện khuôn mặt.
- Theo dõi lịch sử và trạng thái điểm danh.
- Có khả năng đồng bộ dữ liệu với Moodle.

Tuy nhiên, **source code trên nhánh hiện tại mới hoàn thành phần nền tảng xác thực và danh sách lớp học**. Những chức năng QR, GPS, Face Recognition, buổi học, phiên điểm danh, lịch sử điểm danh, Moodle và giao diện Admin chưa được triển khai trong Android app.

Mốc hiện tại có thể mô tả ngắn gọn như sau:

```text
Supabase setup
      ↓
Email/password authentication
      ↓
Restore/observe session
      ↓
Load profile
      ↓
Route by role
      ↓
Display active courses for Student/Lecturer
      ↓
Course detail and attendance features: chưa triển khai
```

---

## 2. Snapshot repository

### 2.1 Git

| Thuộc tính | Giá trị hiện tại |
|---|---|
| Repository remote | `https://github.com/HoangGKhang/SmartAttendance.git` |
| Nhánh đang checkout | `feature/course` |
| Upstream | `origin/feature/course` |
| Đồng bộ với upstream | Có — ahead `0`, behind `0` tại thời điểm khảo sát |
| Working tree trước khi tạo tài liệu này | Sạch |
| HEAD | `fc0c8ce37cc8a7c3420909d86fc4d2037f9be9fd` |
| Commit ngắn | `fc0c8ce` |
| Commit gần nhất | `feat: display lecturer courses` |
| Thời gian commit | `2026-10-10T11:28:28+07:00` |
| Git tag | Chưa có tag |

Các nhánh local quan sát được:

| Nhánh | Commit | Ý nghĩa theo lịch sử |
|---|---:|---|
| `main` | `5d98a90` | Đến bước thêm `CourseRepository` |
| `feature/auth` | `aa5cff9` | Hoàn thành auth và điều hướng theo role |
| `feature/course` | `fc0c8ce` | Thêm state, adapter và màn hình course cho Student/Lecturer |
| `develop` | `a3d2e7e` | Setup Supabase Android |
| `feature/supabase-setup` | `a3d2e7e` | Setup Supabase Android |

Nhánh remote `origin/docs` chứa bộ tài liệu thiết kế database ở commit `6b1393d`, nhưng các file đó **không nằm trong working tree của nhánh `feature/course`**.

### 2.2 Quy mô source hiện tại

Thống kê dưới đây tính trên `app/src`, bao gồm source test và XML launcher mặc định:

| Loại file | Số file | Tổng số dòng gần đúng |
|---|---:|---:|
| Kotlin (`.kt`) | 22 | 1.381 |
| XML (`.xml`) | 16 | 650 |

Trong 22 file Kotlin:

- 20 file thuộc main application.
- 1 file unit test mẫu.
- 1 file instrumented test mẫu.

Project hiện có:

- 1 Gradle module: `:app`.
- 5 layout chính: login, main placeholder, student home, lecturer home, course item.
- 4 Activity: `LoginActivity`, `StudentHomeActivity`, `LecturerHomeActivity`, `MainActivity`.
- 3 ViewModel: auth, profile, course.
- 3 Repository: auth, profile, course.
- 3 domain/data model: profile, role, course.
- 2 DTO: profile, course.
- 1 singleton Supabase client.

---

## 3. Công nghệ và cấu hình build

### 3.1 Stack chính

| Thành phần | Công nghệ |
|---|---|
| Nền tảng client | Android native |
| Ngôn ngữ | Kotlin `2.2.10` |
| UI | XML Views + ViewBinding |
| Design system | Material Components / Material 3 theme |
| Kiến trúc presentation | Activity + ViewModel + StateFlow |
| Backend platform | Supabase |
| Authentication | Supabase Auth, email/password |
| Data access | Supabase PostgREST |
| HTTP engine | Ktor Android client |
| Build system | Gradle Kotlin DSL |
| Gradle wrapper | `9.4.1` |
| Android Gradle Plugin | `9.2.1` |
| JVM Gradle daemon toolchain | Java `21` |
| Java source/target compatibility | Java `11` |

### 3.2 Android SDK và package

| Thuộc tính | Giá trị |
|---|---|
| Namespace | `com.example.smartattendance` |
| Application ID | `com.example.smartattendance` |
| `compileSdk` | 37 |
| `targetSdk` | 36 |
| `minSdk` | 26 |
| Version code | 1 |
| Version name | `1.0` |
| App label | `SmartAttendance` |
| Launcher Activity | `.ui.auth.LoginActivity` |

Lưu ý tên Gradle root vẫn là `My Application`, theme vẫn dùng tên `Theme.MyApplication`, và test package mẫu vẫn là `com.example.myapplication`. Đây là dấu vết còn lại từ template Android Studio, chưa được đồng bộ hoàn toàn với tên SmartAttendance.

### 3.3 Dependency trực tiếp

| Dependency | Version | Vai trò |
|---|---:|---|
| AndroidX Core KTX | `1.19.0` | API Kotlin tiện ích cho Android |
| AndroidX AppCompat | `1.8.0` | Activity và compatibility layer |
| AndroidX Activity KTX | `1.13.0` | `viewModels`, lifecycle/activity KTX |
| ConstraintLayout | `2.2.2` | Layout chính |
| Material Components | `1.14.0` | MaterialButton, TextInputLayout, Card |
| Supabase Auth | `3.6.0` | Login, logout, session |
| Supabase PostgREST | `3.6.0` | Query bảng Supabase/PostgreSQL |
| Ktor Client Android | `3.4.3` | HTTP engine cho Supabase SDK |
| JUnit 4 | `4.13.2` | Local unit test |
| AndroidX Test JUnit | `1.3.0` | Instrumented test |
| Espresso Core | `3.7.0` | UI test |

`desugar-jdk-libs` version `2.1.5` có khai báo trong version catalog nhưng chưa được nối vào `coreLibraryDesugaring` trong `app/build.gradle.kts`. Với `minSdk 26`, `java.time.LocalDate` đã khả dụng trực tiếp nên hiện tại đây chưa phải blocker rõ ràng.

### 3.4 Build features

- `viewBinding = true`.
- `buildConfig = true`.
- Release build hiện `isMinifyEnabled = false`.
- ProGuard file chỉ là template, chưa có rule riêng.
- Chưa có product flavor.
- Chưa thấy signing config release.
- Chưa thấy CI/CD workflow trong repository.
- Chưa thấy convention plugin hoặc multi-module architecture.

---

## 4. Cấu hình môi trường cục bộ

Ứng dụng đọc các giá trị sau từ file `local.properties`:

```properties
sdk.dir=...
SUPABASE_URL=...
SUPABASE_PUBLISHABLE_KEY=...
```

Tại thời điểm khảo sát, cả ba key đều tồn tại và có giá trị không rỗng. Giá trị cụ thể không được đưa vào tài liệu.

Các giá trị Supabase được đưa vào generated `BuildConfig`:

```text
BuildConfig.SUPABASE_URL
BuildConfig.SUPABASE_PUBLISHABLE_KEY
```

Sau đó `SupabaseProvider` dùng hai giá trị này để khởi tạo một `SupabaseClient` duy nhất và cài hai plugin:

- `Auth`
- `Postgrest`

`local.properties` đã được ignore ở cả dạng `/local.properties` và `local.properties`.

Nguyên tắc bảo mật quan trọng:

- Android app chỉ nên chứa Supabase publishable/anon key.
- Không được đưa database password, secret key hoặc service-role key vào app.
- Mọi giới hạn truy cập dữ liệu phải dựa vào JWT + RLS hoặc trusted backend/Edge Function; không dựa vào việc key nằm trong APK.

---

## 5. Kiến trúc hiện tại

### 5.1 Sơ đồ layer

```text
┌─────────────────────────────────────────────────────────────┐
│ UI layer                                                    │
│ LoginActivity                                               │
│ StudentHomeActivity              LecturerHomeActivity       │
│ CourseAdapter                                               │
└──────────────────────────────┬──────────────────────────────┘
                               │ observe StateFlow
┌──────────────────────────────▼──────────────────────────────┐
│ Presentation state                                          │
│ AuthViewModel + AuthUiState                                 │
│ ProfileViewModel + ProfileUiState                           │
│ CourseViewModel + CourseUiState                             │
└──────────────────────────────┬──────────────────────────────┘
                               │ suspend calls / Result<T>
┌──────────────────────────────▼──────────────────────────────┐
│ Data layer                                                  │
│ AuthRepository                                              │
│ ProfileRepository                                           │
│ CourseRepository                                            │
│ ProfileDto / CourseDto → Profile / Course                  │
└──────────────────────────────┬──────────────────────────────┘
                               │ Supabase Kotlin SDK
┌──────────────────────────────▼──────────────────────────────┐
│ Remote platform                                             │
│ Supabase Auth                                               │
│ Supabase PostgREST → PostgreSQL + RLS                       │
└─────────────────────────────────────────────────────────────┘
```

### 5.2 Đặc điểm kiến trúc

- Không dùng Jetpack Compose.
- Không dùng Fragment.
- Không dùng Navigation Component.
- Không có domain/use-case layer riêng.
- Không có dependency injection framework như Hilt/Koin/Dagger.
- Dependency được tạo bằng default constructor trong ViewModel/Repository.
- `SupabaseProvider.client` là singleton toàn cục.
- UI state dùng `StateFlow`.
- Activity thu thập flow bằng `repeatOnLifecycle(Lifecycle.State.STARTED)`.
- Repository trả về `Result<T>` thay vì ném exception trực tiếp lên ViewModel.
- DTO chịu trách nhiệm mapping tên cột snake_case sang Kotlin camelCase.
- ViewBinding loại bỏ phần lớn `findViewById`.

---

## 6. Luồng ứng dụng thực tế

### 6.1 Khởi động và khôi phục session

`LoginActivity` là launcher Activity.

Khi mở app:

1. Inflate `ActivityLoginBinding`.
2. Gắn listener cho nút đăng nhập.
3. Observe `AuthUiState`.
4. Observe `Supabase Auth sessionStatus`.
5. Observe `ProfileUiState`.
6. Trong lúc Supabase khởi tạo session, màn hình hiển thị loading.
7. Nếu session hợp lệ, `ProfileViewModel.loadCurrentUserProfile()` được gọi.
8. Profile được tải từ bảng `profiles` theo `auth.uid()`.
9. App điều hướng theo role.

### 6.2 Login

Luồng bấm nút login:

```text
Nhập email/password
        ↓
Kiểm tra rỗng và password >= 6 ký tự
        ↓
AuthViewModel.login()
        ↓
AuthRepository.login()
        ↓
supabase.auth.signInWith(Email)
        ↓
Supabase sessionStatus = Authenticated
        ↓
Load profiles row của user
        ↓
Điều hướng theo role
```

Validation hiện có:

- Email không được rỗng.
- Password không được rỗng.
- Password phải có ít nhất 6 ký tự.

Validation chưa có:

- Không kiểm tra định dạng email.
- Không chuẩn hóa hoặc kiểm tra domain email.
- Không có rate-limit phía UI.
- Không có forgot password, register hoặc email verification UI.

### 6.3 Điều hướng theo role

| Role | Kết quả |
|---|---|
| `STUDENT` | Mở `StudentHomeActivity`, đóng `LoginActivity` |
| `LECTURER` | Mở `LecturerHomeActivity`, đóng `LoginActivity` |
| `ADMIN` | Hiển thị lỗi “Giao diện Admin chưa được triển khai” |

`MainActivity` có layout “Hello World!” nhưng không phải launcher và không được dùng trong flow hiện tại. `LoginActivity.openMainActivity()` cũng tồn tại nhưng không được gọi.

### 6.4 Tải danh sách lớp học

Cả `StudentHomeActivity` và `LecturerHomeActivity` đều:

1. Khởi tạo `CourseAdapter`.
2. Gắn `LinearLayoutManager` cho RecyclerView.
3. Observe session để quay lại Login khi hết phiên hoặc logout.
4. Observe `CourseUiState`.
5. Gọi `CourseViewModel.loadCourses()` trong `onCreate`.
6. `CourseRepository` query bảng `courses`.
7. Query chỉ thêm điều kiện `is_active = true`.
8. Kết quả DTO được map sang `Course`.
9. UI hiển thị loading, danh sách, empty state hoặc error state.

App **không filter `user_id` hoặc role trực tiếp trong query course**. Thiết kế hiện tại dựa vào RLS của Supabase để chỉ trả về course mà user được phép xem. Điều này hợp lý nếu RLS đang được deploy đúng, nhưng sẽ là rủi ro dữ liệu nếu policy bị thiếu hoặc sai.

### 6.5 Logout

```text
Nút Đăng xuất
      ↓
AuthViewModel.logout()
      ↓
supabase.auth.signOut()
      ↓
sessionStatus = NotAuthenticated
      ↓
Student/Lecturer Activity mở LoginActivity và finish()
```

Nếu logout thất bại, `AuthViewModel` cập nhật error state nhưng hai Home Activity hiện không observe `AuthUiState`, vì vậy lỗi logout không được hiển thị tại màn hình Home.

---

## 7. Mô hình dữ liệu đang dùng trong Android

### 7.1 `UserRole`

```text
STUDENT
LECTURER
ADMIN
```

`ProfileDto.role` là chuỗi từ database. Mapper chuyển chuỗi sang uppercase bằng `Locale.ROOT` rồi gọi `UserRole.valueOf(...)`.

Hệ quả:

- Database có thể trả `student`, `STUDENT`, `Student` và vẫn map được.
- Bất kỳ role ngoài ba giá trị trên sẽ gây `IllegalArgumentException` và làm load profile thất bại.

### 7.2 `Profile`

| Field | Kotlin type | Nguồn database | Ý nghĩa |
|---|---|---|---|
| `id` | `String` | `id` | UUID user/profile |
| `studentCode` | `String?` | `student_code` | Mã sinh viên; nullable để dùng chung Lecturer/Admin |
| `fullName` | `String` | `full_name` | Họ tên |
| `role` | `UserRole` | `role` | Vai trò toàn hệ thống |
| `moodleUserId` | `Long?` | `moodle_user_id` | ID Moodle dự phòng cho tích hợp |

### 7.3 `Course`

| Field | Kotlin type | Nguồn database | Ý nghĩa |
|---|---|---|---|
| `id` | `String` | `id` | UUID lớp học |
| `moodleCourseId` | `Long?` | `moodle_course_id` | ID khóa học Moodle |
| `courseCode` | `String` | `course_code` | Mã môn, ví dụ `CS114` |
| `classCode` | `String` | `class_code` | Mã lớp, ví dụ `CS114.R11` |
| `name` | `String` | `name` | Tên môn/lớp |
| `semester` | `String` | `semester` | Học kỳ |
| `academicYear` | `String` | `academic_year` | Năm học |
| `startDate` | `LocalDate?` | `start_date` | Ngày bắt đầu |
| `endDate` | `LocalDate?` | `end_date` | Ngày kết thúc |
| `isActive` | `Boolean` | `is_active` | Trạng thái hoạt động |

`CourseDto` nhận ngày dưới dạng `String?` và map bằng `LocalDate.parse`. Vì vậy dữ liệu phải dùng định dạng ISO-8601 tương thích, thông thường là `YYYY-MM-DD`.

Các field non-null trong DTO phải luôn hiện diện và không null trong response. Nếu schema hoặc view thay đổi không tương thích, toàn bộ thao tác decode danh sách có thể thất bại.

---

## 8. UI state và cách render

### 8.1 `AuthUiState`

Là data class gồm:

- `isLoading`
- `isLoginSuccess`
- `errorMessage`

Sau login thành công, `isLoginSuccess` được đặt thành `true`, nhưng UI chỉ gọi `loginHandled()` để reset flag. Việc điều hướng thật sự đến từ luồng `sessionStatus → load profile → role`, không dựa trực tiếp vào flag login success.

### 8.2 `ProfileUiState`

Sealed interface gồm:

- `Idle`
- `Loading`
- `Success(profile)`
- `Error(message)`

`ProfileViewModel` không tải lại nếu state đang là `Loading` hoặc đã là `Success`. Điều này giúp tránh query lặp khi session phát lại trạng thái authenticated trong cùng lifecycle ViewModel.

### 8.3 `CourseUiState`

Sealed interface gồm:

- `Idle`
- `Loading`
- `Success(courses)`
- `Error(message)`

Student và Lecturer có logic render gần như giống hệt:

| State | UI |
|---|---|
| `Idle` | Giữ trạng thái danh sách mặc định |
| `Loading` | Hiện ProgressBar, ẩn RecyclerView |
| `Success` có dữ liệu | Submit list và hiện RecyclerView |
| `Success` rỗng | Hiện empty message theo role |
| `Error` | Hiện error message, ẩn RecyclerView |

Không có retry button, swipe-to-refresh hoặc refresh thủ công.

---

## 9. Chi tiết từng thành phần source

### 9.1 Root/build files

#### `settings.gradle.kts`

- Đặt tên project là `My Application`.
- Khai báo module `:app`.
- Dùng Google Maven, Maven Central và Gradle Plugin Portal.
- Dùng Foojay resolver convention `1.0.0`.
- Ép dependency repository ở settings bằng `FAIL_ON_PROJECT_REPOS`.

#### `build.gradle.kts`

- Chỉ khai báo Android application plugin và Kotlin serialization plugin ở root với `apply false`.

#### `gradle/libs.versions.toml`

- Là version catalog duy nhất.
- Quản lý version AndroidX, test, Supabase, Ktor và desugaring.

#### `gradle.properties`

- Gradle daemon heap tối đa 2 GB.
- UTF-8.
- Kotlin code style chính thức.
- Parallel build chưa bật.

#### `app/build.gradle.kts`

- Đọc `local.properties` bằng `Properties`.
- Tạo hai BuildConfig field cho Supabase.
- Bật ViewBinding và BuildConfig.
- Cấu hình SDK, version và dependency.
- Release không minify.

### 9.2 Remote/data layer

#### `SupabaseProvider.kt`

- Singleton khởi tạo eager khi được truy cập lần đầu.
- Client dùng URL và publishable key từ BuildConfig.
- Cài Auth và Postgrest.
- Chưa cài Realtime, Storage hoặc Functions plugin.

#### `AuthRepository.kt`

- `login(email, password)` gọi email sign-in và trả `Result<Unit>`.
- Re-throw `CancellationException` trong login.
- `logout()` gọi `signOut()` và trả `Result<Unit>`.
- `logout()` bắt `Exception` chung, hiện có khả năng bắt cả coroutine cancellation.

#### `ProfileRepository.kt`

- Query bảng `profiles` theo `id = userId`.
- Dùng `decodeSingleOrNull<ProfileDto>()`.
- Trả lỗi riêng nếu không có profile hoặc RLS không cho đọc.
- Re-throw `CancellationException`.

#### `CourseRepository.kt`

- Query bảng `courses` với danh sách cột tường minh.
- Filter duy nhất: `is_active = true`.
- Decode thành `CourseDto`, sau đó map sang `Course`.
- Bắt `Exception` chung và trả `Result.failure`.
- Không re-throw `CancellationException` như hai repository còn lại.
- Không sort rõ ràng ở phía query; thứ tự hiển thị phụ thuộc response mặc định của backend.

### 9.3 Presentation/UI layer

#### `LoginActivity.kt`

- File Kotlin lớn nhất, khoảng 330 dòng.
- Gộp input validation, render state, session observation, profile observation và navigation.
- Log `fullName` và `role` sau khi tải profile thành công.
- Có method `openMainActivity()` không dùng.
- Raw exception message từ backend có thể được hiển thị trực tiếp cho người dùng.

#### `StudentHomeActivity.kt`

- Hiển thị tiêu đề “Lớp học của tôi”.
- Hiển thị danh sách course.
- Có logout.
- Click course chưa làm gì; callback chỉ chứa TODO comment.

#### `LecturerHomeActivity.kt`

- Hiển thị tiêu đề “Lớp học phụ trách”.
- Cấu trúc gần như trùng với Student Home.
- Click course chưa làm gì.

#### `CourseAdapter.kt`

- RecyclerView Adapter dùng ViewBinding.
- Hiển thị class code, course name, semester/năm học và khoảng ngày.
- Nếu thiếu một trong hai ngày bắt đầu/kết thúc thì ẩn toàn bộ chuỗi ngày.
- Ngày hiển thị trực tiếp theo `LocalDate.toString()` dạng ISO.
- `submitList` thay list và gọi `notifyDataSetChanged()`; chưa dùng `ListAdapter`/`DiffUtil`.

#### `MainActivity.kt`

- Chỉ inflate layout “Hello World!”.
- Không nằm trong flow người dùng hiện tại.

### 9.4 Resource layer

- `activity_login.xml`: form email/password, error text, login button, progress bar.
- `activity_student_home.xml`: tiêu đề, logout, RecyclerView, loading, empty, error.
- `activity_lecturer_home.xml`: tương tự Student nhưng wording theo giảng viên.
- `item_course.xml`: MaterialCard hiển thị thông tin course.
- `activity_main.xml`: placeholder “Hello World!”.
- `strings.xml`: mới chứa chuỗi cho login và app name.
- Nhiều chuỗi ở Student/Lecturer/Course Adapter vẫn hard-code trong XML/Kotlin, chưa được đưa vào string resource.
- Theme là `Theme.Material3.DayNight.NoActionBar` nhưng tên style còn là `MyApplication`.
- Icon launcher vẫn là icon template Android mặc định.
- Backup/data extraction rules vẫn là template, chưa có include/exclude cụ thể.

---

## 10. Android Manifest và bề mặt ứng dụng

Manifest hiện khai báo:

- Quyền `android.permission.INTERNET`.
- `android:allowBackup="true"`.
- Hỗ trợ RTL.
- `LoginActivity` exported và là launcher.
- Ba Activity còn lại không exported.

Danh sách Activity:

| Activity | Exported | Trạng thái sử dụng |
|---|---:|---|
| `LoginActivity` | `true` | Launcher, đang dùng |
| `StudentHomeActivity` | `false` | Đang dùng cho Student |
| `LecturerHomeActivity` | `false` | Đang dùng cho Lecturer |
| `MainActivity` | `false` | Placeholder, không dùng trong flow |

Chưa có:

- Camera permission.
- Fine/coarse location permission.
- Notification permission.
- File/storage permission.
- Deep link.
- Service, receiver hoặc content provider tùy chỉnh.

Điều này phù hợp với việc QR/GPS/Face chưa được triển khai.

---

## 11. Thiết kế database lịch sử

Phần này lấy từ commit tài liệu `6b1393d` trên `origin/docs`. Đây là **thiết kế được ghi nhận trong Git**, không phải bằng chứng rằng schema production hiện tại chắc chắn giống hoàn toàn. Repository hiện không chứa migration SQL trên nhánh `feature/course`, và khảo sát này không kết nối trực tiếp vào Supabase để xác minh schema đã deploy.

### 11.1 Quan hệ tổng quát

```text
auth.users
    │ 1:1
    ▼
profiles
    │
    ├──────────────┐
    ▼              │
course_members ◄── courses
                   │
                   ▼
             class_meetings
                   │
                   ├── attendance_sessions
                   └── attendance_records
```

### 11.2 `profiles`

| Cột | Kiểu/constraint chính |
|---|---|
| `id` | UUID PK, FK đến `auth.users(id)`, cascade delete |
| `student_code` | `varchar(20)`, unique, nullable |
| `full_name` | `varchar(150)`, not null |
| `role` | `STUDENT` / `LECTURER` / `ADMIN` |
| `moodle_user_id` | bigint, unique, nullable |
| `created_at` | timestamptz, default now |
| `updated_at` | timestamptz, default now |

### 11.3 `courses`

| Cột | Kiểu/constraint chính |
|---|---|
| `id` | UUID PK |
| `moodle_course_id` | bigint unique, nullable |
| `course_code` | `varchar(20)`, not null |
| `class_code` | `varchar(50)`, not null |
| `name` | `varchar(200)`, not null |
| `semester` | `varchar(20)`, not null |
| `academic_year` | `varchar(20)`, not null |
| `start_date`, `end_date` | date, nullable, start <= end |
| `is_active` | boolean, default true |
| `created_at`, `updated_at` | timestamptz |

Unique nghiệp vụ: `(class_code, semester, academic_year)`.

### 11.4 `course_members`

| Cột | Ý nghĩa |
|---|---|
| `id` | UUID PK |
| `course_id` | FK đến `courses`, cascade delete |
| `user_id` | FK đến `profiles`, cascade delete |
| `member_role` | `STUDENT` hoặc `LECTURER` |
| `is_active` | Thành viên còn hiệu lực |
| `joined_at`, `created_at` | Thời gian |

Unique nghiệp vụ: `(course_id, user_id)`.

### 11.5 `class_meetings`

| Cột | Ý nghĩa |
|---|---|
| `id` | UUID PK |
| `course_id` | Course sở hữu buổi học |
| `meeting_number` | Số thứ tự buổi |
| `meeting_date` | Ngày học |
| `title` | Tiêu đề tùy chọn |
| `start_time`, `end_time` | Giờ bắt đầu/kết thúc |
| `room` | Phòng |
| `status` | `SCHEDULED`, `COMPLETED`, `CANCELLED` |
| `created_at`, `updated_at` | Thời gian |

Unique nghiệp vụ: `(course_id, meeting_number)`.

### 11.6 `attendance_sessions`

| Nhóm | Các cột chính |
|---|---|
| Định danh | `id`, `meeting_id`, `created_by` |
| Vòng đời | `status` (`DRAFT`, `OPEN`, `CLOSED`), `opened_at`, `closed_at`, `expires_at` |
| QR | `qr_secret`, `qr_refresh_seconds` |
| GPS | `latitude`, `longitude`, `radius_meters` |
| Rule | `require_qr`, `require_gps`, `require_face` |
| Audit time | `created_at`, `updated_at` |

Thiết kế tách `class_meeting` khỏi `attendance_session`, vì một buổi học có thể mở nhiều phiên điểm danh.

### 11.7 `attendance_records`

| Nhóm | Các cột chính |
|---|---|
| Định danh | `id`, `meeting_id`, `student_id`, `session_id` |
| Kết quả | `status` (`PRESENT`, `ABSENT`), `checked_in_at` |
| Verification | `qr_verified`, `gps_verified`, `face_verified` |
| GPS evidence | `gps_latitude`, `gps_longitude`, `gps_distance_meters` |
| Face evidence | `face_score` trong khoảng `[-1, 1]` |
| Audit time | `created_at`, `updated_at` |

Unique nghiệp vụ: `(meeting_id, student_id)`, tức một sinh viên có một attendance record cho mỗi buổi học; retry không tạo record mới mà cập nhật record hiện có.

### 11.8 RLS theo tài liệu lịch sử

Thiết kế ghi nhận:

- Student xem profile của chính mình.
- Lecturer có thể xem profile thành viên thuộc lớp mình dạy.
- User chỉ xem course mà mình là active member.
- User chỉ xem meeting/session thuộc course được phép truy cập.
- Student chỉ xem attendance record của mình.
- Lecturer xem attendance của lớp mình phụ trách.
- Client `authenticated` chỉ được đọc trực tiếp.
- Insert/update/delete dữ liệu nghiệp vụ quan trọng bị revoke khỏi Android client.
- Tác vụ create meeting, open/close session và check-in dự kiến đi qua trusted backend hoặc Supabase Edge Function.

Các helper function lịch sử nằm trong schema `private` để giảm rủi ro recursive RLS.

---

## 12. Mức độ hoàn thiện chức năng

| Chức năng | Trạng thái source hiện tại | Ghi chú |
|---|---|---|
| Supabase client setup | Hoàn thành cơ bản | Auth + PostgREST |
| Email/password login | Có | Không có register/reset password |
| Logout | Có | Lỗi logout chưa được render ở Home |
| Session restore/observe | Có | Dựa trên Supabase `sessionStatus` |
| Load current profile | Có | Query `profiles` theo user ID |
| Role routing | Một phần | Student/Lecturer có UI; Admin chưa có |
| Student course list | Có | Dựa vào RLS để giới hạn course |
| Lecturer course list | Có | Dùng chung repository/state/adapter |
| Course empty/loading/error | Có | Không có retry/refresh |
| Course detail | Chưa có | Click course là TODO |
| Member list | Chưa có | Chỉ tồn tại trong database design |
| Class meeting | Chưa có | Chưa có model/repository/UI |
| Attendance session | Chưa có | Chưa có model/repository/UI |
| Dynamic QR | Chưa có | Chưa có SDK/permission/UI |
| GPS verification | Chưa có | Chưa có permission/location client |
| Face registration | Chưa có | Chưa có camera/ML pipeline |
| Face verification | Chưa có | Chưa có model/runtime |
| Attendance history | Chưa có | Chưa có query/UI |
| Realtime counter | Chưa có | Supabase Realtime plugin chưa cài |
| Moodle sync | Chưa có | Chỉ có placeholder ID trong model/schema |
| Admin interface | Chưa có | Chỉ hiển thị thông báo |
| Offline mode/cache | Chưa có | Không có Room/DataStore cache riêng |
| CI/CD | Chưa có | Không thấy workflow |
| Analytics/monitoring | Chưa có | Chỉ có Android log cho profile |

---

## 13. Test và trạng thái xác minh

### 13.1 Test đang có

#### Local unit test

`ExampleUnitTest` chỉ kiểm tra:

```kotlin
assertEquals(4, 2 + 2)
```

Đây là test template, không kiểm tra nghiệp vụ SmartAttendance.

#### Instrumented test

`ExampleInstrumentedTest` kiểm tra package name bằng giá trị cũ:

```text
com.example.myapplication
```

Trong khi application ID hiện tại là:

```text
com.example.smartattendance
```

Vì vậy instrumented test này có khả năng thất bại khi chạy trên device/emulator.

### 13.2 Những phần chưa có test

- Auth repository và auth state.
- Profile mapping và role mapping.
- Course DTO/date mapping.
- Course repository query.
- ViewModel state transitions.
- Empty/error/loading render.
- Session restore và logout navigation.
- RLS integration.
- Database constraint/policy.
- UI test cho login và course list.

### 13.3 Kết quả build/test trong lần khảo sát này

Đã thử chạy:

```powershell
.\gradlew.bat testDebugUnitTest --offline
```

Lệnh chưa thực thi được vì sandbox không có quyền mở Gradle wrapper lock file trong cache người dùng:

```text
C:\Users\<user>\.gradle\wrapper\dists\...\gradle-9.4.1-bin.zip.lck
Access is denied
```

Quyền truy cập ngoài workspace không được cấp, vì vậy **không thể kết luận build/test đang pass hay fail từ lần khảo sát này**. Đây là giới hạn kiểm chứng môi trường, không phải lỗi compile đã được xác nhận trong source.

---

## 14. Rủi ro và điểm cần chú ý

### 14.1 Mức ưu tiên cao

#### Database chưa có source of truth trong nhánh hiện tại

Schema và RLS chỉ xuất hiện trong tài liệu của nhánh `origin/docs`; chưa thấy migration SQL/Supabase CLI project trong nhánh code hiện tại. Điều này làm khó việc:

- Tái tạo môi trường.
- Review thay đổi schema.
- Đồng bộ dev/staging/production.
- Chạy database test trong CI.

#### Course access phụ thuộc hoàn toàn vào RLS

`CourseRepository` chỉ filter `is_active = true`. Nếu RLS `courses_select_member` chưa deploy hoặc cấu hình sai, app có thể đọc nhiều course hơn dự kiến. Cần xem RLS là control bắt buộc, không phải tối ưu tùy chọn.

#### Chưa xác minh build hiện tại

Build không được chạy thành công trong lần khảo sát do giới hạn quyền cache Gradle. Trước khi phát triển tiếp nên xác minh ít nhất:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

#### Test instrumented dùng application ID cũ

Nếu chạy nguyên trạng, assertion package name có khả năng sai.

### 14.2 Mức ưu tiên trung bình

#### Cancellation handling không nhất quán

`AuthRepository.login` và `ProfileRepository.getProfile` re-throw `CancellationException`; `AuthRepository.logout` và `CourseRepository.getCourses` bắt `Exception` chung. Coroutine cancellation có thể bị chuyển thành lỗi nghiệp vụ thay vì được propagate đúng.

#### Lỗi logout không hiển thị ở Home

Hai Home Activity gọi `AuthViewModel.logout()` nhưng không collect `AuthUiState`.

#### Admin bị kẹt ở màn hình login sau khi đã authenticated

Role `ADMIN` chỉ hiện thông báo chưa triển khai. Session vẫn authenticated, và session/profile flow có thể tiếp tục phát lại state tùy lifecycle.

#### Logic Student/Lecturer bị lặp

Hai Activity Home gần như giống nhau, chỉ khác title/empty text và TODO click. Việc nhân đôi làm tăng chi phí sửa bug và mở rộng.

#### Raw backend error được đưa thẳng lên UI

Exception message có thể khó hiểu với người dùng và có khả năng làm lộ chi tiết implementation. Nên map lỗi kỹ thuật sang lỗi thân thiện, đồng thời log chi tiết ở kênh phù hợp.

#### Backup rules chưa được rà soát

`allowBackup=true` trong khi backup/data extraction rules vẫn là template. Trước production cần xác định dữ liệu session/token của SDK có được backup hay không và đặt rule rõ ràng.

### 14.3 Chất lượng và khả năng mở rộng

- `CourseAdapter` dùng `notifyDataSetChanged()` thay vì DiffUtil.
- Không có retry/refresh cho course list.
- Không có ordering tường minh khi query course.
- Course click chưa có hành vi.
- Chuỗi UI còn hard-code, hạn chế localization.
- Ngày hiển thị ISO, chưa format theo locale.
- Nếu chỉ có một trong `startDate`/`endDate`, UI ẩn cả hai.
- `LoginActivity` đang ôm nhiều trách nhiệm.
- `MainActivity` và method `openMainActivity` là dead/placeholder code.
- Chưa có DI nên fake repository trong test sẽ cần factory hoặc refactor.
- Không có abstraction cho Supabase client ở `CourseRepository` như hai repository còn lại.
- Không có pagination cho course list; hiện chưa nghiêm trọng với quy mô lớp thông thường.
- Chưa có handling riêng cho mất mạng, timeout hoặc session refresh lỗi ngoài thông báo chung.
- Profile success log có chứa họ tên và role; nên cân nhắc giảm log dữ liệu người dùng ở release.
- Không có release hardening/minification.

---

## 15. Cấu trúc thư mục có chú giải

```text
SmartAttendance/
├── PROJECT_OVERVIEW.md                 # Tài liệu hiện trạng project
├── settings.gradle.kts                 # Repositories, root name, module list
├── build.gradle.kts                    # Root plugin declarations
├── gradle.properties                   # Gradle JVM/Kotlin settings
├── local.properties                    # Local SDK + Supabase config; ignored
├── gradlew / gradlew.bat                # Gradle wrapper entrypoints
├── gradle/
│   ├── libs.versions.toml              # Version catalog
│   ├── gradle-daemon-jvm.properties    # Daemon JVM 21
│   └── wrapper/                        # Gradle 9.4.1 wrapper
└── app/
    ├── build.gradle.kts                # Android module config/dependencies
    ├── proguard-rules.pro              # Template rules
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── java/com/example/smartattendance/
        │   │   ├── MainActivity.kt
        │   │   ├── data/
        │   │   │   ├── model/
        │   │   │   │   ├── Course.kt
        │   │   │   │   ├── Profile.kt
        │   │   │   │   └── UserRole.kt
        │   │   │   ├── remote/
        │   │   │   │   ├── SupabaseProvider.kt
        │   │   │   │   └── dto/
        │   │   │   │       ├── CourseDto.kt
        │   │   │   │       └── ProfileDto.kt
        │   │   │   └── repository/
        │   │   │       ├── AuthRepository.kt
        │   │   │       ├── CourseRepository.kt
        │   │   │       └── ProfileRepository.kt
        │   │   └── ui/
        │   │       ├── auth/
        │   │       │   ├── AuthUiState.kt
        │   │       │   ├── AuthViewModel.kt
        │   │       │   └── LoginActivity.kt
        │   │       ├── course/
        │   │       │   ├── CourseAdapter.kt
        │   │       │   ├── CourseUiState.kt
        │   │       │   └── CourseViewModel.kt
        │   │       ├── profile/
        │   │       │   ├── ProfileUiState.kt
        │   │       │   └── ProfileViewModel.kt
        │   │       ├── student/StudentHomeActivity.kt
        │   │       └── lecturer/LecturerHomeActivity.kt
        │   └── res/
        │       ├── layout/             # 5 screen/item layouts
        │       ├── values/             # strings, colors, themes
        │       ├── values-night/       # dark theme
        │       ├── drawable/           # launcher vectors
        │       ├── mipmap-*/           # launcher images
        │       └── xml/                # backup/data extraction rules
        ├── test/                       # Local JUnit template test
        └── androidTest/                # Instrumented template test
```

---

## 16. Cách mở và chạy project

### 16.1 Yêu cầu môi trường

- Android Studio tương thích với AGP `9.2.1` và Gradle `9.4.1`.
- JDK 21 cho Gradle daemon theo cấu hình toolchain.
- Android SDK Platform cho `compileSdk 37`.
- Android device/emulator từ API 26 trở lên.
- Supabase project với Auth và các bảng/policy tương thích.

### 16.2 Local configuration

Đảm bảo `local.properties` có:

```properties
sdk.dir=<duong-dan-android-sdk>
SUPABASE_URL=<supabase-project-url>
SUPABASE_PUBLISHABLE_KEY=<publishable-or-anon-key>
```

Không commit file này và không đặt service-role key vào đây cho Android app.

### 16.3 Lệnh kiểm tra đề xuất

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

Nếu có emulator/device:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

Instrumented test cần được đồng bộ package assertion trước khi kỳ vọng test pass.

### 16.4 Dữ liệu tối thiểu để thử flow

Backend cần tối thiểu:

- Một user Supabase Auth.
- Một row `profiles` có cùng UUID với `auth.users.id`.
- Role hợp lệ: `STUDENT` hoặc `LECTURER`.
- Ít nhất một course active.
- Row `course_members` nối user với course.
- RLS policy cho phép user đọc đúng profile/course.

Nếu login thành công nhưng không có row profile hoặc RLS chặn profile, app sẽ báo không tìm thấy hồ sơ/quyền RLS.

---

## 17. Roadmap hợp lý từ trạng thái hiện tại

Đây là đề xuất kỹ thuật dựa trên code hiện tại, chưa phải thay đổi đã thực hiện.

### Giai đoạn 1 — Ổn định nền tảng hiện có

1. Xác minh `testDebugUnitTest` và `assembleDebug` trên môi trường đầy đủ quyền.
2. Sửa package của instrumented test.
3. Thêm test cho mapper, repository và ViewModel.
4. Chuẩn hóa cancellation/error mapping.
5. Hiển thị lỗi logout.
6. Xử lý rõ luồng Admin hoặc chặn role chưa hỗ trợ.
7. Loại bỏ/định nghĩa lại `MainActivity` placeholder.

### Giai đoạn 2 — Đưa database vào version control

1. Thêm Supabase migration/schema chính thức.
2. Version-control RLS, function, trigger và seed dev.
3. Thêm test RLS cho Student/Lecturer.
4. Xác minh course query chỉ thấy course của active member.
5. Tách môi trường dev/staging/production.

### Giai đoạn 3 — Course detail và meeting

1. Điều hướng từ course card sang Course Detail.
2. Thêm model/repository/state cho `class_meetings`.
3. Student xem lịch/buổi học.
4. Lecturer tạo và quản lý buổi học qua trusted backend.

### Giai đoạn 4 — Attendance session

1. Tạo/open/close session qua Edge Function/backend.
2. Sinh attendance record mặc định `ABSENT`.
3. Thiết kế API idempotent và audit trail.
4. Thêm realtime count nếu cần.

### Giai đoạn 5 — Verification pipeline

1. Dynamic QR có chữ ký và time window.
2. GPS validation ở trusted backend.
3. Face registration.
4. Liveness/face verification.
5. Chỉ update `ABSENT → PRESENT` sau khi tất cả rule bắt buộc pass.

### Giai đoạn 6 — Production readiness

1. Localization và accessibility.
2. Release logging/privacy review.
3. Backup rule review.
4. Release signing/minification.
5. CI build/test/lint.
6. Monitoring, crash reporting và audit log.
7. Load test khi nhiều sinh viên check-in đồng thời.

---

## 18. Kết luận hiện trạng

Project hiện ở cuối giai đoạn **authentication + role routing + course list**. Nền tảng Android/Supabase đã được nối và cách tổ chức code đủ rõ cho một MVP nhỏ. Phần tiếp theo tự nhiên nhất là ổn định build/test, đưa schema/RLS thành migration có version control, rồi triển khai Course Detail và Class Meeting trước khi đi vào QR/GPS/Face.

Điểm quan trọng nhất cần giữ trong mọi bước tiếp theo là ranh giới tin cậy:

```text
Android app = untrusted client
Supabase RLS + trusted Edge Function/backend = nơi thực thi quyền và nghiệp vụ nhạy cảm
```

Không nên cho Android client trực tiếp tự xác nhận điểm danh hoặc cập nhật `ABSENT → PRESENT` chỉ dựa trên logic chạy ở thiết bị.
