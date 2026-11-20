import React, {useCallback, useEffect, useMemo, useState} from 'react';
import {javaApi} from '../api/javaClient';
import type {Customer, DiscountType, PricingResult, Product, PromotionConfig} from '../types';
import {discountLabel, formatVnd} from '../utils/format';
import {resolveAutomaticDiscount} from '../utils/storeHelpers';

interface CreateOrderViewProps {
  refreshKey: number;
  onOrderCreated: (orderId: string, goToPayment: boolean) => void;
}

interface CartEntry {
  product: Product;
  quantity: number;
}

export const CreateOrderView: React.FC<CreateOrderViewProps> = ({refreshKey, onOrderCreated}) => {
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [selectedCustomerId, setSelectedCustomerId] = useState('');
  const [productSearch, setProductSearch] = useState('');
  const [cart, setCart] = useState<CartEntry[]>([]);
  const [discountType, setDiscountType] = useState<DiscountType>('NO_DISCOUNT');
  const [promotionConfig, setPromotionConfig] = useState<PromotionConfig | null>(null);
  const [pricing, setPricing] = useState<PricingResult | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadCatalog = useCallback(async () => {
    setLoading(true);
    try {
      const [custList, prodList, promo] = await Promise.all([
        javaApi.getCustomers(),
        javaApi.getProducts({sort: 'NAME_ASC'}),
        javaApi.getPromotions(),
      ]);
      setCustomers(custList);
      setProducts(prodList.filter((p) => p.status === 'ACTIVE' && p.stockQuantity > 0));
      setPromotionConfig(promo);
      if (custList.length > 0 && !selectedCustomerId) {
        const first = custList[0];
        setSelectedCustomerId(first.id);
        setDiscountType(resolveAutomaticDiscount(first, promo));
      } else if (selectedCustomerId) {
        const currentCust = custList.find((c) => c.id === selectedCustomerId) ?? null;
        if (currentCust) {
          setDiscountType(resolveAutomaticDiscount(currentCust, promo));
        }
      }
      setError(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không tải được dữ liệu tạo đơn.');
    } finally {
      setLoading(false);
    }
  }, [selectedCustomerId]);

  useEffect(() => {
    loadCatalog();
  }, [loadCatalog, refreshKey]);

  const rawSubtotal = useMemo(
    () => cart.reduce((sum, entry) => sum + entry.product.price * entry.quantity, 0),
    [cart],
  );

  // Call Java DiscountFactory + DiscountContext in real time whenever cart or discount changes
  useEffect(() => {
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
  }, [rawSubtotal, discountType]);

  const filteredProducts = useMemo(() => {
    const q = productSearch.trim().toLowerCase();
    if (!q) return products;
    return products.filter(
      (p) =>
        p.name.toLowerCase().includes(q) ||
        p.brand.toLowerCase().includes(q) ||
        p.id.toLowerCase().includes(q),
    );
  }, [products, productSearch]);

  const handleCustomerChange = (customerId: string) => {
    setSelectedCustomerId(customerId);
    const cust = customers.find((c) => c.id === customerId) ?? null;
    setDiscountType(resolveAutomaticDiscount(cust, promotionConfig));
  };

  const addToCart = (product: Product) => {
    setError(null);
    setCart((prev) => {
      const existing = prev.find((item) => item.product.id === product.id);
      if (existing) {
        if (existing.quantity >= product.stockQuantity) {
          setError(`Sản phẩm "${product.name}" chỉ còn ${product.stockQuantity} chiếc trong kho.`);
          return prev;
        }
        return prev.map((item) =>
          item.product.id === product.id ? {...item, quantity: item.quantity + 1} : item,
        );
      }
      return [...prev, {product, quantity: 1}];
    });
  };

  const updateCartQuantity = (productId: string, delta: number) => {
    setError(null);
    setCart((prev) =>
      prev
        .map((item) => {
          if (item.product.id !== productId) return item;
          const next = item.quantity + delta;
          if (next > item.product.stockQuantity) {
            setError(`Vượt quá tồn kho tối đa (${item.product.stockQuantity}).`);
            return item;
          }
          return {...item, quantity: next};
        })
        .filter((item) => item.quantity > 0),
    );
  };

  const removeFromCart = (productId: string) => {
    setCart((prev) => prev.filter((item) => item.product.id !== productId));
  };

  const submitOrder = async (goToPayment: boolean) => {
    if (!selectedCustomerId) {
      setError('Vui lòng chọn khách hàng.');
      return;
    }
    if (cart.length === 0) {
      setError('Giỏ hàng đang trống. Hãy thêm ít nhất 1 sản phẩm.');
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      const created = await javaApi.createOrder({
        customerId: selectedCustomerId,
        discountType,
        items: cart.map((c) => ({productId: c.product.id, quantity: c.quantity})),
      });
      setCart([]);
      onOrderCreated(created.id, goToPayment);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể tạo đơn hàng.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="grid grid-cols-1 gap-6 lg:grid-cols-12">
      {/* Left: Available active products */}
      <div className="rounded-xl border border-slate-800 bg-[#111827] p-5 lg:col-span-7">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h2 className="text-base font-semibold text-slate-100">Chọn sản phẩm Gaming Gear</h2>
            <p className="text-xs text-slate-400">
              Hiển thị các sản phẩm đang kinh doanh và còn tồn kho
            </p>
          </div>
          <input
            type="search"
            aria-label="Tìm sản phẩm để thêm vào đơn"
            placeholder="Tìm theo tên hoặc hãng..."
            value={productSearch}
            onChange={(e) => setProductSearch(e.target.value)}
            className="w-60 rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-100 placeholder:text-slate-500 focus:border-sky-500 focus:outline-none"
          />
        </div>

        <div className="mt-4 max-h-[540px] overflow-y-auto pr-1">
          {loading ? (
            <p className="py-8 text-center text-sm text-slate-400">Đang tải danh sách sản phẩm...</p>
          ) : filteredProducts.length === 0 ? (
            <p className="py-8 text-center text-sm text-slate-400">Không tìm thấy sản phẩm phù hợp.</p>
          ) : (
            <table className="w-full text-left text-sm">
              <thead className="border-b border-slate-800 text-xs text-slate-400">
                <tr>
                  <th className="py-2 pr-3 font-medium">Sản phẩm</th>
                  <th className="py-2 pr-3 font-medium">Thương hiệu</th>
                  <th className="py-2 pr-3 text-right font-medium">Giá bán</th>
                  <th className="py-2 pr-3 text-right font-medium">Kho</th>
                  <th className="py-2 text-right font-medium">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/80">
                {filteredProducts.map((product) => (
                  <tr key={product.id} className="hover:bg-slate-800/40">
                    <td className="py-2.5 pr-3">
                      <p className="font-medium text-slate-100">{product.name}</p>
                      <p className="font-mono-tabular text-xs text-slate-400">{product.id}</p>
                    </td>
                    <td className="py-2.5 pr-3 text-slate-300">{product.brand}</td>
                    <td className="font-mono-tabular py-2.5 pr-3 text-right text-slate-100">
                      {formatVnd(product.price)}
                    </td>
                    <td className="font-mono-tabular py-2.5 pr-3 text-right text-slate-300">
                      {product.stockQuantity}
                    </td>
                    <td className="py-2.5 text-right">
                      <button
                        type="button"
                        onClick={() => addToCart(product)}
                        aria-label={`Thêm ${product.name} vào đơn`}
                        className="whitespace-nowrap shrink-0 rounded-lg bg-slate-800 px-3 py-1.5 text-xs font-medium text-sky-400 hover:bg-blue-600 hover:text-white"
                      >
                        + Thêm
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {/* Right: Order Cart & Strategy Pricing */}
      <div className="rounded-xl border border-slate-800 bg-[#111827] p-5 lg:col-span-5">
        <h2 className="text-base font-semibold text-slate-100">Thông tin đơn hàng</h2>

        <div className="mt-4 space-y-4">
          <div>
            <label htmlFor="create-order-customer" className="block text-xs font-medium text-slate-400">
              Khách hàng
            </label>
            <select
              id="create-order-customer"
              value={selectedCustomerId}
              onChange={(e) => handleCustomerChange(e.target.value)}
              className="mt-1.5 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-100"
            >
              {customers.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.fullName} ({c.membershipTier} · {c.phone})
                </option>
              ))}
            </select>
          </div>

          <div>
            <label htmlFor="create-order-discount" className="block text-xs font-medium text-slate-400">
              Chính sách giảm giá (Factory + Strategy)
            </label>
            <select
              id="create-order-discount"
              value={discountType}
              onChange={(e) => setDiscountType(e.target.value as DiscountType)}
              className="mt-1.5 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-100"
            >
              <option value="NO_DISCOUNT">{discountLabel('NO_DISCOUNT')}</option>
              <option value="VIP">{discountLabel('VIP')}</option>
              <option value="SEASONAL">{discountLabel('SEASONAL')}</option>
            </select>
          </div>

          {/* Cart items */}
          <div>
            <div className="flex items-center justify-between">
              <span className="text-xs font-medium text-slate-400">Giỏ hàng ({cart.length} dòng)</span>
              {cart.length > 0 && (
                <button
                  type="button"
                  onClick={() => setCart([])}
                  className="text-xs text-rose-400 hover:underline"
                >
                  Xóa hết
                </button>
              )}
            </div>
            <div className="mt-2 max-h-56 divide-y divide-slate-800 overflow-y-auto border-y border-slate-800">
              {cart.length === 0 ? (
                <p className="py-6 text-center text-sm text-slate-500">
                  Chưa có sản phẩm nào trong giỏ.
                </p>
              ) : (
                cart.map((entry) => (
                  <div key={entry.product.id} className="flex items-center justify-between gap-2 py-2.5 text-sm">
                    <div className="min-w-0 flex-1">
                      <p className="truncate font-medium text-slate-100">{entry.product.name}</p>
                      <p className="font-mono-tabular text-xs text-slate-400">
                        {formatVnd(entry.product.price)} / sp
                      </p>
                    </div>
                    <div className="flex items-center gap-1.5">
                      <button
                        type="button"
                        aria-label={`Giảm số lượng ${entry.product.name}`}
                        onClick={() => updateCartQuantity(entry.product.id, -1)}
                        className="h-7 w-7 rounded border border-slate-700 bg-slate-900 text-slate-200 hover:bg-slate-800"
                      >
                        -
                      </button>
                      <span className="font-mono-tabular w-7 text-center text-slate-100">
                        {entry.quantity}
                      </span>
                      <button
                        type="button"
                        aria-label={`Tăng số lượng ${entry.product.name}`}
                        onClick={() => updateCartQuantity(entry.product.id, 1)}
                        className="h-7 w-7 rounded border border-slate-700 bg-slate-900 text-slate-200 hover:bg-slate-800"
                      >
                        +
                      </button>
                      <button
                        type="button"
                        aria-label={`Xóa ${entry.product.name} khỏi giỏ`}
                        onClick={() => removeFromCart(entry.product.id)}
                        className="ml-1 text-xs text-rose-400 hover:underline"
                      >
                        Xóa
                      </button>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>

          {/* Live Java Strategy Preview */}
          <div className="space-y-1.5 rounded-lg border border-slate-800 bg-slate-900/70 p-3.5 text-sm">
            <div className="flex justify-between text-slate-400">
              <span>Tổng gốc</span>
              <span className="font-mono-tabular text-slate-200">
                {formatVnd(pricing?.originalTotal ?? rawSubtotal)}
              </span>
            </div>
            <div className="flex justify-between text-slate-400">
              <span>Tiền giảm ({Math.round((pricing?.discountRate ?? 0) * 100)}%)</span>
              <span className="font-mono-tabular text-amber-400">
                -{formatVnd(pricing?.discountAmount ?? 0)}
              </span>
            </div>
            <div className="flex justify-between border-t border-slate-800 pt-2 text-base font-semibold">
              <span className="text-slate-100">Thành tiền</span>
              <span className="font-mono-tabular text-emerald-400">
                {formatVnd(pricing?.finalTotal ?? rawSubtotal)}
              </span>
            </div>
            {pricing?.strategyDescription && (
              <p className="pt-1 text-xs text-slate-400">{pricing.strategyDescription}</p>
            )}
          </div>

          {error && (
            <div className="rounded-lg border border-red-800/60 bg-red-950/30 p-3 text-xs text-red-300">
              {error}
            </div>
          )}

          <div className="flex flex-wrap gap-3 pt-1">
            <button
              type="button"
              disabled={submitting || cart.length === 0}
              onClick={() => submitOrder(false)}
              className="whitespace-nowrap shrink-0 flex-1 rounded-lg border border-slate-700 bg-slate-800 px-4 py-2.5 text-sm font-semibold text-slate-100 hover:bg-slate-700 disabled:opacity-50"
            >
              Tạo đơn hàng (PENDING)
            </button>
            <button
              type="button"
              disabled={submitting || cart.length === 0}
              onClick={() => submitOrder(true)}
              className="whitespace-nowrap shrink-0 flex-1 rounded-lg bg-blue-600 px-4 py-2.5 text-sm font-semibold text-white hover:bg-blue-500 disabled:opacity-50"
            >
              Tạo đơn & Thanh toán ngay
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
