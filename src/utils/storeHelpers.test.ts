import test from 'node:test';
import assert from 'node:assert/strict';
import {
  calculateCartCount,
  clampCartQuantity,
  getAvailableDiscountTypes,
  getDefaultDiscountForCustomer,
  getUserInitials,
  isAdminStaffRole,
  isCustomerPurchaseEligible,
  isProductPurchasable,
  resolveAutomaticDiscount,
  resolvePostLoginPortal,
  resolveProductImage,
  STORE_IMAGES,
} from './storeHelpers';

test('resolveProductImage maps gaming gear categories to studio assets and preserves explicit URLs', () => {
  assert.equal(
    resolveProductImage({categoryId: 'CAT-01', imagePath: 'assets/products/wooting60he.png'}, 'Keyboard'),
    STORE_IMAGES.keyboardMouse,
  );
  assert.equal(
    resolveProductImage({categoryId: 'CAT-04', imagePath: 'assets/products/hyperx.png'}, 'Headset'),
    STORE_IMAGES.audioHeadset,
  );
  assert.equal(
    resolveProductImage({categoryId: 'CAT-03', imagePath: 'assets/products/lg.png'}, 'Monitor'),
    STORE_IMAGES.monitorController,
  );
  assert.equal(
    resolveProductImage({categoryId: 'CAT-01', imagePath: 'https://cdn.example.com/kb.jpg'}),
    'https://cdn.example.com/kb.jpg',
  );
});

test('isProductPurchasable requires ACTIVE status and positive stock', () => {
  assert.equal(isProductPurchasable({status: 'ACTIVE', stockQuantity: 5}), true);
  assert.equal(isProductPurchasable({status: 'ACTIVE', stockQuantity: 0}), false);
  assert.equal(isProductPurchasable({status: 'OUT_OF_STOCK', stockQuantity: 0}), false);
  assert.equal(isProductPurchasable({status: 'INACTIVE', stockQuantity: 10}), false);
});

test('discount eligibility and automatic Admin-controlled policy behave deterministically', () => {
  assert.deepEqual(getAvailableDiscountTypes({membershipTier: 'VIP'}), ['VIP', 'SEASONAL', 'NO_DISCOUNT']);
  assert.deepEqual(getAvailableDiscountTypes({membershipTier: 'NORMAL'}), ['SEASONAL', 'NO_DISCOUNT']);
  assert.deepEqual(getAvailableDiscountTypes(null), ['SEASONAL', 'NO_DISCOUNT']);

  // Seasonal OFF -> VIP gets VIP, NORMAL gets NO_DISCOUNT
  assert.equal(resolveAutomaticDiscount({membershipTier: 'VIP'}, {seasonalPromoEnabled: false}), 'VIP');
  assert.equal(resolveAutomaticDiscount({membershipTier: 'NORMAL'}, {seasonalPromoEnabled: false}), 'NO_DISCOUNT');
  assert.equal(getDefaultDiscountForCustomer({membershipTier: 'VIP'}), 'VIP');
  assert.equal(getDefaultDiscountForCustomer({membershipTier: 'NORMAL'}), 'NO_DISCOUNT');

  // Seasonal ON -> ALL accounts (VIP and NORMAL) get SEASONAL
  assert.equal(resolveAutomaticDiscount({membershipTier: 'VIP'}, {seasonalPromoEnabled: true}), 'SEASONAL');
  assert.equal(resolveAutomaticDiscount({membershipTier: 'NORMAL'}, {seasonalPromoEnabled: true}), 'SEASONAL');
});

test('cart quantity clamping and item count calculation behave deterministically', () => {
  assert.equal(clampCartQuantity(0, 5), 1);
  assert.equal(clampCartQuantity(3, 5), 3);
  assert.equal(clampCartQuantity(99, 5), 5);
  assert.equal(clampCartQuantity(2, 0), 0);
  assert.equal(calculateCartCount([{quantity: 2}, {quantity: 3}]), 5);
  assert.equal(calculateCartCount([]), 0);
});

test('post-login portal routing separates ADMIN/STAFF from CUSTOMER accounts', () => {
  assert.equal(isAdminStaffRole({role: 'ADMIN'}), true);
  assert.equal(isAdminStaffRole({role: 'STAFF'}), true);
  assert.equal(isAdminStaffRole({role: 'CUSTOMER'}), false);
  assert.equal(resolvePostLoginPortal({role: 'ADMIN'}), 'ADMIN');
  assert.equal(resolvePostLoginPortal({role: 'STAFF'}), 'ADMIN');
  assert.equal(resolvePostLoginPortal({role: 'CUSTOMER'}), 'STORE');
});

test('getUserInitials formats initials cleanly for avatar badges', () => {
  assert.equal(getUserInitials('Phạm Gia Huy'), 'PH');
  assert.equal(getUserInitials('Admin'), 'AD');
  assert.equal(getUserInitials(''), 'GG');
});

test('customer purchase gate requires active CUSTOMER account linked to complete customer profile', () => {
  const validCustomer = {
    id: 'CUS-001',
    fullName: 'Phạm Gia Huy',
    phone: '0901234567',
    email: 'huy@gmail.com',
    address: '12 Nguyễn Huệ, TP.HCM',
  };
  assert.equal(
    isCustomerPurchaseEligible(
      {role: 'CUSTOMER', customerId: 'CUS-001', active: true},
      validCustomer,
    ),
    true,
  );
  assert.equal(isCustomerPurchaseEligible(null, validCustomer), false);
  assert.equal(
    isCustomerPurchaseEligible({role: 'ADMIN', customerId: null, active: true}, validCustomer),
    false,
  );
  assert.equal(
    isCustomerPurchaseEligible(
      {role: 'CUSTOMER', customerId: 'CUS-001', active: true},
      {...validCustomer, address: '   '},
    ),
    false,
  );
});
