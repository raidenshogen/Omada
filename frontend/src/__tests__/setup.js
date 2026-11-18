import '@testing-library/jest-dom'
Object.defineProperty(window, 'localStorage', {
  value: {
    getItem: () => null,
    setItem: () => {},
    removeItem: () => {},
    clear: () => {}
  },
  writable: true
})
Object.defineProperty(window, 'scrollTo', {
  value: () => {},
  writable: true
})
