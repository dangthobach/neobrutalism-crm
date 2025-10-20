# Neo-Brutalist CRM - Detailed Implementation Plan

## 🎯 Project Overview

Building a modern CRM application with a bold Neo-Brutalist design system, combining enterprise functionality with edgy visual aesthetics.

## 📅 Timeline: 10 Weeks

### Week 1-2: Foundation & Design System
### Week 3-4: Core CRM Features
### Week 5-6: Backend Development
### Week 7-8: Advanced Features
### Week 9-10: Testing & Deployment

---

## 🏗️ Phase 1: Foundation Setup (Week 1-2)

### 1.1 Project Structure Setup

```bash
neo-brutalist-crm/
├── frontend/                 # Next.js 15 App Router
│   ├── src/
│   │   ├── app/             # App Router pages
│   │   ├── components/      # Reusable components
│   │   │   ├── ui/         # shadcn/ui components
│   │   │   ├── forms/      # Form components
│   │   │   └── charts/     # Data visualization
│   │   ├── lib/            # Utilities & configs
│   │   ├── hooks/          # Custom React hooks
│   │   └── types/          # TypeScript definitions
│   ├── public/             # Static assets
│   └── tailwind.config.js  # Brutalist theme config
├── backend/                 # Spring Boot 3.3+
│   ├── src/main/java/
│   │   └── com/crm/
│   │       ├── controller/ # REST controllers
│   │       ├── service/    # Business logic
│   │       ├── repository/ # Data access
│   │       ├── entity/     # JPA entities
│   │       ├── dto/        # Data transfer objects
│   │       └── config/     # Configuration classes
│   └── src/main/resources/
│       ├── application.yml
│       └── db/migration/   # Flyway migrations
└── docker-compose.yml      # Local development
```

### 1.2 Design System Configuration

#### Tailwind Config (brutalist-theme.js)
```javascript
module.exports = {
  theme: {
    extend: {
      colors: {
        // Neo-Brutalist Color Palette
        'neon-green': '#39FF14',
        'electric-blue': '#00A3FF',
        'beige': '#F2EFE6',
        'concrete': '#E5E5E5',
        'brutal-black': '#000000',
        'brutal-white': '#FFFFFF',
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
      borderWidth: {
        '4': '4px',
        '6': '6px',
        '8': '8px',
      },
      spacing: {
        '18': '4.5rem',
        '88': '22rem',
      }
    }
  }
}
```

#### shadcn/ui Component Variants
```typescript
// components/ui/button.tsx
const buttonVariants = cva(
  "inline-flex items-center justify-center font-display font-bold transition-all duration-200 focus-visible:outline-none disabled:pointer-events-none disabled:opacity-50",
  {
    variants: {
      variant: {
        brutal: "border-4 border-brutal-black bg-neon-green text-brutal-black shadow-brutal hover:translate-x-1 hover:translate-y-1 hover:shadow-none",
        "brutal-secondary": "border-4 border-brutal-black bg-brutal-white text-brutal-black shadow-brutal hover:bg-concrete",
        "brutal-destructive": "border-4 border-brutal-black bg-red-500 text-brutal-white shadow-brutal hover:bg-red-600",
      },
      size: {
        brutal: "h-12 px-6 text-lg",
        "brutal-sm": "h-10 px-4 text-base",
        "brutal-lg": "h-16 px-8 text-xl",
      },
    },
    defaultVariants: {
      variant: "brutal",
      size: "brutal",
    },
  }
)
```

### 1.3 Core Layout Components

#### Asymmetrical Grid System
```typescript
// components/layout/brutal-grid.tsx
interface BrutalGridProps {
  children: React.ReactNode;
  className?: string;
}

export const BrutalGrid = ({ children, className }: BrutalGridProps) => (
  <div className={cn(
    "grid grid-cols-12 gap-4 p-6 bg-beige min-h-screen",
    className
  )}>
    {children}
  </div>
);

// components/layout/brutal-container.tsx
export const BrutalContainer = ({ children, className }: BrutalGridProps) => (
  <div className={cn(
    "border-4 border-brutal-black bg-brutal-white shadow-brutal p-6",
    className
  )}>
    {children}
  </div>
);
```

---

## 🎨 Phase 2: CRM Core Features (Week 3-4)

### 2.1 Dashboard Layout Implementation

```typescript
// app/dashboard/page.tsx
export default function DashboardPage() {
  return (
    <BrutalGrid>
      {/* Sidebar Navigation */}
      <aside className="col-span-3">
        <BrutalContainer className="h-full">
          <Navigation />
        </BrutalContainer>
      </aside>

      {/* Main Content Area */}
      <main className="col-span-9 space-y-6">
        {/* Header */}
        <header className="border-4 border-brutal-black bg-neon-green p-6">
          <h1 className="text-4xl font-display font-bold text-brutal-black">
            CRM Dashboard
          </h1>
        </header>

        {/* Dashboard Grid */}
        <div className="grid grid-cols-2 gap-6">
          <TaskList />
          <ClientProfile />
        </div>

        <div className="grid grid-cols-3 gap-6">
          <PipelineView />
          <ActivityFeed />
          <DataVisualization />
        </div>
      </main>
    </BrutalGrid>
  );
}
```

### 2.2 Key Component Implementations

#### Task List Component
```typescript
// components/crm/task-list.tsx
interface Task {
  id: string;
  title: string;
  completed: boolean;
  dueDate?: Date;
  priority: 'high' | 'medium' | 'low';
}

export const TaskList = () => {
  const { data: tasks, mutate } = useTasks();

  const toggleTask = async (taskId: string) => {
    await updateTask(taskId, { completed: !tasks.find(t => t.id === taskId)?.completed });
    mutate();
  };

  return (
    <BrutalContainer>
      <h2 className="text-2xl font-display font-bold mb-6">Tasks</h2>
      <div className="space-y-4">
        {tasks?.map((task) => (
          <div
            key={task.id}
            className={cn(
              "flex items-center gap-4 p-4 border-4 border-brutal-black transition-all",
              task.completed 
                ? "bg-concrete shadow-brutal-sm" 
                : "bg-brutal-white shadow-brutal hover:translate-x-1 hover:translate-y-1 hover:shadow-none"
            )}
          >
            <BrutalCheckbox
              checked={task.completed}
              onCheckedChange={() => toggleTask(task.id)}
            />
            <span className={cn(
              "flex-1 font-body",
              task.completed && "line-through opacity-60"
            )}>
              {task.title}
            </span>
            <PriorityBadge priority={task.priority} />
          </div>
        ))}
      </div>
    </BrutalContainer>
  );
};
```

#### Client Profile Card
```typescript
// components/crm/client-profile.tsx
export const ClientProfile = ({ clientId }: { clientId: string }) => {
  const { data: client } = useClient(clientId);

  return (
    <BrutalContainer>
      <div className="flex items-center gap-4 mb-6">
        <div className="w-16 h-16 border-4 border-brutal-black bg-neon-green flex items-center justify-center">
          <span className="text-2xl font-display font-bold">
            {client?.name.charAt(0)}
          </span>
        </div>
        <div>
          <h2 className="text-2xl font-display font-bold">{client?.name}</h2>
          <p className="text-lg font-body">{client?.company}</p>
        </div>
      </div>

      <div className="grid grid-cols-2 gap-4">
        <div className="border-4 border-brutal-black bg-brutal-white p-4">
          <h3 className="font-display font-bold mb-2">Contact</h3>
          <p className="font-body">{client?.email}</p>
          <p className="font-body">{client?.phone}</p>
        </div>
        
        <div className="border-4 border-brutal-black bg-brutal-white p-4">
          <h3 className="font-display font-bold mb-2">Deals</h3>
          <p className="text-2xl font-display font-bold text-neon-green">
            {client?.deals?.length || 0}
          </p>
        </div>
      </div>
    </BrutalContainer>
  );
};
```

#### Data Visualization (Brutalist Charts)
```typescript
// components/charts/brutal-chart.tsx
export const BrutalChart = ({ data }: { data: any[] }) => {
  return (
    <BrutalContainer>
      <h2 className="text-2xl font-display font-bold mb-6">Sales Pipeline</h2>
      <ResponsiveContainer width="100%" height={300}>
        <BarChart data={data}>
          <XAxis 
            dataKey="stage" 
            tick={{ fontFamily: 'Inter', fontWeight: 'bold' }}
            axisLine={{ stroke: '#000', strokeWidth: 4 }}
          />
          <YAxis 
            tick={{ fontFamily: 'Inter', fontWeight: 'bold' }}
            axisLine={{ stroke: '#000', strokeWidth: 4 }}
          />
          <Bar 
            dataKey="value" 
            fill="#39FF14" 
            stroke="#000" 
            strokeWidth={4}
            radius={[0, 0, 0, 0]} // No rounded corners
          />
        </BarChart>
      </ResponsiveContainer>
    </BrutalContainer>
  );
};
```

### 2.3 Responsive Design Implementation

```typescript
// hooks/use-responsive.ts
export const useResponsive = () => {
  const [screenSize, setScreenSize] = useState<'mobile' | 'tablet' | 'desktop'>('desktop');

  useEffect(() => {
    const handleResize = () => {
      const width = window.innerWidth;
      if (width < 768) setScreenSize('mobile');
      else if (width < 1024) setScreenSize('tablet');
      else setScreenSize('desktop');
    };

    handleResize();
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  return screenSize;
};

// Responsive grid classes
const getGridClasses = (screenSize: string) => {
  switch (screenSize) {
    case 'mobile':
      return 'grid-cols-1 gap-2 p-2';
    case 'tablet':
      return 'grid-cols-8 gap-4 p-4';
    case 'desktop':
      return 'grid-cols-12 gap-6 p-6';
    default:
      return 'grid-cols-12 gap-6 p-6';
  }
};
```

---

## ⚙️ Phase 3: Backend Development (Week 5-6)

### 3.1 Spring Boot Configuration

#### Application Configuration
```yaml
# application.yml
spring:
  application:
    name: neo-brutalist-crm
  
  datasource:
    url: jdbc:postgresql://localhost:5432/crm_db
    username: ${DB_USERNAME:crm_user}
    password: ${DB_PASSWORD:crm_password}
    driver-class-name: org.postgresql.Driver
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
  
  security:
    oauth2:
      client:
        registration:
          keycloak:
            client-id: crm-frontend
            client-secret: ${KEYCLOAK_CLIENT_SECRET}
            scope: openid,profile,email
        provider:
          keycloak:
            issuer-uri: ${KEYCLOAK_ISSUER_URI}

server:
  port: 8080
  servlet:
    context-path: /api

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

#### Database Entities
```java
// entity/Client.java
@Entity
@Table(name = "clients")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Client {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(unique = true)
    private String email;
    
    private String phone;
    private String company;
    private String address;
    
    @Enumerated(EnumType.STRING)
    private ClientStatus status;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL)
    private List<Deal> deals;
}

// entity/Deal.java
@Entity
@Table(name = "deals")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Deal {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "DECIMAL(12,2)")
    private BigDecimal value;
    
    @Enumerated(EnumType.STRING)
    private DealStage stage;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private User assignedTo;
    
    private LocalDate expectedCloseDate;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}

// entity/Task.java
@Entity
@Table(name = "tasks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false)
    private String title;
    
    private String description;
    
    @Builder.Default
    private Boolean completed = false;
    
    private LocalDateTime dueDate;
    
    @Enumerated(EnumType.STRING)
    private TaskPriority priority;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private User assignedTo;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deal_id")
    private Deal deal;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
```

### 3.2 REST API Controllers

```java
// controller/ClientController.java
@RestController
@RequestMapping("/v1/clients")
@RequiredArgsConstructor
@Validated
public class ClientController {
    
    private final ClientService clientService;
    
    @GetMapping
    public ResponseEntity<Page<ClientDTO>> getClients(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) String search) {
        
        Page<ClientDTO> clients = clientService.getClients(page, size, cursor, search);
        return ResponseEntity.ok(clients);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ClientDTO> getClient(@PathVariable UUID id) {
        ClientDTO client = clientService.getClientById(id);
        return ResponseEntity.ok(client);
    }
    
    @PostMapping
    public ResponseEntity<ClientDTO> createClient(@Valid @RequestBody CreateClientRequest request) {
        ClientDTO client = clientService.createClient(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(client);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ClientDTO> updateClient(
            @PathVariable UUID id, 
            @Valid @RequestBody UpdateClientRequest request) {
        ClientDTO client = clientService.updateClient(id, request);
        return ResponseEntity.ok(client);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClient(@PathVariable UUID id) {
        clientService.deleteClient(id);
        return ResponseEntity.noContent().build();
    }
}

// controller/DealController.java
@RestController
@RequestMapping("/v1/deals")
@RequiredArgsConstructor
public class DealController {
    
    private final DealService dealService;
    
    @GetMapping
    public ResponseEntity<Page<DealDTO>> getDeals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) DealStage stage) {
        
        Page<DealDTO> deals = dealService.getDeals(page, size, stage);
        return ResponseEntity.ok(deals);
    }
    
    @PostMapping
    public ResponseEntity<DealDTO> createDeal(@Valid @RequestBody CreateDealRequest request) {
        DealDTO deal = dealService.createDeal(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(deal);
    }
    
    @PutMapping("/{id}/stage")
    public ResponseEntity<DealDTO> updateDealStage(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDealStageRequest request) {
        DealDTO deal = dealService.updateDealStage(id, request.getStage());
        return ResponseEntity.ok(deal);
    }
}
```

### 3.3 Service Layer Implementation

```java
// service/ClientService.java
@Service
@RequiredArgsConstructor
@Transactional
public class ClientService {
    
    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;
    private final ApplicationEventPublisher eventPublisher;
    
    public Page<ClientDTO> getClients(int page, int size, String cursor, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        Page<Client> clients;
        if (StringUtils.hasText(search)) {
            clients = clientRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                search, search, pageable);
        } else {
            clients = clientRepository.findAll(pageable);
        }
        
        return clients.map(clientMapper::toDTO);
    }
    
    public ClientDTO getClientById(UUID id) {
        Client client = clientRepository.findById(id)
            .orElseThrow(() -> new ClientNotFoundException("Client not found with id: " + id));
        return clientMapper.toDTO(client);
    }
    
    public ClientDTO createClient(CreateClientRequest request) {
        Client client = clientMapper.toEntity(request);
        client = clientRepository.save(client);
        
        // Publish event for real-time updates
        eventPublisher.publishEvent(new ClientCreatedEvent(client));
        
        return clientMapper.toDTO(client);
    }
    
    public ClientDTO updateClient(UUID id, UpdateClientRequest request) {
        Client client = clientRepository.findById(id)
            .orElseThrow(() -> new ClientNotFoundException("Client not found with id: " + id));
        
        clientMapper.updateEntity(client, request);
        client = clientRepository.save(client);
        
        eventPublisher.publishEvent(new ClientUpdatedEvent(client));
        
        return clientMapper.toDTO(client);
    }
    
    public void deleteClient(UUID id) {
        if (!clientRepository.existsById(id)) {
            throw new ClientNotFoundException("Client not found with id: " + id);
        }
        clientRepository.deleteById(id);
        eventPublisher.publishEvent(new ClientDeletedEvent(id));
    }
}
```

---

## 🚀 Phase 4: Advanced Features (Week 7-8)

### 4.1 Real-time Updates with WebSocket

```typescript
// hooks/use-websocket.ts
export const useWebSocket = (url: string) => {
  const [socket, setSocket] = useState<WebSocket | null>(null);
  const [lastMessage, setLastMessage] = useState<any>(null);

  useEffect(() => {
    const ws = new WebSocket(url);
    
    ws.onopen = () => {
      console.log('WebSocket connected');
      setSocket(ws);
    };
    
    ws.onmessage = (event) => {
      const data = JSON.parse(event.data);
      setLastMessage(data);
    };
    
    ws.onclose = () => {
      console.log('WebSocket disconnected');
      setSocket(null);
    };

    return () => {
      ws.close();
    };
  }, [url]);

  const sendMessage = (message: any) => {
    if (socket?.readyState === WebSocket.OPEN) {
      socket.send(JSON.stringify(message));
    }
  };

  return { socket, lastMessage, sendMessage };
};

// components/crm/real-time-dashboard.tsx
export const RealTimeDashboard = () => {
  const { lastMessage } = useWebSocket('ws://localhost:8080/ws/dashboard');
  const queryClient = useQueryClient();

  useEffect(() => {
    if (lastMessage) {
      // Invalidate relevant queries for real-time updates
      switch (lastMessage.type) {
        case 'DEAL_UPDATED':
          queryClient.invalidateQueries(['deals']);
          break;
        case 'TASK_COMPLETED':
          queryClient.invalidateQueries(['tasks']);
          break;
        case 'CLIENT_CREATED':
          queryClient.invalidateQueries(['clients']);
          break;
      }
    }
  }, [lastMessage, queryClient]);

  return <DashboardContent />;
};
```

### 4.2 Event-Driven Architecture with Kafka

```java
// config/KafkaConfig.java
@Configuration
@EnableKafka
public class KafkaConfig {
    
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }
    
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}

// event/DealEventHandler.java
@Component
@RequiredArgsConstructor
public class DealEventHandler {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @EventListener
    public void handleDealStageChanged(DealStageChangedEvent event) {
        DealEventMessage message = DealEventMessage.builder()
            .dealId(event.getDealId())
            .oldStage(event.getOldStage())
            .newStage(event.getNewStage())
            .timestamp(Instant.now())
            .build();
        
        kafkaTemplate.send("deal-events", message);
    }
    
    @EventListener
    public void handleTaskCompleted(TaskCompletedEvent event) {
        TaskEventMessage message = TaskEventMessage.builder()
            .taskId(event.getTaskId())
            .completed(event.isCompleted())
            .timestamp(Instant.now())
            .build();
        
        kafkaTemplate.send("task-events", message);
    }
}
```

### 4.3 Performance Optimization

#### Next.js ISR Configuration
```typescript
// app/dashboard/page.tsx
export const revalidate = 60; // Revalidate every 60 seconds

export default async function DashboardPage() {
  // Server-side data fetching for initial load
  const initialData = await getDashboardData();
  
  return (
    <DashboardProvider initialData={initialData}>
      <RealTimeDashboard />
    </DashboardProvider>
  );
}

// lib/cache.ts
export const getCachedData = async (key: string, fetcher: () => Promise<any>) => {
  // Implement Redis caching for frequently accessed data
  const cached = await redis.get(key);
  if (cached) {
    return JSON.parse(cached);
  }
  
  const data = await fetcher();
  await redis.setex(key, 300, JSON.stringify(data)); // 5 minute cache
  return data;
};
```

#### React Server Components
```typescript
// app/dashboard/components/server-dashboard-stats.tsx
// This component runs on the server and doesn't send JS to client
export default async function ServerDashboardStats() {
  const stats = await getDashboardStats();
  
  return (
    <div className="grid grid-cols-4 gap-6">
      {stats.map((stat) => (
        <div key={stat.id} className="border-4 border-brutal-black bg-brutal-white p-6">
          <h3 className="text-lg font-display font-bold">{stat.label}</h3>
          <p className="text-3xl font-display font-bold text-neon-green">
            {stat.value}
          </p>
        </div>
      ))}
    </div>
  );
}
```

---

## 🧪 Phase 5: Testing & Deployment (Week 9-10)

### 5.1 Testing Strategy

#### Frontend Testing
```typescript
// __tests__/components/task-list.test.tsx
import { render, screen, fireEvent } from '@testing-library/react';
import { TaskList } from '@/components/crm/task-list';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

const createTestQueryClient = () => new QueryClient({
  defaultOptions: {
    queries: { retry: false },
    mutations: { retry: false },
  },
});

describe('TaskList Component', () => {
  it('should render with brutalist styling', () => {
    const queryClient = createTestQueryClient();
    
    render(
      <QueryClientProvider client={queryClient}>
        <TaskList />
      </QueryClientProvider>
    );
    
    const taskList = screen.getByRole('list');
    expect(taskList).toHaveClass('border-4', 'border-brutal-black');
  });
  
  it('should toggle task completion with brutalist animation', async () => {
    const queryClient = createTestQueryClient();
    
    render(
      <QueryClientProvider client={queryClient}>
        <TaskList />
      </QueryClientProvider>
    );
    
    const checkbox = screen.getByRole('checkbox');
    fireEvent.click(checkbox);
    
    // Verify brutalist styling changes
    const taskItem = checkbox.closest('[class*="border-4"]');
    expect(taskItem).toHaveClass('bg-concrete', 'shadow-brutal-sm');
  });
});
```

#### Backend Testing
```java
// test/ClientServiceTest.java
@SpringBootTest
@Testcontainers
class ClientServiceTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");
    
    @Autowired
    private ClientService clientService;
    
    @Test
    @Transactional
    void shouldCreateClientWithValidData() {
        // Given
        CreateClientRequest request = CreateClientRequest.builder()
            .name("John Doe")
            .email("john@example.com")
            .company("Acme Corp")
            .build();
        
        // When
        ClientDTO result = clientService.createClient(request);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("John Doe");
        assertThat(result.getEmail()).isEqualTo("john@example.com");
    }
    
    @Test
    void shouldThrowExceptionWhenClientNotFound() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        
        // When & Then
        assertThatThrownBy(() -> clientService.getClientById(nonExistentId))
            .isInstanceOf(ClientNotFoundException.class)
            .hasMessageContaining("Client not found");
    }
}
```

### 5.2 Docker Configuration

```dockerfile
# Dockerfile.frontend
FROM node:18-alpine AS base
WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production

FROM base AS build
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:alpine AS production
COPY --from=build /app/out /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]

# Dockerfile.backend
FROM openjdk:17-jdk-slim AS build
WORKDIR /app
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN ./mvnw dependency:go-offline

COPY src src
RUN ./mvnw clean package -DskipTests

FROM openjdk:17-jdk-slim AS production
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
```

```yaml
# docker-compose.yml
version: '3.8'

services:
  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
    ports:
      - "3000:80"
    environment:
      - NEXT_PUBLIC_API_URL=http://localhost:8080/api
    depends_on:
      - backend

  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - DB_HOST=postgres
      - DB_USERNAME=crm_user
      - DB_PASSWORD=crm_password
      - REDIS_HOST=redis
    depends_on:
      - postgres
      - redis

  postgres:
    image: postgres:15
    environment:
      - POSTGRES_DB=crm_db
      - POSTGRES_USER=crm_user
      - POSTGRES_PASSWORD=crm_password
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  kafka:
    image: confluentinc/cp-kafka:latest
    environment:
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    ports:
      - "9092:9092"
    depends_on:
      - zookeeper

  zookeeper:
    image: confluentinc/cp-zookeeper:latest
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000

volumes:
  postgres_data:
```

### 5.3 CI/CD Pipeline

```yaml
# .github/workflows/deploy.yml
name: Deploy Neo-Brutalist CRM

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  test-frontend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-node@v3
        with:
          node-version: '18'
          cache: 'npm'
          cache-dependency-path: frontend/package-lock.json
      
      - name: Install dependencies
        run: |
          cd frontend
          npm ci
      
      - name: Run tests
        run: |
          cd frontend
          npm run test
      
      - name: Run linting
        run: |
          cd frontend
          npm run lint
      
      - name: Build application
        run: |
          cd frontend
          npm run build

  test-backend:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:15
        env:
          POSTGRES_PASSWORD: postgres
          POSTGRES_DB: testdb
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
        ports:
          - 5432:5432

    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Cache Maven dependencies
        uses: actions/cache@v3
        with:
          path: ~/.m2
          key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
      
      - name: Run tests
        run: |
          cd backend
          ./mvnw test
      
      - name: Build application
        run: |
          cd backend
          ./mvnw clean package -DskipTests

  deploy:
    needs: [test-frontend, test-backend]
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Deploy to production
        run: |
          # Add your deployment script here
          echo "Deploying to production..."
```

---

## 📊 Monitoring & Analytics

### Performance Metrics
```typescript
// lib/analytics.ts
export const trackPerformance = () => {
  if (typeof window !== 'undefined') {
    // Core Web Vitals
    new PerformanceObserver((list) => {
      for (const entry of list.getEntries()) {
        if (entry.entryType === 'largest-contentful-paint') {
          console.log('LCP:', entry.startTime);
        }
      }
    }).observe({ entryTypes: ['largest-contentful-paint'] });
  }
};

// Track brutalist interactions
export const trackBrutalistInteraction = (component: string, action: string) => {
  // Analytics tracking for brutalist UI interactions
  console.log(`Brutalist ${component}: ${action}`);
};
```

### Business Metrics
```java
// service/AnalyticsService.java
@Service
@RequiredArgsConstructor
public class AnalyticsService {
    
    private final MeterRegistry meterRegistry;
    
    public void trackDealCreated(UUID dealId, BigDecimal value) {
        Counter.builder("deals.created")
            .tag("value_range", getValueRange(value))
            .register(meterRegistry)
            .increment();
    }
    
    public void trackTaskCompleted(UUID taskId, TaskPriority priority) {
        Counter.builder("tasks.completed")
            .tag("priority", priority.name())
            .register(meterRegistry)
            .increment();
    }
    
    private String getValueRange(BigDecimal value) {
        if (value.compareTo(new BigDecimal("10000")) < 0) return "small";
        if (value.compareTo(new BigDecimal("100000")) < 0) return "medium";
        return "large";
    }
}
```

---

## 🎯 Success Criteria

### Technical KPIs
- **Performance**: < 2s initial page load, < 100ms API response time
- **Availability**: 99.9% uptime with proper monitoring
- **Security**: Zero critical vulnerabilities, OWASP compliance
- **Scalability**: Support 1000+ concurrent users

### Business KPIs
- **User Adoption**: 80% daily active users within 3 months
- **Task Completion**: 90% completion rate improvement
- **Deal Velocity**: 25% faster deal closure
- **User Satisfaction**: 4.5+ rating in user feedback

### Design KPIs
- **Accessibility**: WCAG 2.1 AA compliance
- **Responsive**: Perfect rendering on all device sizes
- **Brand Consistency**: 100% adherence to brutalist design system
- **Performance**: 90+ Lighthouse score

---

This implementation plan provides a comprehensive roadmap for building your Neo-Brutalist CRM application. Each phase builds upon the previous one, ensuring a solid foundation while maintaining the bold, edgy aesthetic that defines Neo-Brutalism in web design.
