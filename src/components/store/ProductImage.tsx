import React, {useState} from 'react';
import {Monitor, Headphones, Keyboard} from 'lucide-react';

interface ProductImageProps {
  src: string;
  alt: string;
  categoryName?: string;
  className?: string;
}

export const ProductImage: React.FC<ProductImageProps> = ({
  src,
  alt,
  categoryName = '',
  className = 'h-full w-full object-cover',
}) => {
  const [failed, setFailed] = useState(false);

  if (failed || !src) {
    const lower = categoryName.toLowerCase();
    const Icon =
      lower.includes('monitor') || lower.includes('webcam') || lower.includes('controller')
        ? Monitor
        : lower.includes('headset') || lower.includes('speaker') || lower.includes('micro')
          ? Headphones
          : Keyboard;
    return (
      <div className="flex h-full w-full flex-col items-center justify-center bg-gradient-to-br from-slate-800 to-slate-900 p-4 text-center">
        <Icon className="h-10 w-10 text-slate-400" aria-hidden="true" />
        <p className="mt-2 line-clamp-2 text-xs font-medium text-slate-300">{alt}</p>
      </div>
    );
  }

  return (
    <img
      src={src}
      alt={alt}
      referrerPolicy="no-referrer"
      onError={() => setFailed(true)}
      className={className}
    />
  );
};
