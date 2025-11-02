# Neobrutalism CRM

A modern Customer Relationship Management system with integrated Content Management System (CMS) and Learning Management System (LMS), built with **Neobrutalism** design principles.

## 🎨 Design Philosophy

This project embraces **Neobrutalism** - a bold, modern design style featuring:
- Heavy black borders (`border-2 border-black`)
- Hard shadows (`shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]`)
- Bright, vibrant colors
- Bold typography (font-black, font-bold)
- Minimal rounded corners
- High contrast and visual hierarchy

## 🚀 Tech Stack

### Backend
- **Java 21 LTS** - Latest long-term support release
- **Spring Boot 3.3.5** - Modern Java framework
- **Hibernate 6.5.3** - ORM for database management
- **PostgreSQL** - Relational database
- **Maven** - Dependency management

### Frontend
- **Next.js 14** - React framework with App Router
- **TypeScript 5.5** - Type-safe development
- **TailwindCSS** - Utility-first CSS framework
- **shadcn/ui** - High-quality React components
- **React Query (TanStack Query v5)** - Data fetching and caching
- **React Hook Form** - Form management
- **Lucide React** - Icon library

## ✨ Features

### ✅ Core CRM (100% Complete)
- **Customer Management**: Full CRUD operations with profiles
- **Contact Management**: Track interactions and relationships
- **Role-Based Access Control**: Casbin integration for permissions
- **Multi-tenancy Support**: Organization-based data isolation
- **User Management**: Complete authentication and authorization

### ✅ Content Management System (100% Complete)
- **Content CRUD**: Create, read, update, delete articles
- **Category Management**: Hierarchical category tree
- **Tag System**: Flexible content tagging
- **Status Workflow**: Draft → Published → Archived
- **Rich Content**: Full markdown support
- **SEO Friendly**: Slug-based URLs, meta descriptions
- **Author Attribution**: Content ownership tracking

**Documentation**: See [PRIORITY2_CMS_COMPLETE.md](./PRIORITY2_CMS_COMPLETE.md)

### ✅ Learning Management System (100% Complete)
- **Course Management**: Full course lifecycle management
- **3-Level Hierarchy**: Courses → Modules → Lessons
- **Multiple Content Types**: Video, Text, Quiz, Assignment, Resource
- **Enrollment System**: Student enrollment and progress tracking
- **Progress Tracking**: Visual progress indicators and analytics
- **Certificate System**: Automated certificate issuance
- **Course Player**: Immersive full-screen learning experience
- **Student Dashboard**: Track enrolled courses and progress

**Documentation**: See [PRIORITY3_LMS_COMPLETE.md](./PRIORITY3_LMS_COMPLETE.md)

### ⏳ Upcoming Features
- **Notifications Module**: In-app, email, and push notifications
- **Attachments Module**: File upload and management
- **Dashboard Module**: Analytics and reporting
- **Quiz System**: Interactive quizzes with auto-grading
- **Discussion Forums**: Per-course discussion boards
- **Live Classes**: Video conferencing integration

## 📁 Project Structure

```
neobrutalism-crm/
├── src/
│   ├── app/                    # Next.js App Router pages
│   │   ├── admin/             # Admin dashboard
│   │   │   ├── courses/       # LMS admin pages
│   │   │   ├── content/       # CMS admin pages
│   │   │   ├── customers/     # CRM pages
│   │   │   └── contacts/      # CRM pages
│   │   ├── courses/           # Public course pages
│   │   ├── blog/              # Public blog pages
│   │   └── my-courses/        # Student dashboard
│   ├── components/            # React components
│   │   ├── course/           # LMS components
│   │   ├── content/          # CMS components
│   │   └── ui/               # shadcn/ui components
│   ├── hooks/                # React Query hooks
│   │   ├── useCourses.ts
│   │   ├── useContent.ts
│   │   └── ...
│   ├── lib/                  # Utilities and API clients
│   │   ├── api/             # API client functions
│   │   └── utils.ts
│   └── types/               # TypeScript type definitions
│       ├── course.ts
│       ├── content.ts
│       └── ...
├── target/                   # Java build output
├── docs/                     # Documentation
├── public/                   # Static assets
└── pom.xml                  # Maven configuration
```

## 🎯 Design Patterns

### Component Patterns

#### 1. Card Pattern
```tsx
<div className="border-2 border-black bg-white shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
  <div className="border-b-2 border-black bg-yellow-200 px-6 py-4">
    <h2 className="text-2xl font-black uppercase">Title</h2>
  </div>
  <div className="p-6">
    {/* Content */}
  </div>
</div>
```

#### 2. Button Pattern
```tsx
<button className="border-2 border-black bg-green-400 px-6 py-3 font-bold uppercase shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] transition-all hover:translate-x-[2px] hover:translate-y-[2px] hover:shadow-none">
  Action
</button>
```

#### 3. Badge Pattern
```tsx
<span className="rounded-full border-2 border-black bg-blue-200 px-3 py-1 text-xs font-black uppercase">
  Status
</span>
```

#### 4. Form Pattern (4-Section Card)
```tsx
<div className="space-y-6">
  {/* Section 1: Yellow header */}
  {/* Section 2: Green header */}
  {/* Section 3: Blue header */}
  {/* Section 4: Purple header */}
</div>
```

### Color Coding
- **Yellow (200/400)**: Primary actions, highlights, basics
- **Green (200/400)**: Success, completion, organization
- **Blue (200/400)**: Information, details, access
- **Purple (200/400)**: Advanced features, metadata
- **Red (200/400)**: Warnings, delete actions
- **Gray (200/400)**: Neutral, drafts, cancel

## 🚦 Getting Started

### Prerequisites
- **Java 21** or higher
- **Node.js 18** or higher
- **PostgreSQL 14** or higher
- **Maven 3.8** or higher
- **pnpm** (recommended) or npm

### Backend Setup

1. **Clone the repository**:
```bash
git clone https://github.com/yourusername/neobrutalism-crm.git
cd neobrutalism-crm
```

2. **Configure database** in `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/neobrutalism_crm
spring.datasource.username=your_username
spring.datasource.password=your_password
```

3. **Build and run**:
```bash
mvn clean install
mvn spring-boot:run
```

The backend will start on `http://localhost:8080`

### Frontend Setup

1. **Install dependencies**:
```bash
pnpm install
# or
npm install
```

2. **Configure environment variables** in `.env.local`:
```env
NEXT_PUBLIC_API_URL=http://localhost:8080/api
```

3. **Run development server**:
```bash
pnpm dev
# or
npm run dev
```

The frontend will start on `http://localhost:3000`

### Docker Setup (Optional)

```bash
docker-compose up -d
```

## 📚 API Documentation

### Base URL
```
http://localhost:8080/api
```

### Authentication
Most endpoints require JWT authentication. Include the token in the `Authorization` header:
```
Authorization: Bearer <your-jwt-token>
```

### Key Endpoints

#### Content API
- `GET /api/content` - List all content
- `POST /api/content` - Create new content
- `GET /api/content/{id}` - Get content by ID
- `PUT /api/content/{id}` - Update content
- `DELETE /api/content/{id}` - Delete content
- `GET /api/content/slug/{slug}` - Get content by slug

#### Course API
- `GET /api/courses` - List all courses (with pagination)
- `POST /api/courses` - Create new course
- `GET /api/courses/{id}` - Get course by ID
- `PUT /api/courses/{id}` - Update course
- `DELETE /api/courses/{id}` - Delete course
- `GET /api/courses/slug/{slug}` - Get course by slug
- `POST /api/courses/{id}/publish` - Publish course
- `POST /api/courses/{id}/unpublish` - Unpublish course

#### Module API
- `GET /api/courses/{courseId}/modules` - List course modules
- `POST /api/courses/{courseId}/modules` - Create module
- `PUT /api/modules/{id}` - Update module
- `DELETE /api/modules/{id}` - Delete module

#### Enrollment API
- `GET /api/enrollments/user/{userId}` - Get user enrollments
- `POST /api/enrollments` - Create enrollment
- `GET /api/enrollments/{id}/progress` - Get enrollment progress
- `POST /api/enrollments/{id}/certificate` - Issue certificate

## 🧪 Testing

### Frontend Testing
```bash
pnpm test
# or
npm test
```

### Backend Testing
```bash
mvn test
```

### E2E Testing
```bash
pnpm test:e2e
# or
npm run test:e2e
```

## 🏗️ Build for Production

### Frontend
```bash
pnpm build
pnpm start
# or
npm run build
npm start
```

### Backend
```bash
mvn clean package
java -jar target/neobrutalism-crm-0.0.1-SNAPSHOT.jar
```

## 📊 Project Status

| Module | Status | Completion | Files | LOC |
|--------|--------|------------|-------|-----|
| Backend | ✅ Complete | 100% | N/A | N/A |
| Core CRM | ✅ Complete | 100% | N/A | N/A |
| CMS | ✅ Complete | 100% | 21 | ~3,000 |
| LMS | ✅ Complete | 100% | 21 | ~5,000 |
| Notifications | ⏳ Pending | 0% | 0 | 0 |
| Attachments | ⏳ Pending | 0% | 0 | 0 |
| Dashboard | ⏳ Pending | 0% | 0 | 0 |

**Overall Progress**: 85% Complete

## 🤝 Contributing

We welcome contributions! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Code Style
- Follow existing Neobrutalism design patterns
- Use TypeScript for all new frontend code
- Write meaningful commit messages
- Add tests for new features
- Update documentation as needed

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👥 Authors

- Your Name - Initial work

## 🙏 Acknowledgments

- [shadcn/ui](https://ui.shadcn.com/) - Beautiful UI components
- [Lucide](https://lucide.dev/) - Icon library
- [TailwindCSS](https://tailwindcss.com/) - CSS framework
- [Next.js](https://nextjs.org/) - React framework
- [Spring Boot](https://spring.io/projects/spring-boot) - Java framework

## 📞 Support

For support, email support@example.com or open an issue in the GitHub repository.

## 🗺️ Roadmap

### Q1 2026
- [ ] Notifications Module
- [ ] Attachments Module
- [ ] Advanced Dashboard

### Q2 2026
- [ ] Quiz System with Auto-Grading
- [ ] Discussion Forums
- [ ] Mobile App (React Native)

### Q3 2026
- [ ] Live Classes (Video Conferencing)
- [ ] Gamification (Badges, Points, Leaderboards)
- [ ] Social Features (Reviews, Ratings, Sharing)

### Q4 2026
- [ ] Advanced Analytics
- [ ] AI-Powered Recommendations
- [ ] Multi-language Support

---

**Built with ❤️ using Neobrutalism Design**

**Last Updated**: November 3, 2025
