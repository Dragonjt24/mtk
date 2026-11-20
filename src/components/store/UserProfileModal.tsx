import React, {useEffect, useState} from 'react';
import {Save, X} from 'lucide-react';
import {javaApi} from '../../api/javaClient';
import type {Customer, User} from '../../types';
import {formatVnd} from '../../utils/format';

interface UserProfileModalProps {
  open: boolean;
  currentUser: User | null;
  currentCustomer: Customer | null;
  onClose: () => void;
  onProfileUpdated: (result: {user: User; customer: Customer | null}) => void;
}

export const UserProfileModal: React.FC<UserProfileModalProps> = ({
  open,
  currentUser,
  currentCustomer,
  onClose,
  onProfileUpdated,
}) => {
  const [fullName, setFullName] = useState('');
  const [phone, setPhone] = useState('');
  const [email, setEmail] = useState('');
  const [address, setAddress] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const isCustomer = Boolean(currentUser && currentUser.role === 'CUSTOMER' && currentCustomer);

  useEffect(() => {
    if (!open || !currentUser) return;
    setFullName(currentCustomer?.fullName ?? currentUser.fullName ?? '');
    setPhone(currentCustomer?.phone ?? '');
    setEmail(currentCustomer?.email ?? '');
    setAddress(currentCustomer?.address ?? '');
    setNewPassword('');
    setError(null);
  }, [open, currentUser, currentCustomer]);

  if (!open || !currentUser) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!fullName.trim()) {
      setError('Họ và tên không được để trống.');
      return;
    }
    if (isCustomer) {
      if (!phone.trim() || !email.trim() || !address.trim()) {
        setError('Vui lòng điền đầy đủ số điện thoại, email và địa chỉ giao hàng.');
        return;
      }
    }
    if (newPassword.trim() && newPassword.trim().length < 6) {
      setError('Mật khẩu mới phải có ít nhất 6 ký tự.');
      return;
    }

    setSaving(true);
    setError(null);
    try {
      const result = await javaApi.updateCustomerProfile({
        userId: currentUser.id,
        fullName: fullName.trim(),
        phone: phone.trim(),
        email: email.trim(),
        address: address.trim(),
        newPassword: newPassword.trim() || undefined,
      });
      onProfileUpdated(result);
      onClose();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không cập nhật được thông tin tài khoản.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 p-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby="profile-modal-title"
    >
      <div className="w-full max-w-lg rounded-xl border border-slate-800 bg-[#111827] p-6 shadow-2xl">
        <div className="flex items-center justify-between border-b border-slate-800 pb-4">
          <div>
            <h2 id="profile-modal-title" className="font-display text-lg font-semibold text-slate-100">
              Chỉnh sửa thông tin tài khoản
            </h2>
            <p className="mt-0.5 text-xs text-slate-400">
              @{currentUser.username} ·{' '}
              {isCustomer && currentCustomer
                ? `Hạng ${currentCustomer.membershipTier} · Chi tiêu ${formatVnd(currentCustomer.totalSpent)}`
                : currentUser.role}
            </p>
          </div>
          <button
            type="button"
            aria-label="Đóng cửa sổ chỉnh sửa thông tin"
            onClick={onClose}
            className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-800 hover:text-white"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="mt-5 space-y-4">
          <div>
            <label htmlFor="profile-fullname" className="block text-xs font-medium text-slate-300">
              Họ và tên *
            </label>
            <input
              id="profile-fullname"
              type="text"
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
              className="mt-1.5 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-100 focus:border-sky-500 focus:outline-none"
              required
            />
          </div>

          {isCustomer && (
            <>
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div>
                  <label htmlFor="profile-phone" className="block text-xs font-medium text-slate-300">
                    Số điện thoại *
                  </label>
                  <input
                    id="profile-phone"
                    type="tel"
                    value={phone}
                    onChange={(e) => setPhone(e.target.value)}
                    className="mt-1.5 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-100 focus:border-sky-500 focus:outline-none"
                    required
                  />
                </div>
                <div>
                  <label htmlFor="profile-email" className="block text-xs font-medium text-slate-300">
                    Email *
                  </label>
                  <input
                    id="profile-email"
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    className="mt-1.5 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-100 focus:border-sky-500 focus:outline-none"
                    required
                  />
                </div>
              </div>

              <div>
                <label htmlFor="profile-address" className="block text-xs font-medium text-slate-300">
                  Địa chỉ giao hàng *
                </label>
                <input
                  id="profile-address"
                  type="text"
                  value={address}
                  onChange={(e) => setAddress(e.target.value)}
                  className="mt-1.5 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-100 focus:border-sky-500 focus:outline-none"
                  required
                />
              </div>
            </>
          )}

          <div>
            <label htmlFor="profile-new-password" className="block text-xs font-medium text-slate-300">
              Mật khẩu mới (để trống nếu không đổi)
            </label>
            <input
              id="profile-new-password"
              type="password"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              placeholder="Ít nhất 6 ký tự nếu muốn đổi"
              className="mt-1.5 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-100 placeholder:text-slate-500 focus:border-sky-500 focus:outline-none"
            />
          </div>

          {error && (
            <div role="alert" className="rounded-lg border border-red-800/70 bg-red-950/30 p-3 text-xs text-red-300">
              {error}
            </div>
          )}

          <div className="flex justify-end gap-3 border-t border-slate-800 pt-4">
            <button
              type="button"
              onClick={onClose}
              className="rounded-lg border border-slate-700 bg-slate-800 px-4 py-2 text-sm font-medium text-slate-200 hover:bg-slate-700"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={saving}
              className="inline-flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-500 disabled:opacity-50"
            >
              <Save className="h-4 w-4" />
              {saving ? 'Đang lưu...' : 'Lưu thông tin'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
