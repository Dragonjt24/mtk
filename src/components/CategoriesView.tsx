import React, {useCallback, useEffect, useState} from 'react';
import {Trash2} from 'lucide-react';
import {javaApi} from '../api/javaClient';
import type {Category} from '../types';
import {ConfirmDialog} from './ConfirmDialog';

interface CategoriesViewProps {
  refreshKey: number;
  onNotify: (type: 'success' | 'error', text: string) => void;
}

export const CategoriesView: React.FC<CategoriesViewProps> = ({refreshKey, onNotify}) => {
  const [categories, setCategories] = useState<Category[]>([]);
  const [editing, setEditing] = useState<Category | null>(null);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [active, setActive] = useState(true);
  const [deleteTarget, setDeleteTarget] = useState<Category | null>(null);

  const loadCategories = useCallback(async () => {
    try {
      setCategories(await javaApi.getCategories());
    } catch (err) {
      onNotify('error', err instanceof Error ? err.message : 'Lỗi tải danh mục.');
    }
  }, [onNotify]);

  useEffect(() => {
    loadCategories();
  }, [refreshKey, loadCategories]);

  const selectForEdit = (c: Category) => {
    setEditing(c);
    setName(c.name);
    setDescription(c.description);
    setActive(c.active);
  };

  const resetForm = () => {
    setEditing(null);
    setName('');
    setDescription('');
    setActive(true);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      if (editing) {
        await javaApi.updateCategory(editing.id, {name, description, active});
        onNotify('success', `Đã cập nhật danh mục ${name}.`);
      } else {
        await javaApi.createCategory({name, description, active});
        onNotify('success', `Đã thêm danh mục ${name}.`);
      }
      resetForm();
      await loadCategories();
    } catch (err) {
      onNotify('error', err instanceof Error ? err.message : 'Thao tác thất bại.');
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await javaApi.deleteCategory(deleteTarget.id);
      onNotify('success', `Đã xóa danh mục ${deleteTarget.name}.`);
      if (editing?.id === deleteTarget.id) resetForm();
      setDeleteTarget(null);
      await loadCategories();
    } catch (err) {
      onNotify('error', err instanceof Error ? err.message : 'Không thể xóa danh mục.');
      setDeleteTarget(null);
    }
  };

  return (
    <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
      <div className="overflow-hidden rounded-xl border border-slate-800 bg-[#111827] lg:col-span-2">
        <div className="border-b border-slate-800 px-5 py-4">
          <h2 className="text-base font-semibold text-slate-100">Danh mục sản phẩm Gaming Gear</h2>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="border-b border-slate-800 bg-slate-900/60 text-xs text-slate-400">
              <tr>
                <th className="px-4 py-3 font-medium">Mã</th>
                <th className="px-4 py-3 font-medium">Tên danh mục</th>
                <th className="px-4 py-3 font-medium">Mô tả</th>
                <th className="px-4 py-3 text-right font-medium">Số SP</th>
                <th className="px-4 py-3 font-medium">Trạng thái</th>
                <th className="px-4 py-3 text-right font-medium">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/80">
              {categories.map((c) => (
                <tr
                  key={c.id}
                  onClick={() => selectForEdit(c)}
                  className={`cursor-pointer hover:bg-slate-800/40 ${editing?.id === c.id ? 'bg-blue-950/30' : ''}`}
                >
                  <td className="font-mono-tabular px-4 py-3 text-xs text-sky-400">{c.id}</td>
                  <td className="px-4 py-3 font-medium text-slate-100">{c.name}</td>
                  <td className="px-4 py-3 text-slate-400">{c.description}</td>
                  <td className="font-mono-tabular px-4 py-3 text-right text-slate-200">{c.productCount ?? 0}</td>
                  <td className={`px-4 py-3 text-xs font-medium ${c.active ? 'text-emerald-400' : 'text-slate-400'}`}>
                    {c.active ? 'Hoạt động' : 'Tạm ẩn'}
                  </td>
                  <td className="px-4 py-3 text-right" onClick={(e) => e.stopPropagation()}>
                    <button
                      type="button"
                      onClick={() => setDeleteTarget(c)}
                      aria-label={`Xóa danh mục ${c.name}`}
                      className="rounded border border-slate-700 p-1.5 text-slate-300 hover:bg-slate-800 hover:text-rose-400"
                    >
                      <Trash2 className="h-3.5 w-3.5" />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <div className="rounded-xl border border-slate-800 bg-[#111827] p-5">
        <h2 className="text-base font-semibold text-slate-100">
          {editing ? `Cập nhật ${editing.name}` : 'Thêm danh mục mới'}
        </h2>
        <form onSubmit={handleSubmit} className="mt-4 space-y-4">
          <div>
            <label htmlFor="cat-name" className="block text-xs font-medium text-slate-300">
              Tên danh mục
            </label>
            <input
              id="cat-name"
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="mt-1.5 w-full rounded-lg border border-slate-700 bg-[#0b0f19] px-3 py-2 text-sm text-slate-100"
            />
          </div>
          <div>
            <label htmlFor="cat-desc" className="block text-xs font-medium text-slate-300">
              Mô tả
            </label>
            <textarea
              id="cat-desc"
              rows={3}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="mt-1.5 w-full rounded-lg border border-slate-700 bg-[#0b0f19] px-3 py-2 text-sm text-slate-100"
            />
          </div>
          <label className="flex items-center gap-2 text-sm text-slate-200">
            <input type="checkbox" checked={active} onChange={(e) => setActive(e.target.checked)} />
            Đang hoạt động
          </label>
          <div className="flex gap-2.5 pt-2">
            <button
              type="submit"
              className="flex-1 rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-500"
            >
              {editing ? 'Lưu cập nhật' : 'Thêm danh mục'}
            </button>
            {editing && (
              <button
                type="button"
                onClick={resetForm}
                className="rounded-lg border border-slate-700 bg-slate-800 px-3 py-2 text-sm text-slate-200 hover:bg-slate-700"
              >
                Mới
              </button>
            )}
          </div>
        </form>
      </div>

      <ConfirmDialog
        open={deleteTarget !== null}
        title="Xác nhận xóa danh mục"
        message={`Bạn muốn xóa danh mục "${deleteTarget?.name}"? (Hệ thống sẽ chặn nếu còn sản phẩm liên kết).`}
        confirmLabel="Xóa"
        destructive
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
      />
    </div>
  );
};
