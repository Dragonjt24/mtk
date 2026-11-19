import test from 'node:test';
import assert from 'node:assert/strict';
import {
  calculateCartCount,
  clampCartQuantity,
  getAvailableDiscountTypes,
  getDefaultDiscountForCustomer,
  isProductPurchasable,
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

test('discount eligibility respects Java VIP rule', () => {
  assert.deepEqual(getAvailableDiscountTypes({membershipTier: 'VIP'}), ['VIP', 'SEASONAL', 'NO_DISCOUNT']);
  assert.deepEqual(getAvailableDiscountTypes({membershipTier: 'NORMAL'}), ['SEASONAL', 'NO_DISCOUNT']);
  assert.deepEqual(getAvailableDiscountTypes(null), ['SEASONAL', 'NO_DISCOUNT']);
  assert.equal(getDefaultDiscountForCustomer({membershipTier: 'VIP'}), 'VIP');
  assert.equal(getDefaultDiscountForCustomer({membershipTier: 'NORMAL'}), 'NO_DISCOUNT');
});

test('cart quantity clamping and item count calculation behave deterministically', () => {
  assert.equal(clampCartQuantity(0, 5), 1);
  assert.equal(clampCartQuantity(3, 5), 3);
  assert.equal(clampCartQuantity(99, 5), 5);
  assert.equal(clampCartQuantity(2, 0), 0);
  assert.equal(calculateCartCount([{quantity: 2}, {quantity: 3}]), 5);
  assert.equal(calculateCartCount([]), 0);
});
