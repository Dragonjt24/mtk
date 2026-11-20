import React, {useEffect, useState} from 'react';
import {Eye, EyeOff, Lock, Mail, MapPin, Phone, User as UserIcon} from 'lucide-react';
import {javaApi} from '../api/javaClient';
import type {User} from '../types';

export type AuthFormMode = 'LOGIN' | 'REGISTER';

interface LoginViewProps {
  initialMode?: AuthFormMode;
  onSuccess: (user: User) => void;
  onBackToStore?: () => void;
}

export const LoginView: React.FC<LoginViewProps> = ({
  initialMode = 'LOGIN',
  onSuccess,
  onBackToStore,
}) => {
  const [mode, setMode] = useState<AuthFormMode>(initialMode);

  // Login form state
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);

  // Registration form state (full customer profile required)
  const [regFullName, setRegFullName] = useState('');
  const [regUsername, setRegUsername] = useState('');
  const [regPhone, setRegPhone] = useState('');
  const [regEmail, setRegEmail] = useState('');
  const [regAddress, setRegAddress] = useState('');
  const [regPassword, setRegPassword] = useState('');
  const [regConfirmPassword, setRegConfirmPassword] = useState('');
  const [showRegPassword, setShowRegPassword] = useState(false);

  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    setMode(initialMode);
    setError(null);
  }, [initialMode]);

  const handleLoginSubmit = async (e: React.FormEvent) => {
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

  const handleRegisterSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (
      !regFullName.trim() ||
      !regUsername.trim() ||
      !regPhone.trim() ||
      !regEmail.trim() ||
      !regAddress.trim() ||
      !regPassword
    ) {
      setError('Vui lòng điền đầy đủ họ tên, tên đăng nhập, số điện thoại, email, địa chỉ và mật khẩu.');
      return;
    }

    if (regPassword !== regConfirmPassword) {
      setError('Mật khẩu xác nhận không khớp.');
      return;
    }

    setLoading(true);
    try {
      const user = await javaApi.registerCustomer({
        username: regUsername.trim(),
        password: regPassword,
        fullName: regFullName.trim(),
        phone: regPhone.trim(),
        email: regEmail.trim(),
        address: regAddress.trim(),
      });
      onSuccess(user);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Đăng ký tài khoản thất bại.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-[#0b0f19] px-4 py-10">
      <div className={`w-full rounded-xl border border-slate-800 bg-[#111827] p-7 sm:p-8 ${mode === 'REGISTER' ? 'max-w-lg' : 'max-w-md'}`}>
        <div>
          <p className="text-xs font-medium tracking-wide text-sky-400">
            GAMING GEAR
          </p>
          <h1 className="font-display mt-2 text-2xl font-semibold text-slate-100">
            {mode === 'LOGIN' ? 'Đăng nhập tài khoản' : 'Đăng ký tài khoản mua hàng'}
          </h1>
          <p className="mt-1 text-sm text-slate-400">
            {mode === 'LOGIN'
              ? 'Đăng nhập để tiếp tục mua sắm và quản lý đơn hàng của bạn.'
              : 'Điền đầy đủ thông tin liên hệ và địa chỉ giao hàng để kích hoạt mua sắm.'}
          </p>
        </div>

        {/* Mode Switcher */}
        <div className="mt-5 grid grid-cols-2 gap-1 rounded-lg border border-slate-800 bg-[#0b0f19] p-1">
          <button
            type="button"
            onClick={() => {
              setMode('LOGIN');
              setError(null);
            }}
            className={`rounded-md py-2 text-sm font-semibold transition-colors ${
              mode === 'LOGIN'
                ? 'bg-blue-600 text-white'
                : 'text-slate-400 hover:text-slate-100'
            }`}
          >
            Đăng nhập
          </button>
          <button
            type="button"
            onClick={() => {
              setMode('REGISTER');
              setError(null);
            }}
            className={`rounded-md py-2 text-sm font-semibold transition-colors ${
              mode === 'REGISTER'
                ? 'bg-blue-600 text-white'
                : 'text-slate-400 hover:text-slate-100'
            }`}
          >
            Đăng ký
          </button>
        </div>

        {mode === 'LOGIN' ? (
          <form onSubmit={handleLoginSubmit} className="mt-6 space-y-5">
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
                  required
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  autoComplete="username"
                  className="w-full rounded-lg border border-slate-700 bg-[#0b0f19] py-2.5 pl-9 pr-3 text-sm text-slate-100 placeholder-slate-500 focus:border-sky-400 focus:outline-none"
                  placeholder="Ví dụ: khach01 hoặc admin"
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
                  required
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
        ) : (
          <form onSubmit={handleRegisterSubmit} className="mt-6 space-y-4">
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div>
                <label htmlFor="reg-fullname" className="block text-xs font-medium text-slate-300">
                  Họ và tên *
                </label>
                <div className="relative mt-1.5">
                  <UserIcon
                    className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-500"
                    aria-hidden="true"
                  />
                  <input
                    id="reg-fullname"
                    type="text"
                    required
                    value={regFullName}
                    onChange={(e) => setRegFullName(e.target.value)}
                    placeholder="Nguyễn Văn An"
                    className="w-full rounded-lg border border-slate-700 bg-[#0b0f19] py-2 pl-9 pr-3 text-sm text-slate-100 placeholder-slate-500 focus:border-sky-400 focus:outline-none"
                  />
                </div>
              </div>

              <div>
                <label htmlFor="reg-username" className="block text-xs font-medium text-slate-300">
                  Tên đăng nhập *
                </label>
                <input
                  id="reg-username"
                  type="text"
                  required
                  minLength={3}
                  value={regUsername}
                  onChange={(e) => setRegUsername(e.target.value)}
                  autoComplete="username"
                  placeholder="nguyenvanan"
                  className="mt-1.5 w-full rounded-lg border border-slate-700 bg-[#0b0f19] px-3 py-2 text-sm text-slate-100 placeholder-slate-500 focus:border-sky-400 focus:outline-none"
                />
              </div>

              <div>
                <label htmlFor="reg-phone" className="block text-xs font-medium text-slate-300">
                  Số điện thoại *
                </label>
                <div className="relative mt-1.5">
                  <Phone
                    className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-500"
                    aria-hidden="true"
                  />
                  <input
                    id="reg-phone"
                    type="tel"
                    required
                    value={regPhone}
                    onChange={(e) => setRegPhone(e.target.value)}
                    placeholder="0901122334"
                    className="w-full rounded-lg border border-slate-700 bg-[#0b0f19] py-2 pl-9 pr-3 text-sm text-slate-100 placeholder-slate-500 focus:border-sky-400 focus:outline-none"
                  />
                </div>
              </div>

              <div>
                <label htmlFor="reg-email" className="block text-xs font-medium text-slate-300">
                  Email *
                </label>
                <div className="relative mt-1.5">
                  <Mail
                    className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-500"
                    aria-hidden="true"
                  />
                  <input
                    id="reg-email"
                    type="email"
                    required
                    value={regEmail}
                    onChange={(e) => setRegEmail(e.target.value)}
                    placeholder="an.nguyen@gmail.com"
                    className="w-full rounded-lg border border-slate-700 bg-[#0b0f19] py-2 pl-9 pr-3 text-sm text-slate-100 placeholder-slate-500 focus:border-sky-400 focus:outline-none"
                  />
                </div>
              </div>
            </div>

            <div>
              <label htmlFor="reg-address" className="block text-xs font-medium text-slate-300">
                Địa chỉ giao hàng *
              </label>
              <div className="relative mt-1.5">
                <MapPin
                  className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-500"
                  aria-hidden="true"
                />
                <input
                  id="reg-address"
                  type="text"
                  required
                  value={regAddress}
                  onChange={(e) => setRegAddress(e.target.value)}
                  placeholder="Số nhà, Đường, Phường/Quận, Tỉnh/Thành phố"
                  className="w-full rounded-lg border border-slate-700 bg-[#0b0f19] py-2 pl-9 pr-3 text-sm text-slate-100 placeholder-slate-500 focus:border-sky-400 focus:outline-none"
                />
              </div>
            </div>

            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div>
                <label htmlFor="reg-password" className="block text-xs font-medium text-slate-300">
                  Mật khẩu (tối thiểu 6 ký tự) *
                </label>
                <div className="relative mt-1.5">
                  <Lock
                    className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-500"
                    aria-hidden="true"
                  />
                  <input
                    id="reg-password"
                    type={showRegPassword ? 'text' : 'password'}
                    required
                    minLength={6}
                    value={regPassword}
                    onChange={(e) => setRegPassword(e.target.value)}
                    autoComplete="new-password"
                    placeholder="Ít nhất 6 ký tự"
                    className="w-full rounded-lg border border-slate-700 bg-[#0b0f19] py-2 pl-9 pr-10 text-sm text-slate-100 placeholder-slate-500 focus:border-sky-400 focus:outline-none"
                  />
                  <button
                    type="button"
                    onClick={() => setShowRegPassword((prev) => !prev)}
                    aria-label={showRegPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                    className="absolute right-2 top-1/2 -translate-y-1/2 rounded p-1 text-slate-400 hover:text-slate-200"
                  >
                    {showRegPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                  </button>
                </div>
              </div>

              <div>
                <label htmlFor="reg-confirm-password" className="block text-xs font-medium text-slate-300">
                  Xác nhận mật khẩu *
                </label>
                <input
                  id="reg-confirm-password"
                  type={showRegPassword ? 'text' : 'password'}
                  required
                  minLength={6}
                  value={regConfirmPassword}
                  onChange={(e) => setRegConfirmPassword(e.target.value)}
                  autoComplete="new-password"
                  placeholder="Nhập lại mật khẩu"
                  className="mt-1.5 w-full rounded-lg border border-slate-700 bg-[#0b0f19] px-3 py-2 text-sm text-slate-100 placeholder-slate-500 focus:border-sky-400 focus:outline-none"
                />
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
              {loading ? 'Đang tạo tài khoản...' : 'Hoàn tất đăng ký & Quay lại mua sắm'}
            </button>
          </form>
        )}

        <div className="mt-6 space-y-3 border-t border-slate-800 pt-4 text-xs text-slate-400">
          
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
