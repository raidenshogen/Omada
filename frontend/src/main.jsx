import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import App from './App.jsx'
import './index.css'

// SockJS polyfill: browser environments lack Node's `global` object
if (typeof window !== 'undefined' && typeof window.global === 'undefined') {
  window.global = window
}

ReactDOM.createRoot(document.getElementById('root')).render(
  <BrowserRouter>
    <App />
  </BrowserRouter>
)
