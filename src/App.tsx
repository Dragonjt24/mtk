import React, {useCallback, useEffect, useState} from 'react';
import {
  Bell,
  Boxes,
  CreditCard,
  FileText,
  FolderTree,
  LayoutDashboard,
  LogOut,
  Package,
  Percent,
  PlusCircle,
  RefreshCw,
  Settings as SettingsIcon,
  ShoppingCart,
  Store,
  Users,
  X,
} from 'lucide-react';
import {javaApi} from './api/javaClient';
import {CategoriesView} from './components/CategoriesView';
import {CreateOrderView} from './components/CreateOrderView';
import {CustomersView} from './components/CustomersView';
import {DashboardView} from './components/DashboardView';
import {DiscountsView} from './components/DiscountsView';
import {InventoryView} from './components/InventoryView';
import {InvoicesView} from './components/InvoicesView';
import {LoginView} from './components/LoginView';
import {OrdersView} from './components/OrdersView';
import {PaymentsView} from './components/PaymentsView';
import {ProductsView} from './components/ProductsView';
import {SettingsView} from './components/SettingsView';
import {StorefrontView} from './components/store/StorefrontView';
import type {ScreenKey, User} from './types';

interface NavItem {
  key: ScreenKey;
  label: string;
  title: string;
  icon: React.ComponentType<{className?: string}>;
}

const NAV_ITEMS: NavItem[] = [
  {key: 'DASHBOARD', label: 'Tổng quan', title: 'Tổng quan hệ thống', icon: LayoutDashboard},
  {key: 'PRODUCTS', label: 'Sản phẩm', title: 'Quản lý sản phẩm Gaming Gear', icon: Package},
  {key: 'CATEGORIES', label: 'Danh mục', title: 'Quản lý danh mục sản phẩm', icon: FolderTree},
  {key: 'CUSTOMERS', label: 'Khách hàng', title: 'Quản lý khách hàng & Lịch sử mua', icon: Users},
  {key: 'ORDERS', label: 'Đơn hàng', title: 'Quản lý đơn hàng', icon: ShoppingCart},
  {key: 'CREATE_ORDER', label: 'Tạo đơn hàng', title: 'Tạo đơn hàng mới', icon: PlusCircle},
  {key: 'DISCOUNTS', label: 'Giảm giá', title: 'Chính sách giảm giá (Factory & Strategy)', icon: Percent},
  {key: 'PAYMENTS', label: 'Thanh toán', title: 'Thanh toán mô phỏng (Observer Pattern)', icon: CreditCard},
  {key: 'INVOICES', label: 'Hóa đơn', title: 'Quản lý hóa đơn & Xuất PDF', icon: FileText},
  {key: 'INVENTORY', label: 'Tồn kho', title: 'Quản lý tồn kho & Nhật ký biến động', icon: Boxes},
  {key: 'SETTINGS', label: 'Cài đặt', title: 'Cài đặt hệ thống & Nhân viên', icon: SettingsIcon},
];

export default function App() {
  const [portalMode, setPortalMode] = useState<'STORE' | 'ADMIN'>('STORE');
  const [currentUser, setCurrentUser] = useState<User | null>(null);
  const [activeScreen, setActiveScreen] = useState<ScreenKey>('DASHBOARD');
  const [refreshKey, setRefreshKey] = useState(0);
  const [toast, setToast] = useState<{type: 'success' | 'error'; text: string} | null>(null);
  const [pendingPaymentOrderId, setPendingPaymentOrderId] = useState<string | null>(null);
  const [selectedBillId, setSelectedBillId] = useState<string | null>(null);
  const [notifications, setNotifications] = useState<string[]>([]);
  const [showNotifModal, setShowNotifModal] = useState(false);

  const loadNotifications = useCallback(async () => {
    try {
      const list = await javaApi.getNotifications();
      setNotifications(list);
    } catch {
      // Keep previous notifications on transient failure
    }
  }, []);

  useEffect(() => {
    if (currentUser) {
      loadNotifications();
    }
  }, [currentUser, refreshKey, loadNotifications]);

  useEffect(() => {
    if (!toast) return;
    const timer = window.setTimeout(() => setToast(null), 4000);
    return () => window.clearTimeout(timer);
  }, [toast]);

  const handleNotify = useCallback((type: 'success' | 'error', text: string) => {
    setToast({type, text});
    if (type === 'success') {
      setRefreshKey((k) => k + 1);
    }
  }, []);

  const handleDataChanged = useCallback((message?: string) => {
    if (message) {
      setToast({type: 'success', text: message});
    }
    setRefreshKey((k) => k + 1);
  }, []);

  const handleLogout = async () => {
    try {
      await javaApi.logout();
    } catch {
      // Ignore logout network error
    }
    setCurrentUser(null);
    setActiveScreen('DASHBOARD');
  };

  if (portalMode === 'STORE') {
    return (
      <StorefrontView
        refreshKey={refreshKey}
        onDataChanged={handleDataChanged}
        onOpenAdmin={() => {
          setRefreshKey((k) => k + 1);
          setPortalMode('ADMIN');
        }}
      />
    );
  }

  if (!currentUser) {
    return (
      <LoginView
        onSuccess={(user) => {
          setCurrentUser(user);
          setActiveScreen('DASHBOARD');
          setRefreshKey((k) => k + 1);
        }}
        onBackToStore={() => {
          setRefreshKey((k) => k + 1);
          setPortalMode('STORE');
        }}
      />
    );
  }

  const activeNav = NAV_ITEMS.find((n) => n.key === activeScreen) ?? NAV_ITEMS[0];

  return (
    <div className="flex min-h-screen bg-[#0B0F19] text-slate-100">
      {/* Sidebar */}
      <aside className="flex w-60 shrink-0 flex-col border-r border-slate-800 bg-[#0F1524]">
        <div className="border-b border-slate-800 px-5 py-4">
          <p className="font-display text-base font-bold tracking-tight text-slate-100 whitespace-nowrap">
            Gaming Gear Hub
          </p>
          <p className="mt-0.5 text-xs text-slate-400">Java 17 Core · InMemory + JSON</p>
        </div>

        <nav className="flex-1 space-y-1 overflow-y-auto px-3 py-4" aria-label="Điều hướng chính">
          {NAV_ITEMS.map((item) => {
            const Icon = item.icon;
            const isActive = activeScreen === item.key;
            return (
              <button
                key={item.key}
                type="button"
                onClick={() => setActiveScreen(item.key)}
                className={`flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-left text-sm font-medium transition-colors whitespace-nowrap shrink-0 ${
                  isActive
                    ? 'bg-blue-600 text-white'
                    : 'text-slate-300 hover:bg-slate-800/70 hover:text-white'
                }`}
              >
                <Icon className="h-4 w-4 shrink-0" />
                <span className="truncate">{item.label}</span>
              </button>
            );
          })}
        </nav>

        <div className="border-t border-slate-800 p-4">
          <div className="mb-3">
            <p className="truncate text-sm font-medium text-slate-100">{currentUser.fullName}</p>
            <p className="text-xs text-slate-400">
              {currentUser.username} · {currentUser.role}
            </p>
          </div>
          <button
            type="button"
            onClick={handleLogout}
            className="flex w-full items-center justify-center gap-2 rounded-lg border border-slate-700 bg-slate-800 px-3 py-2 text-xs font-medium text-slate-200 hover:bg-slate-700 whitespace-nowrap shrink-0"
          >
            <LogOut className="h-3.5 w-3.5" />
            Đăng xuất
          </button>
        </div>
      </aside>

      {/* Main Content */}
      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex items-center justify-between gap-6 border-b border-slate-800 bg-[#0F1524] px-6 py-4">
          <h1 className="font-display truncate text-xl font-semibold text-slate-100">
            {activeNav.title}
          </h1>

          <div className="flex items-center gap-3 shrink-0">
            <button
              type="button"
              onClick={() => {
                setRefreshKey((k) => k + 1);
                setPortalMode('STORE');
              }}
              className="flex items-center gap-1.5 rounded-lg border border-slate-700 bg-slate-800 px-3 py-2 text-xs font-medium text-sky-300 hover:bg-slate-700 whitespace-nowrap shrink-0"
            >
              <Store className="h-3.5 w-3.5" />
              Xem Cửa hàng
            </button>

            <button
              type="button"
              onClick={() => setRefreshKey((k) => k + 1)}
              aria-label="Làm mới dữ liệu"
              className="flex items-center gap-1.5 rounded-lg border border-slate-700 bg-slate-800 px-3 py-2 text-xs font-medium text-slate-200 hover:bg-slate-700 whitespace-nowrap shrink-0"
            >
              <RefreshCw className="h-3.5 w-3.5" />
              Làm mới
            </button>

            <button
              type="button"
              onClick={() => {
                loadNotifications();
                setShowNotifModal(true);
              }}
              aria-label="Xem thông báo Observer"
              className="flex items-center gap-1.5 rounded-lg border border-slate-700 bg-slate-800 px-3 py-2 text-xs font-medium text-slate-200 hover:bg-slate-700 whitespace-nowrap shrink-0"
            >
              <Bell className="h-3.5 w-3.5 text-amber-400" />
              <span>Thông báo ({notifications.length})</span>
            </button>

            <button
              type="button"
              onClick={() => setActiveScreen('CREATE_ORDER')}
              className="rounded-lg bg-blue-600 px-4 py-2 text-xs font-semibold text-white hover:bg-blue-500 whitespace-nowrap shrink-0"
            >
              Tạo đơn hàng
            </button>
          </div>
        </header>

        {toast && (
          <div
            role="status"
            className={`mx-6 mt-4 flex items-center justify-between rounded-lg border px-4 py-2.5 text-sm ${
              toast.type === 'error'
                ? 'border-red-800/70 bg-red-950/40 text-red-200'
                : 'border-emerald-800/70 bg-emerald-950/40 text-emerald-200'
            }`}
          >
            <span>{toast.text}</span>
            <button
              type="button"
              aria-label="Đóng thông báo trạng thái"
              onClick={() => setToast(null)}
              className={
                toast.type === 'error'
                  ? 'text-red-300 hover:text-white'
                  : 'text-emerald-300 hover:text-white'
              }
            >
              <X className="h-4 w-4" />
            </button>
          </div>
        )}

        <main className="flex-1 overflow-y-auto p-6">
          {activeScreen === 'DASHBOARD' && <DashboardView refreshKey={refreshKey} />}
          {activeScreen === 'PRODUCTS' && (
            <ProductsView refreshKey={refreshKey} onNotify={handleNotify} />
          )}
          {activeScreen === 'CATEGORIES' && (
            <CategoriesView refreshKey={refreshKey} onNotify={handleNotify} />
          )}
          {activeScreen === 'CUSTOMERS' && (
            <CustomersView refreshKey={refreshKey} onNotify={handleNotify} />
          )}
          {activeScreen === 'ORDERS' && (
            <OrdersView
              refreshKey={refreshKey}
              onDataChanged={handleDataChanged}
              onPayOrder={(orderId) => {
                setPendingPaymentOrderId(orderId);
                setActiveScreen('PAYMENTS');
              }}
              onCreateNewOrder={() => setActiveScreen('CREATE_ORDER')}
            />
          )}
          {activeScreen === 'CREATE_ORDER' && (
            <CreateOrderView
              refreshKey={refreshKey}
              onOrderCreated={(orderId, goToPayment) => {
                if (goToPayment) {
                  setPendingPaymentOrderId(orderId);
                  setToast({type: 'success', text: `Đã tạo đơn ${orderId}. Tiếp tục thanh toán.`});
                  setRefreshKey((k) => k + 1);
                  setActiveScreen('PAYMENTS');
                } else {
                  setToast({type: 'success', text: `Đã tạo đơn hàng ${orderId} (PENDING).`});
                  setRefreshKey((k) => k + 1);
                  setActiveScreen('ORDERS');
                }
              }}
            />
          )}
          {activeScreen === 'DISCOUNTS' && <DiscountsView refreshKey={refreshKey} />}
          {activeScreen === 'PAYMENTS' && (
            <PaymentsView
              refreshKey={refreshKey}
              initialOrderId={pendingPaymentOrderId}
              onDataChanged={handleDataChanged}
              onOpenInvoice={(billId) => {
                setSelectedBillId(billId);
                setActiveScreen('INVOICES');
              }}
            />
          )}
          {activeScreen === 'INVOICES' && (
            <InvoicesView refreshKey={refreshKey} initialBillId={selectedBillId} />
          )}
          {activeScreen === 'INVENTORY' && (
            <InventoryView refreshKey={refreshKey} onDataChanged={handleDataChanged} />
          )}
          {activeScreen === 'SETTINGS' && (
            <SettingsView refreshKey={refreshKey} onDataChanged={handleDataChanged} />
          )}
        </main>
      </div>

      {/* Observer Notifications Modal */}
      {showNotifModal && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4"
          role="dialog"
          aria-modal="true"
          aria-labelledby="notif-modal-title"
        >
          <div className="w-full max-w-lg rounded-xl border border-slate-800 bg-[#111827] p-6 shadow-2xl">
            <div className="flex items-center justify-between border-b border-slate-800 pb-3">
              <h2 id="notif-modal-title" className="text-base font-semibold text-slate-100">
                Nhật ký NotificationObserver (Java Core)
              </h2>
              <button
                type="button"
                aria-label="Đóng cửa sổ thông báo"
                onClick={() => setShowNotifModal(false)}
                className="rounded-lg p-1 text-slate-400 hover:bg-slate-800 hover:text-white"
              >
                <X className="h-4 w-4" />
              </button>
            </div>
            <div className="mt-4 max-h-80 space-y-2 overflow-y-auto pr-1">
              {notifications.length === 0 ? (
                <p className="py-8 text-center text-sm text-slate-400">
                  Chưa có thông báo sự kiện thanh toán nào.
                </p>
              ) : (
                notifications.map((msg, idx) => (
                  <div
                    key={`${idx}-${msg.slice(0, 20)}`}
                    className="rounded-lg border border-slate-800 bg-slate-900/70 px-3 py-2 text-xs text-slate-200"
                  >
                    {msg}
                  </div>
                ))
              )}
            </div>
            <div className="mt-4 flex justify-end">
              <button
                type="button"
                onClick={() => setShowNotifModal(false)}
                className="rounded-lg bg-slate-800 px-4 py-2 text-xs font-semibold text-slate-100 hover:bg-slate-700"
              >
                Đóng
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
