export default {
  content: ["./index.html", "./src/**/*.{js,jsx}"],
  theme: {
    extend: {
      colors: {
        // Core palette
        paper: '#F1F2FF',
        surface: '#FFFFFF',
        ink: { DEFAULT: '#1B1D3A', soft: '#5B6085', faint: '#8E92B4' },
        line: '#E0E2F4',
        signal: '#5146E1',
        // People: every teammate gets one of these, everywhere they appear
        person: {
          coral: '#E4572E', amber: '#D99A00', leaf: '#3E9B4F', teal: '#0F9AA8',
          blue: '#2F6FDE', violet: '#7B52D3', magenta: '#C2409B', slate: '#5B6B7A',
        },
        // Legacy tokens kept so pages not yet redesigned inherit the new palette
        background: '#F1F2FF',
        card: '#FFFFFF',
        border: '#E0E2F4',
        input: '#D0D4EE',
        muted: '#5B6085',
        accent: '#5146E1',
        brand: {
          50: '#EEEDFF', 100: '#DAD8FF', 200: '#B4B0F7', 500: '#7068EB',
          600: '#5146E1', 700: '#4036C2', 900: '#2B2480',
        },
      },
      borderRadius: { sm: '4px', DEFAULT: '6px', md: '8px', lg: '10px', xl: '12px', '2xl': '14px', full: '9999px' },
      boxShadow: {
        soft: 'none',
        card: '0 1px 0 0 rgb(20 24 28 / 0.04)',
        'card-hover': '0 6px 18px -8px rgb(20 24 28 / 0.25)',
        sidebar: 'none',
      },
      fontFamily: {
        sans: ['"Instrument Sans"', 'ui-sans-serif', 'system-ui', 'sans-serif'],
        serif: ['"Source Serif 4"', 'Georgia', 'serif'],
        mono: ['ui-monospace', 'monospace'],
      },
    },
    screens: { sm: '640px', md: '768px', lg: '1024px', xl: '1280px', '2xl': '1536px' },
  },
  plugins: [],
}