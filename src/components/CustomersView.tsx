import React, {useCallback, useEffect, useState} from 'react';
import {Edit3, Plus, Search, Trash2} from 'lucide-react';
import {javaApi} from '../api/javaClient';
import type {Customer, MembershipTier, Order} from '../types';
import {formatDateTime, formatVnd, orderStatusLabel} from '../utils/format';
import {ConfirmDialog} from './ConfirmDialog';

interface CustomersViewProps {
  refreshKey: number;
  onNotify: (type: 'success' | 'error', text: string) => void;
}

export const CustomersView: React.FC<CustomersViewProps> = ({refreshKey, onNotify}) => {
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [keyword, setKeyword] = useState('');
  const [selectedCustomer, setSelectedCustomer] = useState<Customer | null>(null);
  const [history, setHistory] = useState<Order[]>([]);

  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<Customer | null>(null);
  const [formState, setFormState] = useState({
    fullName: '',
    phone: '',
    email: '',
    address: '',
    membershipTier: 'NORMAL' as MembershipTier,
  });
  const [deleteTarget, setDeleteTarget] = useState<Customer | null>(null);

  const loadCustomers = useCallback(async () => {
    try {
      const list = await javaApi.getCustomers(keyword);
      setCustomers(list);
      if (list.length > 0 && !selectedCustomer) {
        setSelectedCustomer(list[0]);
      }
    } catch (err) {
      onNotify('error', err instanceof Error ? err.message : 'Lỗi tải khách hàng.');
    }
  }, [keyword, onNotify, selectedCustomer]);

  useEffect(() => {
    loadCustomers();
  }, [refreshKey, loadCustomers]);

  useEffect(() => {
    if (!selectedCustomer) {
      setHistory([]);
      return;
    }
    javaApi
      .getCustomerOrders(selectedCustomer.id)
      .then(setHistory)
      .catch(() => setHistory([]));
  }, [selectedCustomer]);

  const openCreate = () => {
    setEditing(null);
    setFormState({fullName: '', phone: '', email: '', address: '', membershipTier: 'NORMAL'});
    setFormOpen(true);
  };

  const openEdit = (c: Customer) => {
    setEditing(c);
    setFormState({
      fullName: c.fullName,
      phone: c.phone,
      email: c.email,
      address: c.address,
      membershipTier: c.membershipTier,
    });
    setFormOpen(true);
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      if (editing) {
        await javaApi.updateCustomer(editing.id, formState);
        onNotify('success', `Đã cập nhật khách hàng ${formState.fullName}.`);
      } else {
        await javaApi.createCustomer(formState);
        onNotify('success', 'Đã thêm khách hàng mới.');
      }
      setFormOpen(false);
      await loadCustomers();
    } catch (err) {
      onNotify('error', err instanceof Error ? err.message : 'Không thể lưu khách hàng.');
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await javaApi.deleteCustomer(deleteTarget.id);
      onNotify('success', `Đã xóa khách hàng ${deleteTarget.fullName}.`);
      if (selectedCustomer?.id === deleteTarget.id) setSelectedCustomer(null);
      setDeleteTarget(null);
      await loadCustomers();
    } catch (err) {
      onNotify('error', err instanceof Error ? err.message : 'Không thể xóa khách hàng.');
      setDeleteTarget(null);
    }
  };

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-slate-800 bg-[#111827] p-4">
        <div className="relative w-full max-w-sm">
          <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-500" />
          <input
            type="text"
            aria-label="Tìm kiếm khách hàng"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            placeholder="Tìm theo họ tên, SĐT, email..."
            className="w-full rounded-lg border border-slate-700 bg-[#0b0f19] py-2 pl-9 pr-3 text-sm text-slate-100 placeholder-slate-500 focus:border-sky-400 focus:outline-none"
          />
        </div>
        <button
          type="button"
          onClick={openCreate}
          className="inline-flex items-center gap-1.5 whitespace-nowrap rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-500"
        >
          <Plus className="h-4 w-4" />
          Thêm khách hàng
        </button>
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        <div className="overflow-hidden rounded-xl border border-slate-800 bg-[#111827] lg:col-span-2">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-slate-800 bg-slate-900/60 text-xs text-slate-400">
                <tr>
                  <th className="px-4 py-3 font-medium">Mã KH</th>
                  <th className="px-3 py-3 font-medium">Họ tên</th>
                  <th className="px-3 py-3 font-medium">SĐT / Email</th>
                  <th className="px-3 py-3 font-medium">Hạng</th>
                  <th className="px-3 py-3 text-right font-medium">Tổng chi tiêu</th>
                  <th className="px-4 py-3 text-right font-medium">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/80">
                {customers.map((c) => (
                  <tr
                    key={c.id}
                    onClick={() => setSelectedCustomer(c)}
                    className={`cursor-pointer hover:bg-slate-800/40 ${
                      selectedCustomer?.id === c.id ? 'bg-blue-950/30' : ''
                    }`}
                  >
                    <td className="font-mono-tabular px-4 py-3 text-xs text-sky-400">{c.id}</td>
                    <td className="px-3 py-3">
                      <div className="font-medium text-slate-100">{c.fullName}</div>
                      <div className="text-xs text-slate-400">{c.address}</div>
                    </td>
                    <td className="px-3 py-3">
                      <div className="font-mono-tabular text-xs text-slate-200">{c.phone}</div>
                      <div className="text-xs text-slate-400">{c.email}</div>
                    </td>
                    <td
                      className={`px-3 py-3 text-xs font-semibold ${
                        c.membershipTier === 'VIP' ? 'text-amber-400' : 'text-slate-300'
                      }`}
                    >
                      {c.membershipTier}
                    </td>
                    <td className="font-mono-tabular px-3 py-3 text-right text-emerald-400">
                      {formatVnd(c.totalSpent)}
                    </td>
                    <td className="px-4 py-3 text-right" onClick={(e) => e.stopPropagation()}>
                      <div className="inline-flex items-center gap-2">
                        <button
                          type="button"
                          onClick={() => openEdit(c)}
                          aria-label={`Sửa ${c.fullName}`}
                          className="rounded border border-slate-700 p-1.5 text-slate-300 hover:bg-slate-800 hover:text-sky-400"
                        >
                          <Edit3 className="h-3.5 w-3.5" />
                        </button>
                        <button
                          type="button"
                          onClick={() => setDeleteTarget(c)}
                          aria-label={`Xóa ${c.fullName}`}
                          className="rounded border border-slate-700 p-1.5 text-slate-300 hover:bg-slate-800 hover:text-rose-400"
                        >
                          <Trash2 className="h-3.5 w-3.5" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        <div className="rounded-xl border border-slate-800 bg-[#111827] p-5">
          <h2 className="text-base font-semibold text-slate-100">Lịch sử mua hàng</h2>
          <p className="mt-1 text-xs text-slate-400">
            {selectedCustomer ? `${selectedCustomer.fullName} · ${selectedCustomer.membershipTier}` : 'Chọn một khách hàng'}
          </p>
          <div className="mt-4 divide-y divide-slate-800">
            {history.length === 0 ? (
              <p className="py-6 text-sm text-slate-400">Khách hàng này chưa có đơn hàng nào.</p>
            ) : (
              history.map((o) => (
                <div key={o.id} className="py-3 text-sm">
                  <div className="flex items-center justify-between">
                    <span className="font-mono-tabular font-medium text-sky-400">{o.id}</span>
                    <span
                      className={`text-xs font-medium ${
                        o.status === 'PAID'
                          ? 'text-emerald-400'
                          : o.status === 'PENDING'
                            ? 'text-amber-400'
                            : 'text-rose-400'
                      }`}
                    >
                      {orderStatusLabel(o.status)}
                    </span>
                  </div>
                  <div className="mt-1 flex items-center justify-between text-xs text-slate-400">
                    <span className="font-mono-tabular">{formatDateTime(o.createdAt)}</span>
                    <span className="font-mono-tabular text-slate-200">{formatVnd(o.finalTotal)}</span>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      </div>

      {formOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4" role="dialog" aria-modal="true">
          <div className="w-full max-w-md rounded-xl border border-slate-800 bg-[#111827] p-6">
            <h3 className="text-lg font-semibold text-slate-100">
              {editing ? `Cập nhật ${editing.fullName}` : 'Thêm khách hàng mới'}
            </h3>
            <form onSubmit={handleSave} className="mt-4 space-y-3.5">
              <div>
                <label htmlFor="cus-name" className="block text-xs font-medium text-slate-300">
                  Họ và tên
                </label>
                <input
                  id="cus-name"
                  required
                  value={formState.fullName}
                  onChange={(e) => setFormState({...formState, fullName: e.target.value})}
                  className="mt-1 w-full rounded-lg border border-slate-700 bg-[#0b0f19] px-3 py-2 text-sm text-slate-100"
                />
              </div>
              <div>
                <label htmlFor="cus-phone" className="block text-xs font-medium text-slate-300">
                  Số điện thoại
                </label>
                <input
                  id="cus-phone"
                  required
                  value={formState.phone}
                  onChange={(e) => setFormState({...formState, phone: e.target.value})}
                  className="mt-1 w-full rounded-lg border border-slate-700 bg-[#0b0f19] px-3 py-2 text-sm text-slate-100"
                />
              </div>
              <div>
                <label htmlFor="cus-email" className="block text-xs font-medium text-slate-300">
                  Email
                </label>
                <input
                  id="cus-email"
                  type="email"
                  required
                  value={formState.email}
                  onChange={(e) => setFormState({...formState, email: e.target.value})}
                  className="mt-1 w-full rounded-lg border border-slate-700 bg-[#0b0f19] px-3 py-2 text-sm text-slate-100"
                />
              </div>
              <div>
                <label htmlFor="cus-addr" className="block text-xs font-medium text-slate-300">
                  Địa chỉ
                </label>
                <input
                  id="cus-addr"
                  required
                  value={formState.address}
                  onChange={(e) => setFormState({...formState, address: e.target.value})}
                  className="mt-1 w-full rounded-lg border border-slate-700 bg-[#0b0f19] px-3 py-2 text-sm text-slate-100"
                />
              </div>
              <div>
                <label htmlFor="cus-tier" className="block text-xs font-medium text-slate-300">
                  Hạng thành viên
                </label>
                <select
                  id="cus-tier"
                  value={formState.membershipTier}
                  onChange={(e) => setFormState({...formState, membershipTier: e.target.value as MembershipTier})}
                  className="mt-1 w-full rounded-lg border border-slate-700 bg-[#0b0f19] px-3 py-2 text-sm text-slate-100"
                >
                  <option value="NORMAL">NORMAL</option>
                  <option value="VIP">VIP</option>
                </select>
              </div>
              <div className="flex justify-end gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => setFormOpen(false)}
                  className="rounded-lg border border-slate-700 bg-slate-800 px-4 py-2 text-sm text-slate-200 hover:bg-slate-700"
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-500"
                >
                  Lưu khách hàng
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      <ConfirmDialog
        open={deleteTarget !== null}
        title="Xác nhận xóa khách hàng"
        message={`Bạn có chắc muốn xóa khách hàng "${deleteTarget?.fullName}"?`}
        confirmLabel="Xóa"
        destructive
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
      />
    </div>
  );
};
