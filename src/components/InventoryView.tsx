import React, {useCallback, useEffect, useMemo, useState} from 'react';
import {javaApi} from '../api/javaClient';
import type {InventoryLog, Product} from '../types';
import {formatDateTime, formatVnd, productStatusLabel} from '../utils/format';

interface InventoryViewProps {
  refreshKey: number;
  onDataChanged: (message: string) => void;
}

export const InventoryView: React.FC<InventoryViewProps> = ({refreshKey, onDataChanged}) => {
  const [products, setProducts] = useState<Product[]>([]);
  const [lowStockIds, setLowStockIds] = useState<Set<string>>(new Set());
  const [logs, setLogs] = useState<InventoryLog[]>([]);
  const [filterMode, setFilterMode] = useState<'ALL' | 'LOW' | 'OUT'>('ALL');
  const [keyword, setKeyword] = useState('');
  const [adjustTarget, setAdjustTarget] = useState<Product | null>(null);
  const [adjustMode, setAdjustMode] = useState<'DELTA' | 'SET'>('DELTA');
  const [adjustAmount, setAdjustAmount] = useState('10');
  const [adjustReason, setAdjustReason] = useState('Nhập kho bổ sung');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadInventory = useCallback(async () => {
    try {
      const [allProducts, lowList, logList] = await Promise.all([
        javaApi.getProducts({sort: 'STOCK_ASC'}),
        javaApi.getLowStockProducts(),
        javaApi.getInventoryLogs(),
      ]);
      setProducts(allProducts);
      setLowStockIds(new Set(lowList.map((p) => p.id)));
      setLogs(logList);
      setError(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không tải được dữ liệu tồn kho.');
    }
  }, []);

  useEffect(() => {
    loadInventory();
  }, [loadInventory, refreshKey]);

  const filteredProducts = useMemo(() => {
    const q = keyword.trim().toLowerCase();
    return products.filter((p) => {
      if (filterMode === 'LOW' && !lowStockIds.has(p.id)) return false;
      if (filterMode === 'OUT' && p.stockQuantity > 0) return false;
      if (q && !p.name.toLowerCase().includes(q) && !p.id.toLowerCase().includes(q)) {
        return false;
      }
      return true;
    });
  }, [products, lowStockIds, filterMode, keyword]);

  const openAdjustModal = (product: Product) => {
    setAdjustTarget(product);
    setAdjustMode('DELTA');
    setAdjustAmount('10');
    setAdjustReason('Nhập kho bổ sung');
    setError(null);
  };

  const handleAdjustSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!adjustTarget) return;
    const numeric = Number(adjustAmount);
    if (!Number.isInteger(numeric)) {
      setError('Số lượng phải là số nguyên.');
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      await javaApi.adjustInventory({
        productId: adjustTarget.id,
        mode: adjustMode,
        amount: numeric,
        reason: adjustReason.trim() || 'Điều chỉnh tồn kho',
      });
      onDataChanged(`Đã cập nhật tồn kho cho ${adjustTarget.name}.`);
      setAdjustTarget(null);
      await loadInventory();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể điều chỉnh tồn kho.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Toolbar */}
      <div className="flex flex-wrap items-center justify-between gap-4 rounded-xl border border-slate-800 bg-[#111827] p-4">
        <div className="flex flex-wrap items-center gap-3">
          <div className="flex items-center gap-1 rounded-lg bg-slate-900 p-1">
            <button
              type="button"
              onClick={() => setFilterMode('ALL')}
              className={`whitespace-nowrap shrink-0 rounded-md px-3 py-1.5 text-xs font-medium transition-colors ${
                filterMode === 'ALL' ? 'bg-blue-600 text-white' : 'text-slate-400 hover:text-slate-100'
              }`}
            >
              Tất cả ({products.length})
            </button>
            <button
              type="button"
              onClick={() => setFilterMode('LOW')}
              className={`whitespace-nowrap shrink-0 rounded-md px-3 py-1.5 text-xs font-medium transition-colors ${
                filterMode === 'LOW' ? 'bg-amber-600 text-white' : 'text-slate-400 hover:text-slate-100'
              }`}
            >
              Sắp hết hàng ({lowStockIds.size})
            </button>
            <button
              type="button"
              onClick={() => setFilterMode('OUT')}
              className={`whitespace-nowrap shrink-0 rounded-md px-3 py-1.5 text-xs font-medium transition-colors ${
                filterMode === 'OUT' ? 'bg-rose-600 text-white' : 'text-slate-400 hover:text-slate-100'
              }`}
            >
              Hết hàng ({products.filter((p) => p.stockQuantity === 0).length})
            </button>
          </div>

          <input
            type="search"
            aria-label="Tìm sản phẩm trong kho"
            placeholder="Tìm mã hoặc tên sản phẩm..."
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            className="w-60 rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-100 placeholder:text-slate-500 focus:border-sky-500 focus:outline-none"
          />
        </div>
      </div>

      {error && (
        <div className="rounded-xl border border-red-800/60 bg-red-950/30 p-3 text-sm text-red-300">
          {error}
        </div>
      )}

      {/* Split Inventory + Logs */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-12">
        <div className="rounded-xl border border-slate-800 bg-[#111827] p-5 lg:col-span-7">
          <h2 className="text-base font-semibold text-slate-100">
            Tồn kho sản phẩm ({filteredProducts.length})
          </h2>
          <div className="mt-3 overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-slate-800 text-xs text-slate-400">
                <tr>
                  <th className="py-2.5 pr-3 font-medium">Mã SP</th>
                  <th className="py-2.5 pr-3 font-medium">Tên sản phẩm</th>
                  <th className="py-2.5 pr-3 text-right font-medium">Giá bán</th>
                  <th className="py-2.5 pr-3 text-right font-medium">Tồn kho</th>
                  <th className="py-2.5 pr-3 font-medium">Trạng thái</th>
                  <th className="py-2.5 text-right font-medium">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/80">
                {filteredProducts.map((p) => {
                  const isLow = lowStockIds.has(p.id);
                  return (
                    <tr key={p.id} className="hover:bg-slate-800/40">
                      <td className="font-mono-tabular py-2.5 pr-3 text-sky-400">{p.id}</td>
                      <td className="py-2.5 pr-3 text-slate-100">{p.name}</td>
                      <td className="font-mono-tabular py-2.5 pr-3 text-right text-slate-300">
                        {formatVnd(p.price)}
                      </td>
                      <td
                        className={`font-mono-tabular py-2.5 pr-3 text-right font-semibold ${
                          p.stockQuantity === 0
                            ? 'text-rose-400'
                            : isLow
                              ? 'text-amber-400'
                              : 'text-emerald-400'
                        }`}
                      >
                        {p.stockQuantity}
                      </td>
                      <td className="py-2.5 pr-3 text-xs text-slate-300">
                        {productStatusLabel(p.status)}
                      </td>
                      <td className="py-2.5 text-right">
                        <button
                          type="button"
                          onClick={() => openAdjustModal(p)}
                          aria-label={`Điều chỉnh kho ${p.name}`}
                          className="whitespace-nowrap shrink-0 rounded-lg border border-slate-700 bg-slate-800 px-3 py-1 text-xs font-medium text-slate-100 hover:bg-slate-700"
                        >
                          Điều chỉnh
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>

        {/* Inventory Logs */}
        <div className="rounded-xl border border-slate-800 bg-[#111827] p-5 lg:col-span-5">
          <h2 className="text-base font-semibold text-slate-100">
            Nhật ký biến động kho ({logs.length})
          </h2>
          <div className="mt-3 max-h-[520px] space-y-3 overflow-y-auto pr-1">
            {logs.length === 0 ? (
              <p className="py-8 text-center text-sm text-slate-400">Chưa có biến động kho nào.</p>
            ) : (
              logs.map((log) => (
                <div
                  key={log.id}
                  className="border-b border-slate-800 pb-3 text-sm last:border-b-0"
                >
                  <div className="flex items-center justify-between gap-2">
                    <span className="font-medium text-slate-100">{log.productName}</span>
                    <span
                      className={`font-mono-tabular text-xs font-semibold ${
                        log.changeAmount >= 0 ? 'text-emerald-400' : 'text-rose-400'
                      }`}
                    >
                      {log.changeAmount >= 0 ? `+${log.changeAmount}` : log.changeAmount}
                    </span>
                  </div>
                  <p className="mt-0.5 text-xs text-slate-400">{log.reason}</p>
                  <p className="font-mono-tabular mt-1 text-xs text-slate-500">
                    {log.previousStock} → {log.newStock} · {formatDateTime(log.createdAt)}
                  </p>
                </div>
              ))
            )}
          </div>
        </div>
      </div>

      {/* Adjust Stock Modal */}
      {adjustTarget && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4"
          role="dialog"
          aria-modal="true"
          aria-labelledby="adjust-stock-title"
        >
          <form
            onSubmit={handleAdjustSubmit}
            className="w-full max-w-md rounded-xl border border-slate-800 bg-[#111827] p-6 shadow-2xl"
          >
            <h3 id="adjust-stock-title" className="text-lg font-semibold text-slate-100">
              Điều chỉnh tồn kho: {adjustTarget.name}
            </h3>
            <p className="mt-1 text-xs text-slate-400">
              Tồn kho hiện tại:{' '}
              <span className="font-mono-tabular text-slate-200">{adjustTarget.stockQuantity}</span>
            </p>

            <div className="mt-4 space-y-4">
              <div>
                <label htmlFor="adjust-mode" className="block text-xs font-medium text-slate-400">
                  Kiểu điều chỉnh
                </label>
                <select
                  id="adjust-mode"
                  value={adjustMode}
                  onChange={(e) => setAdjustMode(e.target.value as 'DELTA' | 'SET')}
                  className="mt-1.5 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-100"
                >
                  <option value="DELTA">Cộng / trừ số lượng (DELTA)</option>
                  <option value="SET">Đặt trực tiếp tồn kho mới (SET)</option>
                </select>
              </div>

              <div>
                <label htmlFor="adjust-amount" className="block text-xs font-medium text-slate-400">
                  {adjustMode === 'DELTA' ? 'Số lượng thay đổi (+ nhập / - xuất)' : 'Số lượng tồn mới'}
                </label>
                <input
                  id="adjust-amount"
                  type="number"
                  value={adjustAmount}
                  onChange={(e) => setAdjustAmount(e.target.value)}
                  className="font-mono-tabular mt-1.5 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-100"
                  required
                />
              </div>

              <div>
                <label htmlFor="adjust-reason" className="block text-xs font-medium text-slate-400">
                  Lý do điều chỉnh
                </label>
                <input
                  id="adjust-reason"
                  type="text"
                  value={adjustReason}
                  onChange={(e) => setAdjustReason(e.target.value)}
                  className="mt-1.5 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-100"
                  required
                />
              </div>
            </div>

            <div className="mt-6 flex items-center justify-end gap-3">
              <button
                type="button"
                onClick={() => setAdjustTarget(null)}
                className="whitespace-nowrap shrink-0 rounded-lg border border-slate-700 bg-slate-800 px-4 py-2 text-sm font-medium text-slate-200 hover:bg-slate-700"
              >
                Hủy
              </button>
              <button
                type="submit"
                disabled={submitting}
                className="whitespace-nowrap shrink-0 rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-500 disabled:opacity-50"
              >
                {submitting ? 'Đang lưu...' : 'Xác nhận lưu'}
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
};
