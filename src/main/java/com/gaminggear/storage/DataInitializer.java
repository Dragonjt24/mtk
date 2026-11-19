package com.gaminggear.storage;

import com.gaminggear.model.Bill;
import com.gaminggear.model.BillItem;
import com.gaminggear.model.Category;
import com.gaminggear.model.Customer;
import com.gaminggear.model.InventoryLog;
import com.gaminggear.model.Order;
import com.gaminggear.model.OrderItem;
import com.gaminggear.model.Payment;
import com.gaminggear.model.Product;
import com.gaminggear.model.User;
import com.gaminggear.model.enums.MembershipTier;
import com.gaminggear.model.enums.OrderStatus;
import com.gaminggear.model.enums.PaymentMethod;
import com.gaminggear.model.enums.PaymentStatus;
import com.gaminggear.model.enums.ProductStatus;
import com.gaminggear.model.enums.UserRole;
import com.gaminggear.patterns.factory.DiscountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Seeds realistic Gaming Gear store sample data into InMemoryDataStore
 * when no valid local JSON file exists or when resetting demo data.
 */
public final class DataInitializer {

    private DataInitializer() {
    }

    public static void initialize(InMemoryDataStore store) {
        store.clearAll();

        seedUsers(store);
        seedCategories(store);
        seedProducts(store);
        seedCustomers(store);
        seedOrdersPaymentsAndBills(store);
    }

    private static void seedUsers(InMemoryDataStore store) {
        User admin = new User("USR-001", "admin", "admin123", "Nguyễn Minh Quân (Admin)", UserRole.ADMIN, true);
        User staff1 = new User("USR-002", "staff01", "staff123", "Trần Bảo Anh (Nhân viên)", UserRole.STAFF, true);
        User staff2 = new User("USR-003", "staff02", "staff123", "Lê Hoàng Phúc (Thu ngân)", UserRole.STAFF, true);

        store.getUsers().put(admin.getId(), admin);
        store.getUsers().put(staff1.getId(), staff1);
        store.getUsers().put(staff2.getId(), staff2);
    }

    private static void seedCategories(InMemoryDataStore store) {
        List<Category> list = List.of(
                new Category("CAT-01", "Keyboard", "Bàn phím cơ Gaming, Rapid Trigger & Custom", true),
                new Category("CAT-02", "Mouse", "Chuột Gaming siêu nhẹ, không dây độ trễ thấp", true),
                new Category("CAT-03", "Monitor", "Màn hình Gaming tần số quét cao 165Hz - 360Hz", true),
                new Category("CAT-04", "Headset", "Tai nghe Gaming vòm 7.1 & không dây độ trễ thấp", true),
                new Category("CAT-05", "Speaker", "Loa Gaming RGB & Soundbar chuyên nghiệp", true),
                new Category("CAT-06", "Webcam", "Webcam Full HD / 4K phục vụ Stream & Họp", true),
                new Category("CAT-07", "Controller", "Tay cầm chơi game PC/Console không dây", true),
                new Category("CAT-08", "Microphone", "Micro thu âm Condenser & Dynamic cho Streamer", true),
                new Category("CAT-09", "Mouse Pad", "Lót chuột Speed/Control kích thước lớn", true)
        );
        for (Category c : list) {
            store.getCategories().put(c.getId(), c);
        }
    }

    private static void seedProducts(InMemoryDataStore store) {
        List<Product> list = List.of(
                // Keyboards (3)
                new Product("PRD-001", "Wooting 60HE+ Rapid Trigger", "CAT-01", "Wooting",
                        new BigDecimal("4990000"), 14, "Bàn phím từ tính Hall-Effect hỗ trợ Rapid Trigger chuyên FPS.",
                        "assets/products/wooting60he.png", ProductStatus.ACTIVE),
                new Product("PRD-002", "Keychron Q1 Pro QMK/VIA", "CAT-01", "Keychron",
                        new BigDecimal("4250000"), 9, "Bàn phím cơ nhôm CNC Gasket Mount kết nối Bluetooth/USB-C.",
                        "assets/products/keychron_q1_pro.png", ProductStatus.ACTIVE),
                new Product("PRD-003", "Logitech G Pro X TKL Lightspeed", "CAT-01", "Logitech",
                        new BigDecimal("3890000"), 4, "Bàn phím TKL không dây chuyên thi đấu Esport.",
                        "assets/products/gprox_tkl.png", ProductStatus.ACTIVE),

                // Mice (3)
                new Product("PRD-004", "Logitech G Pro X Superlight 2", "CAT-02", "Logitech",
                        new BigDecimal("3590000"), 22, "Chuột không dây siêu nhẹ 60g, cảm biến HERO 2 32K DPI.",
                        "assets/products/gpx_superlight2.png", ProductStatus.ACTIVE),
                new Product("PRD-005", "Razer Viper V3 Pro", "CAT-02", "Razer",
                        new BigDecimal("3990000"), 16, "Chuột Esport 54g, polling rate 8000Hz HyperPolling.",
                        "assets/products/viper_v3_pro.png", ProductStatus.ACTIVE),
                new Product("PRD-006", "Pulsar X2V2 Wireless", "CAT-02", "Pulsar",
                        new BigDecimal("2450000"), 3, "Chuột đối xứng siêu nhẹ tối ưu kiểu cầm Claw Grip.",
                        "assets/products/pulsar_x2v2.png", ProductStatus.ACTIVE),

                // Monitors (3)
                new Product("PRD-007", "ASUS ROG Swift PG27AQDM OLED", "CAT-03", "ASUS ROG",
                        new BigDecimal("21900000"), 6, "Màn hình OLED 27 inch 2K 240Hz 0.03ms chuẩn màu điện ảnh.",
                        "assets/products/rog_pg27aqdm.png", ProductStatus.ACTIVE),
                new Product("PRD-008", "LG UltraGear 27GR75Q-B 165Hz", "CAT-03", "LG",
                        new BigDecimal("6890000"), 12, "Màn hình IPS 2K 165Hz 1ms G-Sync Compatible.",
                        "assets/products/lg_27gr75q.png", ProductStatus.ACTIVE),
                new Product("PRD-009", "BenQ ZOWIE XL2566K 360Hz", "CAT-03", "BenQ ZOWIE",
                        new BigDecimal("15490000"), 4, "Màn hình TN 360Hz công nghệ DyAc+ chuyên CS2/Valorant.",
                        "assets/products/zowie_xl2566k.png", ProductStatus.ACTIVE),

                // Headsets (3)
                new Product("PRD-010", "HyperX Cloud III Wireless", "CAT-04", "HyperX",
                        new BigDecimal("3690000"), 18, "Tai nghe không dây pin 120 giờ, âm thanh DTS Spatial.",
                        "assets/products/hyperx_cloud3.png", ProductStatus.ACTIVE),
                new Product("PRD-011", "SteelSeries Arctis Nova Pro Wireless", "CAT-04", "SteelSeries",
                        new BigDecimal("8990000"), 7, "Tai nghe flagship chống ồn chủ động ANC, pin kép thay nóng.",
                        "assets/products/arctis_nova_pro.png", ProductStatus.ACTIVE),
                new Product("PRD-012", "Razer BlackShark V2 Pro 2023", "CAT-04", "Razer",
                        new BigDecimal("4590000"), 11, "Tai nghe Esport micro băng thông siêu rộng.",
                        "assets/products/blackshark_v2.png", ProductStatus.ACTIVE),

                // Speakers (2)
                new Product("PRD-013", "Razer Leviathan V2 Pro", "CAT-05", "Razer",
                        new BigDecimal("9490000"), 5, "Soundbar Gaming kèm Subwoofer AI Head-Tracking 3D.",
                        "assets/products/leviathan_v2.png", ProductStatus.ACTIVE),
                new Product("PRD-014", "Logitech G560 Lightsync RGB", "CAT-05", "Logitech",
                        new BigDecimal("4790000"), 10, "Hệ thống loa 2.1 công suất 240W đồng bộ RGB theo màn hình.",
                        "assets/products/logitech_g560.png", ProductStatus.ACTIVE),

                // Webcams (2)
                new Product("PRD-015", "Elgato Facecam MK.2", "CAT-06", "Elgato",
                        new BigDecimal("4190000"), 13, "Webcam 1080p60 HDR cảm biến Sony STARVIS cho Streamer.",
                        "assets/products/elgato_facecam.png", ProductStatus.ACTIVE),
                new Product("PRD-016", "Logitech Brio 4K Stream Edition", "CAT-06", "Logitech",
                        new BigDecimal("4490000"), 8, "Webcam 4K Ultra HD hỗ trợ Windows Hello và HDR.",
                        "assets/products/logitech_brio4k.png", ProductStatus.ACTIVE),

                // Controllers (2)
                new Product("PRD-017", "Sony DualSense Edge Wireless", "CAT-07", "Sony",
                        new BigDecimal("5290000"), 9, "Tay cầm Pro cho PS5 & PC tùy biến cần analog và cò bấm.",
                        "assets/products/dualsense_edge.png", ProductStatus.ACTIVE),
                new Product("PRD-018", "Xbox Elite Wireless Series 2", "CAT-07", "Microsoft",
                        new BigDecimal("4390000"), 2, "Tay cầm cao cấp với 4 paddle mặt lưng và pin 40 giờ.",
                        "assets/products/xbox_elite2.png", ProductStatus.ACTIVE),

                // Microphones (2)
                new Product("PRD-019", "Shure MV7X Dynamic Podcast/Stream", "CAT-08", "Shure",
                        new BigDecimal("5490000"), 10, "Micro Dynamic lọc tạp âm phòng cực tốt qua XLR.",
                        "assets/products/shure_mv7x.png", ProductStatus.ACTIVE),
                new Product("PRD-020", "HyperX QuadCast S RGB", "CAT-08", "HyperX",
                        new BigDecimal("3590000"), 15, "Micro USB Condenser 4 hướng thu tích hợp chống rung.",
                        "assets/products/quadcast_s.png", ProductStatus.ACTIVE),

                // Mouse Pads (2)
                new Product("PRD-021", "Artisan Ninja FX Zero Soft XL", "CAT-09", "Artisan",
                        new BigDecimal("1490000"), 20, "Lót chuột Nhật Bản bề mặt Control cân bằng chuẩn Esport.",
                        "assets/products/artisan_zero.png", ProductStatus.ACTIVE),
                new Product("PRD-022", "SteelSeries QcK Heavy XXL", "CAT-09", "SteelSeries",
                        new BigDecimal("890000"), 28, "Lót chuột khổ lớn trải bàn dày 4mm chống trượt.",
                        "assets/products/qck_heavy.png", ProductStatus.ACTIVE)
        );
        for (Product p : list) {
            store.getProducts().put(p.getId(), p);
        }
    }

    private static void seedCustomers(InMemoryDataStore store) {
        List<Customer> list = List.of(
                new Customer("CUS-001", "Phạm Gia Huy", "0901234567", "huy.pham@gmail.com",
                        "12 Nguyễn Huệ, Quận 1, TP.HCM", MembershipTier.VIP, new BigDecimal("18500000")),
                new Customer("CUS-002", "Nguyễn Thảo Vy", "0912345678", "vy.nguyen@gmail.com",
                        "45 Lê Lợi, Hải Châu, Đà Nẵng", MembershipTier.VIP, new BigDecimal("12400000")),
                new Customer("CUS-003", "Trần Quốc Bảo", "0987654321", "bao.tran@outlook.com",
                        "88 Cầu Giấy, Hà Nội", MembershipTier.NORMAL, new BigDecimal("4990000")),
                new Customer("CUS-004", "Lê Minh Khoa", "0933445566", "khoa.le@gmail.com",
                        "210 Cách Mạng Tháng 8, Quận 3, TP.HCM", MembershipTier.NORMAL, new BigDecimal("2380000")),
                new Customer("CUS-005", "Đỗ Anh Tuấn", "0977889900", "tuan.do@gmail.com",
                        "15 Võ Văn Ngân, Thủ Đức, TP.HCM", MembershipTier.VIP, new BigDecimal("21900000"))
        );
        for (Customer c : list) {
            store.getCustomers().put(c.getId(), c);
        }
    }

    private static void seedOrdersPaymentsAndBills(InMemoryDataStore store) {
        LocalDateTime now = LocalDateTime.now();

        // Sample Order 1 - PAID (VIP 15%)
        OrderItem o1Item1 = new OrderItem("PRD-004", "Logitech G Pro X Superlight 2", 1, new BigDecimal("3590000"));
        OrderItem o1Item2 = new OrderItem("PRD-021", "Artisan Ninja FX Zero Soft XL", 1, new BigDecimal("1490000"));
        BigDecimal o1Orig = new BigDecimal("5080000");
        BigDecimal o1Disc = new BigDecimal("762000");
        BigDecimal o1Final = new BigDecimal("4318000");
        Order order1 = new Order("ORD-1001", "CUS-001", "Phạm Gia Huy",
                List.of(o1Item1, o1Item2), DiscountType.VIP, new BigDecimal("0.15"),
                o1Orig, o1Disc, o1Final, now.minusDays(4), OrderStatus.PAID);
        store.getOrders().put(order1.getId(), order1);

        Payment pay1 = new Payment("PAY-1001", order1.getId(), PaymentMethod.MOCK_SUCCESS, o1Final,
                PaymentStatus.SUCCESS, now.minusDays(4).plusMinutes(5), "Thanh toán thành công qua hệ thống mô phỏng.");
        store.getPayments().put(pay1.getId(), pay1);

        Bill bill1 = new Bill("BILL-1001", order1.getId(), "CUS-001", "Phạm Gia Huy",
                "0901234567", "12 Nguyễn Huệ, Quận 1, TP.HCM",
                List.of(
                        new BillItem("PRD-004", "Logitech G Pro X Superlight 2", 1, new BigDecimal("3590000"), new BigDecimal("3590000")),
                        new BillItem("PRD-021", "Artisan Ninja FX Zero Soft XL", 1, new BigDecimal("1490000"), new BigDecimal("1490000"))
                ),
                o1Orig, DiscountType.VIP, new BigDecimal("0.15"), o1Disc, o1Final,
                now.minusDays(4).plusMinutes(5), PaymentMethod.MOCK_SUCCESS, PaymentStatus.SUCCESS);
        store.getBills().put(bill1.getId(), bill1);

        // Sample Order 2 - PAID (SEASONAL 20%)
        OrderItem o2Item1 = new OrderItem("PRD-001", "Wooting 60HE+ Rapid Trigger", 1, new BigDecimal("4990000"));
        OrderItem o2Item2 = new OrderItem("PRD-010", "HyperX Cloud III Wireless", 1, new BigDecimal("3690000"));
        BigDecimal o2Orig = new BigDecimal("8680000");
        BigDecimal o2Disc = new BigDecimal("1736000");
        BigDecimal o2Final = new BigDecimal("6944000");
        Order order2 = new Order("ORD-1002", "CUS-002", "Nguyễn Thảo Vy",
                List.of(o2Item1, o2Item2), DiscountType.SEASONAL, new BigDecimal("0.20"),
                o2Orig, o2Disc, o2Final, now.minusDays(2), OrderStatus.PAID);
        store.getOrders().put(order2.getId(), order2);

        Payment pay2 = new Payment("PAY-1002", order2.getId(), PaymentMethod.COD, o2Final,
                PaymentStatus.SUCCESS, now.minusDays(2).plusHours(2), "Đã thu tiền COD thành công.");
        store.getPayments().put(pay2.getId(), pay2);

        Bill bill2 = new Bill("BILL-1002", order2.getId(), "CUS-002", "Nguyễn Thảo Vy",
                "0912345678", "45 Lê Lợi, Hải Châu, Đà Nẵng",
                List.of(
                        new BillItem("PRD-001", "Wooting 60HE+ Rapid Trigger", 1, new BigDecimal("4990000"), new BigDecimal("4990000")),
                        new BillItem("PRD-010", "HyperX Cloud III Wireless", 1, new BigDecimal("3690000"), new BigDecimal("3690000"))
                ),
                o2Orig, DiscountType.SEASONAL, new BigDecimal("0.20"), o2Disc, o2Final,
                now.minusDays(2).plusHours(2), PaymentMethod.COD, PaymentStatus.SUCCESS);
        store.getBills().put(bill2.getId(), bill2);

        // Sample Order 3 - PAID (NO_DISCOUNT 0%)
        OrderItem o3Item1 = new OrderItem("PRD-008", "LG UltraGear 27GR75Q-B 165Hz", 1, new BigDecimal("6890000"));
        BigDecimal o3Orig = new BigDecimal("6890000");
        Order order3 = new Order("ORD-1003", "CUS-003", "Trần Quốc Bảo",
                List.of(o3Item1), DiscountType.NO_DISCOUNT, BigDecimal.ZERO,
                o3Orig, BigDecimal.ZERO, o3Orig, now.minusDays(1), OrderStatus.PAID);
        store.getOrders().put(order3.getId(), order3);

        Payment pay3 = new Payment("PAY-1003", order3.getId(), PaymentMethod.MOCK_SUCCESS, o3Orig,
                PaymentStatus.SUCCESS, now.minusDays(1).plusMinutes(10), "Thanh toán thành công.");
        store.getPayments().put(pay3.getId(), pay3);

        Bill bill3 = new Bill("BILL-1003", order3.getId(), "CUS-003", "Trần Quốc Bảo",
                "0987654321", "88 Cầu Giấy, Hà Nội",
                List.of(new BillItem("PRD-008", "LG UltraGear 27GR75Q-B 165Hz", 1, new BigDecimal("6890000"), new BigDecimal("6890000"))),
                o3Orig, DiscountType.NO_DISCOUNT, BigDecimal.ZERO, BigDecimal.ZERO, o3Orig,
                now.minusDays(1).plusMinutes(10), PaymentMethod.MOCK_SUCCESS, PaymentStatus.SUCCESS);
        store.getBills().put(bill3.getId(), bill3);

        // Sample Order 4 - PENDING (ready for payment testing in UI)
        OrderItem o4Item1 = new OrderItem("PRD-005", "Razer Viper V3 Pro", 1, new BigDecimal("3990000"));
        OrderItem o4Item2 = new OrderItem("PRD-022", "SteelSeries QcK Heavy XXL", 2, new BigDecimal("890000"));
        BigDecimal o4Orig = new BigDecimal("5770000");
        BigDecimal o4Disc = new BigDecimal("865500");
        BigDecimal o4Final = new BigDecimal("4904500");
        Order order4 = new Order("ORD-1004", "CUS-005", "Đỗ Anh Tuấn",
                List.of(o4Item1, o4Item2), DiscountType.VIP, new BigDecimal("0.15"),
                o4Orig, o4Disc, o4Final, now.minusHours(3), OrderStatus.PENDING);
        store.getOrders().put(order4.getId(), order4);

        // Sample Order 5 - CANCELLED
        OrderItem o5Item1 = new OrderItem("PRD-020", "HyperX QuadCast S RGB", 1, new BigDecimal("3590000"));
        Order order5 = new Order("ORD-1005", "CUS-004", "Lê Minh Khoa",
                List.of(o5Item1), DiscountType.NO_DISCOUNT, BigDecimal.ZERO,
                new BigDecimal("3590000"), BigDecimal.ZERO, new BigDecimal("3590000"),
                now.minusDays(3), OrderStatus.CANCELLED);
        store.getOrders().put(order5.getId(), order5);

        // Initial Inventory Log samples
        store.getInventoryLogs().add(new InventoryLog("INV-001", "PRD-004", "Logitech G Pro X Superlight 2",
                -1, 23, 22, "Xuất kho đơn hàng ORD-1001", now.minusDays(4).plusMinutes(5)));
        store.getInventoryLogs().add(new InventoryLog("INV-002", "PRD-001", "Wooting 60HE+ Rapid Trigger",
                -1, 15, 14, "Xuất kho đơn hàng ORD-1002", now.minusDays(2).plusHours(2)));
        store.getInventoryLogs().add(new InventoryLog("INV-003", "PRD-008", "LG UltraGear 27GR75Q-B 165Hz",
                -1, 13, 12, "Xuất kho đơn hàng ORD-1003", now.minusDays(1).plusMinutes(10)));
    }
}
