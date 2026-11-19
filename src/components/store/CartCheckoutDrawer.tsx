import React, {useEffect, useMemo, useState} from 'react';
import {CheckCircle2, Download, Minus, Plus, Trash2, X} from 'lucide-react';
import {javaApi} from '../../api/javaClient';
import type {
  Bill,
  Customer,
  DiscountType,
  Order,
  Payment,
  PaymentMethod,
  PricingResult,
  Product,
} from '../../types';
import {
  discountLabel,
  formatVnd,
  orderStatusLabel,
  paymentMethodLabel,
} from '../../utils/format';
import {
  getAvailableDiscountTypes,
  getDefaultDiscountForCustomer,
} from '../../utils/storeHelpers';

export interface StoreCartItem {
  product: Product;
  quantity: number;
}

interface CartCheckoutDrawerProps {
  open: boolean;
  cart: StoreCartItem[];
  customers: Customer[];
  selectedCustomerId: string;
  onSelectCustomerId: (id: string) => void;
  onUpdateQty: (productId: string, delta: number) => void;
  onRemoveItem: (productId: string) => void;
  onClearCart: () => void;
  onClose: () => void;
  onCheckoutCompleted: (result: {
    order: Order;
    payment: Payment | null;
    bill: Bill | null;
    customer: Customer;
  }) => void;
  onOpenMyOrders: (customerId: string) => void;
  onSwitchToAdmin: () => void;
}

export const CartCheckoutDrawer: React.FC<CartCheckoutDrawerProps> = ({
  open,
  cart,
  customers,
  selectedCustomerId,
  onSelectCustomerId,
  onUpdateQty,
  onRemoveItem,
  onClearCart,
  onClose,
  onCheckoutCompleted,
  onOpenMyOrders,
  onSwitchToAdmin,
}) => {
  const [customerMode, setCustomerMode] = useState<'EXISTING' | 'NEW'>('EXISTING');
  const [newName, setNewName] = useState('');
  const [newPhone, setNewPhone] = useState('');
  const [newEmail, setNewEmail] = useState('');
  const [newAddress, setNewAddress] = useState('');

  const [discountType, setDiscountType] = useState<DiscountType>('NO_DISCOUNT');
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>('MOCK_SUCCESS');
  const [pricing, setPricing] = useState<PricingResult | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [receipt, setReceipt] = useState<{
    order: Order;
    payment: Payment | null;
    bill: Bill | null;
    customer: Customer;
  } | null>(null);

  const selectedCustomer = useMemo(
    () =>
      customerMode === 'EXISTING'
        ? customers.find((c) => c.id === selectedCustomerId) ?? customers[0] ?? null
        : null,
    [customerMode, customers, selectedCustomerId],
  );

  const availableDiscounts = useMemo(
    () => getAvailableDiscountTypes(selectedCustomer),
    [selectedCustomer],
  );

  // Automatically default discount policy when customer tier changes
  useEffect(() => {
    setDiscountType(getDefaultDiscountForCustomer(selectedCustomer));
  }, [selectedCustomer?.id, selectedCustomer?.membershipTier, customerMode]);

  const rawSubtotal = useMemo(
    () => cart.reduce((sum, item) => sum + item.product.price * item.quantity, 0),
    [cart],
  );

  useEffect(() => {
    if (!open) return;
    let active = true;
    javaApi
      .previewPricing(rawSubtotal, discountType)
      .then((res) => {
        if (active) setPricing(res);
      })
      .catch(() => {
        if (active) setPricing(null);
      });
    return () => {
      active = false;
    };
  }, [open, rawSubtotal, discountType]);

  if (!open) return null;

  const resolveCheckoutCustomer = async (): Promise<Customer> => {
    if (customerMode === 'EXISTING') {
      if (!selectedCustomer) {
        throw new Error('Vui lòng chọn khách hàng.');
      }
      return selectedCustomer;
    }

    const cleanPhone = newPhone.trim();
    const cleanName = newName.trim();
    const cleanEmail = newEmail.trim();
    const cleanAddress = newAddress.trim();
    if (!cleanName || !cleanPhone || !cleanEmail || !cleanAddress) {
      throw new Error('Vui lòng điền đầy đủ họ tên, số điện thoại, email và địa chỉ giao hàng.');
    }

    // Reuse existing customer if phone already registered in Java store
    const matches = await javaApi.getCustomers(cleanPhone);
    const exactMatch = matches.find((c) => c.phone === cleanPhone);
    if (exactMatch) {
      onSelectCustomerId(exactMatch.id);
      return exactMatch;
    }

    const created = await javaApi.createCustomer({
      fullName: cleanName,
      phone: cleanPhone,
      email: cleanEmail,
      address: cleanAddress,
      membershipTier: 'NORMAL',
    });
    onSelectCustomerId(created.id);
    return created;
  };

  const handleCheckout = async (payNow: boolean) => {
    if (cart.length === 0) {
      setError('Giỏ hàng đang trống.');
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      const customer = await resolveCheckoutCustomer();
      const order = await javaApi.createOrder({
        customerId: customer.id,
        discountType,
        items: cart.map((c) => ({productId: c.product.id, quantity: c.quantity})),
      });

      let payment: Payment | null = null;
      let bill: Bill | null = null;
      let finalOrder = order;

      if (payNow) {
        const payRes = await javaApi.processPayment(order.id, paymentMethod);
        payment = payRes.payment;
        bill = payRes.bill;
        finalOrder = payRes.order;
      }

      const outcome = {order: finalOrder, payment, bill, customer};
      setReceipt(outcome);
      onClearCart();
      onCheckoutCompleted(outcome);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể hoàn tất đặt hàng.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex justify-end bg-black/70"
      role="dialog"
      aria-modal="true"
      aria-labelledby="cart-drawer-title"
    >
      <div className="flex h-full w-full max-w-xl flex-col border-l border-slate-800 bg-[#0F1524] shadow-2xl">
        <div className="flex items-center justify-between border-b border-slate-800 px-6 py-4">
          <div>
            <h2 id="cart-drawer-title" className="font-display text-lg font-semibold text-slate-100">
              {receipt ? 'Xác nhận đơn hàng' : `Giỏ hàng & Thanh toán (${cart.length} dòng)`}
            </h2>
            <p className="text-xs text-slate-400">
              Liên kết trực tiếp kho hàng, giảm giá và hóa đơn với hệ thống quản trị Java 17
            </p>
          </div>
          <button
            type="button"
            onClick={() => {
              setReceipt(null);
              setError(null);
              onClose();
            }}
            aria-label="Đóng giỏ hàng"
            className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-800 hover:text-white"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="flex-1 overflow-y-auto p-6">
          {receipt ? (
            <div className="space-y-5">
              <div
                className={`rounded-xl border p-5 ${
                  receipt.order.status === 'PAID'
                    ? 'border-emerald-800/70 bg-emerald-950/25'
                    : receipt.payment?.status === 'FAILED'
                      ? 'border-rose-800/70 bg-rose-950/25'
                      : 'border-amber-800/70 bg-amber-950/25'
                }`}
              >
                <div className="flex items-start gap-3">
                  <CheckCircle2
                    className={`mt-0.5 h-5 w-5 shrink-0 ${
                      receipt.order.status === 'PAID'
                        ? 'text-emerald-400'
                        : receipt.payment?.status === 'FAILED'
                          ? 'text-rose-400'
                          : 'text-amber-400'
                    }`}
                  />
                  <div>
                    <h3 className="text-base font-semibold text-slate-100">
                      Đơn hàng {receipt.order.id} · {orderStatusLabel(receipt.order.status)}
                    </h3>
                    <p className="mt-1 text-sm text-slate-300">
                      {receipt.payment
                        ? receipt.payment.message
                        : 'Đơn hàng đã được tạo ở trạng thái PENDING và đồng bộ sang trang Quản trị.'}
                    </p>
                  </div>
                </div>

                {receipt.bill && (
                  <p className="mt-3 border-t border-slate-800/80 pt-3 text-xs text-slate-300">
                    Observer đã đồng bộ: Tồn kho đã tự động trừ · Hóa đơn{' '}
                    <span className="font-mono-tabular text-sky-400">{receipt.bill.id}</span> đã được phát hành.
                  </p>
                )}
              </div>

              <div className="rounded-xl border border-slate-800 bg-[#111827] p-5 space-y-3 text-sm">
                <div className="flex justify-between">
                  <span className="text-slate-400">Khách hàng</span>
                  <span className="font-medium text-slate-100">{receipt.customer.fullName}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-400">Chính sách áp dụng</span>
                  <span className="text-slate-200">{receipt.order.discountType}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-400">Tổng thanh toán</span>
                  <span className="font-mono-tabular font-semibold text-emerald-400">
                    {formatVnd(receipt.order.finalTotal)}
                  </span>
                </div>
              </div>

              <div className="flex flex-wrap gap-3">
                {receipt.bill && (
                  <a
                    href={javaApi.getBillPdfUrl(receipt.bill.id)}
                    download={`${receipt.bill.id}.pdf`}
                    className="inline-flex whitespace-nowrap shrink-0 flex-1 items-center justify-center gap-2 rounded-lg bg-blue-600 px-4 py-2.5 text-sm font-semibold text-white hover:bg-blue-500"
                  >
                    <Download className="h-4 w-4" />
                    Tải hóa đơn PDF
                  </a>
                )}
                <button
                  type="button"
                  onClick={() => {
                    const custId = receipt.customer.id;
                    setReceipt(null);
                    onClose();
                    onOpenMyOrders(custId);
                  }}
                  className="whitespace-nowrap shrink-0 flex-1 rounded-lg border border-slate-700 bg-slate-800 px-4 py-2.5 text-sm font-semibold text-slate-100 hover:bg-slate-700"
                >
                  Xem Đơn hàng của tôi
                </button>
              </div>

              <button
                type="button"
                onClick={() => {
                  setReceipt(null);
                  onClose();
                  onSwitchToAdmin();
                }}
                className="w-full rounded-lg border border-slate-800 bg-slate-900 px-4 py-2 text-xs font-medium text-sky-400 hover:bg-slate-800"
              >
                Mở Cổng Quản trị để kiểm tra đồng bộ kho & hóa đơn →
              </button>
            </div>
          ) : cart.length === 0 ? (
            <div className="py-16 text-center">
              <p className="text-sm text-slate-400">Giỏ hàng của bạn hiện đang trống.</p>
              <button
                type="button"
                onClick={onClose}
                className="mt-4 rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-500"
              >
                Tiếp tục chọn Gaming Gear
              </button>
            </div>
          ) : (
            <div className="space-y-6">
              {/* Itemized Cart */}
              <div>
                <div className="flex items-center justify-between">
                  <h3 className="text-xs font-semibold uppercase tracking-wider text-slate-400">
                    Sản phẩm đã chọn
                  </h3>
                  <button
                    type="button"
                    onClick={onClearCart}
                    className="text-xs text-rose-400 hover:underline"
                  >
                    Xóa toàn bộ
                  </button>
                </div>
                <div className="mt-2 divide-y divide-slate-800 border-y border-slate-800">
                  {cart.map((entry) => (
                    <div key={entry.product.id} className="flex items-center justify-between gap-3 py-3">
                      <div className="min-w-0 flex-1">
                        <p className="truncate text-sm font-medium text-slate-100">
                          {entry.product.name}
                        </p>
                        <p className="font-mono-tabular text-xs text-slate-400">
                          {formatVnd(entry.product.price)} · Còn {entry.product.stockQuantity}
                        </p>
                      </div>
                      <div className="flex items-center gap-1.5">
                        <button
                          type="button"
                          aria-label={`Giảm số lượng ${entry.product.name}`}
                          onClick={() => onUpdateQty(entry.product.id, -1)}
                          className="flex h-7 w-7 items-center justify-center rounded border border-slate-700 bg-slate-900 text-slate-200 hover:bg-slate-800"
                        >
                          <Minus className="h-3.5 w-3.5" />
                        </button>
                        <span className="font-mono-tabular w-7 text-center text-sm text-slate-100">
                          {entry.quantity}
                        </span>
                        <button
                          type="button"
                          aria-label={`Tăng số lượng ${entry.product.name}`}
                          onClick={() => onUpdateQty(entry.product.id, 1)}
                          className="flex h-7 w-7 items-center justify-center rounded border border-slate-700 bg-slate-900 text-slate-200 hover:bg-slate-800"
                        >
                          <Plus className="h-3.5 w-3.5" />
                        </button>
                        <button
                          type="button"
                          aria-label={`Xóa ${entry.product.name} khỏi giỏ`}
                          onClick={() => onRemoveItem(entry.product.id)}
                          className="ml-1 rounded p-1 text-rose-400 hover:bg-slate-800"
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              {/* Customer Info */}
              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">
                    Thông tin người mua
                  </span>
                  <div className="flex items-center gap-1 rounded-lg bg-slate-900 p-1">
                    <button
                      type="button"
                      onClick={() => setCustomerMode('EXISTING')}
                      className={`rounded px-2.5 py-1 text-xs font-medium ${
                        customerMode === 'EXISTING'
                          ? 'bg-blue-600 text-white'
                          : 'text-slate-400 hover:text-slate-100'
                      }`}
                    >
                      Thành viên
                    </button>
                    <button
                      type="button"
                      onClick={() => setCustomerMode('NEW')}
                      className={`rounded px-2.5 py-1 text-xs font-medium ${
                        customerMode === 'NEW'
                          ? 'bg-blue-600 text-white'
                          : 'text-slate-400 hover:text-slate-100'
                      }`}
                    >
                      Khách mới
                    </button>
                  </div>
                </div>

                {customerMode === 'EXISTING' ? (
                  <div>
                    <label htmlFor="checkout-customer" className="block text-xs text-slate-400">
                      Chọn tài khoản khách hàng
                    </label>
                    <select
                      id="checkout-customer"
                      value={selectedCustomer?.id ?? ''}
                      onChange={(e) => onSelectCustomerId(e.target.value)}
                      className="mt-1.5 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-100"
                    >
                      {customers.map((c) => (
                        <option key={c.id} value={c.id}>
                          {c.fullName} ({c.membershipTier} · {c.phone})
                        </option>
                      ))}
                    </select>
                    {selectedCustomer && (
                      <p className="mt-1.5 text-xs text-slate-400">
                        Giao tới: {selectedCustomer.address} · Hạng {selectedCustomer.membershipTier}
                      </p>
                    )}
                  </div>
                ) : (
                  <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                    <div>
                      <label htmlFor="guest-name" className="block text-xs text-slate-400">
                        Họ và tên
                      </label>
                      <input
                        id="guest-name"
                        type="text"
                        value={newName}
                        onChange={(e) => setNewName(e.target.value)}
                        placeholder="Nguyễn Văn A"
                        className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-100"
                      />
                    </div>
                    <div>
                      <label htmlFor="guest-phone" className="block text-xs text-slate-400">
                        Số điện thoại
                      </label>
                      <input
                        id="guest-phone"
                        type="tel"
                        value={newPhone}
                        onChange={(e) => setNewPhone(e.target.value)}
                        placeholder="0909123456"
                        className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-100"
                      />
                    </div>
                    <div>
                      <label htmlFor="guest-email" className="block text-xs text-slate-400">
                        Email
                      </label>
                      <input
                        id="guest-email"
                        type="email"
                        value={newEmail}
                        onChange={(e) => setNewEmail(e.target.value)}
                        placeholder="khach@gmail.com"
                        className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-100"
                      />
                    </div>
                    <div>
                      <label htmlFor="guest-address" className="block text-xs text-slate-400">
                        Địa chỉ nhận hàng
                      </label>
                      <input
                        id="guest-address"
                        type="text"
                        value={newAddress}
                        onChange={(e) => setNewAddress(e.target.value)}
                        placeholder="Quận 1, TP.HCM"
                        className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-100"
                      />
                    </div>
                  </div>
                )}
              </div>

              {/* Discount Policy */}
              <div>
                <label htmlFor="checkout-discount" className="block text-xs font-medium text-slate-400">
                  Ưu đãi áp dụng (Java Strategy)
                </label>
                <select
                  id="checkout-discount"
                  value={discountType}
                  onChange={(e) => setDiscountType(e.target.value as DiscountType)}
                  className="mt-1.5 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-100"
                >
                  {availableDiscounts.map((type) => (
                    <option key={type} value={type}>
                      {discountLabel(type)}
                    </option>
                  ))}
                </select>
                {selectedCustomer?.membershipTier !== 'VIP' && (
                  <p className="mt-1 text-xs text-slate-500">
                    Ưu đãi VIP (15%) chỉ mở cho khách hàng hạng VIP; bạn có thể chọn Khuyến mãi mùa (20%).
                  </p>
                )}
              </div>

              {/* Payment Method */}
              <div>
                <span className="block text-xs font-medium text-slate-400">
                  Phương thức thanh toán
                </span>
                <div className="mt-1.5 space-y-2">
                  {(['MOCK_SUCCESS', 'COD', 'MOCK_FAILURE'] as PaymentMethod[]).map((method) => (
                    <label
                      key={method}
                      className={`flex cursor-pointer items-center justify-between rounded-lg border px-3 py-2 text-sm ${
                        paymentMethod === method
                          ? 'border-blue-500 bg-blue-950/30 text-slate-100'
                          : 'border-slate-800 bg-slate-900/70 text-slate-300'
                      }`}
                    >
                      <span>{paymentMethodLabel(method)}</span>
                      <input
                        type="radio"
                        name="storePaymentMethod"
                        value={method}
                        checked={paymentMethod === method}
                        onChange={() => setPaymentMethod(method)}
                        className="accent-blue-500"
                      />
                    </label>
                  ))}
                </div>
              </div>

              {/* Live Java Strategy Total */}
              <div className="space-y-1.5 rounded-xl border border-slate-800 bg-[#111827] p-4 text-sm">
                <div className="flex justify-between text-slate-400">
                  <span>Tạm tính</span>
                  <span className="font-mono-tabular text-slate-200">
                    {formatVnd(pricing?.originalTotal ?? rawSubtotal)}
                  </span>
                </div>
                <div className="flex justify-between text-slate-400">
                  <span>Giảm giá ({Math.round((pricing?.discountRate ?? 0) * 100)}%)</span>
                  <span className="font-mono-tabular text-amber-400">
                    -{formatVnd(pricing?.discountAmount ?? 0)}
                  </span>
                </div>
                <div className="flex justify-between border-t border-slate-800 pt-2 text-base font-semibold">
                  <span className="text-slate-100">Tổng thanh toán</span>
                  <span className="font-mono-tabular text-emerald-400">
                    {formatVnd(pricing?.finalTotal ?? rawSubtotal)}
                  </span>
                </div>
              </div>

              {error && (
                <div
                  role="alert"
                  className="rounded-lg border border-red-800/70 bg-red-950/30 p-3 text-xs text-red-300"
                >
                  {error}
                </div>
              )}
            </div>
          )}
        </div>

        {!receipt && cart.length > 0 && (
          <div className="border-t border-slate-800 bg-[#0B0F19] p-5 flex flex-wrap gap-3">
            <button
              type="button"
              disabled={submitting}
              onClick={() => handleCheckout(false)}
              className="whitespace-nowrap shrink-0 flex-1 rounded-lg border border-slate-700 bg-slate-800 px-4 py-2.5 text-sm font-semibold text-slate-100 hover:bg-slate-700 disabled:opacity-50"
            >
              Đặt trước (PENDING)
            </button>
            <button
              type="button"
              disabled={submitting}
              onClick={() => handleCheckout(true)}
              className="whitespace-nowrap shrink-0 flex-1 rounded-lg bg-blue-600 px-4 py-2.5 text-sm font-semibold text-white hover:bg-blue-500 disabled:opacity-50"
            >
              {submitting ? 'Đang xử lý...' : 'Đặt hàng & Thanh toán'}
            </button>
          </div>
        )}
      </div>
    </div>
  );
};
