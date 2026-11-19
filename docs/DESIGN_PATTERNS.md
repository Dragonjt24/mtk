# TÀI LIỆU THIẾT KẾ 3 DESIGN PATTERNS TRONG MÃ NGUỒN

Tài liệu này giải thích chi tiết **vị trí**, **trách nhiệm** và **luồng phối hợp** của 3 mẫu thiết kế bắt buộc trong dự án **GAMING GEAR MANAGEMENT SYSTEM**.

---

## 1. Factory Pattern (`com.gaminggear.patterns.factory`)

### Vị trí mã nguồn
- `src/main/java/com/gaminggear/patterns/factory/DiscountType.java`
- `src/main/java/com/gaminggear/patterns/factory/DiscountFactory.java`

### Vai trò & Trách nhiệm
- **Chỉ chịu trách nhiệm khởi tạo (creation)** đối tượng `DiscountStrategy` tương ứng với `DiscountType` mà người dùng lựa chọn (`NO_DISCOUNT`, `VIP`, `SEASONAL`).
- Sử dụng `EnumMap<DiscountType, Supplier<DiscountStrategy>>` để ánh xạ sạch sẽ, **không dùng chuỗi `if/else` lớn**.
- Kiểm tra tính hợp lệ của tham số đầu vào (`null` hoặc mã không hợp lệ) và ném `BusinessException`.

### Cách sử dụng trong luồng nghiệp vụ
Trong `OrderService.previewPricing(...)`:
```java
DiscountStrategy strategy = DiscountFactory.createStrategy(discountType);
```

---

## 2. Strategy Pattern (`com.gaminggear.patterns.strategy`)

### Vị trí mã nguồn
- `src/main/java/com/gaminggear/patterns/strategy/DiscountStrategy.java` *(Interface)*
- `src/main/java/com/gaminggear/patterns/strategy/NoDiscountStrategy.java` *(0%)*
- `src/main/java/com/gaminggear/patterns/strategy/VIPDiscountStrategy.java` *(15%)*
- `src/main/java/com/gaminggear/patterns/strategy/SeasonalDiscountStrategy.java` *(20%)*
- `src/main/java/com/gaminggear/patterns/strategy/DiscountContext.java` *(Context)*

### Vai trò & Trách nhiệm
- **Chỉ chịu trách nhiệm thực thi thuật toán tính giảm giá** bằng `BigDecimal` (làm tròn `RoundingMode.HALF_UP`).
- `DiscountContext` giữ tham chiếu tới `DiscountStrategy`, cho phép thay đổi chiến lược linh hoạt lúc runtime (`setStrategy(...)`) khi tạo đơn hoặc đổi chính sách giảm giá đơn `PENDING` mà **không cần sửa `OrderService`**.

### Phối hợp giữa Factory và Strategy
1. `OrderService` kiểm tra quy tắc nghiệp vụ (ví dụ: không cộng dồn chính sách, khách `NORMAL` không được dùng giảm giá `VIP`).
2. `OrderService` gọi `DiscountFactory.createStrategy(discountType)` để **tạo** chiến lược.
3. `OrderService` truyền chiến lược đó vào `DiscountContext.setStrategy(strategy)` để **tính toán** `discountAmount` và `finalTotal`.

---

## 3. Observer Pattern (`com.gaminggear.patterns.observer`)

### Vị trí mã nguồn
- `src/main/java/com/gaminggear/patterns/observer/PaymentEvent.java` *(Event Payload)*
- `src/main/java/com/gaminggear/patterns/observer/PaymentObserver.java` *(Observer Interface)*
- `src/main/java/com/gaminggear/patterns/observer/PaymentSubject.java` *(Subject Interface)*
- `src/main/java/com/gaminggear/patterns/observer/PaymentEventPublisher.java` *(Concrete Publisher)*
- `src/main/java/com/gaminggear/patterns/observer/BillObserver.java` *(Tạo hóa đơn)*
- `src/main/java/com/gaminggear/patterns/observer/InventoryObserver.java` *(Trừ tồn kho)*
- `src/main/java/com/gaminggear/patterns/observer/NotificationObserver.java` *(Ghi nhận & đẩy thông báo UI)*

### Vai trò & Luồng hoạt động thực tế
1. Trong `DependencyContainer`, `BillObserver`, `InventoryObserver` và `NotificationObserver` được đăng ký vào `PaymentEventPublisher` thông qua `registerObserver(...)` (hỗ trợ hủy đăng ký `unregisterObserver(...)`).
2. Khi người dùng thanh toán tại `PaymentPanel` $\rightarrow$ gọi `PaymentService.processPayment(orderId, method)`:
   - Nếu chọn `MOCK_FAILURE`: `PaymentService` lưu giao dịch `FAILED`, giữ nguyên đơn ở `PENDING` và **không gọi** `eventPublisher.notifyObservers(...)`.
   - Nếu chọn `MOCK_SUCCESS` hoặc `COD`: `PaymentService` chuyển đơn sang `PAID`, lưu giao dịch `SUCCESS`, cập nhật chi tiêu khách hàng rồi phát `PaymentEvent`.
3. `PaymentEventPublisher` phát sự kiện tới từng Observer:
   - `BillObserver`: kiểm tra chống trùng (`processedOrderIds` + `billService.findByOrderId(...)`) để đảm bảo **không bao giờ tạo hóa đơn trùng**, sau đó tạo `Bill` và `BillItem`.
   - `InventoryObserver`: kiểm tra chống trùng (`processedOrderIds`) để đảm bảo **chỉ trừ tồn kho đúng 1 lần** cho mỗi đơn hàng thành công.
   - `NotificationObserver`: ghi nhận thông báo và chuyển tiếp lên `MainFrame` qua callback `SwingUtilities.invokeLater(...)`.
4. Các Observer hoạt động hoàn toàn độc lập và **không gọi trực tiếp lẫn nhau**.

---

## 4. Bộ kiểm thử tự động (JUnit 5)

| Test Class | Phạm vi kiểm thử |
| :--- | :--- |
| `src/test/java/com/gaminggear/patterns/DiscountFactoryTest.java` | Khởi tạo đúng từng Strategy từ `DiscountFactory` và xử lý loại giảm giá không hợp lệ. |
| `src/test/java/com/gaminggear/patterns/DiscountStrategyTest.java` | Kiểm thử phép tính `1.000.000 VNĐ` cho 0%, 15%, 20%, đổi chiến lược động và kiểm tra số âm. |
| `src/test/java/com/gaminggear/patterns/PaymentObserverTest.java` | Kiểm thử thanh toán thành công/thất bại, hoạt động của 3 Observer, hủy đăng ký Observer và chống trùng lặp sự kiện. |
| `src/test/java/com/gaminggear/service/BusinessLogicTest.java` | Kiểm thử các quy tắc giỏ hàng trống, vượt tồn kho, số âm, hủy/thanh toán đơn sai trạng thái, snapshot đơn giá, lưu/đọc JSON. |
| `src/test/java/com/gaminggear/service/SystemIntegrationTest.java` | Kiểm thử dữ liệu mẫu (9 danh mục, >= 20 sản phẩm), đăng nhập admin, thống kê Dashboard động và xuất hóa đơn PDF. |
