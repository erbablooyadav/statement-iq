import axios from 'axios';

const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1';

export const apiClient = axios.create({
  baseURL: BASE_URL.endsWith('/api/v1') ? BASE_URL : `${BASE_URL}/api/v1`,
  headers: {
    'Content-Type': 'application/json',
  },
});

// JWT interceptor — attaches Firebase token to every request
let getTokenFn: (() => Promise<string | null>) | null = null;

export function setTokenProvider(fn: () => Promise<string | null>) {
  getTokenFn = fn;
}

apiClient.interceptors.request.use(async (config) => {
  if (getTokenFn && !config.headers.Authorization) {
    const token = await getTokenFn();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Token expired — redirect to auth
      window.location.href = '/auth';
    }
    return Promise.reject(error);
  }
);
