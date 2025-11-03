/**
 * Content Editor Component
 * Simple textarea-based editor with Neobrutalism styling
 * TODO: Upgrade to TipTap or Draft.js for rich text editing
 */

'use client'

import { Card } from '@/components/ui/card'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { UseFormRegister, FieldErrors } from 'react-hook-form'
import { CreateContentRequest, UpdateContentRequest } from '@/types/content'
import { Eye, Code } from 'lucide-react'
import { useState } from 'react'

interface ContentEditorProps {
  register: UseFormRegister<CreateContentRequest | UpdateContentRequest>
  errors: FieldErrors<CreateContentRequest | UpdateContentRequest>
  value?: string
}

export function ContentEditor({ register, errors, value }: ContentEditorProps) {
  const [activeTab, setActiveTab] = useState<'write' | 'preview'>('write')

  const renderMarkdown = (markdown: string) => {
    // Simple markdown preview (replace with proper markdown parser later)
    return markdown
      .split('\n')
      .map((line, i) => {
        // Headers
        if (line.startsWith('# ')) {
          return <h1 key={i} className="mb-4 text-4xl font-black">{line.substring(2)}</h1>
        }
        if (line.startsWith('## ')) {
          return <h2 key={i} className="mb-3 text-3xl font-black">{line.substring(3)}</h2>
        }
        if (line.startsWith('### ')) {
          return <h3 key={i} className="mb-2 text-2xl font-black">{line.substring(4)}</h3>
        }
        
        // Bold
        const boldRegex = /\*\*(.*?)\*\*/g
        const withBold = line.replace(boldRegex, '<strong class="font-bold">$1</strong>')
        
        // Italic
        const italicRegex = /\*(.*?)\*/g
        const withItalic = withBold.replace(italicRegex, '<em class="italic">$1</em>')
        
        // Links
        const linkRegex = /\[(.*?)\]\((.*?)\)/g
        const withLinks = withItalic.replace(
          linkRegex,
          '<a href="$2" class="text-blue-600 underline font-bold">$1</a>'
        )
        
        // Empty lines
        if (!line.trim()) {
          return <br key={i} />
        }
        
        // Regular paragraphs
        return (
          <p 
            key={i} 
            className="mb-2"
            dangerouslySetInnerHTML={{ __html: withLinks }}
          />
        )
      })
  }

  return (
    <Card className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
      <div className="border-b-2 border-black bg-pink-200 px-6 py-4">
        <h2 className="text-xl font-black">Content Body</h2>
      </div>
      <div className="p-6">
        <Tabs value={activeTab} onValueChange={(v) => setActiveTab(v as 'write' | 'preview')}>
          <TabsList className="mb-4 grid w-full grid-cols-2">
            <TabsTrigger value="write" className="flex items-center gap-2">
              <Code className="h-4 w-4" />
              Write
            </TabsTrigger>
            <TabsTrigger value="preview" className="flex items-center gap-2">
              <Eye className="h-4 w-4" />
              Preview
            </TabsTrigger>
          </TabsList>

          <TabsContent value="write" className="space-y-2">
            <Label htmlFor="body">
              Content <span className="text-red-500">*</span>
            </Label>
            <Textarea
              id="body"
              {...register('body', { required: 'Content body is required' })}
              rows={20}
              placeholder="Write your content here... (Markdown supported)

# Heading 1
## Heading 2
### Heading 3

**Bold text**
*Italic text*

[Link text](https://example.com)

- List item 1
- List item 2"
              className={`font-mono text-sm ${errors.body ? 'border-red-500' : ''}`}
            />
            {errors.body && (
              <p className="text-sm text-red-500">{errors.body.message}</p>
            )}
            
            <div className="rounded border-2 border-black bg-blue-50 p-4">
              <h4 className="mb-2 font-bold">Markdown Quick Reference:</h4>
              <div className="grid gap-2 text-sm md:grid-cols-2">
                <div>
                  <code className="rounded bg-white px-2 py-1"># Heading 1</code>
                </div>
                <div>
                  <code className="rounded bg-white px-2 py-1">## Heading 2</code>
                </div>
                <div>
                  <code className="rounded bg-white px-2 py-1">**Bold**</code>
                </div>
                <div>
                  <code className="rounded bg-white px-2 py-1">*Italic*</code>
                </div>
                <div className="md:col-span-2">
                  <code className="rounded bg-white px-2 py-1">[Link](url)</code>
                </div>
              </div>
            </div>
          </TabsContent>

          <TabsContent value="preview">
            <div className="min-h-[500px] rounded-lg border-2 border-black bg-white p-6">
              {value ? (
                <div className="prose max-w-none">
                  {renderMarkdown(value)}
                </div>
              ) : (
                <div className="flex h-full items-center justify-center text-muted-foreground">
                  <p>Nothing to preview yet. Start writing in the Write tab.</p>
                </div>
              )}
            </div>
          </TabsContent>
        </Tabs>
      </div>
    </Card>
  )
}
