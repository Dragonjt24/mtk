import React, {useEffect, useState} from 'react';
import {Minus, Plus, ShoppingBag, X} from 'lucide-react';
import type {Product} from '../../types';
import {formatVnd} from '../../utils/format';
import {clampCartQuantity, isProductPurchasable, resolveProductImage} from '../../utils/storeHelpers';
import {ProductImage} from './ProductImage';

interface ProductDetailModalProps {
  product: Product | null;
  categoryName: string;
  onClose: () => void;
  onAddToCart: (product: Product, quantity: number, openCart: boolean) => void;
}

export const ProductDetailModal: React.FC<ProductDetailModalProps> = ({
  product,
  categoryName,
  onClose,
  onAddToCart,
}) => {
  const [quantity, setQuantity] = useState(1);

  useEffect(() => {
    setQuantity(1);
  }, [product?.id]);

  if (!product) return null;

  const purchasable = isProductPurchasable(product);
  const imageUrl = resolveProductImage(product, categoryName);

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 p-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby="pdp-modal-title"
    >
      <div className="w-full max-w-3xl overflow-hidden rounded-xl border border-slate-800 bg-[#111827] shadow-2xl">
        <div className="flex items-center justify-between border-b border-slate-800 px-6 py-4">
          <p className="text-xs font-medium uppercase tracking-wider text-slate-400">
            {product.brand} · {categoryName || product.categoryId} · {product.id}
          </p>
          <button
            type="button"
            onClick={onClose}
            aria-label="Đóng chi tiết sản phẩm"
            className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-800 hover:text-white"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="grid grid-cols-1 gap-6 p-6 md:grid-cols-2">
          {/* Left: Studio Product Media */}
          <div className="aspect-[4/3] overflow-hidden rounded-lg border border-slate-800 bg-[#18181B]">
            <ProductImage
              src={imageUrl}
              alt={product.name}
              categoryName={categoryName}
              className="h-full w-full object-cover"
            />
          </div>

          {/* Right: Contiguous Purchase Module */}
          <div className="flex flex-col justify-between space-y-5">
            <div>
              <h2 id="pdp-modal-title" className="font-display text-2xl font-semibold text-slate-100">
                {product.name}
              </h2>
              <p className="font-mono-tabular mt-3 text-2xl font-semibold text-emerald-400">
                {formatVnd(product.price)}
              </p>
              <p className="mt-2 text-xs font-medium text-slate-300">
                {purchasable
                  ? `Tình trạng: Còn ${product.stockQuantity} sản phẩm trong kho`
                  : product.stockQuantity <= 0
                    ? 'Tình trạng: Hết hàng'
                    : 'Tình trạng: Ngừng kinh doanh'}
              </p>
              <p className="mt-4 text-sm leading-relaxed text-slate-300">
                {product.description || 'Thiết bị Gaming Gear chính hãng bảo hành tiêu chuẩn.'}
              </p>
            </div>

            <div className="space-y-4 border-t border-slate-800 pt-4">
              {purchasable && (
                <div className="flex items-center justify-between">
                  <span className="text-sm font-medium text-slate-300">Số lượng mua</span>
                  <div className="flex items-center gap-2">
                    <button
                      type="button"
                      aria-label="Giảm số lượng mua"
                      onClick={() => setQuantity((q) => clampCartQuantity(q - 1, product.stockQuantity))}
                      className="flex h-9 w-9 items-center justify-center rounded-lg border border-slate-700 bg-slate-900 text-slate-200 hover:bg-slate-800"
                    >
                      <Minus className="h-4 w-4" />
                    </button>
                    <span className="font-mono-tabular w-10 text-center text-sm font-semibold text-slate-100">
                      {quantity}
                    </span>
                    <button
                      type="button"
                      aria-label="Tăng số lượng mua"
                      onClick={() => setQuantity((q) => clampCartQuantity(q + 1, product.stockQuantity))}
                      className="flex h-9 w-9 items-center justify-center rounded-lg border border-slate-700 bg-slate-900 text-slate-200 hover:bg-slate-800"
                    >
                      <Plus className="h-4 w-4" />
                    </button>
                  </div>
                </div>
              )}

              <div className="flex flex-wrap gap-3">
                <button
                  type="button"
                  disabled={!purchasable}
                  onClick={() => {
                    onAddToCart(product, quantity, false);
                    onClose();
                  }}
                  className="whitespace-nowrap shrink-0 flex-1 rounded-lg border border-slate-700 bg-slate-800 px-4 py-2.5 text-sm font-semibold text-slate-100 hover:bg-slate-700 disabled:opacity-45"
                >
                  Thêm vào giỏ
                </button>
                <button
                  type="button"
                  disabled={!purchasable}
                  onClick={() => {
                    onAddToCart(product, quantity, true);
                    onClose();
                  }}
                  className="inline-flex whitespace-nowrap shrink-0 flex-1 items-center justify-center gap-2 rounded-lg bg-blue-600 px-4 py-2.5 text-sm font-semibold text-white hover:bg-blue-500 disabled:opacity-45"
                >
                  <ShoppingBag className="h-4 w-4" />
                  Mua ngay
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
