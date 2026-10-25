# 📺 Trang chủ nhẹ

Màn hình chính cho **FPT ATV Play Box (DV8236)**. Bấm **🏠 Home** trên remote để vào màn **Giải trí** và chọn một trong ba ứng dụng.

| | Ứng dụng |
|---|---|
| ▶️ | YouTube |
| 🟠 | FPT Play |
| 🔴 | TV360 |

Ba ứng dụng này vẫn là bản có sẵn trên box. Màn hình chỉ mở chúng, không sửa bên trong.

## 🎮 Dùng remote

| Phím | Việc xảy ra |
|---|---|
| ⬅️ ➡️ | Đổi ô ứng dụng |
| ⭕ OK | Mở ứng dụng đang chọn. Viền trắng là ô đó |
| ⬇️ rồi ⭕ OK | Vào **Cài đặt** |
| ↩️ Quay lại | Ở lại màn hình chính |
| 🏠 Home | Về màn **Giải trí** |

## 🖼️ Đổi hình nền bằng điện thoại

📱 Điện thoại và 📺 box phải cùng một mạng Wi-Fi.

1. ⬇️ Trên remote, đi xuống **Cài đặt** rồi bấm ⭕ **OK**.
2. 📺 Box hiện một địa chỉ, dạng `http://192.168.100.203:8787`. Số đầu có thể khác nếu IP của box đổi.
3. 📱 Mở trình duyệt trên điện thoại và gõ đúng địa chỉ đó.
4. 🖼️ Chọn một ảnh, rồi bấm **Đặt làm nền**. Ảnh lớn hơn 20 MB sẽ bị từ chối. iPhone để định dạng HEIC thì đổi sang JPG hoặc PNG.
5. ↩️ Trên remote bấm **Quay lại**. Ảnh nằm phía sau các ô, có lớp tối để chữ vẫn đọc được.
6. 🧊 Muốn bỏ ảnh và trở lại nền 3D: vào **Cài đặt**, chọn **Nền 3D**, rồi **Quay lại**.

Trang nhận ảnh chạy khi màn hình chính đang mở, kể cả sau khi bấm **Quay lại**. Nếu điện thoại không vào được link, bấm **🏠 Home** trên remote rồi mở lại địa chỉ.

## 🛠️ Bật chế độ nhà phát triển

Làm trên box, bằng remote:

1. ⚙️ Vào **Cài đặt**.
2. 🔢 Tìm mục **Bản dựng**. Trên Android TV mục này thường nằm trong **Tùy chọn thiết bị** → **Giới thiệu**.
3. 7️⃣ Đứng ở **Bản dựng** và bấm ⭕ **OK bảy lần**. Box báo máy đã là nhà phát triển.
4. ↩️ Bấm **Back** để quay ra.
5. 🛠️ Vào **Tùy chọn nhà phát triển**.
6. 🔌 Bật **Gỡ lỗi USB**.

Sau khi gỡ lỗi USB đã bật, 💻 máy tính cùng Wi-Fi kết nối bằng cổng **5555**:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" connect 192.168.100.203:5555
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices -l
```

✅ Lần đầu, nhìn màn hình box và chọn **Cho phép** cho máy tính này. Trạng thái phải là `device`.

❌ Nếu vẫn là `unauthorized` thì chưa bấm cho phép trên box.

Đổi `192.168.100.203` nếu địa chỉ box khác. Xem địa chỉ trong **Cài đặt** của màn hình chính, phần trước dấu `:`.

## 📦 Cài hoặc cập nhật màn hình chính

Trong PowerShell, tại thư mục `fpt-home`:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\build.ps1
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r .\build\LightHome.apk
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" shell cmd package set-home-activity --user 0 vn.lighttv.home/.HomeActivity
```

🏠 Lệnh cuối chọn ứng dụng này làm màn hình khi bấm Home.

## ♻️ Cài lại ứng dụng đã gỡ khỏi menu

Các lệnh sau chỉ khôi phục cho người dùng hiện tại. ▶️ YouTube không nằm trong danh sách này.

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb shell cmd package install-existing com.google.android.videos
& $adb shell cmd package install-existing com.google.android.youtube.tvmusic
& $adb shell cmd package install-existing com.google.android.play.games
& $adb shell cmd package install-existing com.android.chrome
& $adb shell cmd package install-existing com.ted.android.tv
```

| Lệnh | Ứng dụng |
|---|---|
| `com.google.android.videos` | 🎬 Google Play Movies |
| `com.google.android.youtube.tvmusic` | 🎵 YouTube Music |
| `com.google.android.play.games` | 🎯 Google Play Games |
| `com.android.chrome` | 🌐 Chrome |
| `com.ted.android.tv` | 🎤 TED |
