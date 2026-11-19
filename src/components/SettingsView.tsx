import React, {useCallback, useEffect, useState} from 'react';
import {javaApi} from '../api/javaClient';
import type {AppSettings, User, UserRole} from '../types';
import {ConfirmDialog} from './ConfirmDialog';

interface SettingsViewProps {
  refreshKey: number;
  onDataChanged: (message: string) => void;
}

export const SettingsView: React.FC<SettingsViewProps> = ({refreshKey, onDataChanged}) => {
  const [settings, setSettings] = useState<AppSettings | null>(null);
  const [thresholdInput, setThresholdInput] = useState('5');
  const [autoSave, setAutoSave] = useState(true);
  const [users, setUsers] = useState<User[]>([]);

  // Create user modal state
  const [showCreateUser, setShowCreateUser] = useState(false);
  const [newUsername, setNewUsername] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [newFullName, setNewFullName] = useState('');
  const [newRole, setNewRole] = useState<UserRole>('STAFF');

  // Change password modal state
  const [passwordTarget, setPasswordTarget] = useState<User | null>(null);
  const [passwordValue, setPasswordValue] = useState('');

  const [confirmReset, setConfirmReset] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const loadAll = useCallback(async () => {
    try {
      const [s, u] = await Promise.all([javaApi.getSettings(), javaApi.getUsers()]);
      setSettings(s);
      setThresholdInput(String(s.lowStockThreshold));
      setAutoSave(s.autoSaveEnabled);
      setUsers(u);
      setError(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không tải được cài đặt hệ thống.');
    }
  }, []);

  useEffect(() => {
    loadAll();
  }, [loadAll, refreshKey]);

  const saveConfig = async (e: React.FormEvent) => {
    e.preventDefault();
    const num = Number(thresholdInput);
    if (!Number.isInteger(num) || num < 0) {
      setError('Ngưỡng cảnh báo sắp hết hàng phải là số nguyên >= 0.');
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await javaApi.updateSettings({lowStockThreshold: num, autoSaveEnabled: autoSave});
      onDataChanged('Đã cập nhật cấu hình hệ thống.');
      await loadAll();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể cập nhật cấu hình.');
    } finally {
      setBusy(false);
    }
  };

  const handleSaveJson = async () => {
    setBusy(true);
    try {
      await javaApi.saveJsonNow();
      onDataChanged('Đã lưu toàn bộ InMemoryDataStore xuống tệp JSON cục bộ.');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Lỗi lưu JSON.');
    } finally {
      setBusy(false);
    }
  };

  const handleReloadJson = async () => {
    setBusy(true);
    try {
      await javaApi.reloadJson();
      onDataChanged('Đã tải lại dữ liệu từ tệp JSON.');
      await loadAll();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Lỗi tải lại JSON.');
    } finally {
      setBusy(false);
    }
  };

  const handleResetSample = async () => {
    setBusy(true);
    setConfirmReset(false);
    try {
      await javaApi.resetSampleData();
      onDataChanged('Đã khôi phục dữ liệu mẫu ban đầu.');
      await loadAll();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể khôi phục dữ liệu mẫu.');
    } finally {
      setBusy(false);
    }
  };

  const handleCreateUser = async (e: React.FormEvent) => {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await javaApi.createUser({
        username: newUsername.trim(),
        password: newPassword,
        fullName: newFullName.trim(),
        role: newRole,
      });
      setShowCreateUser(false);
      setNewUsername('');
      setNewPassword('');
      setNewFullName('');
      setNewRole('STAFF');
      onDataChanged('Đã thêm tài khoản nhân viên mới.');
      await loadAll();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không tạo được tài khoản.');
    } finally {
      setBusy(false);
    }
  };

  const toggleUserActive = async (user: User) => {
    setBusy(true);
    setError(null);
    try {
      await javaApi.updateUserStatus(user.id, {
        active: !user.active,
        role: user.role,
        fullName: user.fullName,
      });
      onDataChanged(`Đã ${user.active ? 'khóa' : 'kích hoạt'} tài khoản ${user.username}.`);
      await loadAll();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không cập nhật được trạng thái tài khoản.');
    } finally {
      setBusy(false);
    }
  };

  const handleChangePassword = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!passwordTarget) return;
    setBusy(true);
    setError(null);
    try {
      await javaApi.changePassword(passwordTarget.id, passwordValue);
      onDataChanged(`Đã đổi mật khẩu cho ${passwordTarget.username}.`);
      setPasswordTarget(null);
      setPasswordValue('');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không đổi được mật khẩu.');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="space-y-6">
      {error && (
        <div className="rounded-xl border border-red-800/60 bg-red-950/30 p-3 text-sm text-red-300">
          {error}
        </div>
      )}

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-12">
        {/* App Settings & JSON Storage */}
        <div className="space-y-6 lg:col-span-5">
          <form
            onSubmit={saveConfig}
            className="rounded-xl border border-slate-800 bg-[#111827] p-5 space-y-4"
          >
            <h2 className="text-base font-semibold text-slate-100">Cấu hình vận hành & JSON Storage</h2>
            <div>
              <label htmlFor="low-stock-threshold" className="block text-xs font-medium text-slate-400">
                Ngưỡng cảnh báo sắp hết hàng
              </label>
              <input
                id="low-stock-threshold"
                type="number"
                min={0}
                value={thresholdInput}
                onChange={(e) => setThresholdInput(e.target.value)}
                className="font-mono-tabular mt-1.5 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-100"
              />
            </div>
            <label className="flex items-center gap-2.5 text-sm text-slate-200">
              <input
                type="checkbox"
                checked={autoSave}
                onChange={(e) => setAutoSave(e.target.checked)}
                className="h-4 w-4 accent-blue-600"
              />
              Tự động lưu JSON sau mỗi thao tác ghi
            </label>
            {settings && (
              <p className="font-mono-tabular text-xs text-slate-400">
                Tệp lưu trữ: {settings.storagePath}
              </p>
            )}
            <button
              type="submit"
              disabled={busy}
              className="whitespace-nowrap shrink-0 rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-500 disabled:opacity-50"
            >
              Lưu cấu hình
            </button>
          </form>

          <div className="rounded-xl border border-slate-800 bg-[#111827] p-5 space-y-3">
            <h2 className="text-base font-semibold text-slate-100">Quản lý dữ liệu cục bộ (No-DB)</h2>
            <p className="text-xs text-slate-400">
              Dữ liệu nằm hoàn toàn trong Java InMemoryDataStore (ArrayList / HashMap / LinkedHashMap) và đồng bộ tùy chọn qua JSONStorageService.
            </p>
            <div className="flex flex-wrap gap-2 pt-1">
              <button
                type="button"
                disabled={busy}
                onClick={handleSaveJson}
                className="whitespace-nowrap shrink-0 rounded-lg border border-slate-700 bg-slate-800 px-3.5 py-2 text-xs font-semibold text-slate-100 hover:bg-slate-700"
              >
                Lưu JSON ngay
              </button>
              <button
                type="button"
                disabled={busy}
                onClick={handleReloadJson}
                className="whitespace-nowrap shrink-0 rounded-lg border border-slate-700 bg-slate-800 px-3.5 py-2 text-xs font-semibold text-slate-100 hover:bg-slate-700"
              >
                Tải lại từ JSON
              </button>
              <button
                type="button"
                disabled={busy}
                onClick={() => setConfirmReset(true)}
                className="whitespace-nowrap shrink-0 rounded-lg border border-red-800/80 bg-red-950/40 px-3.5 py-2 text-xs font-semibold text-red-300 hover:bg-red-900/50"
              >
                Khôi phục dữ liệu mẫu
              </button>
            </div>
          </div>
        </div>

        {/* Staff / User Accounts */}
        <div className="rounded-xl border border-slate-800 bg-[#111827] p-5 lg:col-span-7">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h2 className="text-base font-semibold text-slate-100">
                Quản lý nhân viên & tài khoản ({users.length})
              </h2>
              <p className="text-xs text-slate-400">Tài khoản đăng nhập quản trị và bán hàng</p>
            </div>
            <button
              type="button"
              onClick={() => setShowCreateUser(true)}
              className="whitespace-nowrap shrink-0 rounded-lg bg-blue-600 px-3.5 py-2 text-xs font-semibold text-white hover:bg-blue-500"
            >
              Thêm nhân viên
            </button>
          </div>

          <div className="mt-4 overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-slate-800 text-xs text-slate-400">
                <tr>
                  <th className="py-2.5 pr-3 font-medium">Mã NV</th>
                  <th className="py-2.5 pr-3 font-medium">Tên đăng nhập</th>
                  <th className="py-2.5 pr-3 font-medium">Họ tên</th>
                  <th className="py-2.5 pr-3 font-medium">Vai trò</th>
                  <th className="py-2.5 pr-3 font-medium">Trạng thái</th>
                  <th className="py-2.5 text-right font-medium">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/80">
                {users.map((u) => (
                  <tr key={u.id} className="hover:bg-slate-800/40">
                    <td className="font-mono-tabular py-2.5 pr-3 text-sky-400">{u.id}</td>
                    <td className="font-mono-tabular py-2.5 pr-3 text-slate-100">{u.username}</td>
                    <td className="py-2.5 pr-3 text-slate-200">{u.fullName}</td>
                    <td className="py-2.5 pr-3 text-xs text-slate-300">{u.role}</td>
                    <td
                      className={`py-2.5 pr-3 text-xs font-medium ${
                        u.active ? 'text-emerald-400' : 'text-rose-400'
                      }`}
                    >
                      {u.active ? 'Đang hoạt động' : 'Đã khóa'}
                    </td>
                    <td className="py-2.5 text-right">
                      <div className="inline-flex items-center gap-2">
                        <button
                          type="button"
                          onClick={() => {
                            setPasswordTarget(u);
                            setPasswordValue('');
                          }}
                          className="whitespace-nowrap shrink-0 rounded border border-slate-700 bg-slate-800 px-2.5 py-1 text-xs text-slate-200 hover:bg-slate-700"
                        >
                          Đổi MK
                        </button>
                        <button
                          type="button"
                          onClick={() => toggleUserActive(u)}
                          disabled={u.username === 'admin'}
                          className="whitespace-nowrap shrink-0 rounded border border-slate-700 bg-slate-800 px-2.5 py-1 text-xs text-slate-200 hover:bg-slate-700 disabled:opacity-40"
                        >
                          {u.active ? 'Khóa' : 'Mở khóa'}
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      {/* Create User Modal */}
      {showCreateUser && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4"
          role="dialog"
          aria-modal="true"
          aria-labelledby="create-user-title"
        >
          <form
            onSubmit={handleCreateUser}
            className="w-full max-w-md rounded-xl border border-slate-800 bg-[#111827] p-6 shadow-2xl space-y-4"
          >
            <h3 id="create-user-title" className="text-lg font-semibold text-slate-100">
              Thêm tài khoản nhân viên
            </h3>
            <div>
              <label htmlFor="new-username" className="block text-xs font-medium text-slate-400">
                Tên đăng nhập
              </label>
              <input
                id="new-username"
                type="text"
                required
                value={newUsername}
                onChange={(e) => setNewUsername(e.target.value)}
                className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-100"
              />
            </div>
            <div>
              <label htmlFor="new-fullname" className="block text-xs font-medium text-slate-400">
                Họ và tên
              </label>
              <input
                id="new-fullname"
                type="text"
                required
                value={newFullName}
                onChange={(e) => setNewFullName(e.target.value)}
                className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-100"
              />
            </div>
            <div>
              <label htmlFor="new-password" className="block text-xs font-medium text-slate-400">
                Mật khẩu
              </label>
              <input
                id="new-password"
                type="password"
                required
                minLength={4}
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-100"
              />
            </div>
            <div>
              <label htmlFor="new-role" className="block text-xs font-medium text-slate-400">
                Vai trò
              </label>
              <select
                id="new-role"
                value={newRole}
                onChange={(e) => setNewRole(e.target.value as UserRole)}
                className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-100"
              >
                <option value="STAFF">STAFF</option>
                <option value="ADMIN">ADMIN</option>
              </select>
            </div>
            <div className="flex justify-end gap-3 pt-2">
              <button
                type="button"
                onClick={() => setShowCreateUser(false)}
                className="whitespace-nowrap shrink-0 rounded-lg border border-slate-700 bg-slate-800 px-4 py-2 text-sm text-slate-200 hover:bg-slate-700"
              >
                Hủy
              </button>
              <button
                type="submit"
                disabled={busy}
                className="whitespace-nowrap shrink-0 rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-500"
              >
                Tạo tài khoản
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Change Password Modal */}
      {passwordTarget && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4"
          role="dialog"
          aria-modal="true"
          aria-labelledby="change-pw-title"
        >
          <form
            onSubmit={handleChangePassword}
            className="w-full max-w-sm rounded-xl border border-slate-800 bg-[#111827] p-6 shadow-2xl space-y-4"
          >
            <h3 id="change-pw-title" className="text-lg font-semibold text-slate-100">
              Đổi mật khẩu: {passwordTarget.username}
            </h3>
            <div>
              <label htmlFor="pw-input" className="block text-xs font-medium text-slate-400">
                Mật khẩu mới
              </label>
              <input
                id="pw-input"
                type="password"
                required
                minLength={4}
                value={passwordValue}
                onChange={(e) => setPasswordValue(e.target.value)}
                className="mt-1.5 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-100"
              />
            </div>
            <div className="flex justify-end gap-3 pt-2">
              <button
                type="button"
                onClick={() => setPasswordTarget(null)}
                className="whitespace-nowrap shrink-0 rounded-lg border border-slate-700 bg-slate-800 px-4 py-2 text-sm text-slate-200 hover:bg-slate-700"
              >
                Hủy
              </button>
              <button
                type="submit"
                disabled={busy}
                className="whitespace-nowrap shrink-0 rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-500"
              >
                Cập nhật
              </button>
            </div>
          </form>
        </div>
      )}

      <ConfirmDialog
        open={confirmReset}
        title="Khôi phục dữ liệu mẫu"
        message="Toàn bộ thay đổi hiện tại sẽ được thay thế bằng bộ dữ liệu mẫu ban đầu của Gaming Gear Management System. Tiếp tục?"
        confirmLabel="Khôi phục"
        destructive
        onConfirm={handleResetSample}
        onCancel={() => setConfirmReset(false)}
      />
    </div>
  );
};
