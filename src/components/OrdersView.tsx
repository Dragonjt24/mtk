import React, {useCallback, useEffect, useMemo, useState} from 'react';
import {javaApi} from '../api/javaClient';
import type {DiscountType, Order, OrderStatus} from '../types';
import {discountLabel, formatDateTime, formatVnd, orderStatusLabel} from '../utils/format';
import {ConfirmDialog} from './ConfirmDialog';

interface OrdersViewProps {
  refreshKey: number;
  onDataChanged: (message: string) => void;
  onPayOrder: (orderId: string) => void;
  onCreateNewOrder: () => void;
}

export const OrdersView: React.FC<OrdersViewProps> = ({
  refreshKey,
  onDataChanged,
  onPayOrder,
  onCreateNewOrder,
}) => {
  const [orders, setOrders] = useState<Order[]>([]);
  const [statusFilter, setStatusFilter] = useState<'ALL' | OrderStatus>('ALL');
  const [keyword, setKeyword] = useState('');
  const [selectedOrderId, setSelectedOrderId] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [cancelTarget, setCancelTarget] = useState<Order | null>(null);
  const [updatingDiscount, setUpdatingDiscount] = useState(false);

  const loadOrders = useCallback(async () => {
    setLoading(true);
    try {
      const list = await javaApi.getOrders(statusFilter);
      setOrders(list);
      setError(null);
      if (list.length > 0 && (!selectedOrderId || !list.some((o) => o.id === selectedOrderId))) {
        setSelectedOrderId(list[0].id);
      } else if (list.length === 0) {
        setSelectedOrderId(null);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không tải được danh sách đơn hàng.');
    } finally {
      setLoading(false);
    }
  }, [statusFilter, selectedOrderId]);

  useEffect(() => {
    loadOrders();
  }, [loadOrders, refreshKey]);

  const filteredOrders = useMemo(() => {
    const q = keyword.trim().toLowerCase();
    if (!q) return orders;
    return orders.filter(
      (o) =>
        o.id.toLowerCase().includes(q) ||
        o.customerName.toLowerCase().includes(q) ||
        o.customerId.toLowerCase().includes(q),
    );
  }, [orders, keyword]);

  const selectedOrder = useMemo(
    () => orders.find((o) => o.id === selectedOrderId) ?? filteredOrders[0] ?? null,
    [orders, filteredOrders, selectedOrderId],
  );

  const handleChangeDiscount = async (orderId: string, discountType: DiscountType) => {
    setUpdatingDiscount(true);
    setError(null);
    try {
      await javaApi.changeOrderDiscount(orderId, discountType);
      onDataChanged(`Đã cập nhật chiến lược giảm giá cho đơn ${orderId}.`);
      await loadOrders();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể đổi giảm giá.');
    } finally {
      setUpdatingDiscount(false);
    }
  };

  const confirmCancelOrder = async () => {
    if (!cancelTarget) return;
    try {
      await javaApi.cancelOrder(cancelTarget.id);
      onDataChanged(`Đã hủy đơn hàng ${cancelTarget.id}.`);
      setCancelTarget(null);
      await loadOrders();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể hủy đơn hàng.');
      setCancelTarget(null);
    }
  };

  return (
    <div className="space-y-5">
      {/* Toolbar */}
      <div className="flex flex-wrap items-center justify-between gap-4 rounded-xl border border-slate-800 bg-[#111827] p-4">
        <div className="flex flex-wrap items-center gap-3">
          <div className="flex items-center gap-1 rounded-lg bg-slate-900 p-1">
            {(['ALL', 'PENDING', 'PAID', 'CANCELLED'] as const).map((st) => (
              <button
                key={st}
                type="button"
                onClick={() => setStatusFilter(st)}
                className={`whitespace-nowrap shrink-0 rounded-md px-3 py-1.5 text-xs font-medium transition-colors ${
                  statusFilter === st
                    ? 'bg-blue-600 text-white'
                    : 'text-slate-400 hover:text-slate-100'
                }`}
              >
                {st === 'ALL' ? 'Tất cả' : orderStatusLabel(st)}
              </button>
            ))}
          </div>
          <input
            type="search"
            aria-label="Tìm đơn hàng theo mã hoặc tên khách"
            placeholder="Tìm mã đơn hoặc tên khách..."
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            className="w-64 rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-100 placeholder:text-slate-500 focus:border-sky-500 focus:outline-none"
          />
        </div>
        <button
          type="button"
          onClick={onCreateNewOrder}
          className="whitespace-nowrap shrink-0 rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-500"
        >
          Tạo đơn hàng mới
        </button>
      </div>

      {error && (
        <div className="rounded-xl border border-red-800/60 bg-red-950/30 p-3 text-sm text-red-300">
          {error}
        </div>
      )}

      {/* Split layout: Orders list + Order detail */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-12">
        <div className="rounded-xl border border-slate-800 bg-[#111827] p-4 lg:col-span-7">
          <div className="mb-3 flex items-center justify-between">
            <h2 className="text-base font-semibold text-slate-100">
              Danh sách đơn hàng ({filteredOrders.length})
            </h2>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-slate-800 text-xs text-slate-400">
                <tr>
                  <th className="whitespace-nowrap py-2.5 pr-3 font-medium">Mã đơn</th>
                  <th className="whitespace-nowrap py-2.5 pr-3 font-medium">Khách hàng</th>
                  <th className="whitespace-nowrap py-2.5 pr-3 text-right font-medium">Thành tiền</th>
                  <th className="whitespace-nowrap py-2.5 pr-3 font-medium">Giảm giá</th>
                  <th className="whitespace-nowrap py-2.5 pr-3 font-medium">Trạng thái</th>
                  <th className="whitespace-nowrap py-2.5 font-medium">Ngày tạo</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/80">
                {loading && orders.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="py-6 text-center text-slate-400">
                      Đang tải đơn hàng từ Java...
                    </td>
                  </tr>
                ) : filteredOrders.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="py-8 text-center text-slate-400">
                      Không có đơn hàng phù hợp với bộ lọc.
                    </td>
                  </tr>
                ) : (
                  filteredOrders.map((order) => {
                    const isSelected = selectedOrder?.id === order.id;
                    return (
                      <tr
                        key={order.id}
                        className={`transition-colors ${
                          isSelected ? 'bg-slate-800/70' : 'hover:bg-slate-800/40'
                        }`}
                      >
                        <td className="whitespace-nowrap py-2.5 pr-3">
                          <button
                            type="button"
                            onClick={() => setSelectedOrderId(order.id)}
                            className="font-mono-tabular whitespace-nowrap font-medium text-sky-400 hover:underline"
                          >
                            {order.id}
                          </button>
                        </td>
                        <td className="whitespace-nowrap py-2.5 pr-3 text-slate-200">{order.customerName}</td>
                        <td className="font-mono-tabular whitespace-nowrap py-2.5 pr-3 text-right text-slate-100">
                          {formatVnd(order.finalTotal)}
                        </td>
                        <td className="whitespace-nowrap py-2.5 pr-3 text-xs text-slate-300">
                          {order.discountType}
                        </td>
                        <td
                          className={`whitespace-nowrap py-2.5 pr-3 text-xs font-semibold ${
                            order.status === 'PAID'
                              ? 'text-emerald-400'
                              : order.status === 'PENDING'
                                ? 'text-amber-400'
                                : 'text-rose-400'
                          }`}
                        >
                          {orderStatusLabel(order.status)}
                        </td>
                        <td className="font-mono-tabular whitespace-nowrap py-2.5 text-xs text-slate-400">
                          {formatDateTime(order.createdAt)}
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        </div>

        {/* Selected Order Detail */}
        <div className="rounded-xl border border-slate-800 bg-[#111827] p-5 lg:col-span-5">
          {!selectedOrder ? (
            <div className="py-12 text-center text-sm text-slate-400">
              Chọn một đơn hàng để xem chi tiết sản phẩm và áp dụng giảm giá.
            </div>
          ) : (
            <div className="space-y-4">
              <div className="flex items-start justify-between border-b border-slate-800 pb-3">
                <div>
                  <h2 className="font-mono-tabular text-lg font-semibold text-sky-400">
                    {selectedOrder.id}
                  </h2>
                  <p className="mt-0.5 text-sm text-slate-300">
                    {selectedOrder.customerName} · {formatDateTime(selectedOrder.createdAt)}
                  </p>
                </div>
                <span
                  className={`text-xs font-semibold ${
                    selectedOrder.status === 'PAID'
                      ? 'text-emerald-400'
                      : selectedOrder.status === 'PENDING'
                        ? 'text-amber-400'
                        : 'text-rose-400'
                  }`}
                >
                  {orderStatusLabel(selectedOrder.status)}
                </span>
              </div>

              <div>
                <h3 className="text-xs font-semibold uppercase tracking-wider text-slate-400">
                  Sản phẩm trong đơn ({selectedOrder.items.length})
                </h3>
                <div className="mt-2 divide-y divide-slate-800 border-y border-slate-800">
                  {selectedOrder.items.map((item) => (
                    <div
                      key={`${selectedOrder.id}-${item.productId}`}
                      className="flex items-center justify-between py-2.5 text-sm"
                    >
                      <div className="min-w-0 pr-3">
                        <p className="truncate font-medium text-slate-200">{item.productName}</p>
                        <p className="font-mono-tabular text-xs text-slate-400">
                          {item.quantity} × {formatVnd(item.unitPrice)}
                        </p>
                      </div>
                      <p className="font-mono-tabular shrink-0 font-medium text-slate-100">
                        {formatVnd(item.unitPrice * item.quantity)}
                      </p>
                    </div>
                  ))}
                </div>
              </div>

              {/* Discount selector (editable only when PENDING) */}
              <div>
                <label
                  htmlFor="order-discount-select"
                  className="block text-xs font-medium text-slate-400"
                >
                  Chiến lược giảm giá (Strategy + Factory Pattern)
                </label>
                <select
                  id="order-discount-select"
                  disabled={selectedOrder.status !== 'PENDING' || updatingDiscount}
                  value={selectedOrder.discountType}
                  onChange={(e) =>
                    handleChangeDiscount(selectedOrder.id, e.target.value as DiscountType)
                  }
                  className="mt-1.5 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-100 disabled:opacity-60"
                >
                  <option value="NO_DISCOUNT">{discountLabel('NO_DISCOUNT')}</option>
                  <option value="VIP">{discountLabel('VIP')}</option>
                  <option value="SEASONAL">{discountLabel('SEASONAL')}</option>
                </select>
                {selectedOrder.status !== 'PENDING' && (
                  <p className="mt-1 text-xs text-slate-500">
                    Chỉ đơn PENDING mới có thể thay đổi chiến lược giảm giá.
                  </p>
                )}
              </div>

              {/* Totals */}
              <div className="space-y-1.5 border-t border-slate-800 pt-3 text-sm">
                <div className="flex justify-between text-slate-400">
                  <span>Tạm tính</span>
                  <span className="font-mono-tabular text-slate-200">
                    {formatVnd(selectedOrder.originalTotal)}
                  </span>
                </div>
                <div className="flex justify-between text-slate-400">
                  <span>Giảm giá ({Math.round(selectedOrder.discountRate * 100)}%)</span>
                  <span className="font-mono-tabular text-amber-400">
                    -{formatVnd(selectedOrder.discountAmount)}
                  </span>
                </div>
                <div className="flex justify-between border-t border-slate-800 pt-2 text-base font-semibold">
                  <span className="text-slate-100">Tổng thanh toán</span>
                  <span className="font-mono-tabular text-emerald-400">
                    {formatVnd(selectedOrder.finalTotal)}
                  </span>
                </div>
              </div>

              {selectedOrder.status === 'PENDING' && (
                <div className="flex flex-wrap items-center gap-3 pt-2">
                  <button
                    type="button"
                    onClick={() => onPayOrder(selectedOrder.id)}
                    className="whitespace-nowrap shrink-0 flex-1 rounded-lg bg-emerald-600 px-4 py-2 text-sm font-semibold text-white hover:bg-emerald-500"
                  >
                    Đi tới Thanh toán
                  </button>
                  <button
                    type="button"
                    onClick={() => setCancelTarget(selectedOrder)}
                    className="whitespace-nowrap shrink-0 rounded-lg border border-red-800/80 bg-red-950/40 px-4 py-2 text-sm font-medium text-red-300 hover:bg-red-900/50"
                  >
                    Hủy đơn
                  </button>
                </div>
              )}
            </div>
          )}
        </div>
      </div>

      <ConfirmDialog
        open={Boolean(cancelTarget)}
        title="Xác nhận hủy đơn hàng"
        message={
          cancelTarget
            ? `Bạn có chắc chắn muốn hủy đơn hàng ${cancelTarget.id} của khách ${cancelTarget.customerName}?`
            : ''
        }
        confirmLabel="Hủy đơn hàng"
        destructive
        onConfirm={confirmCancelOrder}
        onCancel={() => setCancelTarget(null)}
      />
    </div>
  );
};
