'use client'

import { Button } from '@/components/ui/button'
import { ArrowLeft } from 'lucide-react'
import { useRouter } from 'next/navigation'
import { useCategoryTree } from '@/hooks/useContentCategories'
import { CategoryTree } from '@/components/content/category-tree'

export default function CategoriesPage() {
  const router = useRouter()
  const { data: categories = [], isLoading } = useCategoryTree()

  if (isLoading) {
    return (
      <div className="flex h-screen items-center justify-center">
        <div className="text-center">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-black border-t-transparent" />
          <p className="mt-4 font-bold">Loading categories...</p>
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center gap-4">
        <Button variant="neutral" size="icon" onClick={() => router.back()}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <div>
          <h1 className="text-4xl font-black">Category Management</h1>
          <p className="text-muted-foreground">
            Organize your content with hierarchical categories
          </p>
        </div>
      </div>

      {/* Category Tree */}
      <CategoryTree categories={categories} />
    </div>
  )
}
