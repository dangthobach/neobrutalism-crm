# Migration Error Retrieval Guide

## Hướng Dẫn Lấy Errors Từ Migration System

**Mục đích:** Hướng dẫn chi tiết cách lấy toàn bộ errors theo file Excel hoặc theo từng sheet.

**Cập nhật:** January 2025
**Trạng thái:** Production Ready ✅

---

## 📋 **TÓM TẮT CÁC ENDPOINT**

| Endpoint | Mục đích | Pagination | Response |
|----------|----------|------------|----------|
| `GET /api/migration/jobs/{jobId}/errors` | Lấy errors của **toàn bộ file Excel** (tất cả sheets) | ✅ Yes | List<MigrationErrorResponse> |
| `GET /api/migration/sheets/{sheetId}/errors` | Lấy errors của **1 sheet cụ thể** | ✅ Yes | MigrationErrorResponse |
| `GET /api/migration/jobs/{jobId}/progress` | Lấy summary errors (count only) | ❌ No | JobProgressInfo |

---

## 🎯 **USE CASE 1: Lấy Toàn Bộ Errors Của File Excel**

### **Scenario:**
Bạn upload file `migration_data.xlsx` với 3 sheets:
- Sheet 1: HSBG_HopDong (200,000 rows → 150 errors)
- Sheet 2: HSBG_Cif (200,000 rows → 80 errors)
- Sheet 3: HSBG_Tap (100,000 rows → 20 errors)

**Mục tiêu:** Lấy tất cả 250 errors của cả 3 sheets.

---

### **Bước 1: Upload File và Lấy JobId**

```bash
curl -X POST http://localhost:8080/api/migration/upload \
  -F "file=@migration_data.xlsx"
```

**Response:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "fileName": "migration_data.xlsx",
  "status": "PENDING",
  "totalSheets": 3,
  "createdAt": "2025-01-10T10:00:00Z"
}
```

**Lưu lại:** `jobId = 550e8400-e29b-41d4-a716-446655440000`

---

### **Bước 2: Đợi Migration Hoàn Thành**

**Option A - WebSocket (Recommended):**
```javascript
const socket = new SockJS('http://localhost:8080/ws/migration');
const stompClient = Stomp.over(socket);

stompClient.connect({}, () => {
  stompClient.subscribe(`/topic/migration/${jobId}`, (message) => {
    const progress = JSON.parse(message.body);

    if (progress.status === 'COMPLETED' || progress.status === 'FAILED') {
      console.log('Migration finished, fetching errors...');
      fetchErrors(jobId);
    }
  });
});
```

**Option B - Polling:**
```bash
# Kiểm tra status mỗi 5 giây
while true; do
  curl -s http://localhost:8080/api/migration/jobs/550e8400-e29b-41d4-a716-446655440000/progress \
    | jq '.status'
  sleep 5
done
```

---

### **Bước 3: Lấy TOÀN BỘ Errors Của File**

#### **Cách 1: Lấy Tất Cả Errors Một Lần (Recommended)**

```bash
# Lấy 10,000 errors (đủ cho hầu hết trường hợp)
curl -X GET "http://localhost:8080/api/migration/jobs/550e8400-e29b-41d4-a716-446655440000/errors?page=0&size=10000"
```

**Response Structure:**
```json
[
  {
    "jobId": "550e8400-e29b-41d4-a716-446655440000",
    "sheetId": "660e8400-e29b-41d4-a716-446655440001",
    "sheetName": "HSBG_HopDong",
    "totalErrors": 150,
    "errors": [
      {
        "id": "770e8400-e29b-41d4-a716-446655440001",
        "rowNumber": 150,
        "batchNumber": 1,
        "errorCode": "VALIDATION_ERROR",
        "errorMessage": "Email không hợp lệ",
        "validationRule": "EMAIL_FORMAT",
        "errorData": "{\"field\":\"email\",\"value\":\"invalid@email\",\"expected\":\"valid email format\"}",
        "createdAt": "2025-01-10T10:05:30Z"
      },
      {
        "id": "770e8400-e29b-41d4-a716-446655440002",
        "rowNumber": 500,
        "batchNumber": 1,
        "errorCode": "MISSING_REQUIRED_FIELD",
        "errorMessage": "Thiếu số hợp đồng",
        "validationRule": "REQUIRED_FIELD",
        "errorData": "{\"field\":\"contractNumber\",\"required\":true}",
        "createdAt": "2025-01-10T10:05:31Z"
      }
      // ... 148 more errors for this sheet
    ]
  },
  {
    "jobId": "550e8400-e29b-41d4-a716-446655440000",
    "sheetId": "660e8400-e29b-41d4-a716-446655440002",
    "sheetName": "HSBG_Cif",
    "totalErrors": 80,
    "errors": [
      {
        "id": "770e8400-e29b-41d4-a716-446655440101",
        "rowNumber": 200,
        "batchNumber": 1,
        "errorCode": "DUPLICATE_KEY",
        "errorMessage": "CIF đã tồn tại",
        "validationRule": "UNIQUE_CIF",
        "errorData": "{\"field\":\"cifNumber\",\"value\":\"CIF123456\",\"existingId\":\"uuid-xyz\"}",
        "createdAt": "2025-01-10T10:07:00Z"
      }
      // ... 79 more errors
    ]
  },
  {
    "jobId": "550e8400-e29b-41d4-a716-446655440000",
    "sheetId": "660e8400-e29b-41d4-a716-446655440003",
    "sheetName": "HSBG_Tap",
    "totalErrors": 20,
    "errors": [
      // ... 20 errors
    ]
  }
]
```

**Giải thích Response:**
- ✅ Trả về **array of MigrationErrorResponse** (1 object per sheet)
- ✅ Mỗi object chứa errors của 1 sheet cụ thể
- ✅ `totalErrors` = số errors của sheet đó (không phải tổng toàn job)
- ✅ `errors[]` = danh sách chi tiết errors

---

#### **Cách 2: Lấy Theo Pagination (Nếu Có Quá Nhiều Errors)**

**Ví dụ: File có 50,000 errors**

```bash
# Page 1: Lấy 1000 errors đầu tiên
curl -X GET "http://localhost:8080/api/migration/jobs/{jobId}/errors?page=0&size=1000"

# Page 2: Lấy 1000 errors tiếp theo
curl -X GET "http://localhost:8080/api/migration/jobs/{jobId}/errors?page=1&size=1000"

# Page 3...
curl -X GET "http://localhost:8080/api/migration/jobs/{jobId}/errors?page=2&size=1000"
```

**JavaScript Example:**
```javascript
async function fetchAllJobErrors(jobId) {
  let allErrors = [];
  let page = 0;
  const size = 1000;
  let hasMore = true;

  while (hasMore) {
    const response = await fetch(
      `http://localhost:8080/api/migration/jobs/${jobId}/errors?page=${page}&size=${size}`
    );
    const errors = await response.json();

    if (errors.length === 0) {
      hasMore = false;
    } else {
      // Flatten errors from all sheets
      errors.forEach(sheetError => {
        allErrors.push(...sheetError.errors);
      });
      page++;
    }
  }

  console.log(`Total errors fetched: ${allErrors.length}`);
  return allErrors;
}
```

---

## 🎯 **USE CASE 2: Lấy Errors Của 1 Sheet Cụ Thể**

### **Scenario:**
Bạn chỉ muốn xem errors của sheet "HSBG_HopDong", không quan tâm các sheet khác.

---

### **Bước 1: Lấy SheetId**

**Option A - Từ Progress API:**
```bash
curl -X GET http://localhost:8080/api/migration/jobs/{jobId}/progress
```

**Response (rút gọn):**
```json
{
  "jobId": "550e8400-e29b-41d4-a716-446655440000",
  "sheets": [
    {
      "sheetId": "660e8400-e29b-41d4-a716-446655440001",
      "sheetName": "HSBG_HopDong",
      "invalidRows": 150
    },
    {
      "sheetId": "660e8400-e29b-41d4-a716-446655440002",
      "sheetName": "HSBG_Cif",
      "invalidRows": 80
    }
  ]
}
```

**Lưu lại:**
- Sheet "HSBG_HopDong" → `sheetId = 660e8400-e29b-41d4-a716-446655440001`

---

### **Bước 2: Lấy Errors Của Sheet**

```bash
curl -X GET "http://localhost:8080/api/migration/sheets/660e8400-e29b-41d4-a716-446655440001/errors?page=0&size=10000"
```

**Response:**
```json
{
  "jobId": "550e8400-e29b-41d4-a716-446655440000",
  "sheetId": "660e8400-e29b-41d4-a716-446655440001",
  "sheetName": "HSBG_HopDong",
  "totalErrors": 150,
  "errors": [
    {
      "id": "770e8400-e29b-41d4-a716-446655440001",
      "rowNumber": 150,
      "batchNumber": 1,
      "errorCode": "VALIDATION_ERROR",
      "errorMessage": "Email không hợp lệ",
      "validationRule": "EMAIL_FORMAT",
      "errorData": "{\"field\":\"email\",\"value\":\"invalid@email\"}",
      "createdAt": "2025-01-10T10:05:30Z"
    }
    // ... 149 more errors
  ]
}
```

**Giải thích:**
- ✅ Chỉ trả về errors của sheet cụ thể
- ✅ Không có errors của các sheet khác
- ✅ Response là **single object** (không phải array)

---

## 📊 **USE CASE 3: Lấy Error Summary (Không Cần Chi Tiết)**

### **Scenario:**
Bạn chỉ muốn biết:
- Mỗi sheet có bao nhiêu errors?
- Không cần xem chi tiết từng error

```bash
curl -X GET http://localhost:8080/api/migration/jobs/{jobId}/progress
```

**Response (rút gọn):**
```json
{
  "jobId": "550e8400-e29b-41d4-a716-446655440000",
  "fileName": "migration_data.xlsx",
  "status": "COMPLETED",
  "sheets": [
    {
      "sheetName": "HSBG_HopDong",
      "totalRows": 200000,
      "processedRows": 200000,
      "validRows": 199850,
      "invalidRows": 150,
      "status": "COMPLETED"
    },
    {
      "sheetName": "HSBG_Cif",
      "totalRows": 200000,
      "processedRows": 200000,
      "validRows": 199920,
      "invalidRows": 80,
      "status": "COMPLETED"
    },
    {
      "sheetName": "HSBG_Tap",
      "totalRows": 100000,
      "processedRows": 100000,
      "validRows": 99980,
      "invalidRows": 20,
      "status": "COMPLETED"
    }
  ]
}
```

**Thông tin có được:**
- ✅ `invalidRows`: Số lượng errors mỗi sheet
- ❌ Không có chi tiết error messages

---

## 🔍 **PHÂN TÍCH CẤU TRÚC ERROR DATA**

### **ErrorDetail Object:**

```json
{
  "id": "770e8400-e29b-41d4-a716-446655440001",
  "rowNumber": 150,
  "batchNumber": 1,
  "errorCode": "VALIDATION_ERROR",
  "errorMessage": "Email không hợp lệ",
  "validationRule": "EMAIL_FORMAT",
  "errorData": "{\"field\":\"email\",\"value\":\"invalid@email\",\"expected\":\"valid email format\"}",
  "createdAt": "2025-01-10T10:05:30Z"
}
```

| Field | Type | Mô Tả | Ví Dụ |
|-------|------|-------|-------|
| **id** | UUID | ID duy nhất của error record | "770e8400-..." |
| **rowNumber** | Long | Số dòng trong Excel (1-based) | 150 |
| **batchNumber** | Integer | Batch thứ mấy (mỗi batch 1000 rows) | 1 (rows 1-1000) |
| **errorCode** | String | Mã lỗi | VALIDATION_ERROR, DUPLICATE_KEY, MISSING_REQUIRED_FIELD |
| **errorMessage** | String | Thông báo lỗi (tiếng Việt/English) | "Email không hợp lệ" |
| **validationRule** | String | Quy tắc validation vi phạm | EMAIL_FORMAT, UNIQUE_CIF, REQUIRED_FIELD |
| **errorData** | JSON String | Chi tiết error (field, value, expected) | JSON object as string |
| **createdAt** | Instant | Thời gian phát hiện lỗi | "2025-01-10T10:05:30Z" |

---

### **errorData JSON Structure:**

**Example 1: Email Validation Error**
```json
{
  "field": "email",
  "value": "invalid@email",
  "expected": "valid email format",
  "regex": "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$"
}
```

**Example 2: Missing Required Field**
```json
{
  "field": "contractNumber",
  "required": true,
  "message": "Contract number is mandatory"
}
```

**Example 3: Duplicate Key**
```json
{
  "field": "cifNumber",
  "value": "CIF123456",
  "existingId": "880e8400-e29b-41d4-a716-446655440000",
  "message": "CIF already exists in database"
}
```

**Example 4: Invalid Date Format**
```json
{
  "field": "contractDate",
  "value": "2025-13-45",
  "expected": "yyyy-MM-dd",
  "message": "Invalid date: month must be 1-12"
}
```

---

## 💡 **BEST PRACTICES**

### **1. Lấy Errors Sau Khi Migration Hoàn Thành**

```javascript
// ❌ BAD: Gọi quá sớm
fetch(`/api/migration/jobs/${jobId}/errors`)
  .then(res => res.json())
  .then(errors => {
    // Chưa có errors vì migration chưa xong!
  });

// ✅ GOOD: Đợi migration xong
stompClient.subscribe(`/topic/migration/${jobId}`, (message) => {
  const progress = JSON.parse(message.body);

  if (progress.status === 'COMPLETED') {
    // Bây giờ mới fetch errors
    fetch(`/api/migration/jobs/${jobId}/errors?page=0&size=10000`)
      .then(res => res.json())
      .then(errors => console.log('All errors:', errors));
  }
});
```

---

### **2. Xử Lý Pagination Đúng Cách**

```javascript
// ✅ Fetch all errors with pagination
async function fetchAllErrors(jobId) {
  const allSheetErrors = [];
  let page = 0;
  const pageSize = 1000;

  while (true) {
    const response = await fetch(
      `/api/migration/jobs/${jobId}/errors?page=${page}&size=${pageSize}`
    );

    const sheetErrorsPage = await response.json();

    // Check if we got any data
    if (!sheetErrorsPage || sheetErrorsPage.length === 0) {
      break; // No more data
    }

    allSheetErrors.push(...sheetErrorsPage);

    // Check if we got less than pageSize (last page)
    const totalErrorsInPage = sheetErrorsPage.reduce(
      (sum, sheet) => sum + sheet.errors.length,
      0
    );

    if (totalErrorsInPage < pageSize) {
      break; // Last page
    }

    page++;
  }

  return allSheetErrors;
}
```

---

### **3. Parse errorData JSON**

```javascript
// ✅ Parse errorData để lấy chi tiết
function displayError(error) {
  let errorData = {};

  try {
    errorData = JSON.parse(error.errorData);
  } catch (e) {
    console.error('Failed to parse errorData:', error.errorData);
  }

  console.log(`Row ${error.rowNumber}: ${error.errorMessage}`);
  console.log(`  Field: ${errorData.field}`);
  console.log(`  Invalid Value: ${errorData.value}`);
  console.log(`  Expected: ${errorData.expected}`);
}
```

---

### **4. Filter Errors By Type**

```javascript
// ✅ Lọc errors theo errorCode
function groupErrorsByType(errors) {
  const grouped = {};

  errors.forEach(sheetError => {
    sheetError.errors.forEach(error => {
      if (!grouped[error.errorCode]) {
        grouped[error.errorCode] = [];
      }
      grouped[error.errorCode].push({
        sheet: sheetError.sheetName,
        row: error.rowNumber,
        message: error.errorMessage
      });
    });
  });

  return grouped;
}

// Usage:
const allErrors = await fetchAllErrors(jobId);
const byType = groupErrorsByType(allErrors);

console.log('Validation Errors:', byType['VALIDATION_ERROR'].length);
console.log('Duplicate Keys:', byType['DUPLICATE_KEY'].length);
console.log('Missing Fields:', byType['MISSING_REQUIRED_FIELD'].length);
```

---

### **5. Export Errors to Excel**

```javascript
// ✅ Export errors to downloadable Excel file
async function exportErrorsToExcel(jobId) {
  const allErrors = await fetchAllErrors(jobId);

  // Flatten all errors into single array
  const flatErrors = [];
  allErrors.forEach(sheetError => {
    sheetError.errors.forEach(error => {
      const errorData = JSON.parse(error.errorData || '{}');
      flatErrors.push({
        'Sheet': sheetError.sheetName,
        'Row': error.rowNumber,
        'Batch': error.batchNumber,
        'Error Code': error.errorCode,
        'Error Message': error.errorMessage,
        'Validation Rule': error.validationRule,
        'Field': errorData.field || '',
        'Invalid Value': errorData.value || '',
        'Created At': new Date(error.createdAt).toLocaleString()
      });
    });
  });

  // Use library like SheetJS to create Excel file
  const ws = XLSX.utils.json_to_sheet(flatErrors);
  const wb = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(wb, ws, 'Errors');
  XLSX.writeFile(wb, `migration_errors_${jobId}.xlsx`);
}
```

---

## 🚀 **PERFORMANCE CONSIDERATIONS**

### **Số Lượng Errors Ước Tính:**

| File Size | Total Rows | Error Rate | Expected Errors | Fetch Time |
|-----------|-----------|------------|-----------------|------------|
| 1GB (3 sheets) | 600,000 | 0.1% (tốt) | 600 | <1s |
| 1GB (3 sheets) | 600,000 | 1% (trung bình) | 6,000 | 1-2s |
| 1GB (3 sheets) | 600,000 | 5% (tệ) | 30,000 | 3-5s |
| 5GB (10 sheets) | 5,000,000 | 1% | 50,000 | 5-10s |

**Khuyến nghị:**
- ✅ `size=10000` cho hầu hết trường hợp (1-5% error rate)
- ✅ Pagination với `size=1000` nếu errors > 10,000
- ⚠️ Cân nhắc caching nếu errors > 50,000

---

## 📋 **ENDPOINT SUMMARY TABLE**

| Endpoint | Method | Parameters | Returns | Use Case |
|----------|--------|------------|---------|----------|
| `/api/migration/jobs/{jobId}/errors` | GET | `page`, `size` | List<MigrationErrorResponse> | Lấy tất cả errors của file Excel (all sheets) |
| `/api/migration/sheets/{sheetId}/errors` | GET | `page`, `size` | MigrationErrorResponse | Lấy errors của 1 sheet cụ thể |
| `/api/migration/jobs/{jobId}/progress` | GET | - | JobProgressInfo | Lấy error count summary (không có detail) |
| `/ws/migration` | WS | - | - | Subscribe để biết khi nào migration xong |

---

## ✅ **QUICK REFERENCE**

### **Cách Lấy TOÀN BỘ Errors Của File Excel (3 Bước):**

```bash
# 1. Upload file → lấy jobId
RESPONSE=$(curl -X POST http://localhost:8080/api/migration/upload \
  -F "file=@data.xlsx")
JOB_ID=$(echo $RESPONSE | jq -r '.id')

# 2. Đợi migration xong (check status)
while true; do
  STATUS=$(curl -s http://localhost:8080/api/migration/jobs/$JOB_ID/progress \
    | jq -r '.status')
  if [ "$STATUS" = "COMPLETED" ] || [ "$STATUS" = "FAILED" ]; then
    break
  fi
  sleep 5
done

# 3. Lấy tất cả errors
curl -X GET "http://localhost:8080/api/migration/jobs/$JOB_ID/errors?page=0&size=10000" \
  > errors.json

echo "Errors saved to errors.json"
```

---

## 🆘 **TROUBLESHOOTING**

### **Issue 1: Response trống mặc dù có errors**

**Nguyên nhân:** Migration chưa hoàn thành, errors chưa được lưu.

**Fix:**
```javascript
// Đợi status = COMPLETED hoặc FAILED trước khi fetch errors
if (progress.status === 'COMPLETED' || progress.status === 'FAILED') {
  fetchErrors(jobId);
}
```

---

### **Issue 2: Chỉ lấy được 100 errors**

**Nguyên nhân:** Default `size=100` trong controller.

**Fix:**
```bash
# Tăng size parameter
curl "...errors?page=0&size=10000"
```

---

### **Issue 3: Không biết sheetId**

**Nguyên nhân:** SheetId không được trả về trong upload response.

**Fix:**
```bash
# Lấy sheetId từ progress endpoint
curl http://localhost:8080/api/migration/jobs/{jobId}/progress \
  | jq '.sheets[] | {sheetId, sheetName, invalidRows}'
```

---

## 📝 **CONCLUSION**

**Hệ thống hiện tại hỗ trợ đầy đủ:**

✅ Lấy toàn bộ errors của file Excel (tất cả sheets)
✅ Lấy errors của từng sheet riêng lẻ
✅ Pagination cho file lớn
✅ Chi tiết đầy đủ (row number, error message, field, value)
✅ Error metadata (errorCode, validationRule, errorData JSON)

**Workflow chuẩn:**
1. Upload file → Nhận `jobId`
2. Subscribe WebSocket hoặc poll progress → Đợi `status = COMPLETED`
3. Call `/jobs/{jobId}/errors?size=10000` → Lấy tất cả errors
4. Parse `errorData` JSON → Hiển thị chi tiết
5. (Optional) Export to Excel cho user review

---

**Last Updated:** January 2025
**Version:** 1.0
**Status:** Production Ready ✅
