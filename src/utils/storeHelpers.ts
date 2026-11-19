import type {Customer, DiscountType, Product} from '../types';

export const STORE_IMAGES = {
  hero: '/src/assets/images/hero_gaming_setup_1795065890256.jpg',
  keyboardMouse: '/src/assets/images/gear_keyboard_mouse_1795065889406.jpg',
  audioHeadset: '/src/assets/images/gear_audio_headset_1795065890324.jpg',
  monitorController: '/src/assets/images/gear_monitor_controller_1795065890684.jpg',
} as const;

export function resolveProductImage(product: Pick<Product, 'categoryId' | 'imagePath'>, categoryName = ''): string {
  const path = (product.imagePath || '').trim();
  if (path.startsWith('/src/assets/') || path.startsWith('http://') || path.startsWith('https://')) {
    return path;
  }
  const cat = `${product.categoryId} ${categoryName}`.toLowerCase();
  if (cat.includes('cat-03') || cat.includes('cat-06') || cat.includes('cat-07') || cat.includes('monitor') || cat.includes('webcam') || cat.includes('controller')) {
    return STORE_IMAGES.monitorController;
  }
  if (cat.includes('cat-04') || cat.includes('cat-05') || cat.includes('cat-08') || cat.includes('headset') || cat.includes('speaker') || cat.includes('microphone')) {
    return STORE_IMAGES.audioHeadset;
  }
  return STORE_IMAGES.keyboardMouse;
}

export function isProductPurchasable(product: Pick<Product, 'status' | 'stockQuantity'>): boolean {
  return product.status === 'ACTIVE' && product.stockQuantity > 0;
}

export function getAvailableDiscountTypes(customer: Pick<Customer, 'membershipTier'> | null): DiscountType[] {
  if (customer?.membershipTier === 'VIP') {
    return ['VIP', 'SEASONAL', 'NO_DISCOUNT'];
  }
  return ['SEASONAL', 'NO_DISCOUNT'];
}

export function getDefaultDiscountForCustomer(customer: Pick<Customer, 'membershipTier'> | null): DiscountType {
  return customer?.membershipTier === 'VIP' ? 'VIP' : 'NO_DISCOUNT';
}

export function clampCartQuantity(desired: number, stockQuantity: number): number {
  if (!Number.isFinite(desired) || !Number.isFinite(stockQuantity) || stockQuantity <= 0) {
    return 0;
  }
  const rounded = Math.floor(desired);
  if (rounded < 1) return 1;
  return Math.min(rounded, stockQuantity);
}

export function calculateCartCount(items: ReadonlyArray<{quantity: number}>): number {
  return items.reduce((sum, item) => sum + Math.max(0, Math.floor(item.quantity || 0)), 0);
}
