import './index.css'
import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { AuthProvider } from './hooks/useAuth'
import App from './App.jsx'
import ErrorBoundary from './components/ErrorBoundary'

const el = document.getElementById('root')
if (!el) {
  throw new Error('عنصر #root غير موجود في index.html')
}

el.innerHTML = ''

ReactDOM.createRoot(el).render(
  <React.StrictMode>
    <ErrorBoundary>
      <AuthProvider>
        <BrowserRouter
          future={{
            v7_startTransition: true,
            v7_relativeSplatPath: true,
          }}
        >
          <App />
        </BrowserRouter>
      </AuthProvider>
    </ErrorBoundary>
  </React.StrictMode>,
)
