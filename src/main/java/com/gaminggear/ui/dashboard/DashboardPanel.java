package com.gaminggear.ui.dashboard;

import com.gaminggear.model.Order;
import com.gaminggear.service.DashboardService;
import com.gaminggear.ui.components.OrderStatusChartPanel;
import com.gaminggear.ui.components.RefreshablePanel;
import com.gaminggear.ui.components.RevenueChartPanel;
import com.gaminggear.ui.components.StatCard;
import com.gaminggear.ui.theme.GamingTheme;
import com.gaminggear.util.CurrencyUtil;
import com.gaminggear.util.DateTimeUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Screen 2: Real-time Dashboard displaying live KPI cards, charts, recent orders, and top selling products.
 */
public class DashboardPanel extends JPanel implements RefreshablePanel {

    private final DashboardService dashboardService;

    private final StatCard totalProductsCard;
    private final StatCard totalOrdersCard;
    private final StatCard totalRevenueCard;
    private final StatCard pendingOrdersCard;
    private final StatCard lowStockCard;

    private final RevenueChartPanel revenueChartPanel;
    private final OrderStatusChartPanel orderStatusChartPanel;
    private final DefaultTableModel recentOrdersModel;
    private final DefaultTableModel topProductsModel;

    public DashboardPanel(DashboardService dashboardService) {
        this.dashboardService = dashboardService;

        setLayout(new BorderLayout(16, 16));
        setBackground(GamingTheme.BG_APP);
        setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        // Top KPI Grid
        JPanel kpiGrid = new JPanel(new GridLayout(1, 5, 14, 0));
        kpiGrid.setOpaque(false);

        totalProductsCard = new StatCard("Tổng sản phẩm", "0", "Trong danh mục", GamingTheme.ACCENT_BLUE);
        totalOrdersCard = new StatCard("Tổng đơn hàng", "0", "Tất cả trạng thái", GamingTheme.ACCENT_PURPLE);
        totalRevenueCard = new StatCard("Tổng doanh thu", "0 VNĐ", "Đơn đã thanh toán", GamingTheme.SUCCESS);
        pendingOrdersCard = new StatCard("Đơn chờ xử lý", "0", "Trạng thái PENDING", GamingTheme.WARNING);
        lowStockCard = new StatCard("Sắp hết hàng", "0", "Tồn kho thấp", GamingTheme.DANGER);

        kpiGrid.add(totalProductsCard);
        kpiGrid.add(totalOrdersCard);
        kpiGrid.add(totalRevenueCard);
        kpiGrid.add(pendingOrdersCard);
        kpiGrid.add(lowStockCard);
        add(kpiGrid, BorderLayout.NORTH);

        // Center Charts + Tables
        JPanel centerContainer = new JPanel(new GridLayout(2, 1, 0, 16));
        centerContainer.setOpaque(false);

        JPanel chartsRow = new JPanel(new GridLayout(1, 2, 16, 0));
        chartsRow.setOpaque(false);
        revenueChartPanel = new RevenueChartPanel();
        orderStatusChartPanel = new OrderStatusChartPanel();
        chartsRow.add(revenueChartPanel);
        chartsRow.add(orderStatusChartPanel);

        JPanel tablesRow = new JPanel(new GridLayout(1, 2, 16, 0));
        tablesRow.setOpaque(false);

        // Recent Orders Card
        JPanel recentPanel = GamingTheme.createCardPanel();
        recentPanel.setLayout(new BorderLayout(0, 10));
        recentPanel.add(GamingTheme.createSectionTitle("Đơn hàng gần đây"), BorderLayout.NORTH);
        recentOrdersModel = new DefaultTableModel(new String[]{"Mã đơn", "Khách hàng", "Tổng tiền", "Trạng thái", "Ngày tạo"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable recentTable = new JTable(recentOrdersModel);
        recentPanel.add(GamingTheme.wrapTable(recentTable), BorderLayout.CENTER);

        // Top Selling Products Card
        JPanel topPanel = GamingTheme.createCardPanel();
        topPanel.setLayout(new BorderLayout(0, 10));
        topPanel.add(GamingTheme.createSectionTitle("Sản phẩm bán chạy"), BorderLayout.NORTH);
        topProductsModel = new DefaultTableModel(new String[]{"Mã SP", "Tên sản phẩm", "Đã bán", "Doanh thu"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable topTable = new JTable(topProductsModel);
        topPanel.add(GamingTheme.wrapTable(topTable), BorderLayout.CENTER);

        tablesRow.add(recentPanel);
        tablesRow.add(topPanel);

        centerContainer.add(chartsRow);
        centerContainer.add(tablesRow);
        add(centerContainer, BorderLayout.CENTER);

        refreshData();
    }

    @Override
    public void refreshData() {
        DashboardService.DashboardSummary summary = dashboardService.getSummary();
        totalProductsCard.setValue(String.valueOf(summary.totalProducts()));
        totalOrdersCard.setValue(String.valueOf(summary.totalOrders()));
        totalRevenueCard.setValue(CurrencyUtil.formatVnd(summary.totalRevenue()));
        pendingOrdersCard.setValue(String.valueOf(summary.pendingOrders()));
        lowStockCard.setValue(String.valueOf(summary.lowStockProducts()));

        revenueChartPanel.setData("Doanh thu 7 ngày gần nhất", dashboardService.getRevenueLastDays(7));
        orderStatusChartPanel.setStatusCounts(dashboardService.getOrderCountsByStatus());

        recentOrdersModel.setRowCount(0);
        List<Order> recent = dashboardService.getRecentOrders(8);
        for (Order o : recent) {
            recentOrdersModel.addRow(new Object[]{
                    o.getId(),
                    o.getCustomerName(),
                    CurrencyUtil.formatVnd(o.getFinalTotal()),
                    o.getStatus().name(),
                    DateTimeUtil.format(o.getCreatedAt())
            });
        }

        topProductsModel.setRowCount(0);
        for (DashboardService.TopSellingProduct item : dashboardService.getTopSellingProducts(8)) {
            topProductsModel.addRow(new Object[]{
                    item.productId(),
                    item.productName(),
                    item.totalQuantitySold(),
                    CurrencyUtil.formatVnd(item.totalRevenue())
            });
        }
    }
}
