import React, {useCallback, useEffect, useMemo, useState} from 'react';
import {ArrowRight, Search, ShoppingBag, X} from 'lucide-react';
import {javaApi} from '../../api/javaClient';
import type {AppSettings, Category, Customer, DiscountInfo, Product} from '../../types';
import {formatVnd} from '../../utils/format';
import {
  STORE_IMAGES,
  calculateCartCount,
  clampCartQuantity,
  isProductPurchasable,
  resolveProductImage,
} from '../../utils/storeHelpers';
import {CartCheckoutDrawer, type StoreCartItem} from './CartCheckoutDrawer';
import {CustomerOrdersView} from './CustomerOrdersView';
import {ProductDetailModal} from './ProductDetailModal';
import {ProductImage} from './ProductImage';

interface StorefrontViewProps {
  refreshKey: number;
  onDataChanged: (message?: string) => void;
  onOpenAdmin: () => void;
}

type StoreSubPage = 'CATALOG' | 'MY_ORDERS';

export const StorefrontView: React.FC<StorefrontViewProps> = ({
  refreshKey,
  onDataChanged,
  onOpenAdmin,
}) => {
  const [subPage, setSubPage] = useState<StoreSubPage>('CATALOG');
  const [categories, setCategories] = useState<Category[]>([]);
  const [brands, setBrands] = useState<string[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [discounts, setDiscounts] = useState<DiscountInfo[]>([]);
  const [settings, setSettings] = useState<AppSettings | null>(null);

  const [keyword, setKeyword] = useState('');
  const [selectedCategoryId, setSelectedCategoryId] = useState('ALL');
  const [selectedBrand, setSelectedBrand] = useState('ALL');
  const [sortBy, setSortBy] = useState('');
  const [loadingCatalog, setLoadingCatalog] = useState(true);
  const [catalogError, setCatalogError] = useState<string | null>(null);

  const [cart, setCart] = useState<StoreCartItem[]>([]);
  const [cartOpen, setCartOpen] = useState(false);
  const [detailProduct, setDetailProduct] = useState<Product | null>(null);
  const [selectedCustomerId, setSelectedCustomerId] = useState('');
  const [storeToast, setStoreToast] = useState<{type: 'success' | 'error'; text: string} | null>(null);

  useEffect(() => {
    if (!storeToast) return;
    const timer = window.setTimeout(() => setStoreToast(null), 4000);
    return () => window.clearTimeout(timer);
  }, [storeToast]);

  const loadMetadata = useCallback(async () => {
    try {
      const [cats, brs, custs, discs, appSettings] = await Promise.all([
        javaApi.getCategories(),
        javaApi.getBrands(),
        javaApi.getCustomers(),
        javaApi.getDiscounts(),
        javaApi.getSettings(),
      ]);
      setCategories(cats);
      setBrands(brs);
      setCustomers(custs);
      setDiscounts(discs);
      setSettings(appSettings);
      setSelectedCustomerId((prev) =>
        prev && custs.some((c) => c.id === prev) ? prev : (custs[0]?.id ?? ''),
      );
    } catch (err) {
      setCatalogError(err instanceof Error ? err.message : 'Không tải được dữ liệu cửa hàng.');
    }
  }, []);

  const loadProducts = useCallback(async () => {
    setLoadingCatalog(true);
    try {
      const list = await javaApi.getProducts({
        keyword: keyword.trim() || undefined,
        categoryId: selectedCategoryId === 'ALL' ? undefined : selectedCategoryId,
        brand: selectedBrand === 'ALL' ? undefined : selectedBrand,
        sort: sortBy || undefined,
      });
      setProducts(list);
      setCatalogError(null);
    } catch (err) {
      setCatalogError(err instanceof Error ? err.message : 'Không tải được danh sách sản phẩm.');
    } finally {
      setLoadingCatalog(false);
    }
  }, [keyword, selectedCategoryId, selectedBrand, sortBy]);

  useEffect(() => {
    loadMetadata();
  }, [loadMetadata, refreshKey]);

  useEffect(() => {
    loadProducts();
  }, [loadProducts, refreshKey]);

  // Keep cart synced with freshest inventory/status from Java backend
  useEffect(() => {
    if (products.length === 0) return;
    const byId = new Map(products.map((p) => [p.id, p]));
    setCart((prev) => {
      let changed = false;
      const next: StoreCartItem[] = [];
      for (const entry of prev) {
        const fresh = byId.get(entry.product.id);
        if (!fresh) {
          next.push(entry);
          continue;
        }
        if (!isProductPurchasable(fresh)) {
          changed = true;
          continue;
        }
        const clamped = clampCartQuantity(entry.quantity, fresh.stockQuantity);
        if (
          clamped !== entry.quantity ||
          fresh.price !== entry.product.price ||
          fresh.stockQuantity !== entry.product.stockQuantity
        ) {
          changed = true;
        }
        if (clamped > 0) {
          next.push({product: fresh, quantity: clamped});
        }
      }
      return changed ? next : prev;
    });
  }, [products]);

  const categoryMap = useMemo(() => {
    const map = new Map<string, Category>();
    categories.forEach((c) => map.set(c.id, c));
    return map;
  }, [categories]);

  const activeCategories = useMemo(
    () => categories.filter((c) => c.active),
    [categories],
  );

  // Respect active category setting from Admin when displaying storefront catalog
  const visibleProducts = useMemo(() => {
    if (categories.length === 0) return products;
    return products.filter((p) => {
      const cat = categoryMap.get(p.categoryId);
      return cat ? cat.active : true;
    });
  }, [products, categories.length, categoryMap]);

  const cartCount = useMemo(() => calculateCartCount(cart), [cart]);

  const handleAddToCart = (product: Product, quantity: number, openDrawer: boolean) => {
    if (!isProductPurchasable(product)) {
      setStoreToast({type: 'error', text: `${product.name} hiện không khả dụng để mua.`});
      return;
    }

    setCart((prev) => {
      const existing = prev.find((i) => i.product.id === product.id);
      const nextDesired = (existing?.quantity ?? 0) + quantity;
      const clamped = clampCartQuantity(nextDesired, product.stockQuantity);
      if (existing) {
        return prev.map((i) =>
          i.product.id === product.id ? {product, quantity: clamped} : i,
        );
      }
      return [...prev, {product, quantity: clamped}];
    });

    if (openDrawer) {
      setCartOpen(true);
    } else {
      setStoreToast({type: 'success', text: `Đã thêm ${quantity} × ${product.name} vào giỏ hàng.`});
    }
  };

  const handleUpdateCartQty = (productId: string, delta: number) => {
    setCart((prev) =>
      prev
        .map((entry) => {
          if (entry.product.id !== productId) return entry;
          const desired = entry.quantity + delta;
          if (desired <= 0) return null;
          const clamped = clampCartQuantity(desired, entry.product.stockQuantity);
          return {product: entry.product, quantity: clamped};
        })
        .filter((x): x is StoreCartItem => x !== null),
    );
  };

  const handleRemoveCartItem = (productId: string) => {
    setCart((prev) => prev.filter((item) => item.product.id !== productId));
  };

  const jumpToSection = (sectionId: string) => {
    setSubPage('CATALOG');
    window.setTimeout(() => {
      document.getElementById(sectionId)?.scrollIntoView({behavior: 'smooth'});
    }, 50);
  };

  return (
    <div className="min-h-screen bg-[#0B0F19] text-slate-100 flex flex-col">
      {/* Strict 3-Zone Single-Row Top Bar */}
      <header className="sticky top-0 z-40 border-b border-slate-800 bg-[#0F1524]/95 backdrop-blur">
        <div className="mx-auto flex h-16 max-w-7xl items-center justify-between gap-4 px-4 sm:px-6">
          <button
            type="button"
            onClick={() => {
              setSubPage('CATALOG');
              window.scrollTo({top: 0, behavior: 'smooth'});
            }}
            className="font-display whitespace-nowrap shrink-0 text-lg font-bold tracking-tight text-slate-100 hover:text-sky-400"
          >
            Gaming Gear Hub
          </button>

          <nav className="hidden md:flex items-center gap-6" aria-label="Điều hướng cửa hàng">
            <button
              type="button"
              onClick={() => jumpToSection('store-catalog')}
              className={`whitespace-nowrap shrink-0 text-sm font-medium transition-colors ${
                subPage === 'CATALOG' ? 'text-slate-100 hover:text-sky-400' : 'text-slate-400 hover:text-slate-100'
              }`}
            >
              Sản phẩm
            </button>
            <button
              type="button"
              onClick={() => jumpToSection('store-discounts')}
              className="whitespace-nowrap shrink-0 text-sm font-medium text-slate-300 transition-colors hover:text-sky-400"
            >
              Ưu đãi
            </button>
            <button
              type="button"
              onClick={() => setSubPage('MY_ORDERS')}
              className={`whitespace-nowrap shrink-0 text-sm font-medium transition-colors ${
                subPage === 'MY_ORDERS' ? 'text-sky-400' : 'text-slate-300 hover:text-sky-400'
              }`}
            >
              Đơn hàng của tôi
            </button>
            <button
              type="button"
              onClick={onOpenAdmin}
              className="whitespace-nowrap shrink-0 text-sm font-medium text-slate-300 transition-colors hover:text-sky-400"
            >
              Quản trị viên
            </button>
          </nav>

          <button
            type="button"
            onClick={() => setCartOpen(true)}
            className="inline-flex whitespace-nowrap shrink-0 items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-500"
          >
            <ShoppingBag className="h-4 w-4" />
            <span>Giỏ hàng ({cartCount})</span>
          </button>
        </div>

        {/* Mobile sub-navigation bar to prevent wrapping/overflow on 390px */}
        <div className="flex md:hidden items-center gap-5 overflow-x-auto border-t border-slate-800/80 px-4 py-2.5">
          <button
            type="button"
            onClick={() => jumpToSection('store-catalog')}
            className="whitespace-nowrap shrink-0 text-xs font-medium text-slate-200"
          >
            Sản phẩm
          </button>
          <button
            type="button"
            onClick={() => jumpToSection('store-discounts')}
            className="whitespace-nowrap shrink-0 text-xs font-medium text-slate-300"
          >
            Ưu đãi
          </button>
          <button
            type="button"
            onClick={() => setSubPage('MY_ORDERS')}
            className={`whitespace-nowrap shrink-0 text-xs font-medium ${
              subPage === 'MY_ORDERS' ? 'text-sky-400' : 'text-slate-300'
            }`}
          >
            Đơn hàng của tôi
          </button>
          <button
            type="button"
            onClick={onOpenAdmin}
            className="whitespace-nowrap shrink-0 text-xs font-medium text-slate-300"
          >
            Quản trị viên
          </button>
        </div>
      </header>

      {storeToast && (
        <div
          role="status"
          className={`mx-auto mt-4 flex w-full max-w-7xl items-center justify-between rounded-lg border px-4 py-2.5 text-sm ${
            storeToast.type === 'error'
              ? 'border-red-800/70 bg-red-950/40 text-red-200'
              : 'border-emerald-800/70 bg-emerald-950/40 text-emerald-200'
          }`}
        >
          <span>{storeToast.text}</span>
          <button
            type="button"
            aria-label="Đóng thông báo cửa hàng"
            onClick={() => setStoreToast(null)}
            className="text-slate-300 hover:text-white"
          >
            <X className="h-4 w-4" />
          </button>
        </div>
      )}

      {subPage === 'MY_ORDERS' ? (
        <main className="flex-1">
          <CustomerOrdersView
            customers={customers}
            selectedCustomerId={selectedCustomerId}
            onSelectCustomerId={setSelectedCustomerId}
            refreshKey={refreshKey}
            onDataChanged={(msg) => {
              setStoreToast({type: 'success', text: msg});
              onDataChanged(msg);
            }}
            onBackToCatalog={() => setSubPage('CATALOG')}
          />
        </main>
      ) : (
        <main className="flex-1">
          {/* Section 1: Editorial Split Hero */}
          <section className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:py-12">
            <div className="grid grid-cols-1 items-stretch gap-8 lg:grid-cols-12">
              <div className="flex flex-col justify-between rounded-xl border border-slate-800 bg-[#111827] p-7 lg:col-span-6 lg:p-9">
                <div>
                  <p className="text-xs font-semibold uppercase tracking-widest text-sky-400">
                    BỘ SƯU TẬP GAMING GEAR CHÍNH HÃNG
                  </p>
                  <h1 className="font-display mt-3 text-3xl font-bold leading-tight tracking-tight text-slate-100 sm:text-4xl">
                    Trang bị chuẩn thi đấu cho góc máy của bạn
                  </h1>
                  <p className="mt-4 text-sm leading-relaxed text-slate-300 sm:text-base">
                    Chọn bàn phím cơ, chuột siêu nhẹ, màn hình tần số quét cao và tai nghe phòng thu.
                    Mọi đơn hàng, tồn kho và hóa đơn PDF đều đồng bộ trực tiếp theo thời gian thực với hệ thống quản trị Java 17.
                  </p>
                </div>

                <div className="mt-8 flex flex-wrap items-center gap-3">
                  <button
                    type="button"
                    onClick={() => jumpToSection('store-catalog')}
                    className="inline-flex whitespace-nowrap shrink-0 items-center gap-2 rounded-lg bg-blue-600 px-5 py-2.5 text-sm font-semibold text-white hover:bg-blue-500"
                  >
                    Khám phá sản phẩm
                    <ArrowRight className="h-4 w-4" />
                  </button>
                  <button
                    type="button"
                    onClick={() => setSubPage('MY_ORDERS')}
                    className="whitespace-nowrap shrink-0 rounded-lg border border-slate-700 bg-slate-800 px-5 py-2.5 text-sm font-semibold text-slate-100 hover:bg-slate-700"
                  >
                    Theo dõi đơn hàng
                  </button>
                </div>
              </div>

              <div className="overflow-hidden rounded-xl border border-slate-800 bg-[#18181B] lg:col-span-6">
                <img
                  src={STORE_IMAGES.hero}
                  alt="Không gian bàn máy Gaming Gear chuyên nghiệp"
                  referrerPolicy="no-referrer"
                  className="h-full min-h-[260px] w-full object-cover"
                />
              </div>
            </div>
          </section>

          {/* Section 2: Interactive Product Catalog */}
          <section id="store-catalog" className="mx-auto max-w-7xl px-4 pb-12 sm:px-6">
            <div className="flex flex-wrap items-end justify-between gap-4 border-b border-slate-800 pb-5">
              <div>
                <h2 className="font-display text-2xl font-semibold text-slate-100">
                  Danh mục thiết bị Gaming Gear
                </h2>
                <p className="mt-1 text-sm text-slate-400">
                  Hiển thị {visibleProducts.length} sản phẩm đồng bộ từ kho hàng trung tâm
                </p>
              </div>

              <div className="flex flex-wrap items-center gap-3">
                <div className="relative min-w-[220px]">
                  <Search
                    className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-500"
                    aria-hidden="true"
                  />
                  <input
                    type="text"
                    aria-label="Tìm kiếm sản phẩm Gaming Gear"
                    value={keyword}
                    onChange={(e) => setKeyword(e.target.value)}
                    placeholder="Tìm bàn phím, chuột, tai nghe..."
                    className="w-full rounded-lg border border-slate-700 bg-[#111827] py-2 pl-9 pr-3 text-sm text-slate-100 placeholder-slate-500 focus:border-sky-400 focus:outline-none"
                  />
                </div>

                <select
                  aria-label="Lọc theo thương hiệu"
                  value={selectedBrand}
                  onChange={(e) => setSelectedBrand(e.target.value)}
                  className="rounded-lg border border-slate-700 bg-[#111827] px-3 py-2 text-sm text-slate-100"
                >
                  <option value="ALL">Tất cả thương hiệu</option>
                  {brands.map((brand) => (
                    <option key={brand} value={brand}>
                      {brand}
                    </option>
                  ))}
                </select>

                <select
                  aria-label="Sắp xếp sản phẩm"
                  value={sortBy}
                  onChange={(e) => setSortBy(e.target.value)}
                  className="rounded-lg border border-slate-700 bg-[#111827] px-3 py-2 text-sm text-slate-100"
                >
                  <option value="">Sắp xếp mặc định</option>
                  <option value="price_asc">Giá tăng dần</option>
                  <option value="price_desc">Giá giảm dần</option>
                  <option value="name_asc">Tên A - Z</option>
                </select>
              </div>
            </div>

            {/* Category Segmented Bar */}
            <div className="mt-5 flex flex-wrap items-center gap-2">
              <button
                type="button"
                onClick={() => setSelectedCategoryId('ALL')}
                className={`whitespace-nowrap shrink-0 rounded-lg border px-3.5 py-1.5 text-xs font-medium transition-colors ${
                  selectedCategoryId === 'ALL'
                    ? 'border-blue-500 bg-blue-600 text-white'
                    : 'border-slate-800 bg-[#111827] text-slate-300 hover:border-slate-700 hover:text-white'
                }`}
              >
                Tất cả danh mục
              </button>
              {activeCategories.map((cat) => (
                <button
                  key={cat.id}
                  type="button"
                  onClick={() => setSelectedCategoryId(cat.id)}
                  className={`whitespace-nowrap shrink-0 rounded-lg border px-3.5 py-1.5 text-xs font-medium transition-colors ${
                    selectedCategoryId === cat.id
                      ? 'border-blue-500 bg-blue-600 text-white'
                      : 'border-slate-800 bg-[#111827] text-slate-300 hover:border-slate-700 hover:text-white'
                  }`}
                >
                  {cat.name}
                </button>
              ))}
            </div>

            {catalogError && (
              <div
                role="alert"
                className="mt-6 rounded-xl border border-red-800/70 bg-red-950/30 p-4 text-sm text-red-300"
              >
                {catalogError}
              </div>
            )}

            {loadingCatalog ? (
              <div className="mt-6 rounded-xl border border-slate-800 bg-[#111827] p-12 text-center text-sm text-slate-400">
                Đang tải danh mục thiết bị Gaming Gear...
              </div>
            ) : visibleProducts.length === 0 ? (
              <div className="mt-6 rounded-xl border border-slate-800 bg-[#111827] p-12 text-center">
                <p className="text-sm text-slate-300">
                  Không tìm thấy sản phẩm phù hợp với bộ lọc hiện tại.
                </p>
                <button
                  type="button"
                  onClick={() => {
                    setKeyword('');
                    setSelectedCategoryId('ALL');
                    setSelectedBrand('ALL');
                    setSortBy('');
                  }}
                  className="mt-4 rounded-lg border border-slate-700 bg-slate-800 px-4 py-2 text-xs font-semibold text-slate-100 hover:bg-slate-700"
                >
                  Đặt lại bộ lọc
                </button>
              </div>
            ) : (
              <div className="mt-6 grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
                {visibleProducts.map((product) => {
                  const categoryName = categoryMap.get(product.categoryId)?.name ?? product.categoryId;
                  const purchasable = isProductPurchasable(product);
                  const imageUrl = resolveProductImage(product, categoryName);

                  return (
                    <article
                      key={product.id}
                      className="flex flex-col overflow-hidden rounded-xl border border-slate-800 bg-[#111827] transition-colors hover:border-slate-700"
                    >
                      <button
                        type="button"
                        onClick={() => setDetailProduct(product)}
                        className="aspect-[4/3] w-full overflow-hidden bg-[#18181B] text-left"
                        aria-label={`Xem chi tiết ${product.name}`}
                      >
                        <ProductImage
                          src={imageUrl}
                          alt={product.name}
                          categoryName={categoryName}
                          className="h-full w-full object-cover transition-transform duration-300 hover:scale-[1.03]"
                        />
                      </button>

                      <div className="flex flex-1 flex-col justify-between p-5">
                        <div>
                          <p className="text-xs font-medium uppercase tracking-wider text-slate-400">
                            {product.brand} · {categoryName}
                          </p>
                          <h3 className="font-display mt-1.5 line-clamp-1 text-base font-semibold text-slate-100">
                            {product.name}
                          </h3>
                          <div className="mt-3 flex items-baseline justify-between gap-2">
                            <span className="font-mono-tabular text-lg font-semibold text-emerald-400">
                              {formatVnd(product.price)}
                            </span>
                            <span
                              className={`text-xs font-medium ${
                                purchasable
                                  ? 'text-slate-400'
                                  : product.stockQuantity <= 0
                                    ? 'text-amber-400'
                                    : 'text-rose-400'
                              }`}
                            >
                              {purchasable
                                ? `Còn ${product.stockQuantity} sp`
                                : product.stockQuantity <= 0
                                  ? 'Hết hàng'
                                  : 'Ngừng bán'}
                            </span>
                          </div>
                        </div>

                        <div className="mt-5 flex items-center gap-2.5 border-t border-slate-800 pt-4">
                          <button
                            type="button"
                            onClick={() => setDetailProduct(product)}
                            className="whitespace-nowrap shrink-0 flex-1 rounded-lg border border-slate-700 bg-slate-800 px-3 py-2 text-xs font-semibold text-slate-100 hover:bg-slate-700"
                          >
                            Chi tiết
                          </button>
                          <button
                            type="button"
                            disabled={!purchasable}
                            onClick={() => handleAddToCart(product, 1, false)}
                            className="whitespace-nowrap shrink-0 flex-1 rounded-lg bg-blue-600 px-3 py-2 text-xs font-semibold text-white hover:bg-blue-500 disabled:opacity-45"
                          >
                            Thêm vào giỏ
                          </button>
                        </div>
                      </div>
                    </article>
                  );
                })}
              </div>
            )}
          </section>

          {/* Section 3: Discount Strategy Showcase */}
          <section id="store-discounts" className="mx-auto max-w-7xl px-4 pb-12 sm:px-6">
            <div className="rounded-xl border border-slate-800 bg-[#111827] p-6 sm:p-8">
              <div className="flex flex-wrap items-center justify-between gap-4 border-b border-slate-800 pb-4">
                <div>
                  <h2 className="font-display text-xl font-semibold text-slate-100">
                    Chính sách ưu đãi áp dụng khi thanh toán
                  </h2>
                  <p className="mt-1 text-sm text-slate-400">
                    Tính toán tự động qua Factory & Strategy Pattern trên máy chủ Java 17
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() => setCartOpen(true)}
                  className="whitespace-nowrap shrink-0 rounded-lg border border-slate-700 bg-slate-800 px-4 py-2 text-xs font-semibold text-slate-100 hover:bg-slate-700"
                >
                  Mở giỏ hàng áp dụng ngay
                </button>
              </div>

              <div className="mt-6 grid grid-cols-1 gap-4 md:grid-cols-3">
                {discounts.map((d) => (
                  <div
                    key={d.type}
                    className="rounded-xl border border-slate-800 bg-[#0B0F19] p-5"
                  >
                    <p className="font-mono-tabular text-2xl font-bold text-sky-400">
                      -{Math.round(d.rate * 100)}%
                    </p>
                    <h3 className="font-display mt-2 text-base font-semibold text-slate-100">
                      {d.displayName}
                    </h3>
                    <p className="mt-2 text-xs leading-relaxed text-slate-400">
                      {d.description}
                    </p>
                  </div>
                ))}
              </div>
            </div>
          </section>
        </main>
      )}

      {/* Section 4: Store Footer */}
      <footer className="border-t border-slate-800 bg-[#0F1524]">
        <div className="mx-auto flex max-w-7xl flex-wrap items-center justify-between gap-4 px-4 py-6 text-xs text-slate-400 sm:px-6">
          <div>
            <p className="font-semibold text-slate-200">
              {settings?.storeName || 'Gaming Gear Hub'}
            </p>
            <p className="mt-1">
              {settings?.storeAddress || 'TP. Hồ Chí Minh'} · Hotline: {settings?.storePhone || '1900-6868'}
            </p>
          </div>
          <button
            type="button"
            onClick={onOpenAdmin}
            className="whitespace-nowrap shrink-0 rounded-lg border border-slate-700 bg-slate-800 px-3.5 py-2 text-xs font-medium text-slate-200 hover:bg-slate-700"
          >
            Truy cập Cổng Quản trị (Admin)
          </button>
        </div>
      </footer>

      <ProductDetailModal
        product={detailProduct}
        categoryName={detailProduct ? (categoryMap.get(detailProduct.categoryId)?.name ?? detailProduct.categoryId) : ''}
        onClose={() => setDetailProduct(null)}
        onAddToCart={handleAddToCart}
      />

      <CartCheckoutDrawer
        open={cartOpen}
        cart={cart}
        customers={customers}
        selectedCustomerId={selectedCustomerId}
        onSelectCustomerId={setSelectedCustomerId}
        onUpdateQty={handleUpdateCartQty}
        onRemoveItem={handleRemoveCartItem}
        onClearCart={() => setCart([])}
        onClose={() => setCartOpen(false)}
        onCheckoutCompleted={(outcome) => {
          setStoreToast({
            type: 'success',
            text: `Đã tạo đơn hàng ${outcome.order.id} (${outcome.order.status}) và đồng bộ hệ thống quản trị.`,
          });
          onDataChanged();
        }}
        onOpenMyOrders={(customerId) => {
          setSelectedCustomerId(customerId);
          setSubPage('MY_ORDERS');
        }}
        onSwitchToAdmin={onOpenAdmin}
      />
    </div>
  );
};
