# 🚀 Quick Start Guide - Neo-Brutalist CRM

Get your Neo-Brutalist CRM up and running in 15 minutes!

## ⚡ Prerequisites

- Node.js 18+
- Java 21+
- PostgreSQL 15+
- Redis 7+
- Docker (optional but recommended)

## 🏃‍♂️ Quick Setup

### 1. Clone & Setup Frontend

```bash
# Create new Next.js project with brutalist setup
npx create-next-app@latest neo-brutalist-crm --typescript --tailwind --app
cd neo-brutalist-crm

# Install dependencies
pnpm add @radix-ui/react-accordion @radix-ui/react-alert-dialog @radix-ui/react-avatar @radix-ui/react-checkbox @radix-ui/react-dialog @radix-ui/react-dropdown-menu @radix-ui/react-label @radix-ui/react-popover @radix-ui/react-select @radix-ui/react-separator @radix-ui/react-slot @radix-ui/react-switch @radix-ui/react-tabs @radix-ui/react-toast @radix-ui/react-tooltip
pnpm add lucide-react framer-motion zustand @tanstack/react-query react-hook-form @hookform/resolvers zod recharts clsx tailwind-merge class-variance-authority

# Install dev dependencies
pnpm add -D @types/node @types/react @types/react-dom eslint eslint-config-next prettier prettier-plugin-tailwindcss
```

### 2. Configure Tailwind for Brutalism

```javascript
// tailwind.config.js
/** @type {import('tailwindcss').Config} */
module.exports = {
  darkMode: ["class"],
  content: [
    './pages/**/*.{ts,tsx}',
    './components/**/*.{ts,tsx}',
    './app/**/*.{ts,tsx}',
    './src/**/*.{ts,tsx}',
  ],
  theme: {
    container: {
      center: true,
      padding: "2rem",
      screens: {
        "2xl": "1400px",
      },
    },
    extend: {
      colors: {
        // Neo-Brutalist Color Palette
        'neon-green': '#39FF14',
        'electric-blue': '#00A3FF',
        'beige': '#F2EFE6',
        'concrete': '#E5E5E5',
        'brutal-black': '#000000',
        'brutal-white': '#FFFFFF',
        border: "hsl(var(--border))",
        input: "hsl(var(--input))",
        ring: "hsl(var(--ring))",
        background: "hsl(var(--background))",
        foreground: "hsl(var(--foreground))",
        primary: {
          DEFAULT: "hsl(var(--primary))",
          foreground: "hsl(var(--primary-foreground))",
        },
        secondary: {
          DEFAULT: "hsl(var(--secondary))",
          foreground: "hsl(var(--secondary-foreground))",
        },
        destructive: {
          DEFAULT: "hsl(var(--destructive))",
          foreground: "hsl(var(--destructive-foreground))",
        },
        muted: {
          DEFAULT: "hsl(var(--muted))",
          foreground: "hsl(var(--muted-foreground))",
        },
        accent: {
          DEFAULT: "hsl(var(--accent))",
          foreground: "hsl(var(--accent-foreground))",
        },
        popover: {
          DEFAULT: "hsl(var(--popover))",
          foreground: "hsl(var(--popover-foreground))",
        },
        card: {
          DEFAULT: "hsl(var(--card))",
          foreground: "hsl(var(--card-foreground))",
        },
      },
      fontFamily: {
        'display': ['Space Grotesk', 'sans-serif'],
        'body': ['Inter', 'sans-serif'],
      },
      boxShadow: {
        'brutal': '8px 8px 0px #000000',
        'brutal-lg': '12px 12px 0px #000000',
        'brutal-sm': '4px 4px 0px #000000',
      },
      borderRadius: {
        lg: "var(--radius)",
        md: "calc(var(--radius) - 2px)",
        sm: "calc(var(--radius) - 4px)",
      },
      keyframes: {
        "accordion-down": {
          from: { height: 0 },
          to: { height: "var(--radix-accordion-content-height)" },
        },
        "accordion-up": {
          from: { height: "var(--radix-accordion-content-height)" },
          to: { height: 0 },
        },
      },
      animation: {
        "accordion-down": "accordion-down 0.2s ease-out",
        "accordion-up": "accordion-up 0.2s ease-out",
      },
    },
  },
  plugins: [require("tailwindcss-animate")],
}
```

### 3. Create Brutalist Components

```typescript
// lib/utils.ts
import { type ClassValue, clsx } from "clsx"
import { twMerge } from "tailwind-merge"

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

// components/ui/button.tsx
import * as React from "react"
import { Slot } from "@radix-ui/react-slot"
import { cva, type VariantProps } from "class-variance-authority"
import { cn } from "@/lib/utils"

const buttonVariants = cva(
  "inline-flex items-center justify-center whitespace-nowrap rounded-md text-sm font-medium ring-offset-background transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50",
  {
    variants: {
      variant: {
        default: "bg-primary text-primary-foreground hover:bg-primary/90",
        destructive:
          "bg-destructive text-destructive-foreground hover:bg-destructive/90",
        outline:
          "border border-input bg-background hover:bg-accent hover:text-accent-foreground",
        secondary:
          "bg-secondary text-secondary-foreground hover:bg-secondary/80",
        ghost: "hover:bg-accent hover:text-accent-foreground",
        link: "text-primary underline-offset-4 hover:underline",
        // Brutalist variants
        brutal: "border-4 border-brutal-black bg-neon-green text-brutal-black shadow-brutal hover:translate-x-1 hover:translate-y-1 hover:shadow-none font-display font-bold",
        "brutal-secondary": "border-4 border-brutal-black bg-brutal-white text-brutal-black shadow-brutal hover:bg-concrete font-display font-bold",
        "brutal-destructive": "border-4 border-brutal-black bg-red-500 text-brutal-white shadow-brutal hover:bg-red-600 font-display font-bold",
      },
      size: {
        default: "h-10 px-4 py-2",
        sm: "h-9 rounded-md px-3",
        lg: "h-11 rounded-md px-8",
        icon: "h-10 w-10",
        // Brutalist sizes
        brutal: "h-12 px-6 text-lg",
        "brutal-sm": "h-10 px-4 text-base",
        "brutal-lg": "h-16 px-8 text-xl",
      },
    },
    defaultVariants: {
      variant: "default",
      size: "default",
    },
  }
)

export interface ButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement>,
    VariantProps<typeof buttonVariants> {
  asChild?: boolean
}

const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant, size, asChild = false, ...props }, ref) => {
    const Comp = asChild ? Slot : "button"
    return (
      <Comp
        className={cn(buttonVariants({ variant, size, className }))}
        ref={ref}
        {...props}
      />
    )
  }
)
Button.displayName = "Button"

export { Button, buttonVariants }

// components/ui/card.tsx
import * as React from "react"
import { cn } from "@/lib/utils"

const Card = React.forwardRef<
  HTMLDivElement,
  React.HTMLAttributes<HTMLDivElement>
>(({ className, ...props }, ref) => (
  <div
    ref={ref}
    className={cn(
      "rounded-lg border bg-card text-card-foreground shadow-sm",
      className
    )}
    {...props}
  />
))
Card.displayName = "Card"

const CardHeader = React.forwardRef<
  HTMLDivElement,
  React.HTMLAttributes<HTMLDivElement>
>(({ className, ...props }, ref) => (
  <div
    ref={ref}
    className={cn("flex flex-col space-y-1.5 p-6", className)}
    {...props}
  />
))
CardHeader.displayName = "CardHeader"

const CardTitle = React.forwardRef<
  HTMLParagraphElement,
  React.HTMLAttributes<HTMLHeadingElement>
>(({ className, ...props }, ref) => (
  <h3
    ref={ref}
    className={cn(
      "text-2xl font-semibold leading-none tracking-tight",
      className
    )}
    {...props}
  />
))
CardTitle.displayName = "CardTitle"

const CardDescription = React.forwardRef<
  HTMLParagraphElement,
  React.HTMLAttributes<HTMLParagraphElement>
>(({ className, ...props }, ref) => (
  <p
    ref={ref}
    className={cn("text-sm text-muted-foreground", className)}
    {...props}
  />
))
CardDescription.displayName = "CardDescription"

const CardContent = React.forwardRef<
  HTMLDivElement,
  React.HTMLAttributes<HTMLDivElement>
>(({ className, ...props }, ref) => (
  <div ref={ref} className={cn("p-6 pt-0", className)} {...props} />
))
CardContent.displayName = "CardContent"

const CardFooter = React.forwardRef<
  HTMLDivElement,
  React.HTMLAttributes<HTMLDivElement>
>(({ className, ...props }, ref) => (
  <div
    ref={ref}
    className={cn("flex items-center p-6 pt-0", className)}
    {...props}
  />
))
CardFooter.displayName = "CardFooter"

// Brutalist Card variants
const BrutalCard = React.forwardRef<
  HTMLDivElement,
  React.HTMLAttributes<HTMLDivElement>
>(({ className, ...props }, ref) => (
  <div
    ref={ref}
    className={cn(
      "border-4 border-brutal-black bg-brutal-white shadow-brutal p-6",
      className
    )}
    {...props}
  />
))
BrutalCard.displayName = "BrutalCard"

export { Card, CardHeader, CardFooter, CardTitle, CardDescription, CardContent, BrutalCard }
```

### 4. Create Dashboard Layout

```typescript
// app/dashboard/page.tsx
import { Button } from "@/components/ui/button"
import { BrutalCard } from "@/components/ui/card"

export default function DashboardPage() {
  return (
    <div className="grid grid-cols-12 gap-4 p-6 bg-beige min-h-screen">
      {/* Sidebar */}
      <aside className="col-span-3">
        <BrutalCard className="h-full">
          <h2 className="text-2xl font-display font-bold mb-6">Navigation</h2>
          <nav className="space-y-4">
            <Button variant="brutal" className="w-full justify-start">
              Dashboard
            </Button>
            <Button variant="brutal-secondary" className="w-full justify-start">
              Clients
            </Button>
            <Button variant="brutal-secondary" className="w-full justify-start">
              Deals
            </Button>
            <Button variant="brutal-secondary" className="w-full justify-start">
              Tasks
            </Button>
          </nav>
        </BrutalCard>
      </aside>

      {/* Main Content */}
      <main className="col-span-9 space-y-6">
        {/* Header */}
        <header className="border-4 border-brutal-black bg-neon-green p-6">
          <h1 className="text-4xl font-display font-bold text-brutal-black">
            CRM Dashboard
          </h1>
        </header>

        {/* Dashboard Grid */}
        <div className="grid grid-cols-2 gap-6">
          <BrutalCard>
            <h2 className="text-2xl font-display font-bold mb-4">Tasks</h2>
            <div className="space-y-4">
              <div className="flex items-center gap-4 p-4 border-4 border-brutal-black bg-brutal-white">
                <input type="checkbox" className="w-6 h-6" />
                <span className="flex-1 font-body">Complete project proposal</span>
              </div>
              <div className="flex items-center gap-4 p-4 border-4 border-brutal-black bg-concrete">
                <input type="checkbox" className="w-6 h-6" checked />
                <span className="flex-1 font-body line-through opacity-60">
                  Review client requirements
                </span>
              </div>
            </div>
          </BrutalCard>

          <BrutalCard>
            <h2 className="text-2xl font-display font-bold mb-4">Client Profile</h2>
            <div className="flex items-center gap-4 mb-4">
              <div className="w-16 h-16 border-4 border-brutal-black bg-neon-green flex items-center justify-center">
                <span className="text-2xl font-display font-bold">J</span>
              </div>
              <div>
                <h3 className="text-xl font-display font-bold">John Doe</h3>
                <p className="font-body">Acme Corporation</p>
              </div>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="border-4 border-brutal-black bg-brutal-white p-4">
                <h4 className="font-display font-bold">Contact</h4>
                <p className="font-body">john@acme.com</p>
              </div>
              <div className="border-4 border-brutal-black bg-brutal-white p-4">
                <h4 className="font-display font-bold">Deals</h4>
                <p className="text-2xl font-display font-bold text-neon-green">5</p>
              </div>
            </div>
          </BrutalCard>
        </div>

        {/* Data Visualization */}
        <BrutalCard>
          <h2 className="text-2xl font-display font-bold mb-6">Sales Pipeline</h2>
          <div className="h-64 bg-concrete border-4 border-brutal-black flex items-center justify-center">
            <p className="text-lg font-body">Chart will go here</p>
          </div>
        </BrutalCard>
      </main>
    </div>
  )
}
```

### 5. Add Global Styles

```css
/* app/globals.css */
@import url('https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@300;400;500;600;700&family=Inter:wght@300;400;500;600;700&display=swap');
@tailwind base;
@tailwind components;
@tailwind utilities;

@layer base {
  :root {
    --background: 0 0% 100%;
    --foreground: 222.2 84% 4.9%;
    --card: 0 0% 100%;
    --card-foreground: 222.2 84% 4.9%;
    --popover: 0 0% 100%;
    --popover-foreground: 222.2 84% 4.9%;
    --primary: 222.2 47.4% 11.2%;
    --primary-foreground: 210 40% 98%;
    --secondary: 210 40% 96%;
    --secondary-foreground: 222.2 84% 4.9%;
    --muted: 210 40% 96%;
    --muted-foreground: 215.4 16.3% 46.9%;
    --accent: 210 40% 96%;
    --accent-foreground: 222.2 84% 4.9%;
    --destructive: 0 84.2% 60.2%;
    --destructive-foreground: 210 40% 98%;
    --border: 214.3 31.8% 91.4%;
    --input: 214.3 31.8% 91.4%;
    --ring: 222.2 84% 4.9%;
    --radius: 0.5rem;
  }

  .dark {
    --background: 222.2 84% 4.9%;
    --foreground: 210 40% 98%;
    --card: 222.2 84% 4.9%;
    --card-foreground: 210 40% 98%;
    --popover: 222.2 84% 4.9%;
    --popover-foreground: 210 40% 98%;
    --primary: 210 40% 98%;
    --primary-foreground: 222.2 47.4% 11.2%;
    --secondary: 217.2 32.6% 17.5%;
    --secondary-foreground: 210 40% 98%;
    --muted: 217.2 32.6% 17.5%;
    --muted-foreground: 215 20.2% 65.1%;
    --accent: 217.2 32.6% 17.5%;
    --accent-foreground: 210 40% 98%;
    --destructive: 0 62.8% 30.6%;
    --destructive-foreground: 210 40% 98%;
    --border: 217.2 32.6% 17.5%;
    --input: 217.2 32.6% 17.5%;
    --ring: 212.7 26.8% 83.9%;
  }
}

@layer base {
  * {
    @apply border-border;
  }
  body {
    @apply bg-background text-foreground;
  }
}

/* Brutalist custom styles */
@layer components {
  .brutal-shadow {
    box-shadow: 8px 8px 0px #000000;
  }
  
  .brutal-shadow-lg {
    box-shadow: 12px 12px 0px #000000;
  }
  
  .brutal-shadow-sm {
    box-shadow: 4px 4px 0px #000000;
  }
  
  .brutal-hover {
    @apply transition-all duration-200 hover:translate-x-1 hover:translate-y-1 hover:shadow-none;
  }
}
```

### 6. Start Development Server

```bash
# Start the development server
pnpm dev

# Open http://localhost:3000/dashboard
```

## 🎨 Customization

### Color Scheme
```typescript
// lib/theme.ts
export const brutalistTheme = {
  colors: {
    primary: '#39FF14',    // Neon Green
    secondary: '#00A3FF',  // Electric Blue
    background: '#F2EFE6', // Beige
    surface: '#E5E5E5',    // Concrete
    text: '#000000',       // Black
    textSecondary: '#FFFFFF' // White
  },
  shadows: {
    brutal: '8px 8px 0px #000000',
    brutalLg: '12px 12px 0px #000000',
    brutalSm: '4px 4px 0px #000000'
  }
}
```

### Typography Scale
```typescript
// lib/typography.ts
export const typography = {
  display: {
    fontFamily: 'Space Grotesk',
    fontWeight: '700',
    sizes: {
      xs: '1.25rem',   // 20px
      sm: '1.5rem',    // 24px
      md: '2rem',      // 32px
      lg: '2.5rem',    // 40px
      xl: '3rem',      // 48px
      '2xl': '3.5rem', // 56px
    }
  },
  body: {
    fontFamily: 'Inter',
    fontWeight: '400',
    sizes: {
      xs: '0.75rem',   // 12px
      sm: '0.875rem',  // 14px
      md: '1rem',      // 16px
      lg: '1.125rem',  // 18px
      xl: '1.25rem',   // 20px
    }
  }
}
```

## 🚀 Next Steps

1. **Add State Management**: Implement Zustand for client state
2. **API Integration**: Connect to Spring Boot backend
3. **Forms**: Add react-hook-form with Zod validation
4. **Charts**: Implement brutalist data visualizations
5. **Responsive**: Test on all device sizes
6. **Performance**: Optimize with Next.js features

## 📚 Resources

- [Neo-Brutalism Design Guide](https://www.neobrutalism.dev/)
- [shadcn/ui Components](https://ui.shadcn.com/)
- [Tailwind CSS](https://tailwindcss.com/)
- [Next.js Documentation](https://nextjs.org/docs)

---

**Ready to build something brutal! 🎨💪**
