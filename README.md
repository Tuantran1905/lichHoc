# Lịch học TA.CĐ.CNT.1.25 – app Android

App tự tải file thời khóa biểu "TKB_KT CNT" của khoa trên Google Drive, đọc lịch lớp **TA.CĐ. CNT.1.25** và:

- hiện **widget** trên màn hình chính: buổi học tiếp theo (môn, giờ, phòng, giáo viên), nhãn *Hoàn chỉnh / Đang xử lý*, các buổi hôm nay;
- **báo thông báo** khi lịch của lớp chuyển sang HOÀN CHỈNH, khi lịch thay đổi (ghi rõ buổi nào, cũ → mới), khi trường đăng lịch tuần sau, và tóm tắt lịch tuần sau vào tối Chủ nhật;
- màn hình xem lịch tuần này và tuần sau, nút **Cập nhật ngay**.

App tự kiểm tra khoảng 2 giờ một lần khi có mạng. Không cần tài khoản, không cần app Claude.

## Trực lớp (dành cho lớp trưởng)

- Tab **Trực lớp**: mỗi ngày có học có 3 bạn trực (đổi được). Mỗi buổi bấm **Điểm danh**, chạm tên bạn vắng, bấm **Lưu**.
  Bạn đến lượt mà vắng sẽ được thay bằng bạn kế tiếp đang có mặt; buổi sau trong ngày điểm danh lại.
  Hết ngày, ai đã trực xuống cuối hàng chờ; ai đến lượt mà vắng cả ngày giữ chỗ đầu hàng, trực vào ngày học kế tiếp, nên không ai bị bỏ sót.
- Nút **Gửi lịch trực cho lớp** mở bảng chia sẻ để dán vào Zalo/Messenger.
- Tab **Lớp**: danh sách 36 bạn nạp sẵn (thêm, sửa tên, cho nghỉ trực), số ngày/buổi trực, trực thay, buổi vắng của từng bạn; đổi số người trực, ngày bắt đầu xếp, thêm buổi học bù.
- Buổi học tự lấy từ file TKB. Buổi đã điểm danh không bao giờ bị ghi đè. Dữ liệu điểm danh lưu trong máy.
- Widget hiện thêm dòng **Trực hôm nay**.

## Tạo file APK bằng GitHub (không cần cài gì trên máy)

1. Đăng nhập https://github.com, bấm **New repository**, đặt tên `LichHoc`, chọn **Public** hoặc **Private**, bấm **Create repository**.
2. Bấm **uploading an existing file**, kéo toàn bộ nội dung thư mục `LichHoc` (thư mục `app`, các file `build.gradle`, `settings.gradle`, `gradle.properties`, `README.md`) vào, bấm **Commit changes**.
3. Thư mục `.github` là thư mục ẩn nên có thể không được kéo lên. Nếu vậy: vào tab **Actions** → **set up a workflow yourself**, đặt tên file `build.yml`, dán nội dung file `.github/workflows/build.yml`, bấm **Commit changes**.
4. Vào tab **Actions**, mở lần chạy **Build APK** mới nhất, đợi khoảng 3–5 phút đến khi có dấu ✓ xanh.
5. Kéo xuống mục **Artifacts**, tải **LichHoc-apk** (file zip chứa `app-debug.apk`).

## Cài lên điện thoại

1. Chuyển file `app-debug.apk` sang điện thoại (hoặc mở GitHub trên điện thoại và tải trực tiếp), giải nén nếu là file zip.
2. Mở file APK. Android sẽ hỏi cho phép cài từ nguồn này: bấm **Cài đặt** → bật **Cho phép từ nguồn này** → quay lại bấm **Cài đặt**.
3. Mở app **Lịch học**, cho phép **thông báo**. App tải lịch lần đầu ngay.
4. Thêm widget: giữ lâu chỗ trống trên màn hình chính → **Tiện ích (Widgets)** → tìm **Lịch học** → kéo ra màn hình.

Nếu điện thoại có chế độ tiết kiệm pin mạnh (Xiaomi, Oppo, Vivo, Samsung…), vào **Cài đặt → Ứng dụng → Lịch học → Pin** và chọn **Không hạn chế** để app kiểm tra lịch đúng giờ.

## Ghi chú

- Nguồn lịch: file Drive `1v3fjuXH9cKIpYZdZ3ul7-QBSOsWSSsDM`. Nếu khoa đổi file khác hoặc tắt chia sẻ công khai, app sẽ báo lỗi trên màn hình; sửa `FILE_ID` trong `Repo.java`.
- Đổi lớp: sửa `CLASS_NAME` trong `Repo.java`.
- Có thể mở thư mục bằng Android Studio và bấm Run nếu bạn đã cài sẵn.
