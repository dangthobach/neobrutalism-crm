# Neo-Brutalist CRM Application

A modern, edgy CRM dashboard built with Next.js and Spring Boot, featuring a bold Neo-Brutalist design system.

## 🎨 Design Philosophy

**Neo-Brutalism** meets **Enterprise CRM** - Clean yet edgy interface with asymmetrical layouts, thick borders, and vibrant accents on muted backgrounds.

### Visual Identity
- **Layout**: Asymmetrical 12-column grid with obvious gutters/dividers
- **Borders**: Thick strokes (`border-4`) with hard drop-shadows (`shadow-[8px_8px_0_#000]`)
- **Colors**: 
  - Primary: Neon Green `#39FF14` or Electric Blue `#00A3FF`
  - Background: Muted Beige `#F2EFE6` or Concrete Gray `#E5E5E5`
  - Text: High contrast black/white
- **Typography**: 
  - Display: Bold geometric (Space Grotesk, Plus Jakarta Sans)
  - Body: Readable sans-serif (Inter)
- **Motion**: Minimal, chunky transitions - no blur/glow effects

## 🏗️ Architecture Overview

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Frontend      │    │   Backend       │    │   Database      │
│   Next.js 15    │◄──►│   Spring Boot   │◄──►│   PostgreSQL    │
│   App Router    │    │   3.3+          │    │   15+           │
│   React 18      │    │   WebFlux/MVC   │    │   Redis Cache   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Design System │    │   Security      │    │   Monitoring    │
│   shadcn/ui     │    │   Keycloak      │    │   Grafana       │
│   Tailwind CSS  │    │   Spring Sec    │    │   OpenTelemetry │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## 🛠️ Tech Stack

### Frontend
- **Framework**: Next.js 15 (App Router) + React 18
- **Styling**: Tailwind CSS + shadcn/ui components
- **State Management**: Zustand (client) + TanStack Query (server)
- **Forms**: react-hook-form + Zod validation
- **Animation**: Framer Motion (chunky, sharp transitions)
- **Icons**: Lucide React
- **Charts**: Recharts (brutalist styling)

### Backend
- **Framework**: Spring Boot 3.3+
- **Architecture**: WebFlux (reactive) or MVC
- **Database**: PostgreSQL 15+ with Flyway migrations
- **Security**: Spring Security 6 + Keycloak (OIDC)
- **Mapping**: MapStruct for DTOs
- **Validation**: Jakarta Validation
- **Caching**: Redis for sessions/rate limiting
- **Messaging**: Kafka for event-driven actions

### Infrastructure
- **Monitoring**: Micrometer + OpenTelemetry → Grafana
- **API**: REST (JSON) with versioned routes
- **Performance**: Edge caching, HTTP/2, image optimization

## 📋 Implementation Plan

### Phase 1: Foundation Setup (Week 1-2)

#### 1.1 Project Initialization
```bash
# Frontend
npx create-next-app@latest crm-frontend --typescript --tailwind --app
cd crm-frontend
pnpm add @radix-ui/react-* lucide-react framer-motion zustand @tanstack/react-query react-hook-form @hookform/resolvers zod recharts

# Backend
spring init --dependencies=web,data-jpa,security,validation,actuator crm-backend
```

#### 1.2 Design System Setup
- [ ] Configure Tailwind with custom Neo-Brutalist theme
- [ ] Set up shadcn/ui with brutalist component variants
- [ ] Create typography scale (Space Grotesk + Inter)
- [ ] Define color palette and spacing system
- [ ] Build base layout components (Grid, Container, Divider)

#### 1.3 Core Components
- [ ] **Button**: Thick borders, hard shadows, chunky hover states
- [ ] **Card**: Asymmetrical layouts, prominent borders
- [ ] **Input**: Blocky design with focus states
- [ ] **Modal**: Sharp corners, no rounded edges
- [ ] **Navigation**: Bold, geometric menu items

### Phase 2: CRM Core Features (Week 3-4)

#### 2.1 Dashboard Layout
```typescript
// Asymmetrical grid system
const DashboardLayout = () => (
  <div className="grid grid-cols-12 gap-4 p-6 bg-beige min-h-screen">
    <aside className="col-span-3 border-4 border-black bg-white shadow-[8px_8px_0_#000]">
      <Navigation />
    </aside>
    <main className="col-span-9 space-y-6">
      <header className="border-4 border-black bg-neon-green p-4">
        <h1 className="text-4xl font-bold">CRM Dashboard</h1>
      </header>
      <div className="grid grid-cols-2 gap-6">
        <TaskList />
        <ClientProfile />
      </div>
    </main>
  </div>
);
```

#### 2.2 Key Components
- [ ] **Task List**: Checkbox interactions with brutalist styling
- [ ] **Client Profile Card**: Distinct borders, hard shadows
- [ ] **Data Visualization**: Blocky charts with neon accents
- [ ] **Pipeline View**: Asymmetrical deal stages
- [ ] **Activity Feed**: Timeline with geometric elements

#### 2.3 Responsive Design
- [ ] Mobile-first approach with breakpoint-specific layouts
- [ ] Tablet: 8-column grid adaptation
- [ ] Desktop: Full 12-column asymmetrical grid
- [ ] Touch-friendly interactions (44px minimum)

### Phase 3: Backend Development (Week 5-6)

#### 3.1 API Structure
```java
@RestController
@RequestMapping("/api/v1")
public class CRMController {
    
    @GetMapping("/clients")
    public ResponseEntity<Page<ClientDTO>> getClients(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String cursor
    ) {
        // Cursor-based pagination for performance
    }
    
    @PostMapping("/deals")
    public ResponseEntity<DealDTO> createDeal(@Valid @RequestBody CreateDealRequest request) {
        // Event-driven deal creation
    }
}
```

#### 3.2 Database Schema
```sql
-- Core entities
CREATE TABLE clients (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE,
    company VARCHAR(255),
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE deals (
    id UUID PRIMARY KEY,
    client_id UUID REFERENCES clients(id),
    title VARCHAR(255) NOT NULL,
    value DECIMAL(12,2),
    stage VARCHAR(50),
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE tasks (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    completed BOOLEAN DEFAULT FALSE,
    due_date TIMESTAMP,
    assigned_to UUID
);
```

#### 3.3 Event-Driven Architecture
```java
@Component
public class DealEventHandler {
    
    @EventListener
    public void handleDealStageChanged(DealStageChangedEvent event) {
        // Publish to Kafka for real-time updates
        kafkaTemplate.send("deal-events", event);
    }
}
```

### Phase 4: Advanced Features (Week 7-8)

#### 4.1 Real-time Updates
- [ ] WebSocket integration for live dashboard updates
- [ ] Kafka event streaming for deal/task changes
- [ ] Optimistic UI updates with rollback capability

#### 4.2 Performance Optimization
- [ ] Next.js ISR for read-heavy widgets
- [ ] React Server Components for initial page loads
- [ ] Edge caching for pipeline snapshots
- [ ] Image optimization with Next.js Image component

#### 4.3 Security & Authentication
- [ ] Keycloak integration for SSO
- [ ] Role-based access control (RBAC)
- [ ] API rate limiting with Redis
- [ ] Input validation and sanitization

### Phase 5: Testing & Deployment (Week 9-10)

#### 5.1 Testing Strategy
```typescript
// Frontend testing
describe('TaskList Component', () => {
  it('should render with brutalist styling', () => {
    render(<TaskList />);
    expect(screen.getByRole('list')).toHaveClass('border-4', 'border-black');
  });
});
```

```java
// Backend testing
@SpringBootTest
class CRMServiceTest {
    
    @Test
    void shouldCreateDealWithValidData() {
        // Integration test with TestContainers
    }
}
```

#### 5.2 Deployment Pipeline
- [ ] Docker containerization
- [ ] CI/CD with GitHub Actions
- [ ] Staging environment setup
- [ ] Production deployment with monitoring

## 🎯 Key Features

### Dashboard Components
1. **Asymmetrical Grid Layout**
   - 12-column responsive grid
   - Obvious gutters and dividers
   - Mobile-first approach

2. **Task Management**
   - Brutalist checkbox interactions
   - Hard shadows on completion
   - Geometric progress indicators

3. **Client Profiles**
   - Distinct borders and shadows
   - Blocky contact cards
   - Sharp hover states

4. **Data Visualization**
   - Blocky charts with neon accents
   - No rounded corners
   - High contrast colors

5. **Pipeline Management**
   - Asymmetrical deal stages
   - Chunky drag-and-drop
   - Bold stage indicators

### Performance Features
- **Edge Caching**: ISR for dashboard widgets
- **Server Components**: Reduced client-side JS
- **Optimistic Updates**: Immediate UI feedback
- **Cursor Pagination**: Efficient data loading

## 🚀 Getting Started

### Prerequisites
- Node.js 18+
- Java 17+
- PostgreSQL 15+
- Redis 7+
- Docker (optional)

### Quick Start
```bash
# Clone repository
git clone <repository-url>
cd neo-brutalist-crm

# Frontend setup
cd frontend
pnpm install
pnpm dev

# Backend setup
cd ../backend
./mvnw spring-boot:run

# Access application
open http://localhost:3000
```

## 📊 Monitoring & Analytics

### Metrics Dashboard
- **Performance**: Core Web Vitals, API response times
- **Business**: Deal conversion rates, task completion
- **Technical**: Error rates, cache hit ratios
- **User**: Page views, feature usage

### Alerting
- API response time > 500ms
- Error rate > 1%
- Database connection pool exhaustion
- Memory usage > 80%

## 🔧 Development Guidelines

### Code Style
- **Frontend**: ESLint + Prettier with brutalist naming conventions
- **Backend**: Google Java Style Guide
- **Commits**: Conventional Commits format

### Component Naming
```typescript
// Brutalist component naming
<BlockyButton />
<SharpCard />
<GeometricInput />
<ChunkyModal />
```

### API Conventions
```java
// RESTful with brutalist clarity
GET    /api/v1/clients          // List clients
POST   /api/v1/clients          // Create client
GET    /api/v1/clients/{id}     // Get client
PUT    /api/v1/clients/{id}     // Update client
DELETE /api/v1/clients/{id}     // Delete client
```

## 📈 Success Metrics

### Technical KPIs
- **Performance**: < 2s initial page load
- **Availability**: 99.9% uptime
- **Security**: Zero critical vulnerabilities
- **Scalability**: Support 1000+ concurrent users

### Business KPIs
- **User Adoption**: 80% daily active users
- **Task Completion**: 90% completion rate
- **Deal Velocity**: 25% faster deal closure
- **User Satisfaction**: 4.5+ rating

## 🤝 Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/brutalist-component`)
3. Commit changes (`git commit -m 'Add brutalist task list'`)
4. Push to branch (`git push origin feature/brutalist-component`)
5. Open Pull Request

## 📄 License

MIT License - see [LICENSE](LICENSE) file for details.

---

**Built with ❤️ and lots of sharp edges** 🎨