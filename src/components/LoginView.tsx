import React, {useState} from 'react';
import {Eye, EyeOff, Lock, User as UserIcon} from 'lucide-react';
import {javaApi} from '../api/javaClient';
import type {User} from '../types';

interface LoginViewProps {
  onSuccess: (user: User) => void;
  onBackToStore?: () => void;
}

export const LoginView: React.FC<LoginViewProps> = ({onSuccess, onBackToStore}) => {
  const [username, setUsername] = useState('admin');
  const [password, setPassword] = useState('admin123');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const user = await javaApi.login(username, password);
      onSuccess(user);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Đăng nhập thất bại.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-[#0b0f19] px-4 py-10">
      <div className="w-full max-w-md rounded-xl border border-slate-800 bg-[#111827] p-8">
        <div>
          <p className="text-xs font-medium tracking-wide text-sky-400">
            GAMING GEAR MANAGEMENT SYSTEM
          </p>
          <h1 className="mt-2 text-2xl font-semibold text-slate-100">
            Đăng nhập hệ thống quản lý
          </h1>
          <p className="mt-1 text-sm text-slate-400">
            Kết nối trực tiếp tới máy chủ nghiệp vụ Java 17
          </p>
        </div>

        <form onSubmit={handleSubmit} className="mt-6 space-y-5">
          <div>
            <label htmlFor="login-username" className="block text-sm font-medium text-slate-300">
              Tên đăng nhập
            </label>
            <div className="relative mt-1.5">
              <UserIcon
                className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-500"
                aria-hidden="true"
              />
              <input
                id="login-username"
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                autoComplete="username"
                className="w-full rounded-lg border border-slate-700 bg-[#0b0f19] py-2.5 pl-9 pr-3 text-sm text-slate-100 placeholder-slate-500 focus:border-sky-400 focus:outline-none"
                placeholder="Nhập tên đăng nhập"
              />
            </div>
          </div>

          <div>
            <label htmlFor="login-password" className="block text-sm font-medium text-slate-300">
              Mật khẩu
            </label>
            <div className="relative mt-1.5">
              <Lock
                className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-500"
                aria-hidden="true"
              />
              <input
                id="login-password"
                type={showPassword ? 'text' : 'password'}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                autoComplete="current-password"
                className="w-full rounded-lg border border-slate-700 bg-[#0b0f19] py-2.5 pl-9 pr-11 text-sm text-slate-100 placeholder-slate-500 focus:border-sky-400 focus:outline-none"
                placeholder="Nhập mật khẩu"
              />
              <button
                type="button"
                onClick={() => setShowPassword((prev) => !prev)}
                aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                className="absolute right-2.5 top-1/2 -translate-y-1/2 rounded p-1 text-slate-400 hover:text-slate-200 focus-visible:outline-2 focus-visible:outline-sky-400"
              >
                {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
              </button>
            </div>
          </div>

          {error && (
            <div
              role="alert"
              className="rounded-lg border border-red-800/70 bg-red-950/40 px-3.5 py-2.5 text-sm text-red-300"
            >
              {error}
            </div>
          )}

          <button
            type="submit"
            disabled={loading}
            className="w-full whitespace-nowrap rounded-lg bg-blue-600 px-4 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-blue-500 disabled:opacity-60 focus-visible:outline-2 focus-visible:outline-sky-400"
          >
            {loading ? 'Đang xác thực...' : 'Đăng nhập'}
          </button>
        </form>

        <div className="mt-6 space-y-3 border-t border-slate-800 pt-4 text-xs text-slate-400">
          <p>
            Tài khoản demo: <span className="font-mono-tabular text-slate-200">admin / admin123</span>
            {' · '}
            <span className="font-mono-tabular text-slate-200">staff01 / staff123</span>
          </p>
          {onBackToStore && (
            <button
              type="button"
              onClick={onBackToStore}
              className="w-full whitespace-nowrap rounded-lg border border-slate-700 bg-slate-800/80 px-3 py-2 text-xs font-medium text-slate-200 transition-colors hover:bg-slate-700"
            >
              ← Quay lại Cửa hàng Gaming Gear
            </button>
          )}
        </div>
      </div>
    </div>
  );
};
