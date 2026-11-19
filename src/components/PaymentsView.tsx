import React, {useCallback, useEffect, useState} from 'react';
import {javaApi} from '../api/javaClient';
import type {Bill, Order, Payment, PaymentMethod} from '../types';
import {
  formatDateTime,
  formatVnd,
  paymentMethodLabel,
  paymentStatusLabel,
} from '../utils/format';

interface PaymentsViewProps {
  refreshKey: number;
  initialOrderId: string | null;
  onDataChanged: (message: string) => void;
  onOpenInvoice: (billId: string) => void;
}

export const PaymentsView: React.FC<PaymentsViewProps> = ({
  refreshKey,
  initialOrderId,
  onDataChanged,
  onOpenInvoice,
}) => {
  const [pendingOrders, setPendingOrders] = useState<Order[]>([]);
  const [payments, setPayments] = useState<Payment[]>([]);
  const [selectedOrderId, setSelectedOrderId] = useState('');
  const [method, setMethod] = useState<PaymentMethod>('MOCK_SUCCESS');
  const [processing, setProcessing] = useState(false);
  const [lastResult, setLastResult] = useState<{
    payment: Payment;
    order: Order;
    bill: Bill | null;
  } | null>(null);
  const [error, setError] = useState<string | null>(null);

  const loadData = useCallback(async () => {
    try {
      const [orders, paymentList] = await Promise.all([
        javaApi.getOrders('PENDING'),
        javaApi.getPayments(),
      ]);
      setPendingOrders(orders);
      setPayments(paymentList);
      if (initialOrderId && orders.some((o) => o.id === initialOrderId)) {
        setSelectedOrderId(initialOrderId);
      } else if (orders.length > 0 && !orders.some((o) => o.id === selectedOrderId)) {
        setSelectedOrderId(orders[0].id);
      } else if (orders.length === 0) {
        setSelectedOrderId('');
      }
      setError(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không tải được dữ liệu thanh toán.');
    }
  }, [initialOrderId, selectedOrderId]);

  useEffect(() => {
    loadData();
  }, [loadData, refreshKey]);

  const handlePay = async () => {
    if (!selectedOrderId) {
      setError('Không có đơn PENDING nào được chọn.');
      return;
    }
    setProcessing(true);
    setError(null);
    try {
      const result = await javaApi.processPayment(selectedOrderId, method);
      setLastResult(result);
      if (result.payment.status === 'SUCCESS') {
        onDataChanged(
          `Thanh toán thành công cho ${result.order.id}. Đã phát hành hóa đơn ${result.bill?.id ?? ''}.`,
        );
      } else {
        onDataChanged(`Thanh toán thất bại cho ${result.order.id}. Đơn vẫn giữ trạng thái PENDING.`);
      }
      await loadData();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Lỗi xử lý thanh toán.');
    } finally {
      setProcessing(false);
    }
  };

  const selectedOrder = pendingOrders.find((o) => o.id === selectedOrderId) ?? null;

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-12">
        {/* Payment Execution Panel */}
        <div className="rounded-xl border border-slate-800 bg-[#111827] p-5 lg:col-span-4">
          <h2 className="text-base font-semibold text-slate-100">
            Xử lý thanh toán mô phỏng (Observer Pattern)
          </h2>
          <p className="mt-1 text-xs text-slate-400">
            Khi thanh toán thành công, Java PaymentEventPublisher kích hoạt đồng thời BillObserver, InventoryObserver và NotificationObserver.
          </p>

          <div className="mt-4 space-y-4">
            <div>
              <label htmlFor="pay-order-select" className="block text-xs font-medium text-slate-400">
                Chọn đơn hàng chờ thanh toán (PENDING)
              </label>
              <select
                id="pay-order-select"
                value={selectedOrderId}
                onChange={(e) => setSelectedOrderId(e.target.value)}
                disabled={pendingOrders.length === 0}
                className="mt-1.5 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-100"
              >
                {pendingOrders.length === 0 ? (
                  <option value="">Không có đơn PENDING</option>
                ) : (
                  pendingOrders.map((o) => (
                    <option key={o.id} value={o.id}>
                      {o.id} — {o.customerName} ({formatVnd(o.finalTotal)})
                    </option>
                  ))
                )}
              </select>
            </div>

            {selectedOrder && (
              <div className="space-y-1 border-y border-slate-800 py-3 text-sm">
                <div className="flex justify-between text-slate-400">
                  <span>Khách hàng</span>
                  <span className="text-slate-100">{selectedOrder.customerName}</span>
                </div>
                <div className="flex justify-between text-slate-400">
                  <span>Giảm giá áp dụng</span>
                  <span className="text-slate-200">{selectedOrder.discountType}</span>
                </div>
                <div className="flex justify-between text-slate-400">
                  <span>Số tiền cần thanh toán</span>
                  <span className="font-mono-tabular font-semibold text-emerald-400">
                    {formatVnd(selectedOrder.finalTotal)}
                  </span>
                </div>
              </div>
            )}

            <div>
              <span className="block text-xs font-medium text-slate-400">
                Phương thức thanh toán
              </span>
              <div className="mt-2 space-y-2">
                {(['MOCK_SUCCESS', 'COD', 'MOCK_FAILURE'] as PaymentMethod[]).map((m) => (
                  <label
                    key={m}
                    className={`flex cursor-pointer items-center justify-between rounded-lg border px-3 py-2.5 text-sm transition-colors ${
                      method === m
                        ? 'border-blue-500 bg-blue-950/30 text-slate-100'
                        : 'border-slate-800 bg-slate-900/60 text-slate-300 hover:border-slate-700'
                    }`}
                  >
                    <span>{paymentMethodLabel(m)}</span>
                    <input
                      type="radio"
                      name="paymentMethod"
                      value={m}
                      checked={method === m}
                      onChange={() => setMethod(m)}
                      className="accent-blue-500"
                    />
                  </label>
                ))}
              </div>
            </div>

            {error && (
              <div className="rounded-lg border border-red-800/60 bg-red-950/30 p-3 text-xs text-red-300">
                {error}
              </div>
            )}

            <button
              type="button"
              disabled={!selectedOrder || processing}
              onClick={handlePay}
              className="whitespace-nowrap shrink-0 w-full rounded-lg bg-blue-600 px-4 py-2.5 text-sm font-semibold text-white hover:bg-blue-500 disabled:opacity-50"
            >
              {processing ? 'Đang xử lý...' : 'Thanh toán ngay'}
            </button>
          </div>
        </div>

        {/* Result & Observer outcome + Payment history */}
        <div className="space-y-6 lg:col-span-8">
          {lastResult && (
            <div
              className={`rounded-xl border p-5 ${
                lastResult.payment.status === 'SUCCESS'
                  ? 'border-emerald-800/70 bg-emerald-950/20'
                  : 'border-rose-800/70 bg-rose-950/20'
              }`}
            >
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div>
                  <h3 className="text-base font-semibold text-slate-100">
                    Kết quả giao dịch {lastResult.payment.id}
                  </h3>
                  <p className="mt-1 text-sm text-slate-300">{lastResult.payment.message}</p>
                </div>
                <span
                  className={`text-xs font-semibold ${
                    lastResult.payment.status === 'SUCCESS' ? 'text-emerald-400' : 'text-rose-400'
                  }`}
                >
                  {paymentStatusLabel(lastResult.payment.status)}
                </span>
              </div>

              {lastResult.payment.status === 'SUCCESS' && lastResult.bill ? (
                <div className="mt-4 flex flex-wrap items-center justify-between gap-3 border-t border-emerald-800/40 pt-3 text-xs text-slate-300">
                  <span>
                    BillObserver: Đã tạo {lastResult.bill.id} · InventoryObserver: Đã trừ kho · NotificationObserver: Đã ghi nhật ký
                  </span>
                  <button
                    type="button"
                    onClick={() => onOpenInvoice(lastResult.bill!.id)}
                    className="whitespace-nowrap shrink-0 rounded-lg bg-emerald-600 px-3 py-1.5 font-semibold text-white hover:bg-emerald-500"
                  >
                    Mở hóa đơn vừa tạo
                  </button>
                </div>
              ) : (
                <p className="mt-3 text-xs text-rose-300">
                  Đơn hàng {lastResult.order.id} vẫn giữ trạng thái PENDING, không trừ tồn kho và không xuất hóa đơn.
                </p>
              )}
            </div>
          )}

          <div className="rounded-xl border border-slate-800 bg-[#111827] p-5">
            <h2 className="text-base font-semibold text-slate-100">
              Lịch sử giao dịch thanh toán ({payments.length})
            </h2>
            <div className="mt-3 overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead className="border-b border-slate-800 text-xs text-slate-400">
                  <tr>
                    <th className="whitespace-nowrap py-2.5 pr-2 font-medium">Mã GD</th>
                    <th className="whitespace-nowrap py-2.5 pr-2 font-medium">Mã đơn</th>
                    <th className="whitespace-nowrap py-2.5 pr-2 font-medium">Phương thức</th>
                    <th className="whitespace-nowrap py-2.5 pr-2 text-right font-medium">Số tiền</th>
                    <th className="whitespace-nowrap py-2.5 pr-2 font-medium">Kết quả</th>
                    <th className="whitespace-nowrap py-2.5 font-medium">Thời gian</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800/80">
                  {payments.length === 0 ? (
                    <tr>
                      <td colSpan={6} className="py-8 text-center text-sm text-slate-400">
                        Chưa có giao dịch thanh toán nào.
                      </td>
                    </tr>
                  ) : (
                    payments.map((p) => (
                      <tr key={p.id} className="hover:bg-slate-800/40">
                        <td className="font-mono-tabular whitespace-nowrap py-2.5 pr-2 text-sky-400">{p.id}</td>
                        <td className="font-mono-tabular whitespace-nowrap py-2.5 pr-2 text-slate-200">{p.orderId}</td>
                        <td className="whitespace-nowrap py-2.5 pr-2 text-slate-300">
                          {paymentMethodLabel(p.method)}
                        </td>
                        <td className="font-mono-tabular whitespace-nowrap py-2.5 pr-2 text-right text-slate-100">
                          {formatVnd(p.amount)}
                        </td>
                        <td
                          className={`whitespace-nowrap py-2.5 pr-2 font-semibold ${
                            p.status === 'SUCCESS' ? 'text-emerald-400' : 'text-rose-400'
                          }`}
                        >
                          {paymentStatusLabel(p.status)}
                        </td>
                        <td className="font-mono-tabular whitespace-nowrap py-2.5 text-slate-400">
                          {formatDateTime(p.paidAt)}
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
