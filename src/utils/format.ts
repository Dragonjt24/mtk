import type {DiscountType, OrderStatus, PaymentMethod, PaymentStatus, ProductStatus} from '../types';

export function formatVnd(amount: number | string | null | undefined): string {
  const num = Number(amount ?? 0);
  if (!Number.isFinite(num)) {
    return '0 VNĐ';
  }
  return `${Math.round(num).toLocaleString('vi-VN')} VNĐ`;
}

export function formatPercent(rate: number | null | undefined): string {
  const num = Number(rate ?? 0);
  if (!Number.isFinite(num)) {
    return '0%';
  }
  return `${Math.round(num * 100)}%`;
}

export function formatDateTime(iso: string | null | undefined): string {
  if (!iso) {
    return '—';
  }
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) {
    return iso;
  }
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${pad(date.getDate())}/${pad(date.getMonth() + 1)}/${date.getFullYear()} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

export function orderStatusLabel(status: OrderStatus): string {
  switch (status) {
    case 'PAID':
      return 'Đã thanh toán';
    case 'PENDING':
      return 'Chờ xử lý';
    case 'CANCELLED':
      return 'Đã hủy';
  }
}

export function productStatusLabel(status: ProductStatus): string {
  switch (status) {
    case 'ACTIVE':
      return 'Đang kinh doanh';
    case 'OUT_OF_STOCK':
      return 'Hết hàng';
    case 'INACTIVE':
      return 'Ngừng kinh doanh';
  }
}

export function discountLabel(type: DiscountType): string {
  switch (type) {
    case 'NO_DISCOUNT':
      return 'Không giảm giá (0%)';
    case 'VIP':
      return 'Khách hàng VIP (15%)';
    case 'SEASONAL':
      return 'Khuyến mãi mùa (20%)';
  }
}

export function paymentMethodLabel(method: PaymentMethod): string {
  switch (method) {
    case 'COD':
      return 'COD (Thu tiền khi nhận)';
    case 'MOCK_SUCCESS':
      return 'Mô phỏng thành công';
    case 'MOCK_FAILURE':
      return 'Mô phỏng thất bại';
  }
}

export function paymentStatusLabel(status: PaymentStatus): string {
  return status === 'SUCCESS' ? 'Thành công' : 'Thất bại';
}
