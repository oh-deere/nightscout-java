import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { CssBaseline, ThemeProvider } from '@mui/material'
import { MutationCache, QueryCache, QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ApiError } from './api/client'
import { theme } from './theme/theme'
import { AppShell } from './components/AppShell'
import { ViewSettingsProvider } from './hooks/useViewSettings'
import './i18n'

// Any 401 on any query (entries / treatments / properties / agp / admin …) is
// the signal that our auth context has gone away — session expired, api-secret
// rotated, OAuth token revoked. Re-poll verifyauth so AppShell's dialog logic
// picks it up immediately instead of waiting for the next stale-time tick.
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: false,
      refetchOnWindowFocus: true,
    },
  },
  queryCache: new QueryCache({
    onError: (error, query) => {
      if (error instanceof ApiError && error.status === 401) {
        // Don't recursively invalidate the trigger — verifyauth's own 401
        // is what drives the dialog via its useQuery result.
        if (query.queryKey[0] !== 'verifyauth') {
          void queryClient.invalidateQueries({ queryKey: ['verifyauth'] })
        }
      }
    },
  }),
  mutationCache: new MutationCache({
    onError: (error) => {
      if (error instanceof ApiError && error.status === 401) {
        void queryClient.invalidateQueries({ queryKey: ['verifyauth'] })
      }
    },
  }),
})

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <QueryClientProvider client={queryClient}>
        <ViewSettingsProvider>
          <AppShell />
        </ViewSettingsProvider>
      </QueryClientProvider>
    </ThemeProvider>
  </StrictMode>,
)

// Register service worker for PWA install + offline shell
if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/sw.js').catch((err) => {
      console.warn('Service worker registration failed', err)
    })
  })
}
