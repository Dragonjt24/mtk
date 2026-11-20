import React, {useEffect, useState} from 'react';
import {javaApi} from '../api/javaClient';
import type {DiscountInfo, DiscountType, PricingResult, PromotionConfig} from '../types';
import {formatPercent, formatVnd} from '../utils/format';

interface DiscountsViewProps {
  refreshKey: number;
  onDataChanged?: (message?: string) => void;
}

export const DiscountsView: React.FC<DiscountsViewProps> = ({refreshKey, onDataChanged}) => {
  const [discounts, setDiscounts] = useState<DiscountInfo[]>([]);
  const [promoConfig, setPromoConfig] = useState<PromotionConfig | null>(null);
  const [seasonalEnabled, setSeasonalEnabled] = useState(false);
  const [seasonalName, setSeasonalName] = useState('Khuyến mãi sự kiện Gaming Festival');
  const [seasonalPercent, setSeasonalPercent] = useState('20');
  const [vipPercent, setVipPercent] = useState('15');
  const [savingPromo, setSavingPromo] = useState(false);

  const [amountInput, setAmountInput] = useState('1000000');
  const [selectedType, setSelectedType] = useState<DiscountType>('VIP');
  const [preview, setPreview] = useState<PricingResult | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setLoading(true);
    Promise.all([javaApi.getDiscounts(), javaApi.getPromotions()])
      .then(([list, promo]) => {
        setDiscounts(list);
        setPromoConfig(promo);
        setSeasonalEnabled(promo.seasonalPromoEnabled);
        setSeasonalName(promo.seasonalPromoName);
        setSeasonalPercent(String(Math.round(promo.seasonalDiscountRate * 100)));
        setVipPercent(String(Math.round(promo.vipDiscountRate * 100)));
        setError(null);
      })
      .catch((err) => {
        setError(err instanceof Error ? err.message : 'Không tải được chính sách giảm giá.');
      })
      .finally(() => setLoading(false));
  }, [refreshKey]);

  const handleSavePromotions = async (e: React.FormEvent) => {
    e.preventDefault();
    const sPct = Number(seasonalPercent);
    const vPct = Number(vipPercent);
    if (!Number.isFinite(sPct) || sPct < 1 || sPct > 90) {
      setError('Tỉ lệ giảm giá mùa phải từ 1% đến 90%.');
      return;
    }
    if (!Number.isFinite(vPct) || vPct < 1 || vPct > 90) {
      setError('Tỉ lệ giảm giá VIP phải từ 1% đến 90%.');
      return;
    }
    setSavingPromo(true);
    setError(null);
    try {
      const updated = await javaApi.updatePromotions({
        seasonalPromoEnabled: seasonalEnabled,
        seasonalPromoName: seasonalName.trim(),
        seasonalDiscountRate: sPct / 100,
        vipDiscountRate: vPct / 100,
      });
      setPromoConfig(updated);
      const freshDiscounts = await javaApi.getDiscounts();
      setDiscounts(freshDiscounts);
      onDataChanged?.(
        updated.seasonalPromoEnabled
          ? `Đã BẬT ưu đãi mùa "${updated.seasonalPromoName}" (${Math.round(updated.seasonalDiscountRate * 100)}%) cho toàn bộ tài khoản.`
          : 'Đã TẮT ưu đãi mùa. Hệ thống áp dụng mặc định: VIP giảm theo hạng, tài khoản thường 0%.',
      );
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không lưu được cấu hình ưu đãi.');
    } finally {
      setSavingPromo(false);
    }
  };

  const runPreview = async (rawValue: string, type: DiscountType) => {
    const parsed = Number(rawValue);
    if (!Number.isFinite(parsed) || parsed < 0) {
      setError('Vui lòng nhập số tiền hợp lệ (>= 0).');
      return;
    }
    setError(null);
    try {
      const res = await javaApi.previewPricing(parsed, type);
      setPreview(res);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không tính được giảm giá.');
    }
  };

  useEffect(() => {
    runPreview(amountInput, selectedType);
  }, [selectedType, promoConfig]);

  return (
    <div className="space-y-6">
      {/* Admin Storefront Promotion Control */}
      <div className="rounded-xl border border-slate-800 bg-[#111827] p-5">
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-800 pb-4">
          <div>
            <h2 className="text-base font-semibold text-slate-100">
              Quyết định Chính sách Ưu đãi trên Cửa hàng (Admin Setup)
            </h2>
            <p className="mt-1 text-xs text-slate-400">
              Khi tắt ưu đãi mùa: Tài khoản VIP hưởng giảm giá VIP, tài khoản thường không giảm giá (0%). Khi bật ưu đãi mùa: Toàn bộ tài khoản đều áp dụng giảm giá mùa.
            </p>
          </div>
          <span
            className={`text-xs font-semibold ${
              seasonalEnabled ? 'text-amber-400' : 'text-sky-400'
            }`}
          >
            {seasonalEnabled ? 'ĐANG BẬT ƯU ĐÃI MÙA TOÀN HỆ THỐNG' : 'MẶC ĐỊNH THEO HẠNG VIP / THƯỜNG'}
          </span>
        </div>

        <form onSubmit={handleSavePromotions} className="mt-4 space-y-4">
          <label className="flex cursor-pointer items-center justify-between rounded-lg border border-slate-800 bg-slate-900/70 px-4 py-3">
            <div>
              <p className="text-sm font-medium text-slate-100">
                Kích hoạt Ưu đãi theo mùa cho toàn bộ tài khoản
              </p>
              <p className="text-xs text-slate-400">
                Bật chế độ này khi có sự kiện Gaming Festival / Sale mùa để áp dụng cho cả tài khoản thường và VIP.
              </p>
            </div>
            <input
              id="toggle-seasonal-promo"
              type="checkbox"
              checked={seasonalEnabled}
              onChange={(e) => setSeasonalEnabled(e.target.checked)}
              className="h-5 w-5 accent-blue-600"
            />
          </label>

          <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
            <div>
              <label htmlFor="promo-seasonal-name" className="block text-xs font-medium text-slate-400">
                Tên chương trình ưu đãi mùa
              </label>
              <input
                id="promo-seasonal-name"
                type="text"
                value={seasonalName}
                onChange={(e) => setSeasonalName(e.target.value)}
                className="mt-1.5 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-100"
                required
              />
            </div>
            <div>
              <label htmlFor="promo-seasonal-pct" className="block text-xs font-medium text-slate-400">
                Mức giảm ưu đãi mùa (%)
              </label>
              <input
                id="promo-seasonal-pct"
                type="number"
                min={1}
                max={90}
                value={seasonalPercent}
                onChange={(e) => setSeasonalPercent(e.target.value)}
                className="font-mono-tabular mt-1.5 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-100"
                required
              />
            </div>
            <div>
              <label htmlFor="promo-vip-pct" className="block text-xs font-medium text-slate-400">
                Mức giảm thành viên VIP (%)
              </label>
              <input
                id="promo-vip-pct"
                type="number"
                min={1}
                max={90}
                value={vipPercent}
                onChange={(e) => setVipPercent(e.target.value)}
                className="font-mono-tabular mt-1.5 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-100"
                required
              />
            </div>
          </div>

          <div className="flex justify-end">
            <button
              type="submit"
              disabled={savingPromo}
              className="whitespace-nowrap shrink-0 rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-500 disabled:opacity-50"
            >
              {savingPromo ? 'Đang lưu...' : 'Lưu cấu hình ưu đãi'}
            </button>
          </div>
        </form>
      </div>

      <div className="rounded-xl border border-slate-800 bg-[#111827] p-5">
        <h2 className="text-base font-semibold text-slate-100">
          Danh sách chiến lược giảm giá (Factory & Strategy Pattern)
        </h2>
        <p className="mt-1 text-xs text-slate-400">
          DiscountFactory khởi tạo đúng cài đặt DiscountStrategy và truyền vào DiscountContext trong Java Core.
        </p>

        {error && (
          <div className="mt-3 rounded-lg border border-red-800/60 bg-red-950/30 p-3 text-sm text-red-300">
            {error}
          </div>
        )}

        <div className="mt-4 overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="border-b border-slate-800 text-xs text-slate-400">
              <tr>
                <th className="py-2.5 pr-3 font-medium">Mã loại</th>
                <th className="py-2.5 pr-3 font-medium">Tên hiển thị</th>
                <th className="py-2.5 pr-3 text-right font-medium">Tỉ lệ giảm</th>
                <th className="py-2.5 pr-3 font-medium">Lớp Strategy</th>
                <th className="py-2.5 font-medium">Mô tả</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/80">
              {loading && discounts.length === 0 ? (
                <tr>
                  <td colSpan={5} className="py-6 text-center text-slate-400">
                    Đang tải cấu hình từ DiscountFactory...
                  </td>
                </tr>
              ) : (
                discounts.map((d) => (
                  <tr key={d.type} className="hover:bg-slate-800/40">
                    <td className="font-mono-tabular py-2.5 pr-3 text-sky-400">{d.type}</td>
                    <td className="py-2.5 pr-3 font-medium text-slate-100">{d.displayName}</td>
                    <td className="font-mono-tabular py-2.5 pr-3 text-right text-amber-400">
                      {formatPercent(d.rate)}
                    </td>
                    <td className="font-mono-tabular py-2.5 pr-3 text-xs text-slate-300">
                      {d.strategyClass}
                    </td>
                    <td className="py-2.5 text-slate-300">{d.description}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Interactive Calculator */}
      <div className="rounded-xl border border-slate-800 bg-[#111827] p-5">
        <h2 className="text-base font-semibold text-slate-100">
          Mô phỏng tính giá đơn hàng bằng Java Strategy
        </h2>
        <div className="mt-4 grid grid-cols-1 gap-4 md:grid-cols-3">
          <div>
            <label htmlFor="sim-amount" className="block text-xs font-medium text-slate-400">
              Tổng tiền gốc (VNĐ)
            </label>
            <input
              id="sim-amount"
              type="number"
              min={0}
              step={10000}
              value={amountInput}
              onChange={(e) => setAmountInput(e.target.value)}
              className="font-mono-tabular mt-1.5 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-100"
            />
          </div>
          <div>
            <label htmlFor="sim-type" className="block text-xs font-medium text-slate-400">
              Loại giảm giá
            </label>
            <select
              id="sim-type"
              value={selectedType}
              onChange={(e) => setSelectedType(e.target.value as DiscountType)}
              className="mt-1.5 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-100"
            >
              <option value="NO_DISCOUNT">Không giảm giá (0%)</option>
              <option value="VIP">Khách hàng VIP (15%)</option>
              <option value="SEASONAL">Khuyến mãi mùa (20%)</option>
            </select>
          </div>
          <div className="flex items-end">
            <button
              type="button"
              onClick={() => runPreview(amountInput, selectedType)}
              className="whitespace-nowrap shrink-0 w-full rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-500"
            >
              Tính thử qua Java Engine
            </button>
          </div>
        </div>

        {preview && (
          <div className="mt-5 grid grid-cols-1 gap-4 border-t border-slate-800 pt-4 sm:grid-cols-3">
            <div>
              <p className="text-xs text-slate-400">Tổng tiền gốc</p>
              <p className="font-mono-tabular mt-1 text-lg font-semibold text-slate-100">
                {formatVnd(preview.originalTotal)}
              </p>
            </div>
            <div>
              <p className="text-xs text-slate-400">
                Số tiền giảm ({formatPercent(preview.discountRate)})
              </p>
              <p className="font-mono-tabular mt-1 text-lg font-semibold text-amber-400">
                -{formatVnd(preview.discountAmount)}
              </p>
            </div>
            <div>
              <p className="text-xs text-slate-400">Thành tiền sau giảm</p>
              <p className="font-mono-tabular mt-1 text-lg font-semibold text-emerald-400">
                {formatVnd(preview.finalTotal)}
              </p>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
