import React, {useCallback, useEffect, useMemo, useState} from 'react';
import {Download, Printer} from 'lucide-react';
import {javaApi} from '../../api/javaClient';
import type {Bill, Customer, Order, PaymentMethod} from '../../types';
import {
  formatDateTime,
  formatVnd,
  orderStatusLabel,
  paymentMethodLabel,
} from '../../utils/format';
import {ConfirmDialog} from '../ConfirmDialog';

interface CustomerOrdersViewProps {
  customers: Customer[];
  selectedCustomerId: string;
  onSelectCustomerId: (id: string) => void;
  refreshKey: number;
  onDataChanged: (message: string) => void;
  onBackToCatalog: () => void;
}

export const CustomerOrdersView: React.FC<CustomerOrdersViewProps> = ({
  customers,
  selectedCustomerId,
  onSelectCustomerId,
  refreshKey,
  onDataChanged,
  onBackToCatalog,
}) => {
  const [orders, setOrders] = useState<Order[]>([]);
  const [bills, setBills] = useState<Bill[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [retryMethod, setRetryMethod] = useState<PaymentMethod>('MOCK_SUCCESS');
  const [payingOrderId, setPayingOrderId] = useState<string | null>(null);
  const [cancelTarget, setCancelTarget] = useState<Order | null>(null);
  const [previewBill, setPreviewBill] = useState<Bill | null>(null);

  const activeCustomerId = selectedCustomerId || customers[0]?.id || '';

  const loadOrders = useCallback(async () => {
    if (!activeCustomerId) {
      setOrders([]);
      setLoading(false);
      return;
    }
    setLoading(true);
    try {
      const [custOrders, allBills] = await Promise.all([
        javaApi.getCustomerOrders(activeCustomerId),
        javaApi.getBills(),
      ]);
      setOrders(custOrders);
      setBills(allBills);
      setError(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không tải được đơn hàng của khách hàng.');
    } finally {
      setLoading(false);
    }
  }, [activeCustomerId]);

  useEffect(() => {
    loadOrders();
  }, [loadOrders, refreshKey]);

  const billByOrderId = useMemo(() => {
    const map = new Map<string, Bill>();
    bills.forEach((b) => map.set(b.orderId, b));
    return map;
  }, [bills]);

  const handlePayPending = async (orderId: string) => {
    setPayingOrderId(orderId);
    setError(null);
    try {
      const res = await javaApi.processPayment(orderId, retryMethod);
      if (res.payment.status === 'SUCCESS') {
        onDataChanged(`Thanh toán thành công đơn ${orderId}. Đã xuất hóa đơn ${res.bill?.id ?? ''}.`);
      } else {
        setError(res.payment.message);
      }
      await loadOrders();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Thanh toán thất bại.');
    } finally {
      setPayingOrderId(null);
    }
  };

  const handleCancelOrder = async () => {
    if (!cancelTarget) return;
    try {
      await javaApi.cancelOrder(cancelTarget.id);
      onDataChanged(`Đã hủy đơn hàng ${cancelTarget.id}.`);
      setCancelTarget(null);
      await loadOrders();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không hủy được đơn hàng.');
      setCancelTarget(null);
    }
  };

  const activeCustomer = customers.find((c) => c.id === activeCustomerId) ?? null;

  return (
    <div className="mx-auto max-w-6xl space-y-6 px-6 py-8">
      <div className="flex flex-wrap items-center justify-between gap-4 rounded-xl border border-slate-800 bg-[#111827] p-5">
        <div>
          <h1 className="font-display text-xl font-semibold text-slate-100">Đơn hàng của tôi</h1>
          <p className="mt-1 text-xs text-slate-400">
            Theo dõi trạng thái, thanh toán đơn chờ xử lý hoặc tải hóa đơn PDF đồng bộ với cửa hàng
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-3">
          <div>
            <label htmlFor="my-orders-customer" className="sr-only">
              Chọn khách hàng xem đơn hàng
            </label>
            <select
              id="my-orders-customer"
              value={activeCustomerId}
              onChange={(e) => onSelectCustomerId(e.target.value)}
              className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-100"
            >
              {customers.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.fullName} ({c.membershipTier} · {c.phone})
                </option>
              ))}
            </select>
          </div>
          <button
            type="button"
            onClick={onBackToCatalog}
            className="whitespace-nowrap shrink-0 rounded-lg border border-slate-700 bg-slate-800 px-4 py-2 text-sm font-medium text-slate-100 hover:bg-slate-700"
          >
            Quay lại mua sắm
          </button>
        </div>
      </div>

      {activeCustomer && (
        <div className="flex flex-wrap items-center justify-between gap-4 rounded-xl border border-slate-800 bg-[#111827] px-5 py-3.5 text-sm">
          <div className="text-slate-300">
            Khách hàng: <span className="font-semibold text-slate-100">{activeCustomer.fullName}</span>
            {' · '}Hạng <span className="font-semibold text-sky-400">{activeCustomer.membershipTier}</span>
            {' · '}{activeCustomer.address}
          </div>
          <div className="font-mono-tabular text-slate-300">
            Tổng chi tiêu tích lũy:{' '}
            <span className="font-semibold text-emerald-400">{formatVnd(activeCustomer.totalSpent)}</span>
          </div>
        </div>
      )}

      {error && (
        <div role="alert" className="rounded-xl border border-red-800/70 bg-red-950/30 p-4 text-sm text-red-300">
          {error}
        </div>
      )}

      {loading ? (
        <div className="rounded-xl border border-slate-800 bg-[#111827] p-8 text-center text-sm text-slate-400">
          Đang tải lịch sử đơn hàng...
        </div>
      ) : orders.length === 0 ? (
        <div className="rounded-xl border border-slate-800 bg-[#111827] p-12 text-center">
          <p className="text-sm text-slate-300">Khách hàng này chưa có đơn hàng nào.</p>
          <button
            type="button"
            onClick={onBackToCatalog}
            className="mt-4 rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-500"
          >
            Mua sắm ngay
          </button>
        </div>
      ) : (
        <div className="space-y-4">
          {orders.map((order) => {
            const bill = billByOrderId.get(order.id);
            return (
              <div
                key={order.id}
                className="rounded-xl border border-slate-800 bg-[#111827] p-5 space-y-4"
              >
                <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-800 pb-3">
                  <div>
                    <span className="font-mono-tabular text-base font-semibold text-sky-400">
                      {order.id}
                    </span>
                    <span className="mx-2 text-slate-600">·</span>
                    <span className="font-mono-tabular text-xs text-slate-400">
                      {formatDateTime(order.createdAt)}
                    </span>
                    <span className="mx-2 text-slate-600">·</span>
                    <span className="text-xs text-slate-300">{order.discountType}</span>
                  </div>
                  <span
                    className={`text-xs font-semibold ${
                      order.status === 'PAID'
                        ? 'text-emerald-400'
                        : order.status === 'PENDING'
                          ? 'text-amber-400'
                          : 'text-rose-400'
                    }`}
                  >
                    {orderStatusLabel(order.status)}
                  </span>
                </div>

                <div className="divide-y divide-slate-800/80">
                  {order.items.map((item) => (
                    <div
                      key={`${order.id}-${item.productId}`}
                      className="flex items-center justify-between py-2 text-sm"
                    >
                      <span className="text-slate-200">
                        {item.productName}{' '}
                        <span className="font-mono-tabular text-xs text-slate-400">× {item.quantity}</span>
                      </span>
                      <span className="font-mono-tabular text-slate-100">
                        {formatVnd(item.unitPrice * item.quantity)}
                      </span>
                    </div>
                  ))}
                </div>

                <div className="flex flex-wrap items-center justify-between gap-4 border-t border-slate-800 pt-3">
                  <div className="text-sm">
                    <span className="text-slate-400">Tổng thanh toán: </span>
                    <span className="font-mono-tabular text-base font-semibold text-emerald-400">
                      {formatVnd(order.finalTotal)}
                    </span>
                  </div>

                  {order.status === 'PENDING' && (
                    <div className="flex flex-wrap items-center gap-2">
                      <select
                        aria-label={`Chọn phương thức thanh toán cho đơn ${order.id}`}
                        value={retryMethod}
                        onChange={(e) => setRetryMethod(e.target.value as PaymentMethod)}
                        className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-1.5 text-xs text-slate-100"
                      >
                        <option value="MOCK_SUCCESS">{paymentMethodLabel('MOCK_SUCCESS')}</option>
                        <option value="COD">{paymentMethodLabel('COD')}</option>
                        <option value="MOCK_FAILURE">{paymentMethodLabel('MOCK_FAILURE')}</option>
                      </select>
                      <button
                        type="button"
                        disabled={payingOrderId === order.id}
                        onClick={() => handlePayPending(order.id)}
                        className="whitespace-nowrap shrink-0 rounded-lg bg-emerald-600 px-3.5 py-1.5 text-xs font-semibold text-white hover:bg-emerald-500 disabled:opacity-50"
                      >
                        {payingOrderId === order.id ? 'Đang xử lý...' : 'Thanh toán ngay'}
                      </button>
                      <button
                        type="button"
                        onClick={() => setCancelTarget(order)}
                        className="whitespace-nowrap shrink-0 rounded-lg border border-red-800/80 bg-red-950/40 px-3 py-1.5 text-xs font-medium text-red-300 hover:bg-red-900/50"
                      >
                        Hủy đơn
                      </button>
                    </div>
                  )}

                  {order.status === 'PAID' && bill && (
                    <div className="flex flex-wrap items-center gap-2">
                      <button
                        type="button"
                        onClick={() => setPreviewBill(bill)}
                        className="whitespace-nowrap shrink-0 rounded-lg border border-slate-700 bg-slate-800 px-3 py-1.5 text-xs font-medium text-slate-100 hover:bg-slate-700"
                      >
                        Xem hóa đơn {bill.id}
                      </button>
                      <a
                        href={javaApi.getBillPdfUrl(bill.id)}
                        download={`${bill.id}.pdf`}
                        className="inline-flex whitespace-nowrap shrink-0 items-center gap-1.5 rounded-lg bg-blue-600 px-3 py-1.5 text-xs font-semibold text-white hover:bg-blue-500"
                      >
                        <Download className="h-3.5 w-3.5" />
                        Tải PDF
                      </a>
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Bill Quick Sheet Modal */}
      {previewBill && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 p-4"
          role="dialog"
          aria-modal="true"
          aria-labelledby="customer-bill-title"
        >
          <div className="w-full max-w-xl rounded-xl border border-slate-800 bg-[#111827] p-6 shadow-2xl space-y-4">
            <div className="flex items-center justify-between border-b border-slate-800 pb-3">
              <h2 id="customer-bill-title" className="font-mono-tabular text-base font-semibold text-sky-400">
                Hóa đơn {previewBill.id}
              </h2>
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={() => window.print()}
                  className="inline-flex items-center gap-1.5 rounded-lg border border-slate-700 bg-slate-800 px-3 py-1.5 text-xs font-medium text-slate-100 hover:bg-slate-700"
                >
                  <Printer className="h-3.5 w-3.5" />
                  In
                </button>
                <a
                  href={javaApi.getBillPdfUrl(previewBill.id)}
                  download={`${previewBill.id}.pdf`}
                  className="inline-flex items-center gap-1.5 rounded-lg bg-blue-600 px-3 py-1.5 text-xs font-semibold text-white hover:bg-blue-500"
                >
                  <Download className="h-3.5 w-3.5" />
                  PDF
                </a>
                <button
                  type="button"
                  onClick={() => setPreviewBill(null)}
                  className="rounded-lg border border-slate-700 px-3 py-1.5 text-xs text-slate-300 hover:bg-slate-800"
                >
                  Đóng
                </button>
              </div>
            </div>
            <div id="printable-invoice" className="space-y-3 text-sm">
              <p className="font-semibold text-slate-100">
                {previewBill.customerName} · {previewBill.customerPhone}
              </p>
              <p className="text-xs text-slate-400">{previewBill.customerAddress}</p>
              <div className="divide-y divide-slate-800 border-y border-slate-800">
                {previewBill.items.map((i) => (
                  <div key={i.productId} className="flex justify-between py-2">
                    <span>
                      {i.productName} × {i.quantity}
                    </span>
                    <span className="font-mono-tabular">{formatVnd(i.lineTotal)}</span>
                  </div>
                ))}
              </div>
              <div className="flex justify-between font-semibold text-emerald-400">
                <span>Tổng đã thanh toán</span>
                <span className="font-mono-tabular">{formatVnd(previewBill.finalTotal)}</span>
              </div>
            </div>
          </div>
        </div>
      )}

      <ConfirmDialog
        open={Boolean(cancelTarget)}
        title="Hủy đơn hàng"
        message={cancelTarget ? `Bạn muốn hủy đơn hàng ${cancelTarget.id}?` : ''}
        confirmLabel="Xác nhận hủy"
        destructive
        onConfirm={handleCancelOrder}
        onCancel={() => setCancelTarget(null)}
      />
    </div>
  );
};
