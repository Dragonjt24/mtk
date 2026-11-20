import type {
  AppSettings,
  Bill,
  Category,
  Customer,
  DashboardPayload,
  DiscountInfo,
  DiscountType,
  InventoryLog,
  MembershipTier,
  Order,
  Payment,
  PaymentMethod,
  PricingResult,
  Product,
  ProductStatus,
  PromotionConfig,
  User,
  UserRole,
} from '../types';

async function request<T>(url: string, options?: RequestInit): Promise<T> {
  const response = await fetch(url, {
    headers: {
      'Content-Type': 'application/json',
      ...(options?.headers ?? {}),
    },
    ...options,
  });

  if (response.status === 204) {
    return undefined as T;
  }

  const text = await response.text();
  const data = text ? JSON.parse(text) : null;
  if (!response.ok) {
    throw new Error(data?.error || `Lỗi yêu cầu HTTP ${response.status}`);
  }
  return data as T;
}

export const javaApi = {
  // Auth & Users
  login: (username: string, password: string) =>
    request<User>('/api/java/auth/login', {
      method: 'POST',
      body: JSON.stringify({username, password}),
    }),
  registerCustomer: (payload: {
    username: string;
    password: string;
    fullName: string;
    phone: string;
    email: string;
    address: string;
  }) =>
    request<User>('/api/java/auth/register', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  logout: () => request<{loggedOut: boolean}>('/api/java/auth/logout', {method: 'POST'}),
  getUsers: () => request<User[]>('/api/java/auth/users'),
  createUser: (payload: {username: string; password: string; fullName: string; role: UserRole}) =>
    request<User>('/api/java/auth/users', {method: 'POST', body: JSON.stringify(payload)}),
  updateUserStatus: (id: string, payload: {active: boolean; role: UserRole; fullName: string}) =>
    request<User>(`/api/java/auth/users/${encodeURIComponent(id)}/status`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    }),
  changePassword: (id: string, newPassword: string) =>
    request<{updated: boolean}>(`/api/java/auth/users/${encodeURIComponent(id)}/password`, {
      method: 'PUT',
      body: JSON.stringify({newPassword}),
    }),
  updateCustomerProfile: (payload: {
    userId: string;
    fullName: string;
    phone: string;
    email: string;
    address: string;
    newPassword?: string;
  }) =>
    request<{user: User; customer: Customer | null}>('/api/java/auth/profile', {
      method: 'PUT',
      body: JSON.stringify(payload),
    }),

  // Dashboard
  getDashboard: () => request<DashboardPayload>('/api/java/dashboard'),

  // Categories
  getCategories: () => request<Category[]>('/api/java/categories'),
  createCategory: (payload: {name: string; description: string; active: boolean}) =>
    request<Category>('/api/java/categories', {method: 'POST', body: JSON.stringify(payload)}),
  updateCategory: (id: string, payload: {name: string; description: string; active: boolean}) =>
    request<Category>(`/api/java/categories/${encodeURIComponent(id)}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    }),
  deleteCategory: (id: string) =>
    request<{deleted: boolean}>(`/api/java/categories/${encodeURIComponent(id)}`, {method: 'DELETE'}),

  // Products
  getBrands: () => request<string[]>('/api/java/brands'),
  getProducts: (params: {
    keyword?: string;
    categoryId?: string;
    brand?: string;
    minPrice?: string;
    maxPrice?: string;
    sort?: string;
  }) => {
    const qs = new URLSearchParams();
    Object.entries(params).forEach(([k, v]) => {
      if (v !== undefined && v !== '') qs.set(k, v);
    });
    const suffix = qs.toString() ? `?${qs.toString()}` : '';
    return request<Product[]>(`/api/java/products${suffix}`);
  },
  createProduct: (payload: {
    name: string;
    categoryId: string;
    brand: string;
    price: number;
    stockQuantity: number;
    description: string;
    imagePath: string;
    status: ProductStatus;
  }) => request<Product>('/api/java/products', {method: 'POST', body: JSON.stringify(payload)}),
  updateProduct: (
    id: string,
    payload: {
      name: string;
      categoryId: string;
      brand: string;
      price: number;
      stockQuantity: number;
      description: string;
      imagePath: string;
      status: ProductStatus;
    },
  ) =>
    request<Product>(`/api/java/products/${encodeURIComponent(id)}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    }),
  deleteProduct: (id: string) =>
    request<{deleted: boolean}>(`/api/java/products/${encodeURIComponent(id)}`, {method: 'DELETE'}),
  batchDeleteProducts: (ids: string[]) =>
    request<{deletedCount: number}>('/api/java/products/batch-delete', {
      method: 'POST',
      body: JSON.stringify({ids}),
    }),

  // Customers
  getCustomers: (keyword = '') => {
    const qs = keyword ? `?keyword=${encodeURIComponent(keyword)}` : '';
    return request<Customer[]>(`/api/java/customers${qs}`);
  },
  createCustomer: (payload: {
    fullName: string;
    phone: string;
    email: string;
    address: string;
    membershipTier: MembershipTier;
  }) => request<Customer>('/api/java/customers', {method: 'POST', body: JSON.stringify(payload)}),
  updateCustomer: (
    id: string,
    payload: {
      fullName: string;
      phone: string;
      email: string;
      address: string;
      membershipTier: MembershipTier;
    },
  ) =>
    request<Customer>(`/api/java/customers/${encodeURIComponent(id)}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    }),
  deleteCustomer: (id: string) =>
    request<{deleted: boolean}>(`/api/java/customers/${encodeURIComponent(id)}`, {method: 'DELETE'}),
  getCustomerOrders: (id: string) =>
    request<Order[]>(`/api/java/customers/${encodeURIComponent(id)}/orders`),

  // Discounts & Orders
  getDiscounts: () => request<DiscountInfo[]>('/api/java/discounts'),
  getPromotions: () => request<PromotionConfig>('/api/java/promotions'),
  updatePromotions: (payload: PromotionConfig) =>
    request<PromotionConfig>('/api/java/promotions', {
      method: 'PUT',
      body: JSON.stringify(payload),
    }),
  previewPricing: (
    originalTotal: number,
    discountType?: DiscountType,
    options?: {customerId?: string; useAutoPolicy?: boolean},
  ) =>
    request<PricingResult>('/api/java/orders/preview-pricing', {
      method: 'POST',
      body: JSON.stringify({
        originalTotal,
        discountType,
        customerId: options?.customerId,
        useAutoPolicy: options?.useAutoPolicy ?? false,
      }),
    }),
  getOrders: (status = 'ALL') =>
    request<Order[]>(`/api/java/orders?status=${encodeURIComponent(status)}`),
  createOrder: (payload: {
    customerId: string;
    discountType?: DiscountType;
    useAutoPolicy?: boolean;
    items: Array<{productId: string; quantity: number}>;
  }) => request<Order>('/api/java/orders', {method: 'POST', body: JSON.stringify(payload)}),
  changeOrderDiscount: (id: string, discountType: DiscountType) =>
    request<Order>(`/api/java/orders/${encodeURIComponent(id)}/discount`, {
      method: 'PUT',
      body: JSON.stringify({discountType}),
    }),
  cancelOrder: (id: string) =>
    request<Order>(`/api/java/orders/${encodeURIComponent(id)}/cancel`, {method: 'POST'}),

  // Payments & Notifications
  getPayments: () => request<Payment[]>('/api/java/payments'),
  processPayment: (orderId: string, method: PaymentMethod) =>
    request<{payment: Payment; order: Order; bill: Bill | null}>('/api/java/payments/process', {
      method: 'POST',
      body: JSON.stringify({orderId, method}),
    }),
  getNotifications: () => request<string[]>('/api/java/notifications'),

  // Invoices
  getBills: () => request<Bill[]>('/api/java/bills'),
  getBillPdfUrl: (billId: string) => `/api/java/bills/${encodeURIComponent(billId)}/pdf`,

  // Inventory
  getLowStockProducts: () => request<Product[]>('/api/java/inventory/low-stock'),
  getInventoryLogs: () => request<InventoryLog[]>('/api/java/inventory/logs'),
  adjustInventory: (payload: {productId: string; mode: 'DELTA' | 'SET'; amount: number; reason: string}) =>
    request<Product>('/api/java/inventory/adjust', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),

  // Settings
  getSettings: () => request<AppSettings>('/api/java/settings'),
  updateSettings: (payload: {lowStockThreshold?: number; autoSaveEnabled?: boolean}) =>
    request<{updated: boolean}>('/api/java/settings', {
      method: 'PUT',
      body: JSON.stringify(payload),
    }),
  saveJsonNow: () => request<{saved: boolean}>('/api/java/settings/save', {method: 'POST'}),
  reloadJson: () => request<{reloaded: boolean}>('/api/java/settings/reload', {method: 'POST'}),
  resetSampleData: () => request<{reset: boolean}>('/api/java/settings/reset', {method: 'POST'}),
};
