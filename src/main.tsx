import React from 'react';
import ReactDOM from 'react-dom/client';
import { App } from './App';
import './index.css';

// Global API Interceptor for JWT Authorization Bearer Token
const originalFetch = window.fetch;
window.fetch = async (input, init) => {
  const token = localStorage.getItem('trace_access_token');
  if (token && typeof input === 'string' && input.startsWith('/api')) {
    init = init || {};
    init.headers = {
      ...init.headers,
      'Authorization': `Bearer ${token}`
    };
  }
  return originalFetch(input, init);
};

ReactDOM.createRoot(document.getElementById('root') as HTMLElement).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
