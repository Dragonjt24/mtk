import React, {useCallback, useEffect, useState} from 'react';
import {Edit3, Plus, Search, Trash2} from 'lucide-react';
import {javaApi} from '../api/javaClient';
import type {Category, Product, ProductStatus} from '../types';
import {formatVnd, productStatusLabel} from '../utils/format';
import {ConfirmDialog} from './ConfirmDialog';

interface ProductsViewProps {
  refreshKey: number;
  onNotify: (type: 'success' | 'error', text: string) => void;
}

export const ProductsView: React.FC<ProductsViewProps> = ({refreshKey, onNotify}) => {
  const [products, setProducts] = useState<Product[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [brands, setBrands] = useState<string[]>([]);
  const [selectedIds, setSelectedIds] = useState<string[]>([]);

  const [keyword, setKeyword] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [brand, setBrand] = useState('');
  const [minPrice, setMinPrice] = useState('');
  const [maxPrice, setMaxPrice] = useState('');
  const [sort, setSort] = useState('NAME_ASC');

  const [editing, setEditing] = useState<Product | null>(null);
  const [formOpen, setFormOpen] = useState(false);
  const [formState, setFormState] = useState({
    name: '',
    categoryId: '',
    brand: '',
    price: '',
    stockQuantity: '10',
    description: '',
    imagePath: 'assets/products/gear_item.png',
    status: 'ACTIVE' as ProductStatus,
  });

  const [confirmDelete, setConfirmDelete] = useState<{mode: 'SINGLE' | 'BATCH'; product?: Product} | null>(null);

  const loadMeta = useCallback(async () => {
    const [cats, brs] = await Promise.all([javaApi.getCategories(), javaApi.getBrands()]);
    setCategories(cats);
    setBrands(brs);
  }, []);

  const loadProducts = useCallback(async () => {
    try {
      const list = await javaApi.getProducts({keyword, categoryId, brand, minPrice, maxPrice, sort});
      setProducts(list);
      setSelectedIds([]);
    } catch (err) {
      onNotify('error', err instanceof Error ? err.message : 'Lỗi tải sản phẩm.');
    }
  }, [keyword, categoryId, brand, minPrice, maxPrice, sort, onNotify]);

  useEffect(() => {
    loadMeta().then(loadProducts).catch((err) => onNotify('error', String(err)));
  }, [refreshKey, loadMeta, loadProducts, onNotify]);

  const categoryNameMap = new Map(categories.map((c) => [c.id, c.name]));

  const toggleSelect = (id: string) => {
    setSelectedIds((prev) => (prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]));
  };

  const toggleAll = () => {
    if (selectedIds.length === products.length) {
      setSelectedIds([]);
    } else {
      setSelectedIds(products.map((p) => p.id));
    }
  };

  const openCreateModal = () => {
    setEditing(null);
    setFormState({
      name: '',
      categoryId: categories[0]?.id ?? '',
      brand: '',
      price: '',
      stockQuantity: '10',
      description: '',
      imagePath: 'assets/products/gear_item.png',
      status: 'ACTIVE',
    });
    setFormOpen(true);
  };

  const openEditModal = (p: Product) => {
    setEditing(p);
    setFormState({
      name: p.name,
      categoryId: p.categoryId,
      brand: p.brand,
      price: String(p.price),
      stockQuantity: String(p.stockQuantity),
      description: p.description,
      imagePath: p.imagePath,
      status: p.status,
    });
    setFormOpen(true);
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const payload = {
        ...formState,
        price: Number(formState.price),
        stockQuantity: Number(formState.stockQuantity),
      };
      if (editing) {
        await javaApi.updateProduct(editing.id, payload);
        onNotify('success', `Đã cập nhật sản phẩm ${editing.id}.`);
      } else {
        await javaApi.createProduct(payload);
        onNotify('success', 'Đã thêm sản phẩm mới.');
      }
      setFormOpen(false);
      await loadMeta();
      await loadProducts();
    } catch (err) {
      onNotify('error', err instanceof Error ? err.message : 'Không thể lưu sản phẩm.');
    }
  };

  const executeDelete = async () => {
    if (!confirmDelete) return;
    try {
      if (confirmDelete.mode === 'SINGLE' && confirmDelete.product) {
        await javaApi.deleteProduct(confirmDelete.product.id);
        onNotify('success', `Đã xóa sản phẩm ${confirmDelete.product.name}.`);
      } else if (confirmDelete.mode === 'BATCH') {
        const res = await javaApi.batchDeleteProducts(selectedIds);
        onNotify('success', `Đã xóa ${res.deletedCount} sản phẩm.`);
      }
      setConfirmDelete(null);
      await loadMeta();
      await loadProducts();
    } catch (err) {
      onNotify('error', err instanceof Error ? err.message : 'Xóa sản phẩm thất bại.');
      setConfirmDelete(null);
    }
  };

  return (
    <div className="space-y-5">
      {/* Filter & Actions Bar */}
      <div className="rounded-xl border border-slate-800 bg-[#111827] p-4 space-y-4">
        <div className="grid grid-cols-1 gap-3 md:grid-cols-6">
          <div className="relative md:col-span-2">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-500" />
            <input
              type="text"
              aria-label="Tìm sản phẩm theo tên, mã hoặc hãng"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              placeholder="Tìm theo tên, mã SP, thương hiệu..."
              className="w-full rounded-lg border border-slate-700 bg-[#0b0f19] py-2 pl-9 pr-3 text-sm text-slate-100 placeholder-slate-500 focus:border-sky-400 focus:outline-none"
            />
          </div>
          <select
            aria-label="Lọc theo danh mục"
            value={categoryId}
            onChange={(e) => setCategoryId(e.target.value)}
            className="rounded-lg border border-slate-700 bg-[#0b0f19] px-3 py-2 text-sm text-slate-100 focus:border-sky-400 focus:outline-none"
          >
            <option value="">Tất cả danh mục</option>
            {categories.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>
          <select
            aria-label="Lọc theo thương hiệu"
            value={brand}
            onChange={(e) => setBrand(e.target.value)}
            className="rounded-lg border border-slate-700 bg-[#0b0f19] px-3 py-2 text-sm text-slate-100 focus:border-sky-400 focus:outline-none"
          >
            <option value="">Tất cả hãng</option>
            {brands.map((b) => (
              <option key={b} value={b}>
                {b}
              </option>
            ))}
          </select>
          <div className="flex items-center gap-2">
            <input
              type="number"
              aria-label="Giá tối thiểu"
              value={minPrice}
              onChange={(e) => setMinPrice(e.target.value)}
              placeholder="Giá từ"
              className="w-1/2 rounded-lg border border-slate-700 bg-[#0b0f19] px-2.5 py-2 text-sm text-slate-100 placeholder-slate-500 focus:border-sky-400 focus:outline-none"
            />
            <input
              type="number"
              aria-label="Giá tối đa"
              value={maxPrice}
              onChange={(e) => setMaxPrice(e.target.value)}
              placeholder="Đến"
              className="w-1/2 rounded-lg border border-slate-700 bg-[#0b0f19] px-2.5 py-2 text-sm text-slate-100 placeholder-slate-500 focus:border-sky-400 focus:outline-none"
            />
          </div>
          <select
            aria-label="Sắp xếp sản phẩm"
            value={sort}
            onChange={(e) => setSort(e.target.value)}
            className="rounded-lg border border-slate-700 bg-[#0b0f19] px-3 py-2 text-sm text-slate-100 focus:border-sky-400 focus:outline-none"
          >
            <option value="NAME_ASC">Tên A -&gt; Z</option>
            <option value="NAME_DESC">Tên Z -&gt; A</option>
            <option value="PRICE_ASC">Giá tăng dần</option>
            <option value="PRICE_DESC">Giá giảm dần</option>
            <option value="STOCK_ASC">Tồn kho tăng dần</option>
          </select>
        </div>

        <div className="flex flex-wrap items-center justify-between gap-3 border-t border-slate-800 pt-3">
          <p className="text-xs text-slate-400">
            Hiển thị <span className="font-mono-tabular text-slate-200">{products.length}</span> sản phẩm
            {selectedIds.length > 0 && ` · Đã chọn ${selectedIds.length}`}
          </p>
          <div className="flex items-center gap-2.5">
            {selectedIds.length > 0 && (
              <button
                type="button"
                onClick={() => setConfirmDelete({mode: 'BATCH'})}
                className="inline-flex items-center gap-1.5 whitespace-nowrap rounded-lg bg-red-600 px-3.5 py-2 text-sm font-medium text-white hover:bg-red-500"
              >
                <Trash2 className="h-4 w-4" />
                Xóa {selectedIds.length} đã chọn
              </button>
            )}
            <button
              type="button"
              onClick={() => {
                setKeyword('');
                setCategoryId('');
                setBrand('');
                setMinPrice('');
                setMaxPrice('');
                setSort('NAME_ASC');
              }}
              className="whitespace-nowrap rounded-lg border border-slate-700 bg-slate-800 px-3.5 py-2 text-sm font-medium text-slate-200 hover:bg-slate-700"
            >
              Đặt lại bộ lọc
            </button>
            <button
              type="button"
              onClick={openCreateModal}
              className="inline-flex items-center gap-1.5 whitespace-nowrap rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-500"
            >
              <Plus className="h-4 w-4" />
              Thêm sản phẩm
            </button>
          </div>
        </div>
      </div>

      {/* Products Table */}
      <div className="overflow-hidden rounded-xl border border-slate-800 bg-[#111827]">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="border-b border-slate-800 bg-slate-900/60 text-xs text-slate-400">
              <tr>
                <th className="w-10 px-4 py-3">
                  <input
                    type="checkbox"
                    aria-label="Chọn tất cả sản phẩm"
                    checked={products.length > 0 && selectedIds.length === products.length}
                    onChange={toggleAll}
                  />
                </th>
                <th className="px-3 py-3 font-medium">Mã SP</th>
                <th className="px-3 py-3 font-medium">Tên sản phẩm</th>
                <th className="px-3 py-3 font-medium">Danh mục</th>
                <th className="px-3 py-3 font-medium">Hãng</th>
                <th className="px-3 py-3 text-right font-medium">Giá bán</th>
                <th className="px-3 py-3 text-right font-medium">Tồn kho</th>
                <th className="px-3 py-3 font-medium">Trạng thái</th>
                <th className="px-4 py-3 text-right font-medium">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/80">
              {products.length === 0 ? (
                <tr>
                  <td colSpan={9} className="py-10 text-center text-slate-400">
                    Không tìm thấy sản phẩm phù hợp với bộ lọc hiện tại.
                  </td>
                </tr>
              ) : (
                products.map((p) => (
                  <tr key={p.id} className="hover:bg-slate-800/40">
                    <td className="px-4 py-3">
                      <input
                        type="checkbox"
                        aria-label={`Chọn ${p.name}`}
                        checked={selectedIds.includes(p.id)}
                        onChange={() => toggleSelect(p.id)}
                      />
                    </td>
                    <td className="font-mono-tabular px-3 py-3 text-xs text-sky-400">{p.id}</td>
                    <td className="px-3 py-3">
                      <div className="font-medium text-slate-100">{p.name}</div>
                      <div className="max-w-xs truncate text-xs text-slate-400">{p.description}</div>
                    </td>
                    <td className="px-3 py-3 text-slate-300">{categoryNameMap.get(p.categoryId) ?? p.categoryId}</td>
                    <td className="px-3 py-3 text-slate-300">{p.brand}</td>
                    <td className="font-mono-tabular px-3 py-3 text-right text-slate-100">{formatVnd(p.price)}</td>
                    <td
                      className={`font-mono-tabular px-3 py-3 text-right font-medium ${
                        p.stockQuantity <= 5 ? 'text-amber-400' : 'text-slate-200'
                      }`}
                    >
                      {p.stockQuantity}
                    </td>
                    <td
                      className={`px-3 py-3 text-xs font-medium ${
                        p.status === 'ACTIVE'
                          ? 'text-emerald-400'
                          : p.status === 'OUT_OF_STOCK'
                            ? 'text-rose-400'
                            : 'text-slate-400'
                      }`}
                    >
                      {productStatusLabel(p.status)}
                    </td>
                    <td className="px-4 py-3 text-right">
                      <div className="inline-flex items-center gap-2">
                        <button
                          type="button"
                          onClick={() => openEditModal(p)}
                          aria-label={`Chỉnh sửa ${p.name}`}
                          className="rounded border border-slate-700 p-1.5 text-slate-300 hover:bg-slate-800 hover:text-sky-400"
                        >
                          <Edit3 className="h-3.5 w-3.5" />
                        </button>
                        <button
                          type="button"
                          onClick={() => setConfirmDelete({mode: 'SINGLE', product: p})}
                          aria-label={`Xóa ${p.name}`}
                          className="rounded border border-slate-700 p-1.5 text-slate-300 hover:bg-slate-800 hover:text-rose-400"
                        >
                          <Trash2 className="h-3.5 w-3.5" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Add/Edit Product Modal */}
      {formOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4" role="dialog" aria-modal="true">
          <div className="w-full max-w-xl rounded-xl border border-slate-800 bg-[#111827] p-6">
            <h3 className="text-lg font-semibold text-slate-100">
              {editing ? `Chỉnh sửa sản phẩm ${editing.id}` : 'Thêm sản phẩm Gaming Gear mới'}
            </h3>
            <form onSubmit={handleSave} className="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div className="sm:col-span-2">
                <label htmlFor="prod-name" className="block text-xs font-medium text-slate-300">
                  Tên sản phẩm
                </label>
                <input
                  id="prod-name"
                  required
                  value={formState.name}
                  onChange={(e) => setFormState({...formState, name: e.target.value})}
                  className="mt-1 w-full rounded-lg border border-slate-700 bg-[#0b0f19] px-3 py-2 text-sm text-slate-100"
                />
              </div>
              <div>
                <label htmlFor="prod-cat" className="block text-xs font-medium text-slate-300">
                  Danh mục
                </label>
                <select
                  id="prod-cat"
                  value={formState.categoryId}
                  onChange={(e) => setFormState({...formState, categoryId: e.target.value})}
                  className="mt-1 w-full rounded-lg border border-slate-700 bg-[#0b0f19] px-3 py-2 text-sm text-slate-100"
                >
                  {categories.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.name}
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label htmlFor="prod-brand" className="block text-xs font-medium text-slate-300">
                  Thương hiệu
                </label>
                <input
                  id="prod-brand"
                  required
                  value={formState.brand}
                  onChange={(e) => setFormState({...formState, brand: e.target.value})}
                  className="mt-1 w-full rounded-lg border border-slate-700 bg-[#0b0f19] px-3 py-2 text-sm text-slate-100"
                />
              </div>
              <div>
                <label htmlFor="prod-price" className="block text-xs font-medium text-slate-300">
                  Giá bán (VNĐ)
                </label>
                <input
                  id="prod-price"
                  type="number"
                  required
                  value={formState.price}
                  onChange={(e) => setFormState({...formState, price: e.target.value})}
                  className="mt-1 w-full rounded-lg border border-slate-700 bg-[#0b0f19] px-3 py-2 text-sm text-slate-100"
                />
              </div>
              <div>
                <label htmlFor="prod-stock" className="block text-xs font-medium text-slate-300">
                  Số lượng tồn kho
                </label>
                <input
                  id="prod-stock"
                  type="number"
                  required
                  value={formState.stockQuantity}
                  onChange={(e) => setFormState({...formState, stockQuantity: e.target.value})}
                  className="mt-1 w-full rounded-lg border border-slate-700 bg-[#0b0f19] px-3 py-2 text-sm text-slate-100"
                />
              </div>
              <div>
                <label htmlFor="prod-status" className="block text-xs font-medium text-slate-300">
                  Trạng thái kinh doanh
                </label>
                <select
                  id="prod-status"
                  value={formState.status}
                  onChange={(e) => setFormState({...formState, status: e.target.value as ProductStatus})}
                  className="mt-1 w-full rounded-lg border border-slate-700 bg-[#0b0f19] px-3 py-2 text-sm text-slate-100"
                >
                  <option value="ACTIVE">Đang kinh doanh</option>
                  <option value="INACTIVE">Ngừng kinh doanh</option>
                  <option value="OUT_OF_STOCK">Hết hàng</option>
                </select>
              </div>
              <div>
                <label htmlFor="prod-img" className="block text-xs font-medium text-slate-300">
                  Đường dẫn ảnh
                </label>
                <input
                  id="prod-img"
                  value={formState.imagePath}
                  onChange={(e) => setFormState({...formState, imagePath: e.target.value})}
                  className="mt-1 w-full rounded-lg border border-slate-700 bg-[#0b0f19] px-3 py-2 text-sm text-slate-100"
                />
              </div>
              <div className="sm:col-span-2">
                <label htmlFor="prod-desc" className="block text-xs font-medium text-slate-300">
                  Mô tả sản phẩm
                </label>
                <textarea
                  id="prod-desc"
                  rows={2}
                  value={formState.description}
                  onChange={(e) => setFormState({...formState, description: e.target.value})}
                  className="mt-1 w-full rounded-lg border border-slate-700 bg-[#0b0f19] px-3 py-2 text-sm text-slate-100"
                />
              </div>
              <div className="flex justify-end gap-3 pt-2 sm:col-span-2">
                <button
                  type="button"
                  onClick={() => setFormOpen(false)}
                  className="rounded-lg border border-slate-700 bg-slate-800 px-4 py-2 text-sm font-medium text-slate-200 hover:bg-slate-700"
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-500"
                >
                  Lưu sản phẩm
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      <ConfirmDialog
        open={confirmDelete !== null}
        title="Xác nhận xóa sản phẩm"
        message={
          confirmDelete?.mode === 'BATCH'
            ? `Bạn có chắc chắn muốn xóa ${selectedIds.length} sản phẩm đã chọn?`
            : `Bạn có chắc chắn muốn xóa sản phẩm "${confirmDelete?.product?.name}"?`
        }
        confirmLabel="Xóa"
        destructive
        onConfirm={executeDelete}
        onCancel={() => setConfirmDelete(null)}
      />
    </div>
  );
};
