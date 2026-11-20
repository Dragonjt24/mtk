import React, {useEffect, useMemo, useState} from 'react';
import {CheckCircle2, Download, Lock, Minus, Plus, Trash2, UserPlus, X} from 'lucide-react';
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
  PromotionConfig,
  User,
} from '../../types';
import {
  discountLabel,
  formatVnd,
  orderStatusLabel,
  paymentMethodLabel,
} from '../../utils/format';
import {
  isCustomerPurchaseEligible,
  resolveAutomaticDiscount,
} from '../../utils/storeHelpers';

export interface StoreCartItem {
  product: Product;
  quantity: number;
}

interface CartCheckoutDrawerProps {
  open: boolean;
  cart: StoreCartItem[];
  currentUser: User | null;
  currentCustomer: Customer | null;
  promotionConfig: PromotionConfig | null;
  onUpdateQty: (productId: string, delta: number) => void;
  onRemoveItem: (productId: string) => void;
  onClearCart: () => void;
  onClose: () => void;
  onRequireAuth: (mode: 'LOGIN' | 'REGISTER') => void;
  onCheckoutCompleted: (result: {
    order: Order;
    payment: Payment | null;
    bill: Bill | null;
    customer: Customer;
  }) => void;
  onOpenMyOrders: () => void;
}

export const CartCheckoutDrawer: React.FC<CartCheckoutDrawerProps> = ({
  open,
  cart,
  currentUser,
  currentCustomer,
  promotionConfig,
  onUpdateQty,
  onRemoveItem,
  onClearCart,
  onClose,
  onRequireAuth,
  onCheckoutCompleted,
  onOpenMyOrders,
}) => {
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

  const canCheckout = isCustomerPurchaseEligible(currentUser, currentCustomer);

  const effectiveDiscountType: DiscountType = useMemo(
    () => resolveAutomaticDiscount(currentCustomer, promotionConfig),
    [currentCustomer, promotionConfig],
  );

  const rawSubtotal = useMemo(
    () => cart.reduce((sum, item) => sum + item.product.price * item.quantity, 0),
    [cart],
  );

  useEffect(() => {
    if (!open) return;
    let active = true;
    javaApi
      .previewPricing(rawSubtotal, effectiveDiscountType, {
        customerId: currentCustomer?.id,
        useAutoPolicy: Boolean(currentCustomer?.id),
      })
      .then((res) => {
        if (active) setPricing(res);
      })
      .catch(() => {
        if (active) setPricing(null);
      });
    return () => {
      active = false;
    };
  }, [open, rawSubtotal, effectiveDiscountType, currentCustomer?.id, promotionConfig?.seasonalPromoEnabled]);

  if (!open) return null;

  const handleCheckout = async (payNow: boolean) => {
    if (cart.length === 0) {
      setError('Giỏ hàng đang trống.');
      return;
    }
    if (!canCheckout || !currentCustomer) {
      setError('Bạn cần đăng ký hoặc đăng nhập tài khoản khách hàng đầy đủ thông tin trước khi mua hàng.');
      return;
    }

    setSubmitting(true);
    setError(null);
    try {
      const order = await javaApi.createOrder({
        customerId: currentCustomer.id,
        useAutoPolicy: true,
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

      const outcome = {order: finalOrder, payment, bill, customer: currentCustomer};
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
                        : 'Đơn hàng đã được tạo ở trạng thái PENDING và đồng bộ sang hệ thống quản trị.'}
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
                  <span className="text-slate-200">
                    {discountLabel(receipt.order.discountType, promotionConfig)}
                  </span>
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
                    setReceipt(null);
                    onClose();
                    onOpenMyOrders();
                  }}
                  className="whitespace-nowrap shrink-0 flex-1 rounded-lg border border-slate-700 bg-slate-800 px-4 py-2.5 text-sm font-semibold text-slate-100 hover:bg-slate-700"
                >
                  Xem Đơn hàng của tôi
                </button>
              </div>
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

              {/* Mandatory Customer Authentication Gate */}
              {!canCheckout ? (
                <div className="rounded-xl border border-amber-700/60 bg-amber-950/20 p-5 space-y-4">
                  <div className="flex items-start gap-3">
                    <Lock className="mt-0.5 h-5 w-5 shrink-0 text-amber-400" />
                    <div>
                      <h3 className="text-sm font-semibold text-slate-100">
                        {currentUser && currentUser.role !== 'CUSTOMER'
                          ? 'Vui lòng dùng tài khoản Khách hàng để đặt mua'
                          : 'Yêu cầu đăng ký hoặc đăng nhập tài khoản để mua hàng'}
                      </h3>
                      <p className="mt-1 text-xs leading-relaxed text-slate-300">
                        {currentUser && currentUser.role !== 'CUSTOMER'
                          ? 'Bạn đang đăng nhập bằng tài khoản Quản trị / Nhân viên. Để đặt hàng trên Cửa hàng, hãy đăng nhập tài khoản Khách hàng hoặc đăng ký tài khoản mới.'
                          : 'Khách hàng cần đăng ký tài khoản với đầy đủ họ tên, số điện thoại, email và địa chỉ giao hàng (hoặc đăng nhập nếu đã có tài khoản) trước khi thanh toán. Giỏ hàng hiện tại của bạn sẽ được giữ nguyên.'}
                      </p>
                    </div>
                  </div>
                  <div className="flex flex-wrap gap-3">
                    <button
                      type="button"
                      onClick={() => {
                        onClose();
                        onRequireAuth('REGISTER');
                      }}
                      className="inline-flex whitespace-nowrap shrink-0 flex-1 items-center justify-center gap-2 rounded-lg bg-blue-600 px-4 py-2.5 text-sm font-semibold text-white hover:bg-blue-500"
                    >
                      <UserPlus className="h-4 w-4" />
                      Đăng ký tài khoản
                    </button>
                    <button
                      type="button"
                      onClick={() => {
                        onClose();
                        onRequireAuth('LOGIN');
                      }}
                      className="whitespace-nowrap shrink-0 flex-1 rounded-lg border border-slate-700 bg-slate-800 px-4 py-2.5 text-sm font-semibold text-slate-100 hover:bg-slate-700"
                    >
                      Đăng nhập
                    </button>
                  </div>
                </div>
              ) : (
                <>
                  {/* Verified Authenticated Customer Info */}
                  <div className="rounded-xl border border-slate-800 bg-[#111827] p-4 space-y-1.5">
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">
                        Thông tin người nhận hàng
                      </span>
                      <span className="text-xs font-semibold text-sky-400">
                        Hạng {currentCustomer?.membershipTier}
                      </span>
                    </div>
                    <p className="text-sm font-semibold text-slate-100">
                      {currentCustomer?.fullName} · {currentCustomer?.phone}
                    </p>
                    <p className="text-xs text-slate-400">{currentCustomer?.email}</p>
                    <p className="text-xs text-slate-300">Giao tới: {currentCustomer?.address}</p>
                  </div>

                  {/* Admin-Controlled Automatic Discount Policy */}
                  <div className="rounded-xl border border-slate-800 bg-[#111827] p-4 space-y-1.5">
                    <div className="flex items-center justify-between gap-2">
                      <span className="text-xs font-medium text-slate-400">
                        Ưu đãi áp dụng tự động
                      </span>
                      <span
                        className={`text-xs font-semibold ${
                          effectiveDiscountType === 'SEASONAL'
                            ? 'text-amber-400'
                            : effectiveDiscountType === 'VIP'
                              ? 'text-sky-400'
                              : 'text-slate-400'
                        }`}
                      >
                        {discountLabel(effectiveDiscountType, promotionConfig)}
                      </span>
                    </div>
                    <p className="text-xs leading-relaxed text-slate-300">
                      {promotionConfig?.seasonalPromoEnabled
                        ? `Quản trị viên đang kích hoạt "${promotionConfig.seasonalPromoName}" — Toàn bộ tài khoản đều được áp dụng giảm ${Math.round(promotionConfig.seasonalDiscountRate * 100)}%.`
                        : currentCustomer?.membershipTier === 'VIP'
                          ? `Áp dụng mặc định ưu đãi hạng thành viên VIP (-${Math.round((promotionConfig?.vipDiscountRate ?? 0.15) * 100)}%).`
                          : 'Tài khoản thường mặc định không giảm giá (0%). Khi Admin bật chương trình ưu đãi theo mùa, toàn bộ tài khoản sẽ tự động được áp dụng.'}
                    </p>
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
                </>
              )}

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

        {!receipt && cart.length > 0 && canCheckout && (
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
