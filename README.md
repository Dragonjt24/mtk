# GAMING GEAR MANAGEMENT SYSTEM

Ứng dụng Desktop quản lý kinh doanh **Gaming Gear** được xây dựng bằng **Java 17**, **Java Swing + FlatLaf (Dark Mode)**, quản lý dữ liệu hoàn toàn trên bộ nhớ bằng Java Collections (`InMemoryDataStore`) kết hợp lưu trữ tệp JSON cục bộ (`JSONStorageService`), **không sử dụng cơ sở dữ liệu (SQL/NoSQL/JDBC/ORM)**.

---

## 1. Công nghệ sử dụng

- **Ngôn ngữ**: Java 17
- **Giao diện Desktop**: Java Swing + FlatLaf `3.5.4` (Dark Mode, bảng màu đen/xám đậm kết hợp xanh dương & tím neon)
- **Quản lý phụ thuộc & Build**: Apache Maven
- **Lưu trữ bộ nhớ**: `InMemoryDataStore` (`LinkedHashMap`, `ArrayList`, `Optional`)
- **Lưu trữ JSON cục bộ**: Jackson Databind + JavaTimeModule (`JSONStorageService`)
- **Xuất hóa đơn PDF**: OpenPDF (`2.0.3`) + `java.awt.print.PrinterJob`
- **Kiểm thử đơn vị**: JUnit 5 (`5.11.4`)

---

## 2. Tài khoản đăng nhập mẫu

| Vai trò | Username | Password |
| :--- | :--- | :--- |
| **Quản trị viên (Admin)** | `admin` | `admin123` |
| **Nhân viên (Staff)** | `staff01` | `staff123` |

---

## 3. Các màn hình & chức năng chính

1. **Login (`LoginFrame`)**: Đăng nhập, nút Hiện/Ẩn mật khẩu, kiểm tra tài khoản và thông báo lỗi.
2. **Dashboard**: Thống kê động từ dữ liệu thật (Tổng sản phẩm, Tổng đơn hàng, Tổng doanh thu, Đơn chờ xử lý, Sản phẩm sắp hết hàng), biểu đồ doanh thu 7 ngày, biểu đồ phân bổ đơn hàng theo trạng thái, đơn hàng gần đây và sản phẩm bán chạy.
3. **Products**: CRUD sản phẩm, xóa nhiều sản phẩm cùng lúc, tìm kiếm theo tên/mã/hãng, lọc danh mục/thương hiệu/khoảng giá, sắp xếp theo giá/tên/tồn kho, cập nhật nhanh tồn kho (khởi tạo sẵn 22 sản phẩm thuộc 9 danh mục chuẩn).
4. **Categories**: Quản lý 9 danh mục (`Keyboard`, `Mouse`, `Monitor`, `Headset`, `Speaker`, `Webcam`, `Controller`, `Microphone`, `Mouse Pad`), kiểm tra ràng buộc trước khi xóa.
5. **Customers**: CRUD khách hàng, tìm kiếm, hạng thành viên (`NORMAL`, `VIP`), tổng chi tiêu và lịch sử mua hàng.
6. **Orders**: Danh sách đơn hàng, lọc trạng thái (`PENDING`, `PAID`, `CANCELLED`), xem chi tiết snapshot đơn giá tại thời điểm mua, đổi chính sách giảm giá, hủy đơn, chuyển sang thanh toán.
7. **Create Order**: Chọn khách hàng, thêm/bớt/xóa sản phẩm vào giỏ, chọn chính sách giảm giá thông qua `DiscountFactory` + `DiscountContext`, hiển thị trực tiếp giá gốc / tỷ lệ giảm / tiền giảm / tổng thanh toán.
8. **Discounts**: Xem quy tắc các chính sách giảm giá (`0%`, `VIP 15%`, `Seasonal 20%`) và bộ mô phỏng tính toán trực tiếp.
9. **Payments**: Thanh toán mô phỏng (`COD`, `MOCK_SUCCESS`, `MOCK_FAILURE`). Chỉ phát `PaymentEvent` khi thanh toán thành công; khi thất bại sẽ không tạo hóa đơn và không trừ tồn kho.
10. **Invoices**: Xem danh sách và chi tiết hóa đơn được `BillObserver` tự động tạo, hỗ trợ **In hóa đơn** và **Xuất PDF**.
11. **Inventory**: Theo dõi tồn kho, lọc sản phẩm sắp hết/hết hàng, nhập kho, điều chỉnh kho và xem lịch sử biến động `InventoryLog`.
12. **Settings**: Quản lý nhân viên & tài khoản, đổi mật khẩu, cấu hình ngưỡng cảnh báo tồn kho, lưu/tải lại JSON và khôi phục dữ liệu mẫu.

---

## 4. Hướng dẫn cài đặt và chạy ứng dụng

### Yêu cầu môi trường
- **JDK 17** trở lên
- **Apache Maven 3.8+**
- Môi trường Desktop (Windows / macOS / Linux có X11/Wayland) để hiển thị cửa sổ Java Swing

### Bước 1: Biên dịch và chạy toàn bộ Unit Test (JUnit 5)
```bash
mvn clean test
```

### Bước 2: Chạy ứng dụng Desktop trực tiếp bằng Maven
```bash
mvn compile exec:java
```

### Bước 3: Đóng gói file JAR thực thi (Uber JAR) và chạy
```bash
mvn clean package
java -jar target/gaming-gear-management-1.0.0.jar
```

> Dữ liệu cục bộ được lưu tự động tại `data/gaming_gear_data.json` (tách biệt hoàn toàn với logic nghiệp vụ). Nếu tệp chưa tồn tại hoặc bị lỗi định dạng, hệ thống tự động khởi tạo lại dữ liệu mẫu qua `DataInitializer`.

---

## 5. Tài liệu kiến trúc Design Patterns

Xem chi tiết vị trí mã nguồn, sơ đồ và vai trò của từng mẫu thiết kế (Factory, Strategy, Observer) trong tài liệu:
- [`docs/DESIGN_PATTERNS.md`](docs/DESIGN_PATTERNS.md)
