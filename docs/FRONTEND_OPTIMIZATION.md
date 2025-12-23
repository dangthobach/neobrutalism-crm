# Frontend Performance Optimization Guide

## 🎯 Overview

This guide covers performance optimization strategies for the Neobrutalism CRM frontend built with Next.js 16 and React 19.

**Current Stack:**
- Next.js 16.0.4 (App Router)
- React 19.0.0
- TypeScript 5.1.6
- TailwindCSS 4.0.9
- Radix UI components

---

## ✅ Implemented Optimizations

### 1. Next.js Configuration

#### SWC Minification
```javascript
swcMinify: true
```
- Uses Rust-based SWC compiler
- 17x faster than Terser
- Smaller bundle sizes

#### Image Optimization
```javascript
images: {
  formats: ['image/avif', 'image/webp'],
  minimumCacheTTL: 60,
}
```
- Automatic format conversion (AVIF, WebP)
- Responsive images with srcset
- Lazy loading by default

#### Modularized Imports
```javascript
modularizeImports: {
  'lucide-react': {
    transform: 'lucide-react/dist/esm/icons/{{kebabCase member}}',
  },
}
```
- Tree-shaking for icon libraries
- Import only used components
- Reduces bundle size significantly

#### CSS Optimization
```javascript
experimental: {
  optimizeCss: true,
  optimizePackageImports: ['lucide-react', '@radix-ui/react-icons', 'date-fns'],
}
```
- Automatic CSS minification
- Remove unused CSS
- Optimize package imports

---

## 📦 Bundle Size Optimization

### Install Bundle Analyzer

```bash
npm install --save-dev @next/bundle-analyzer
```

### Create bundle-analyzer.config.mjs

```javascript
import withBundleAnalyzer from '@next/bundle-analyzer'
import baseConfig from './next.config.mjs'

const bundleAnalyzer = withBundleAnalyzer({
  enabled: process.env.ANALYZE === 'true',
})

export default bundleAnalyzer(baseConfig)
```

### Analyze Bundle

```bash
# Analyze production bundle
ANALYZE=true npm run build

# This will open browser with bundle visualization
```

### Target Bundle Sizes

| Metric | Target | Current | Status |
|--------|--------|---------|--------|
| First Load JS | < 200 KB | TBD | 🔍 |
| Route JS | < 50 KB | TBD | 🔍 |
| Total Bundle | < 1 MB | TBD | 🔍 |

---

## 🚀 Code Splitting Strategies

### 1. Dynamic Imports for Heavy Components

```typescript
// ❌ Bad: Import everything upfront
import { Chart } from '@/components/Chart'
import { DataTable } from '@/components/DataTable'

// ✅ Good: Lazy load heavy components
const Chart = dynamic(() => import('@/components/Chart'), {
  loading: () => <ChartSkeleton />,
  ssr: false // Disable SSR for client-only components
})

const DataTable = dynamic(() => import('@/components/DataTable'))
```

### 2. Route-based Code Splitting

```typescript
// app/dashboard/page.tsx - Automatically code-split
export default function DashboardPage() {
  return <Dashboard />
}

// app/admin/page.tsx - Separate bundle
export default function AdminPage() {
  return <AdminPanel />
}
```

### 3. Conditional Loading

```typescript
// Only load admin features for admin users
const AdminPanel = user.isAdmin
  ? dynamic(() => import('@/components/AdminPanel'))
  : () => null
```

---

## 🎨 CSS Optimization

### TailwindCSS Configuration

```javascript
// tailwind.config.js
module.exports = {
  content: [
    './src/**/*.{js,ts,jsx,tsx,mdx}',
  ],
  theme: {
    extend: {},
  },
  plugins: [],
  // ✅ Purge unused CSS in production
  purge: {
    enabled: process.env.NODE_ENV === 'production',
    content: ['./src/**/*.{js,ts,jsx,tsx}'],
  },
}
```

### CSS Modules

```typescript
// Use CSS Modules for component-specific styles
import styles from './Button.module.css'

export function Button() {
  return <button className={styles.button}>Click me</button>
}
```

---

## ⚡ React Performance

### 1. Memoization

```typescript
// ✅ Memoize expensive computations
const sortedData = useMemo(
  () => data.sort((a, b) => a.name.localeCompare(b.name)),
  [data]
)

// ✅ Memoize callbacks
const handleClick = useCallback(() => {
  console.log('Clicked')
}, [])

// ✅ Memoize components
const MemoizedTable = memo(DataTable)
```

### 2. Virtual Scrolling

```typescript
// For long lists, use virtual scrolling
import { FixedSizeList } from 'react-window'

function VirtualList({ items }) {
  return (
    <FixedSizeList
      height={600}
      itemCount={items.length}
      itemSize={50}
      width="100%"
    >
      {({ index, style }) => (
        <div style={style}>{items[index].name}</div>
      )}
    </FixedSizeList>
  )
}
```

### 3. Pagination vs Infinite Scroll

```typescript
// ✅ Pagination for better performance
function CustomerList() {
  const [page, setPage] = useState(0)
  const { data } = useQuery(['customers', page], () =>
    fetchCustomers({ page, size: 20 })
  )

  return (
    <>
      <Table data={data.content} />
      <Pagination
        page={page}
        totalPages={data.totalPages}
        onPageChange={setPage}
      />
    </>
  )
}
```

---

## 📊 Data Fetching Optimization

### React Query Configuration

```typescript
// lib/react-query.ts
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000, // 5 minutes
      cacheTime: 10 * 60 * 1000, // 10 minutes
      refetchOnWindowFocus: false,
      retry: 1,
    },
  },
})
```

### Prefetching

```typescript
// Prefetch data on hover
function CustomerCard({ customerId }) {
  const queryClient = useQueryClient()

  const handleMouseEnter = () => {
    queryClient.prefetchQuery(
      ['customer', customerId],
      () => fetchCustomer(customerId)
    )
  }

  return <div onMouseEnter={handleMouseEnter}>...</div>
}
```

### Parallel Queries

```typescript
// ✅ Fetch multiple resources in parallel
function Dashboard() {
  const [users, customers, tasks] = useQueries([
    { queryKey: ['users'], queryFn: fetchUsers },
    { queryKey: ['customers'], queryFn: fetchCustomers },
    { queryKey: ['tasks'], queryFn: fetchTasks },
  ])

  return <DashboardView data={{ users, customers, tasks }} />
}
```

---

## 🖼️ Image Optimization

### Next.js Image Component

```typescript
import Image from 'next/image'

// ✅ Optimized image loading
<Image
  src="/logo.png"
  alt="Logo"
  width={200}
  height={100}
  priority // Load immediately (above the fold)
  placeholder="blur" // Show blur while loading
  blurDataURL="data:image/..." // Optional: custom blur
/>

// ✅ Responsive images
<Image
  src="/hero.jpg"
  alt="Hero"
  fill
  style={{ objectFit: 'cover' }}
  sizes="(max-width: 768px) 100vw, (max-width: 1200px) 50vw, 33vw"
/>
```

### Image CDN

```javascript
// next.config.mjs
images: {
  domains: ['your-cdn.com'],
  loader: 'custom',
  loaderFile: './lib/imageLoader.js',
}
```

---

## 🔤 Font Optimization

### Next.js Font Optimization

```typescript
// app/layout.tsx
import { Inter } from 'next/font/google'

const inter = Inter({
  subsets: ['latin'],
  display: 'swap', // Use 'swap' for faster initial load
  preload: true,
  variable: '--font-inter',
})

export default function RootLayout({ children }) {
  return (
    <html lang="en" className={inter.variable}>
      <body>{children}</body>
    </html>
  )
}
```

---

## 📱 Progressive Web App (PWA)

### Install next-pwa

```bash
npm install next-pwa
```

### Configure PWA

```javascript
// next.config.mjs
import withPWA from 'next-pwa'

const pwaConfig = withPWA({
  dest: 'public',
  register: true,
  skipWaiting: true,
  disable: process.env.NODE_ENV === 'development',
})

export default pwaConfig(nextConfig)
```

---

## 🎯 Core Web Vitals Targets

| Metric | Target | Description |
|--------|--------|-------------|
| **LCP** (Largest Contentful Paint) | < 2.5s | Time to render largest content |
| **FID** (First Input Delay) | < 100ms | Time from user interaction to browser response |
| **CLS** (Cumulative Layout Shift) | < 0.1 | Visual stability score |
| **FCP** (First Contentful Paint) | < 1.8s | Time to render first content |
| **TTI** (Time to Interactive) | < 3.8s | Time until page is fully interactive |

---

## 📈 Performance Monitoring

### Next.js Analytics

```typescript
// app/layout.tsx
import { Analytics } from '@vercel/analytics/react'

export default function RootLayout({ children }) {
  return (
    <html>
      <body>
        {children}
        <Analytics />
      </body>
    </html>
  )
}
```

### Custom Performance Tracking

```typescript
// lib/analytics.ts
export function trackWebVitals(metric) {
  const { name, value, id } = metric

  // Send to analytics
  gtag('event', name, {
    value: Math.round(name === 'CLS' ? value * 1000 : value),
    event_label: id,
    non_interaction: true,
  })
}
```

```typescript
// app/layout.tsx
export function reportWebVitals(metric) {
  trackWebVitals(metric)
}
```

---

## 🛠️ Development Tools

### Performance Profiling

```bash
# Chrome DevTools Performance tab
# React DevTools Profiler
# Lighthouse CI
```

### Bundle Analysis

```bash
# Generate bundle report
ANALYZE=true npm run build

# Check bundle sizes
npm run build --profile

# Lighthouse score
npx lighthouse https://your-app.com --view
```

---

## ✅ Performance Checklist

### Before Production

- [ ] Run bundle analyzer - ensure no duplicate dependencies
- [ ] Check Core Web Vitals - all metrics in green
- [ ] Test on 3G connection - acceptable load times
- [ ] Enable compression - Gzip/Brotli
- [ ] Configure CDN - static assets served from CDN
- [ ] Enable caching headers - aggressive caching for static assets
- [ ] Lazy load non-critical components
- [ ] Prefetch critical routes
- [ ] Optimize images - WebP/AVIF formats
- [ ] Tree-shake unused code
- [ ] Minimize third-party scripts
- [ ] Enable service worker - offline support
- [ ] Test on real devices - iOS, Android, Desktop

---

## 🎓 Best Practices

1. **Measure First**: Use Lighthouse and bundle analyzer before optimizing
2. **Progressive Enhancement**: Start with basic functionality, enhance progressively
3. **Lazy Load**: Load code when needed, not upfront
4. **Cache Aggressively**: Use React Query, browser cache, CDN cache
5. **Optimize Images**: Use Next.js Image component
6. **Code Split**: Split by route and component
7. **Memoize**: Use memo, useMemo, useCallback appropriately
8. **Virtual Scrolling**: For long lists (100+ items)
9. **Monitor**: Track Core Web Vitals in production
10. **Test**: Test on real devices and slow connections

---

## 📚 Resources

- [Next.js Performance](https://nextjs.org/docs/pages/building-your-application/optimizing)
- [React Performance](https://react.dev/learn/render-and-commit)
- [Web Vitals](https://web.dev/vitals/)
- [Lighthouse](https://developers.google.com/web/tools/lighthouse)
- [Bundle Phobia](https://bundlephobia.com/)

---

**Last Updated**: December 2025
**Framework**: Next.js 16.0.4
**React**: 19.0.0
