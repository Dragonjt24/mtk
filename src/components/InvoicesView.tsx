import React, {useCallback, useEffect, useMemo, useState} from 'react';
import {javaApi} from '../api/javaClient';
import type {Bill} from '../types';
import {
  formatDateTime,
  formatPercent,
  formatVnd,
  paymentMethodLabel,
  paymentStatusLabel,
} from '../utils/format';

interface InvoicesViewProps {
  refreshKey: number;
  initialBillId: string | null;
}

export const InvoicesView: React.FC<InvoicesViewProps> = ({refreshKey, initialBillId}) => {
  const [bills, setBills] = useState<Bill[]>([]);
  const [selectedBillId, setSelectedBillId] = useState<string | null>(initialBillId);
  const [keyword, setKeyword] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadBills = useCallback(async () => {
    setLoading(true);
    try {
      const list = await javaApi.getBills();
      setBills(list);
      if (initialBillId && list.some((b) => b.id === initialBillId)) {
        setSelectedBillId(initialBillId);
      } else if (list.length > 0 && (!selectedBillId || !list.some((b) => b.id === selectedBillId))) {
        setSelectedBillId(list[0].id);
      }
      setError(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không tải được danh sách hóa đơn.');
    } finally {
      setLoading(false);
    }
  }, [initialBillId, selectedBillId]);

  useEffect(() => {
    loadBills();
  }, [loadBills, refreshKey]);

  const filteredBills = useMemo(() => {
    const q = keyword.trim().toLowerCase();
    if (!q) return bills;
    return bills.filter(
      (b) =>
        b.id.toLowerCase().includes(q) ||
        b.orderId.toLowerCase().includes(q) ||
        b.customerName.toLowerCase().includes(q),
    );
  }, [bills, keyword]);

  const activeBill = useMemo(
    () => bills.find((b) => b.id === selectedBillId) ?? filteredBills[0] ?? null,
    [bills, filteredBills, selectedBillId],
  );

  return (
    <div className="grid grid-cols-1 gap-6 lg:grid-cols-12">
      {/* Left: Bill List */}
      <div className="rounded-xl border border-slate-800 bg-[#111827] p-5 lg:col-span-6">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-base font-semibold text-slate-100">
            Hóa đơn đã xuất ({filteredBills.length})
          </h2>
          <input
            type="search"
            aria-label="Tìm hóa đơn theo mã hoặc tên khách hàng"
            placeholder="Tìm mã hóa đơn, khách..."
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            className="w-48 rounded-lg border border-slate-700 bg-slate-900 px-3 py-1.5 text-sm text-slate-100 placeholder:text-slate-500 focus:border-sky-500 focus:outline-none"
          />
        </div>

        {error && (
          <div className="mt-3 rounded-lg border border-red-800/60 bg-red-950/30 p-3 text-xs text-red-300">
            {error}
          </div>
        )}

        <div className="mt-4 overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="border-b border-slate-800 text-xs text-slate-400">
              <tr>
                <th className="whitespace-nowrap py-2 pr-2 font-medium">Mã HĐ</th>
                <th className="whitespace-nowrap py-2 pr-2 font-medium">Khách hàng</th>
                <th className="whitespace-nowrap py-2 pr-2 text-right font-medium">Tổng tiền</th>
                <th className="whitespace-nowrap py-2 font-medium">Ngày xuất</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/80">
              {loading && bills.length === 0 ? (
                <tr>
                  <td colSpan={4} className="py-6 text-center text-sm text-slate-400">
                    Đang tải hóa đơn...
                  </td>
                </tr>
              ) : filteredBills.length === 0 ? (
                <tr>
                  <td colSpan={4} className="py-8 text-center text-sm text-slate-400">
                    Chưa có hóa đơn nào khớp bộ lọc.
                  </td>
                </tr>
              ) : (
                filteredBills.map((bill) => {
                  const isSelected = activeBill?.id === bill.id;
                  return (
                    <tr
                      key={bill.id}
                      className={`transition-colors ${
                        isSelected ? 'bg-slate-800/70' : 'hover:bg-slate-800/40'
                      }`}
                    >
                      <td className="whitespace-nowrap py-2.5 pr-2">
                        <button
                          type="button"
                          onClick={() => setSelectedBillId(bill.id)}
                          className="font-mono-tabular whitespace-nowrap font-medium text-sky-400 hover:underline"
                        >
                          {bill.id}
                        </button>
                      </td>
                      <td className="whitespace-nowrap py-2.5 pr-2 text-slate-200">{bill.customerName}</td>
                      <td className="font-mono-tabular whitespace-nowrap py-2.5 pr-2 text-right text-emerald-400">
                        {formatVnd(bill.finalTotal)}
                      </td>
                      <td className="font-mono-tabular whitespace-nowrap py-2.5 text-slate-400">
                        {formatDateTime(bill.issuedAt)}
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Right: Printable Invoice Sheet & OpenPDF Export */}
      <div className="rounded-xl border border-slate-800 bg-[#111827] p-5 lg:col-span-6">
        {!activeBill ? (
          <div className="py-12 text-center text-sm text-slate-400">
            Chọn một hóa đơn bên trái để xem chi tiết, in hoặc xuất PDF.
          </div>
        ) : (
          <div className="space-y-5">
            <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-800 pb-4">
              <div>
                <h2 className="text-base font-semibold text-slate-100">
                  Chi tiết hóa đơn {activeBill.id}
                </h2>
                <p className="text-xs text-slate-400">
                  Liên kết đơn hàng {activeBill.orderId} · {formatDateTime(activeBill.issuedAt)}
                </p>
              </div>
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={() => window.print()}
                  className="whitespace-nowrap shrink-0 rounded-lg border border-slate-700 bg-slate-800 px-3.5 py-2 text-xs font-semibold text-slate-100 hover:bg-slate-700"
                >
                  In hóa đơn
                </button>
                <a
                  href={javaApi.getBillPdfUrl(activeBill.id)}
                  download={`${activeBill.id}.pdf`}
                  className="whitespace-nowrap shrink-0 rounded-lg bg-blue-600 px-3.5 py-2 text-xs font-semibold text-white hover:bg-blue-500"
                >
                  Xuất PDF (OpenPDF)
                </a>
              </div>
            </div>

            <div id="printable-invoice" className="rounded-xl border border-slate-800 bg-slate-950/60 p-6">
              <div className="flex flex-wrap items-start justify-between gap-4 border-b border-slate-800 pb-4">
                <div>
                  <h3 className="text-lg font-bold tracking-tight text-slate-100">
                    GAMING GEAR HUB STORE
                  </h3>
                  <p className="mt-1 text-xs text-slate-400">
                    268 Lý Thường Kiệt, Quận 10, TP. Hồ Chí Minh · Hotline: 1900 6868
                  </p>
                </div>
                <div className="text-right">
                  <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">
                    HÓA ĐƠN BÁN HÀNG
                  </p>
                  <p className="font-mono-tabular mt-1 text-base font-semibold text-sky-400">
                    {activeBill.id}
                  </p>
                </div>
              </div>

              <div className="mt-4 grid grid-cols-1 gap-3 text-sm sm:grid-cols-2">
                <div>
                  <p className="text-xs text-slate-400">Khách hàng</p>
                  <p className="font-medium text-slate-100">{activeBill.customerName}</p>
                  <p className="font-mono-tabular text-xs text-slate-300">
                    SĐT: {activeBill.customerPhone}
                  </p>
                  <p className="text-xs text-slate-400">{activeBill.customerAddress}</p>
                </div>
                <div className="sm:text-right">
                  <p className="text-xs text-slate-400">Thông tin thanh toán</p>
                  <p className="text-slate-200">{paymentMethodLabel(activeBill.paymentMethod)}</p>
                  <p className="text-xs font-medium text-emerald-400">
                    {paymentStatusLabel(activeBill.paymentStatus)}
                  </p>
                  <p className="font-mono-tabular text-xs text-slate-400">
                    Mã đơn: {activeBill.orderId}
                  </p>
                </div>
              </div>

              <div className="mt-5 overflow-x-auto">
                <table className="w-full text-left text-sm">
                  <thead className="border-y border-slate-800 text-xs text-slate-400">
                    <tr>
                      <th className="py-2 pr-3 font-medium">Sản phẩm</th>
                      <th className="py-2 pr-3 text-right font-medium">SL</th>
                      <th className="py-2 pr-3 text-right font-medium">Đơn giá</th>
                      <th className="py-2 text-right font-medium">Thành tiền</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800/80">
                    {activeBill.items.map((item) => (
                      <tr key={`${activeBill.id}-${item.productId}`}>
                        <td className="py-2.5 pr-3 text-slate-100">{item.productName}</td>
                        <td className="font-mono-tabular py-2.5 pr-3 text-right text-slate-200">
                          {item.quantity}
                        </td>
                        <td className="font-mono-tabular py-2.5 pr-3 text-right text-slate-300">
                          {formatVnd(item.unitPrice)}
                        </td>
                        <td className="font-mono-tabular py-2.5 text-right font-medium text-slate-100">
                          {formatVnd(item.lineTotal)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              <div className="mt-4 space-y-1.5 border-t border-slate-800 pt-4 text-sm">
                <div className="flex justify-between text-slate-400">
                  <span>Tổng tiền gốc</span>
                  <span className="font-mono-tabular text-slate-200">
                    {formatVnd(activeBill.originalTotal)}
                  </span>
                </div>
                <div className="flex justify-between text-slate-400">
                  <span>
                    Giảm giá ({activeBill.discountType} · {formatPercent(activeBill.discountRate)})
                  </span>
                  <span className="font-mono-tabular text-amber-400">
                    -{formatVnd(activeBill.discountAmount)}
                  </span>
                </div>
                <div className="flex justify-between border-t border-slate-800 pt-2 text-base font-bold">
                  <span className="text-slate-100">Tổng thanh toán</span>
                  <span className="font-mono-tabular text-emerald-400">
                    {formatVnd(activeBill.finalTotal)}
                  </span>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
