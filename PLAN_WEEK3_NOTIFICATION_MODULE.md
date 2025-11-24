# TUẦN 3: HOÀN THIỆN NOTIFICATION MODULE

## 🎯 MỤC TIÊU TỔNG
- Build complete Notification Center UI
- Implement Notification Preferences
- Setup Email/Push notification delivery
- Test WebSocket fallback mechanisms
- Achieve notification feature parity with design

---

## 📅 SPRINT 3.1: NOTIFICATION CENTER UI (Ngày 1-3)

### **DAY 1: Build Notification Center Page**

#### **1.1. Create Notification Center Page**
```typescript
// File: src/app/admin/notifications/page.tsx

'use client'

import { useState } from 'react'
import { useNotifications, useMarkAsRead, useMarkAllAsRead } from '@/hooks/useNotifications'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Button } from '@/components/ui/button'
import { CheckCheck, Filter, Settings } from 'lucide-react'
import NotificationList from '@/components/notifications/notification-list'
import NotificationFilters from '@/components/notifications/notification-filters'
import { NotificationType, NotificationPriority } from '@/types/notification'

export default function NotificationsPage() {
  const [filter, setFilter] = useState<{
    type?: NotificationType
    priority?: NotificationPriority
    status?: 'UNREAD' | 'READ' | 'ARCHIVED'
  }>({})

  const { data, isLoading, refetch } = useNotifications({
    page: 1,
    limit: 50,
    ...filter,
  })

  const markAllMutation = useMarkAllAsRead()

  const handleMarkAllAsRead = async () => {
    await markAllMutation.mutateAsync()
    refetch()
  }

  return (
    <div className="container mx-auto py-8">
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-3xl font-bold">Notifications</h1>
          <p className="text-gray-600">
            {data?.unreadCount || 0} unread notification(s)
          </p>
        </div>

        <div className="flex gap-2">
          <Button
            variant="outline"
            onClick={handleMarkAllAsRead}
            disabled={!data?.unreadCount}
          >
            <CheckCheck className="w-4 h-4 mr-2" />
            Mark All as Read
          </Button>

          <Button
            variant="outline"
            onClick={() => window.location.href = '/admin/notifications/preferences'}
          >
            <Settings className="w-4 h-4 mr-2" />
            Preferences
          </Button>
        </div>
      </div>

      {/* Filters */}
      <div className="mb-6">
        <NotificationFilters filter={filter} onFilterChange={setFilter} />
      </div>

      {/* Tabs */}
      <Tabs defaultValue="all" className="w-full">
        <TabsList>
          <TabsTrigger value="all">All</TabsTrigger>
          <TabsTrigger value="unread">
            Unread {data?.unreadCount ? `(${data.unreadCount})` : ''}
          </TabsTrigger>
          <TabsTrigger value="archived">Archived</TabsTrigger>
        </TabsList>

        <TabsContent value="all">
          <NotificationList
            notifications={data?.content || []}
            isLoading={isLoading}
          />
        </TabsContent>

        <TabsContent value="unread">
          <NotificationList
            notifications={data?.content?.filter(n => n.status === 'UNREAD') || []}
            isLoading={isLoading}
          />
        </TabsContent>

        <TabsContent value="archived">
          <NotificationList
            notifications={data?.content?.filter(n => n.status === 'ARCHIVED') || []}
            isLoading={isLoading}
          />
        </TabsContent>
      </Tabs>
    </div>
  )
}
```

#### **1.2. Create Notification List Component**
```typescript
// File: src/components/notifications/notification-list.tsx

import { Notification } from '@/types/notification'
import NotificationItem from './notification-item'
import { Bell } from 'lucide-react'

interface NotificationListProps {
  notifications: Notification[]
  isLoading: boolean
}

export default function NotificationList({ notifications, isLoading }: NotificationListProps) {
  if (isLoading) {
    return (
      <div className="space-y-2">
        {[1, 2, 3].map(i => (
          <div key={i} className="h-20 bg-gray-100 animate-pulse rounded" />
        ))}
      </div>
    )
  }

  if (notifications.length === 0) {
    return (
      <div className="text-center py-12">
        <Bell className="w-16 h-16 mx-auto mb-4 text-gray-300" />
        <p className="text-gray-500">No notifications</p>
      </div>
    )
  }

  return (
    <div className="space-y-2">
      {notifications.map((notification) => (
        <NotificationItem
          key={notification.id}
          notification={notification}
        />
      ))}
    </div>
  )
}
```

#### **1.3. Enhance Notification Item Component**
```typescript
// File: src/components/notifications/notification-item.tsx
// (Enhance existing component if it exists)

import { Notification, NotificationPriority, NotificationType } from '@/types/notification'
import { useMarkAsRead, useArchiveNotification } from '@/hooks/useNotifications'
import { cn } from '@/lib/utils'
import { formatDistanceToNow } from 'date-fns'
import {
  Bell,
  CheckCircle,
  AlertCircle,
  Info,
  Archive,
  ExternalLink,
  MoreVertical
} from 'lucide-react'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { Button } from '@/components/ui/button'

interface NotificationItemProps {
  notification: Notification
}

const priorityStyles = {
  LOW: 'border-l-gray-400',
  NORMAL: 'border-l-blue-400',
  HIGH: 'border-l-orange-400',
  URGENT: 'border-l-red-600',
}

const typeIcons = {
  SYSTEM: Info,
  TASK_ASSIGNED: CheckCircle,
  TASK_UPDATED: AlertCircle,
  TASK_COMPLETED: CheckCircle,
  CUSTOMER_CREATED: Info,
  ACTIVITY_REMINDER: Bell,
}

export default function NotificationItem({ notification }: NotificationItemProps) {
  const markAsReadMutation = useMarkAsRead()
  const archiveMutation = useArchiveNotification()

  const Icon = typeIcons[notification.type] || Bell

  const handleClick = async () => {
    if (notification.status === 'UNREAD') {
      await markAsReadMutation.mutateAsync(notification.id)
    }

    if (notification.actionUrl) {
      window.location.href = notification.actionUrl
    }
  }

  const handleArchive = async (e: React.MouseEvent) => {
    e.stopPropagation()
    await archiveMutation.mutateAsync(notification.id)
  }

  return (
    <div
      onClick={handleClick}
      className={cn(
        'p-4 border-l-4 rounded-r bg-white hover:bg-gray-50 cursor-pointer transition-colors',
        priorityStyles[notification.priority],
        notification.status === 'UNREAD' && 'bg-blue-50'
      )}
    >
      <div className="flex items-start gap-3">
        {/* Icon */}
        <div className={cn(
          'p-2 rounded-full',
          notification.priority === 'URGENT' ? 'bg-red-100 text-red-600' :
          notification.priority === 'HIGH' ? 'bg-orange-100 text-orange-600' :
          'bg-blue-100 text-blue-600'
        )}>
          <Icon className="w-5 h-5" />
        </div>

        {/* Content */}
        <div className="flex-1 min-w-0">
          <div className="flex items-start justify-between gap-2">
            <div className="flex-1">
              <p className={cn(
                'font-medium',
                notification.status === 'UNREAD' && 'font-semibold'
              )}>
                {notification.title}
              </p>
              <p className="text-sm text-gray-600 mt-1">
                {notification.message}
              </p>
            </div>

            {/* Actions */}
            <DropdownMenu>
              <DropdownMenuTrigger asChild onClick={(e) => e.stopPropagation()}>
                <Button variant="ghost" size="icon" className="h-8 w-8">
                  <MoreVertical className="w-4 h-4" />
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end">
                {notification.status === 'UNREAD' && (
                  <DropdownMenuItem onClick={handleClick}>
                    <CheckCircle className="w-4 h-4 mr-2" />
                    Mark as read
                  </DropdownMenuItem>
                )}
                <DropdownMenuItem onClick={handleArchive}>
                  <Archive className="w-4 h-4 mr-2" />
                  Archive
                </DropdownMenuItem>
                {notification.actionUrl && (
                  <DropdownMenuItem onClick={handleClick}>
                    <ExternalLink className="w-4 h-4 mr-2" />
                    View details
                  </DropdownMenuItem>
                )}
              </DropdownMenuContent>
            </DropdownMenu>
          </div>

          {/* Timestamp */}
          <p className="text-xs text-gray-500 mt-2">
            {formatDistanceToNow(new Date(notification.createdAt), { addSuffix: true })}
          </p>
        </div>
      </div>
    </div>
  )
}
```

**Deliverables Day 1:**
- ✅ Notification center page with tabs
- ✅ Notification list with filtering
- ✅ Enhanced notification item component
- ✅ Mark as read/archive actions

---

### **DAY 2-3: Notification Preferences Page**

#### **2.1. Backend: Notification Preferences Entity**
```java
// File: src/main/java/com/neobrutalism/crm/domain/notification/model/NotificationPreference.java

package com.neobrutalism.crm.domain.notification.model;

import com.neobrutalism.crm.common.model.BaseEntity;
import com.neobrutalism.crm.domain.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "notification_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreference extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // Channel preferences
    @Column(name = "email_enabled")
    private boolean emailEnabled = true;

    @Column(name = "push_enabled")
    private boolean pushEnabled = true;

    @Column(name = "in_app_enabled")
    private boolean inAppEnabled = true;

    // Type-specific preferences
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "notification_preference_types",
        joinColumns = @JoinColumn(name = "preference_id")
    )
    @Column(name = "notification_type")
    @Enumerated(EnumType.STRING)
    private Set<NotificationType> enabledTypes;

    // Frequency
    @Enumerated(EnumType.STRING)
    @Column(name = "digest_frequency")
    private DigestFrequency digestFrequency = DigestFrequency.REAL_TIME;

    // Quiet hours
    @Column(name = "quiet_hours_start")
    private String quietHoursStart; // HH:mm format

    @Column(name = "quiet_hours_end")
    private String quietHoursEnd; // HH:mm format

    public enum DigestFrequency {
        REAL_TIME,
        HOURLY,
        DAILY,
        WEEKLY
    }
}
```

#### **2.2. Backend: Preferences Service**
```java
// File: src/main/java/com/neobrutalism/crm/domain/notification/service/NotificationPreferenceService.java

@Service
@RequiredArgsConstructor
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;
    private final UserContext userContext;

    @Transactional(readOnly = true)
    public NotificationPreference getPreferences() {
        String userId = userContext.getCurrentUserIdOrThrow();
        return preferenceRepository.findByUserId(UUID.fromString(userId))
            .orElseGet(() -> createDefaultPreferences(userId));
    }

    @Transactional
    public NotificationPreference updatePreferences(NotificationPreferenceRequest request) {
        String userId = userContext.getCurrentUserIdOrThrow();
        NotificationPreference preference = preferenceRepository.findByUserId(UUID.fromString(userId))
            .orElseGet(() -> createDefaultPreferences(userId));

        preference.setEmailEnabled(request.isEmailEnabled());
        preference.setPushEnabled(request.isPushEnabled());
        preference.setInAppEnabled(request.isInAppEnabled());
        preference.setEnabledTypes(request.getEnabledTypes());
        preference.setDigestFrequency(request.getDigestFrequency());
        preference.setQuietHoursStart(request.getQuietHoursStart());
        preference.setQuietHoursEnd(request.getQuietHoursEnd());

        return preferenceRepository.save(preference);
    }

    private NotificationPreference createDefaultPreferences(String userId) {
        NotificationPreference preference = NotificationPreference.builder()
            .user(userRepository.findById(UUID.fromString(userId)).orElseThrow())
            .emailEnabled(true)
            .pushEnabled(true)
            .inAppEnabled(true)
            .enabledTypes(Set.of(NotificationType.values()))
            .digestFrequency(DigestFrequency.REAL_TIME)
            .build();

        return preferenceRepository.save(preference);
    }

    // Check if notification should be sent based on preferences
    public boolean shouldSendNotification(String userId, NotificationType type, String channel) {
        NotificationPreference pref = preferenceRepository.findByUserId(UUID.fromString(userId))
            .orElse(null);

        if (pref == null) return true; // Default: send all

        // Check if type is enabled
        if (!pref.getEnabledTypes().contains(type)) return false;

        // Check channel
        return switch (channel) {
            case "EMAIL" -> pref.isEmailEnabled();
            case "PUSH" -> pref.isPushEnabled();
            case "IN_APP" -> pref.isInAppEnabled();
            default -> true;
        };
    }
}
```

#### **2.3. Frontend: Preferences Page**
```typescript
// File: src/app/admin/notifications/preferences/page.tsx

'use client'

import { useState } from 'react'
import { useNotificationPreferences, useUpdateNotificationPreferences } from '@/hooks/useNotifications'
import { Button } from '@/components/ui/button'
import { Switch } from '@/components/ui/switch'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { NotificationType } from '@/types/notification'
import { Save } from 'lucide-react'
import { toast } from 'sonner'

export default function NotificationPreferencesPage() {
  const { data: preferences, isLoading } = useNotificationPreferences()
  const updateMutation = useUpdateNotificationPreferences()

  const [formData, setFormData] = useState({
    emailEnabled: true,
    pushEnabled: true,
    inAppEnabled: true,
    enabledTypes: Object.values(NotificationType),
    digestFrequency: 'REAL_TIME',
    quietHoursStart: '',
    quietHoursEnd: '',
  })

  // Update formData when preferences load
  useEffect(() => {
    if (preferences) {
      setFormData(preferences)
    }
  }, [preferences])

  const handleSave = async () => {
    await updateMutation.mutateAsync(formData)
    toast.success('Preferences saved successfully')
  }

  if (isLoading) return <div>Loading...</div>

  return (
    <div className="container mx-auto py-8 max-w-3xl">
      <div className="mb-6">
        <h1 className="text-3xl font-bold">Notification Preferences</h1>
        <p className="text-gray-600">Manage how you receive notifications</p>
      </div>

      <div className="space-y-8 bg-white p-6 rounded-lg border">
        {/* Channels */}
        <section>
          <h2 className="text-xl font-semibold mb-4">Notification Channels</h2>
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <Label htmlFor="email">Email Notifications</Label>
                <p className="text-sm text-gray-600">Receive notifications via email</p>
              </div>
              <Switch
                id="email"
                checked={formData.emailEnabled}
                onCheckedChange={(checked) =>
                  setFormData({ ...formData, emailEnabled: checked })
                }
              />
            </div>

            <div className="flex items-center justify-between">
              <div>
                <Label htmlFor="push">Push Notifications</Label>
                <p className="text-sm text-gray-600">Receive browser push notifications</p>
              </div>
              <Switch
                id="push"
                checked={formData.pushEnabled}
                onCheckedChange={(checked) =>
                  setFormData({ ...formData, pushEnabled: checked })
                }
              />
            </div>

            <div className="flex items-center justify-between">
              <div>
                <Label htmlFor="inApp">In-App Notifications</Label>
                <p className="text-sm text-gray-600">Show notifications in the app</p>
              </div>
              <Switch
                id="inApp"
                checked={formData.inAppEnabled}
                onCheckedChange={(checked) =>
                  setFormData({ ...formData, inAppEnabled: checked })
                }
              />
            </div>
          </div>
        </section>

        {/* Notification Types */}
        <section>
          <h2 className="text-xl font-semibold mb-4">Notification Types</h2>
          <div className="space-y-3">
            {Object.values(NotificationType).map((type) => (
              <div key={type} className="flex items-center justify-between">
                <Label htmlFor={type}>{type.replace(/_/g, ' ')}</Label>
                <Switch
                  id={type}
                  checked={formData.enabledTypes.includes(type)}
                  onCheckedChange={(checked) => {
                    const newTypes = checked
                      ? [...formData.enabledTypes, type]
                      : formData.enabledTypes.filter(t => t !== type)
                    setFormData({ ...formData, enabledTypes: newTypes })
                  }}
                />
              </div>
            ))}
          </div>
        </section>

        {/* Frequency */}
        <section>
          <h2 className="text-xl font-semibold mb-4">Digest Frequency</h2>
          <Select
            value={formData.digestFrequency}
            onValueChange={(value) =>
              setFormData({ ...formData, digestFrequency: value })
            }
          >
            <SelectTrigger>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="REAL_TIME">Real-time (instant)</SelectItem>
              <SelectItem value="HOURLY">Hourly digest</SelectItem>
              <SelectItem value="DAILY">Daily digest</SelectItem>
              <SelectItem value="WEEKLY">Weekly digest</SelectItem>
            </SelectContent>
          </Select>
        </section>

        {/* Quiet Hours */}
        <section>
          <h2 className="text-xl font-semibold mb-4">Quiet Hours</h2>
          <p className="text-sm text-gray-600 mb-3">
            Don't send notifications during these hours
          </p>
          <div className="flex gap-4">
            <div className="flex-1">
              <Label>Start Time</Label>
              <input
                type="time"
                value={formData.quietHoursStart}
                onChange={(e) =>
                  setFormData({ ...formData, quietHoursStart: e.target.value })
                }
                className="w-full px-3 py-2 border rounded"
              />
            </div>
            <div className="flex-1">
              <Label>End Time</Label>
              <input
                type="time"
                value={formData.quietHoursEnd}
                onChange={(e) =>
                  setFormData({ ...formData, quietHoursEnd: e.target.value })
                }
                className="w-full px-3 py-2 border rounded"
              />
            </div>
          </div>
        </section>

        {/* Save Button */}
        <div className="pt-4 border-t">
          <Button
            onClick={handleSave}
            disabled={updateMutation.isPending}
            className="w-full"
          >
            <Save className="w-4 h-4 mr-2" />
            {updateMutation.isPending ? 'Saving...' : 'Save Preferences'}
          </Button>
        </div>
      </div>
    </div>
  )
}
```

**Deliverables Day 2-3:**
- ✅ Notification preferences backend
- ✅ Preferences UI page
- ✅ Channel toggles (email, push, in-app)
- ✅ Type-specific preferences
- ✅ Digest frequency settings
- ✅ Quiet hours configuration

---

## 📅 SPRINT 3.2: EMAIL & PUSH DELIVERY (Ngày 4-7)

### **Email Integration (Spring Boot + JavaMailSender)**

#### **4.1. Configure Email in application.yml**
```yaml
spring:
  mail:
    host: smtp.gmail.com # or your SMTP server
    port: 587
    username: ${EMAIL_USERNAME}
    password: ${EMAIL_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

#### **4.2. Create Email Service**
```java
// File: src/main/java/com/neobrutalism/crm/domain/notification/service/EmailNotificationService.java

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {

    private final JavaMailSender mailSender;
    private final NotificationPreferenceService preferenceService;

    @Async
    public void sendEmailNotification(Notification notification, String recipientEmail) {
        // Check preferences
        if (!preferenceService.shouldSendNotification(
            notification.getUserId().toString(),
            notification.getType(),
            "EMAIL"
        )) {
            log.debug("Email disabled for user {}", notification.getUserId());
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(recipientEmail);
            helper.setSubject(notification.getTitle());
            helper.setText(buildEmailContent(notification), true); // true = HTML

            mailSender.send(message);
            log.info("Email sent to {}", recipientEmail);
        } catch (Exception e) {
            log.error("Failed to send email to {}", recipientEmail, e);
        }
    }

    private String buildEmailContent(Notification notification) {
        // Use Thymeleaf or simple HTML template
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: #3b82f6; color: white; padding: 20px; }
                    .content { padding: 20px; background: #f9fafb; }
                    .footer { padding: 20px; text-align: center; color: #6b7280; }
                    .button { background: #3b82f6; color: white; padding: 10px 20px;
                              text-decoration: none; border-radius: 5px; display: inline-block; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>%s</h1>
                    </div>
                    <div class="content">
                        <p>%s</p>
                        %s
                    </div>
                    <div class="footer">
                        <p>© 2025 Neobrutalism CRM. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """,
            notification.getTitle(),
            notification.getMessage(),
            notification.getActionUrl() != null ?
                String.format("<a href='%s' class='button'>View Details</a>", notification.getActionUrl()) :
                ""
        );
    }
}
```

#### **4.3. Integrate Email into Notification Service**
```java
// File: src/main/java/com/neobrutalism/crm/domain/notification/service/NotificationService.java
// Add email sending after creating notification

@Async
public void sendNotification(NotificationRequest request) {
    // Create in-app notification
    Notification notification = createNotification(request);

    // Send email if enabled
    User user = userRepository.findById(request.getUserId()).orElse(null);
    if (user != null && user.getEmail() != null) {
        emailNotificationService.sendEmailNotification(notification, user.getEmail());
    }

    // Send push if enabled
    // pushNotificationService.sendPush(notification, user);

    // Send WebSocket real-time update
    webSocketService.sendNotificationToUser(user.getId(), notification);
}
```

### **Push Notifications (Optional - Web Push API)**

#### **4.4. Setup Web Push (Using Firebase Cloud Messaging)**
```typescript
// File: src/lib/push-notifications.ts

import { getMessaging, getToken, onMessage } from 'firebase/messaging'
import { initializeApp } from 'firebase/app'

const firebaseConfig = {
  apiKey: process.env.NEXT_PUBLIC_FIREBASE_API_KEY,
  projectId: process.env.NEXT_PUBLIC_FIREBASE_PROJECT_ID,
  messagingSenderId: process.env.NEXT_PUBLIC_FIREBASE_SENDER_ID,
  appId: process.env.NEXT_PUBLIC_FIREBASE_APP_ID,
}

const app = initializeApp(firebaseConfig)
const messaging = getMessaging(app)

export async function requestPushPermission() {
  try {
    const permission = await Notification.requestPermission()
    if (permission === 'granted') {
      const token = await getToken(messaging, {
        vapidKey: process.env.NEXT_PUBLIC_FIREBASE_VAPID_KEY,
      })
      // Send token to backend
      await savePushToken(token)
      return token
    }
  } catch (error) {
    console.error('Push permission denied', error)
  }
}

export function onPushMessage(callback: (payload: any) => void) {
  onMessage(messaging, callback)
}
```

**Deliverables Day 4-7:**
- ✅ Email notifications working
- ✅ Email templates (HTML)
- ✅ Push notifications setup (optional)
- ✅ Integration with preferences
- ✅ Async delivery with error handling

---

## ✅ DEFINITION OF DONE - TUẦN 3

- [ ] Notification center page complete
- [ ] Notification preferences working
- [ ] Email delivery integrated
- [ ] Push notifications (optional)
- [ ] WebSocket tested with fallback
- [ ] All notification features documented
- [ ] No TODO comments in Notification module
- [ ] Code reviewed and merged
