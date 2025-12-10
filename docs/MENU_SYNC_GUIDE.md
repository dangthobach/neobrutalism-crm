# Menu Synchronization Guide

## Tổng quan

Tính năng **Menu Sync** cho phép bạn đồng bộ hóa cấu trúc menu từ code UI sang database một cách tự động, giúp quản lý menu động và dễ dàng hơn.

## Cách sử dụng

### 1. Xem trước cấu trúc Menu từ UI

1. Truy cập trang **Menu Management**: `http://localhost:3000/admin/menus`
2. Click nút **"Preview UI Menus (X)"** để xem trước cấu trúc menu hiện có trong code
3. Dialog sẽ hiển thị toàn bộ cấu trúc menu dạng tree với:
   - Tên menu
   - Code
   - Icon
   - Route (nếu có)

### 2. Đồng bộ Menu vào Database

**Cách 1: Từ trang Menus**
1. Click nút **"Sync from UI"**
2. Confirm dialog sẽ hiển thị số lượng menu sẽ được import
3. Click **"OK"** để bắt đầu đồng bộ

**Cách 2: Từ Preview Dialog**
1. Click **"Preview UI Menus"**
2. Xem trước cấu trúc
3. Click **"Sync to Database"** trong dialog

### 3. Kết quả Sync

Sau khi sync, bạn sẽ nhận được thông báo với:
- **Số menu đã tạo**: Menus mới được thêm vào database
- **Số menu đã bỏ qua**: Menus đã tồn tại (kiểm tra bằng `code`)
- **Lỗi** (nếu có): Chi tiết lỗi khi sync

## Cấu trúc Menu UI

Menu được định nghĩa trong file: `src/lib/menu-sync-util.ts`

```typescript
const UI_MENUS: UIMenuItem[] = [
  {
    code: 'USER_MANAGEMENT',
    name: 'User Management',
    icon: '👥',
    displayOrder: 1,
    children: [
      {
        code: 'USERS',
        name: 'Users',
        icon: '👤',
        route: '/admin/users',
        description: 'Manage user accounts',
        displayOrder: 1,
      },
      // ... more children
    ],
  },
  // ... more root menus
]
```

## Thêm Menu mới vào UI

Để thêm menu mới:

1. Mở file `src/lib/menu-sync-util.ts`
2. Thêm menu vào mảng `UI_MENUS`:

```typescript
{
  code: 'MY_NEW_MENU',
  name: 'My New Menu',
  icon: '🆕',
  route: '/admin/my-new-menu', // optional
  displayOrder: 5,
  children: [ // optional
    {
      code: 'SUBMENU_1',
      name: 'Submenu 1',
      route: '/admin/my-new-menu/submenu1',
      displayOrder: 1,
    }
  ]
}
```

3. Save file
4. Vào trang Menus và click **"Sync from UI"**
5. Menu mới sẽ được tự động tạo trong database

## Lưu ý quan trọng

### Code uniqueness
- Mỗi menu phải có `code` duy nhất
- Khi sync, nếu menu với `code` đã tồn tại, nó sẽ được **bỏ qua** (không cập nhật)
- Để cập nhật menu, bạn cần xóa menu cũ hoặc sửa trực tiếp trong database

### Thứ tự hiển thị
- `displayOrder` quyết định thứ tự hiển thị của menu
- Số càng nhỏ, menu càng ở trên

### Menu cha con
- Menu có thể có nhiều cấp lồng nhau
- `parentId` tự động được set khi sync menu con
- Không thể xóa menu cha nếu còn menu con

### Route
- Route là optional
- Menu cha thường không có route (chỉ dùng để nhóm)
- Menu con (leaf nodes) nên có route

## Workflow khuyến nghị

1. **Development**:
   - Tạo menu mới trong code (`menu-sync-util.ts`)
   - Test local
   - Sync vào database

2. **Production**:
   - Deploy code mới
   - Chạy sync một lần duy nhất
   - Sau đó quản lý menu trực tiếp từ UI hoặc database

3. **Maintenance**:
   - Sử dụng trang Menu Management để:
     - Thay đổi thứ tự hiển thị
     - Ẩn/hiện menu
     - Thêm menu mới
     - Xóa menu không cần thiết

## Troubleshooting

### Lỗi "Menu already exists"
- Đây không phải lỗi, menu đã tồn tại nên bị bỏ qua
- Nếu muốn cập nhật, xóa menu cũ trước

### Lỗi "Cannot find parent menu"
- Đảm bảo menu cha được tạo trước menu con
- Sync sẽ tự động tạo theo thứ tự đúng

### Menu không hiển thị sau khi sync
- Kiểm tra `isVisible = true`
- Kiểm tra permissions của role hiện tại
- Refresh browser cache

## API Endpoints liên quan

- `GET /api/menus` - Lấy danh sách menus
- `GET /api/menus/code/{code}` - Lấy menu theo code
- `POST /api/menus` - Tạo menu mới
- `PUT /api/menus/{id}` - Cập nhật menu
- `DELETE /api/menus/{id}` - Xóa menu

## Example: Thêm nhóm menu "Reports"

```typescript
// Trong src/lib/menu-sync-util.ts, thêm vào UI_MENUS:
{
  code: 'REPORTS',
  name: 'Reports',
  icon: '📊',
  displayOrder: 5,
  children: [
    {
      code: 'SALES_REPORT',
      name: 'Sales Report',
      icon: '💰',
      route: '/admin/reports/sales',
      displayOrder: 1,
    },
    {
      code: 'USER_ACTIVITY_REPORT',
      name: 'User Activity',
      icon: '📈',
      route: '/admin/reports/activity',
      displayOrder: 2,
    },
    {
      code: 'ANALYTICS',
      name: 'Analytics',
      icon: '🔍',
      route: '/admin/reports/analytics',
      displayOrder: 3,
    }
  ]
}
```

Sau đó sync, bạn sẽ có:
- 1 menu cha: "Reports" (REPORTS)
- 3 menu con: Sales Report, User Activity, Analytics
- Tất cả đều visible và require authentication
