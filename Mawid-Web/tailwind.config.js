/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        primary: '#1A73E8',
        sidebar: '#1E293B',
      },
      keyframes: {
        float: {
          '0%, 100%': { transform: 'translateY(0)' },
          '50%': { transform: 'translateY(-10px)' },
        },
        floatSlow: {
          '0%, 100%': { transform: 'translateY(0) translateX(0)' },
          '33%': { transform: 'translateY(-6px) translateX(4px)' },
          '66%': { transform: 'translateY(4px) translateX(-3px)' },
        },
        slideInFromEnd: {
          from: { opacity: '0', transform: 'translateX(48px)' },
          to: { opacity: '1', transform: 'translateX(0)' },
        },
        fadeInUp: {
          from: { opacity: '0', transform: 'translateY(28px)' },
          to: { opacity: '1', transform: 'translateY(0)' },
        },
        pulseGlow: {
          '0%, 100%': {
            boxShadow: '0 0 24px rgba(26, 115, 232, 0.35), 0 0 60px rgba(26, 115, 232, 0.15)',
          },
          '50%': {
            boxShadow: '0 0 40px rgba(26, 115, 232, 0.55), 0 0 80px rgba(26, 115, 232, 0.25)',
          },
        },
        rotateCross: {
          from: { transform: 'rotate(0deg)' },
          to: { transform: 'rotate(360deg)' },
        },
        slideStat: {
          '0%, 100%': { transform: 'translateX(0)' },
          '50%': { transform: 'translateX(-8px)' },
        },
        bounceSoft: {
          '0%, 100%': { transform: 'translateY(0)' },
          '50%': { transform: 'translateY(-6px)' },
        },
        errorSlide: {
          from: { opacity: '0', transform: 'translateY(-8px)' },
          to: { opacity: '1', transform: 'translateY(0)' },
        },
        checkPop: {
          from: { opacity: '0', transform: 'scale(0.5)' },
          to: { opacity: '1', transform: 'scale(1)' },
        },
        gridMove: {
          from: { backgroundPosition: '0 0' },
          to: { backgroundPosition: '24px 24px' },
        },
        rowEnter: {
          from: { opacity: '0', transform: 'translateY(6px)' },
          to: { opacity: '1', transform: 'translateY(0)' },
        },
        skeletonShimmer: {
          '0%': { backgroundPosition: '-200% 0' },
          '100%': { backgroundPosition: '200% 0' },
        },
        toastSlide: {
          from: { opacity: '0', transform: 'translateY(-100%)' },
          to: { opacity: '1', transform: 'translateY(0)' },
        },
        flashBlue: {
          '0%, 100%': { boxShadow: '0 0 0 0 rgba(26, 115, 232, 0)' },
          '50%': { boxShadow: '0 0 0 6px rgba(26, 115, 232, 0.25)' },
        },
        floatBlob: {
          '0%, 100%': { transform: 'translate(0, 0) scale(1)' },
          '33%': { transform: 'translate(8px, -12px) scale(1.05)' },
          '66%': { transform: 'translate(-6px, 8px) scale(0.98)' },
        },
        subtitlePulse: {
          '0%, 100%': { opacity: '0.85' },
          '50%': { opacity: '1' },
        },
      },
      animation: {
        float: 'float 5s ease-in-out infinite',
        'float-slow': 'floatSlow 8s ease-in-out infinite',
        'slide-in-panel': 'slideInFromEnd 0.7s ease-out forwards',
        'fade-in-up': 'fadeInUp 0.8s ease-out forwards',
        'pulse-glow': 'pulseGlow 3s ease-in-out infinite',
        'rotate-cross': 'rotateCross 20s linear infinite',
        'slide-stat': 'slideStat 4s ease-in-out infinite',
        'bounce-soft': 'bounceSoft 3s ease-in-out infinite',
        'error-slide': 'errorSlide 0.35s ease-out forwards',
        'check-pop': 'checkPop 0.45s ease-out forwards',
        'grid-shift': 'gridMove 20s linear infinite',
        'row-enter': 'rowEnter 0.35s ease-out forwards',
        'skeleton-shimmer': 'skeletonShimmer 1.2s ease-in-out infinite',
        'toast-slide': 'toastSlide 0.45s cubic-bezier(0.22, 1, 0.36, 1) forwards',
        'flash-blue': 'flashBlue 0.6s ease-out',
        'float-blob': 'floatBlob 12s ease-in-out infinite',
        'subtitle-pulse': 'subtitlePulse 3s ease-in-out infinite',
      },
    },
  },
  plugins: [],
}
