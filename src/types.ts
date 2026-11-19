export type UserRole = 'ADMIN' | 'STAFF';
export type ProductStatus = 'ACTIVE' | 'INACTIVE' | 'OUT_OF_STOCK';
export type MembershipTier = 'NORMAL' | 'VIP';
export type OrderStatus = 'PENDING' | 'PAID' | 'CANCELLED';
export type PaymentMethod = 'COD' | 'MOCK_SUCCESS' | 'MOCK_FAILURE';
export type PaymentStatus = 'SUCCESS' | 'FAILED';
export type DiscountType = 'NO_DISCOUNT' | 'VIP' | 'SEASONAL';

export type ScreenKey =
  | 'DASHBOARD'
  | 'PRODUCTS'
  | 'CATEGORIES'
  | 'CUSTOMERS'
  | 'ORDERS'
  | 'CREATE_ORDER'
  | 'DISCOUNTS'
  | 'PAYMENTS'
  | 'INVOICES'
  | 'INVENTORY'
  | 'SETTINGS';

export interface User {
  id: string;
  username: string;
  fullName: string;
  role: UserRole;
  active: boolean;
}

export interface Category {
  id: string;
  name: string;
  description: string;
  active: boolean;
  productCount?: number;
}

export interface Product {
  id: string;
  name: string;
  categoryId: string;
  brand: string;
  price: number;
  stockQuantity: number;
  description: string;
  imagePath: string;
  status: ProductStatus;
}

export interface Customer {
  id: string;
  fullName: string;
  phone: string;
  email: string;
  address: string;
  membershipTier: MembershipTier;
  totalSpent: number;
}

export interface OrderItem {
  productId: string;
  productName: string;
  quantity: number;
  unitPrice: number;
}

export interface Order {
  id: string;
  customerId: string;
  customerName: string;
  items: OrderItem[];
  discountType: DiscountType;
  discountRate: number;
  originalTotal: number;
  discountAmount: number;
  finalTotal: number;
  createdAt: string;
  status: OrderStatus;
}

export interface PricingResult {
  discountType: DiscountType;
  discountRate: number;
  originalTotal: number;
  discountAmount: number;
  finalTotal: number;
  strategyDescription: string;
}

export interface DiscountInfo {
  type: DiscountType;
  displayName: string;
  rate: number;
  description: string;
  strategyClass: string;
}

export interface Payment {
  id: string;
  orderId: string;
  method: PaymentMethod;
  amount: number;
  status: PaymentStatus;
  paidAt: string;
  message: string;
}

export interface BillItem {
  productId: string;
  productName: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
}

export interface Bill {
  id: string;
  orderId: string;
  customerId: string;
  customerName: string;
  customerPhone: string;
  customerAddress: string;
  items: BillItem[];
  originalTotal: number;
  discountType: DiscountType;
  discountRate: number;
  discountAmount: number;
  finalTotal: number;
  issuedAt: string;
  paymentMethod: PaymentMethod;
  paymentStatus: PaymentStatus;
}

export interface InventoryLog {
  id: string;
  productId: string;
  productName: string;
  changeAmount: number;
  previousStock: number;
  newStock: number;
  reason: string;
  createdAt: string;
}

export interface DashboardPayload {
  summary: {
    totalProducts: number;
    totalOrders: number;
    totalRevenue: number;
    pendingOrders: number;
    lowStockProducts: number;
  };
  revenueLastDays: Record<string, number>;
  revenueByCategory: Record<string, number>;
  orderStatusCounts: Record<OrderStatus, number>;
  recentOrders: Order[];
  topProducts: Array<{
    productId: string;
    productName: string;
    totalQuantitySold: number;
    totalRevenue: number;
  }>;
}

export interface AppSettings {
  lowStockThreshold: number;
  autoSaveEnabled: boolean;
  storagePath: string;
  storeName: string;
  storeAddress: string;
  storePhone: string;
}
