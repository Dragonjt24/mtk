import React, {useEffect, useState} from 'react';
import {javaApi} from '../api/javaClient';
import type {DashboardPayload} from '../types';
import {formatDateTime, formatVnd, orderStatusLabel} from '../utils/format';

export const DashboardView: React.FC<{refreshKey: number}> = ({refreshKey}) => {
  const [data, setData] = useState<DashboardPayload | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    setLoading(true);
    javaApi
      .getDashboard()
      .then((res) => {
        if (active) {
          setData(res);
          setError(null);
        }
      })
      .catch((err) => {
        if (active) setError(err instanceof Error ? err.message : 'Không tải được dữ liệu Dashboard.');
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [refreshKey]);

  if (loading && !data) {
    return (
      <div className="space-y-6">
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-5">
          {Array.from({length: 5}).map((_, i) => (
            <div key={i} className="h-24 animate-pulse rounded-xl border border-slate-800 bg-[#111827]" />
          ))}
        </div>
        <div className="h-64 animate-pulse rounded-xl border border-slate-800 bg-[#111827]" />
      </div>
    );
  }

  if (error || !data) {
    return (
      <div className="rounded-xl border border-red-800/60 bg-red-950/30 p-5 text-sm text-red-300">
        {error ?? 'Không có dữ liệu tổng quan.'}
      </div>
    );
  }

  const revenueEntries = Object.entries(data.revenueLastDays || {});
  const maxRevenue = Math.max(1, ...revenueEntries.map(([, v]) => Number(v || 0)));

  const pendingCount = data.orderStatusCounts?.PENDING ?? 0;
  const paidCount = data.orderStatusCounts?.PAID ?? 0;
  const cancelledCount = data.orderStatusCounts?.CANCELLED ?? 0;
  const totalStatusCount = Math.max(1, pendingCount + paidCount + cancelledCount);

  return (
    <div className="space-y-6">
      {/* 5 Live KPI Cards */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-5">
        <div className="rounded-xl border border-slate-800 bg-[#111827] p-4">
          <p className="text-xs font-medium text-slate-400">Tổng sản phẩm</p>
          <p className="font-mono-tabular mt-2 text-2xl font-semibold text-slate-100">
            {data.summary.totalProducts}
          </p>
          <p className="mt-1 text-xs text-sky-400">Trong InMemoryDataStore</p>
        </div>
        <div className="rounded-xl border border-slate-800 bg-[#111827] p-4">
          <p className="text-xs font-medium text-slate-400">Tổng đơn hàng</p>
          <p className="font-mono-tabular mt-2 text-2xl font-semibold text-slate-100">
            {data.summary.totalOrders}
          </p>
          <p className="mt-1 text-xs text-slate-400">Tất cả trạng thái</p>
        </div>
        <div className="rounded-xl border border-slate-800 bg-[#111827] p-4">
          <p className="text-xs font-medium text-slate-400">Tổng doanh thu</p>
          <p className="font-mono-tabular mt-2 text-xl font-semibold text-emerald-400">
            {formatVnd(data.summary.totalRevenue)}
          </p>
          <p className="mt-1 text-xs text-slate-400">Từ đơn đã thanh toán</p>
        </div>
        <div className="rounded-xl border border-slate-800 bg-[#111827] p-4">
          <p className="text-xs font-medium text-slate-400">Đơn chờ xử lý</p>
          <p className="font-mono-tabular mt-2 text-2xl font-semibold text-amber-400">
            {data.summary.pendingOrders}
          </p>
          <p className="mt-1 text-xs text-slate-400">Trạng thái PENDING</p>
        </div>
        <div className="rounded-xl border border-slate-800 bg-[#111827] p-4">
          <p className="text-xs font-medium text-slate-400">Sắp hết hàng</p>
          <p className="font-mono-tabular mt-2 text-2xl font-semibold text-rose-400">
            {data.summary.lowStockProducts}
          </p>
          <p className="mt-1 text-xs text-slate-400">Dưới ngưỡng cảnh báo</p>
        </div>
      </div>

      {/* Charts Row */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        <div className="rounded-xl border border-slate-800 bg-[#111827] p-5 lg:col-span-2">
          <div className="flex items-center justify-between">
            <h2 className="text-base font-semibold text-slate-100">Doanh thu 7 ngày gần nhất</h2>
            <span className="text-xs text-slate-400">Đơn vị: VNĐ</span>
          </div>
          <div className="mt-5 grid h-52 grid-cols-7 items-end gap-3 border-b border-slate-800 pb-3">
            {revenueEntries.map(([day, value]) => {
              const numeric = Number(value || 0);
              const heightPct = numeric <= 0 ? 4 : Math.max(10, Math.round((numeric / maxRevenue) * 100));
              return (
                <div key={day} className="flex h-full flex-col items-center justify-end gap-2">
                  <span className="font-mono-tabular text-[11px] text-slate-300">
                    {numeric > 0 ? `${(numeric / 1_000_000).toFixed(1)}M` : '0'}
                  </span>
                  <div
                    className="w-full max-w-[44px] rounded-t-md bg-gradient-to-t from-blue-600 to-sky-400 transition-all"
                    style={{height: `${heightPct}%`}}
                    title={`${day}: ${formatVnd(numeric)}`}
                  />
                  <span className="font-mono-tabular text-xs text-slate-400">{day}</span>
                </div>
              );
            })}
          </div>
        </div>

        <div className="rounded-xl border border-slate-800 bg-[#111827] p-5">
          <h2 className="text-base font-semibold text-slate-100">Đơn hàng theo trạng thái</h2>
          <p className="mt-1 text-xs text-slate-400">
            Tổng {pendingCount + paidCount + cancelledCount} đơn hàng
          </p>
          <div className="mt-5 space-y-4">
            <div>
              <div className="flex items-center justify-between text-sm">
                <span className="text-emerald-400">Đã thanh toán (PAID)</span>
                <span className="font-mono-tabular text-slate-200">{paidCount}</span>
              </div>
              <div className="mt-1.5 h-2.5 w-full overflow-hidden rounded bg-slate-800">
                <div
                  className="h-full bg-emerald-500"
                  style={{width: `${Math.round((paidCount / totalStatusCount) * 100)}%`}}
                />
              </div>
            </div>
            <div>
              <div className="flex items-center justify-between text-sm">
                <span className="text-amber-400">Chờ xử lý (PENDING)</span>
                <span className="font-mono-tabular text-slate-200">{pendingCount}</span>
              </div>
              <div className="mt-1.5 h-2.5 w-full overflow-hidden rounded bg-slate-800">
                <div
                  className="h-full bg-amber-500"
                  style={{width: `${Math.round((pendingCount / totalStatusCount) * 100)}%`}}
                />
              </div>
            </div>
            <div>
              <div className="flex items-center justify-between text-sm">
                <span className="text-rose-400">Đã hủy (CANCELLED)</span>
                <span className="font-mono-tabular text-slate-200">{cancelledCount}</span>
              </div>
              <div className="mt-1.5 h-2.5 w-full overflow-hidden rounded bg-slate-800">
                <div
                  className="h-full bg-rose-500"
                  style={{width: `${Math.round((cancelledCount / totalStatusCount) * 100)}%`}}
                />
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Bottom Tables */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <div className="rounded-xl border border-slate-800 bg-[#111827] p-5">
          <h2 className="text-base font-semibold text-slate-100">Đơn hàng gần đây</h2>
          <div className="mt-3 overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-slate-800 text-xs text-slate-400">
                <tr>
                  <th className="whitespace-nowrap py-2.5 pr-3 font-medium">Mã đơn</th>
                  <th className="whitespace-nowrap py-2.5 pr-3 font-medium">Khách hàng</th>
                  <th className="whitespace-nowrap py-2.5 pr-3 text-right font-medium">Thanh toán</th>
                  <th className="whitespace-nowrap py-2.5 pr-3 font-medium">Trạng thái</th>
                  <th className="whitespace-nowrap py-2.5 font-medium">Ngày tạo</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/80">
                {data.recentOrders.map((order) => (
                  <tr key={order.id} className="hover:bg-slate-800/40">
                    <td className="font-mono-tabular whitespace-nowrap py-2.5 pr-3 text-sky-400">{order.id}</td>
                    <td className="whitespace-nowrap py-2.5 pr-3 text-slate-200">{order.customerName}</td>
                    <td className="font-mono-tabular whitespace-nowrap py-2.5 pr-3 text-right text-slate-100">
                      {formatVnd(order.finalTotal)}
                    </td>
                    <td
                      className={`whitespace-nowrap py-2.5 pr-3 text-xs font-medium ${
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
                ))}
              </tbody>
            </table>
          </div>
        </div>

        <div className="rounded-xl border border-slate-800 bg-[#111827] p-5">
          <h2 className="text-base font-semibold text-slate-100">Sản phẩm bán chạy</h2>
          <div className="mt-3 overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-slate-800 text-xs text-slate-400">
                <tr>
                  <th className="py-2.5 pr-3 font-medium">Mã SP</th>
                  <th className="py-2.5 pr-3 font-medium">Tên sản phẩm</th>
                  <th className="py-2.5 pr-3 text-right font-medium">Đã bán</th>
                  <th className="py-2.5 text-right font-medium">Doanh thu</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/80">
                {data.topProducts.length === 0 ? (
                  <tr>
                    <td colSpan={4} className="py-6 text-center text-slate-400">
                      Chưa có sản phẩm bán ra.
                    </td>
                  </tr>
                ) : (
                  data.topProducts.map((item) => (
                    <tr key={item.productId} className="hover:bg-slate-800/40">
                      <td className="font-mono-tabular py-2.5 pr-3 text-sky-400">{item.productId}</td>
                      <td className="py-2.5 pr-3 text-slate-200">{item.productName}</td>
                      <td className="font-mono-tabular py-2.5 pr-3 text-right text-slate-100">
                        {item.totalQuantitySold}
                      </td>
                      <td className="font-mono-tabular py-2.5 text-right text-emerald-400">
                        {formatVnd(item.totalRevenue)}
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
  );
};
