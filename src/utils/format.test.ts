import test from 'node:test';
import assert from 'node:assert/strict';
import {
  discountLabel,
  formatDateTime,
  formatPercent,
  formatVnd,
  orderStatusLabel,
  paymentMethodLabel,
  productStatusLabel,
} from './format';

test('formatVnd formats currency cleanly with VND suffix', () => {
  assert.equal(formatVnd(1000000), '1.000.000 VNĐ');
  assert.equal(formatVnd(0), '0 VNĐ');
  assert.equal(formatVnd(null), '0 VNĐ');
});

test('formatPercent converts decimal rate to percentage string', () => {
  assert.equal(formatPercent(0), '0%');
  assert.equal(formatPercent(0.15), '15%');
  assert.equal(formatPercent(0.2), '20%');
});

test('status and policy labels return expected Vietnamese labels', () => {
  assert.equal(orderStatusLabel('PAID'), 'Đã thanh toán');
  assert.equal(orderStatusLabel('PENDING'), 'Chờ xử lý');
  assert.equal(orderStatusLabel('CANCELLED'), 'Đã hủy');
  assert.equal(productStatusLabel('ACTIVE'), 'Đang kinh doanh');
  assert.equal(discountLabel('VIP'), 'Khách hàng VIP (15%)');
  assert.equal(
    discountLabel('SEASONAL', {
      seasonalPromoEnabled: true,
      seasonalPromoName: 'Sale Cuối Năm',
      seasonalDiscountRate: 0.25,
      vipDiscountRate: 0.18,
    }),
    'Sale Cuối Năm (25%)',
  );
  assert.equal(
    discountLabel('VIP', {
      seasonalPromoEnabled: false,
      seasonalPromoName: 'Sale Cuối Năm',
      seasonalDiscountRate: 0.25,
      vipDiscountRate: 0.18,
    }),
    'Khách hàng VIP (18%)',
  );
  assert.equal(paymentMethodLabel('MOCK_FAILURE'), 'Mô phỏng thất bại');
});

test('formatDateTime handles empty and valid ISO strings safely', () => {
  assert.equal(formatDateTime(''), '—');
  assert.match(formatDateTime('2026-11-18T10:30:00'), /^\d{2}\/\d{2}\/\d{4} \d{2}:\d{2}$/);
});
