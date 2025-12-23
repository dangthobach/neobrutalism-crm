# Migration Error Architecture - Critical Issue

## 🚨 **CRITICAL ISSUE DISCOVERED**

**Date:** January 2025
**Severity:** HIGH
**Impact:** Error retrieval endpoints return EMPTY or INCOMPLETE results

---

## ❌ **VẤN ĐỀ**

### **Errors Được Lưu Ở 2 Nơi Khác Nhau:**

#### **1. Staging Tables (WHERE ERRORS ACTUALLY ARE)**
```sql
-- Errors are stored in JSONB columns in staging tables
staging_hsbg_hop_dong.validation_errors  -- JSONB with error details
staging_hsbg_cif.validation_errors       -- JSONB with error details
staging_hsbg_tap.validation_errors       -- JSONB with error details
```

**Evidence from ExcelMigrationService.java:**
```java
// Line 368, 454, 537 - Errors are saved to staging.validationErrors
if (!validationResult.isValid()) {
    staging.setValidationErrors(
        objectMapper.writeValueAsString(validationResult.getErrors())
    );
    invalidCount++;
}
```

#### **2. MigrationError Table (CURRENTLY EMPTY OR UNUSED)**
```sql
-- Dedicated error table
migration_errors (
    id UUID,
    sheet_id UUID,
    row_number BIGINT,
    error_code VARCHAR,
    error_message TEXT,
    validation_rule VARCHAR,
    error_data JSONB,
    created_at TIMESTAMP
)
```

**Current Repository:**
```java
// MigrationErrorEntityRepository.java - Line 20
List<MigrationError> findErrorsBySheetId(@Param("sheetId") UUID sheetId);
// ❌ Returns EMPTY because no code writes to migration_errors table!
```

---

## 🔍 **ROOT CAUSE ANALYSIS**

### **What Went Wrong:**

1. **Design Intent:** Originally planned to have dedicated `migration_errors` table
2. **Implementation:** Errors are actually stored in staging table `validation_errors` column
3. **API Mismatch:** Error retrieval endpoints query `migration_errors` table (EMPTY)
4. **Result:** `/api/migration/jobs/{jobId}/errors` returns [] even when errors exist

### **Code Flow:**

```
User uploads file with errors
  ↓
ExcelMigrationService validates records
  ↓
staging.setValidationErrors(JSON)  ← Errors saved HERE (staging tables)
  ↓
staging.setValidationStatus("INVALID")
  ↓
Save to staging_hsbg_hop_dong / staging_hsbg_cif / staging_hsbg_tap
  ↓
MigrationErrorService queries migration_errors table  ← WRONG TABLE!
  ↓
Returns [] (empty)
```

---

## 📊 **IMPACT ASSESSMENT**

### **Affected Endpoints:**

| Endpoint | Current Behavior | Expected Behavior |
|----------|------------------|-------------------|
| `GET /api/migration/jobs/{jobId}/errors` | Returns `[]` | Should return all errors from all sheets |
| `GET /api/migration/sheets/{sheetId}/errors` | Returns `{errors: []}` | Should return errors for that sheet |
| `GET /api/migration/jobs/{jobId}/progress` | Shows `invalidRows: 150` | Correct (count is accurate) |

### **User Impact:**

✅ **What Works:**
- Error **count** is correct (`invalidRows` in progress)
- Validation is working (invalid records are marked)
- Migration processing is correct

❌ **What's Broken:**
- Error **details** cannot be retrieved
- Users cannot see WHY records failed
- Cannot export error reports
- Cannot fix data issues without error messages

---

## ✅ **SOLUTION OPTIONS**

### **Option 1: Fix Repositories to Query Staging Tables (RECOMMENDED)**

**Pros:**
- ✅ Quick fix (no data migration needed)
- ✅ No code changes to validation logic
- ✅ Errors are already in staging tables

**Cons:**
- ⚠️ Need to union 3 staging tables
- ⚠️ JSONB parsing in query

**Implementation:**

#### **New Repository Method:**
```java
// MigrationErrorRepository.java (Native Query)
@Query(value = """
    SELECT
        s.id::text as sheet_id,
        s.sheet_name,
        sh.id::text as record_id,
        sh.row_number,
        sh.validation_errors,
        sh.validation_status
    FROM staging_hsbg_hop_dong sh
    JOIN excel_migration_sheets s ON sh.sheet_id = s.id
    WHERE sh.job_id = :jobId
      AND sh.validation_status = 'INVALID'

    UNION ALL

    SELECT
        s.id::text as sheet_id,
        s.sheet_name,
        sc.id::text as record_id,
        sc.row_number,
        sc.validation_errors,
        sc.validation_status
    FROM staging_hsbg_cif sc
    JOIN excel_migration_sheets s ON sc.sheet_id = s.id
    WHERE sc.job_id = :jobId
      AND sc.validation_status = 'INVALID'

    UNION ALL

    SELECT
        s.id::text as sheet_id,
        s.sheet_name,
        st.id::text as record_id,
        st.row_number,
        st.validation_errors,
        st.validation_status
    FROM staging_hsbg_tap st
    JOIN excel_migration_sheets s ON st.sheet_id = s.id
    WHERE st.job_id = :jobId
      AND st.validation_status = 'INVALID'

    ORDER BY sheet_name, row_number
    LIMIT :limit OFFSET :offset
    """, nativeQuery = true)
List<Object[]> findAllInvalidRecordsByJobId(
    @Param("jobId") UUID jobId,
    @Param("limit") int limit,
    @Param("offset") int offset
);
```

#### **For Single Sheet:**
```java
@Query(value = """
    SELECT
        id::text as record_id,
        row_number,
        validation_errors::text,
        validation_status
    FROM staging_hsbg_hop_dong
    WHERE sheet_id = :sheetId
      AND validation_status = 'INVALID'
    ORDER BY row_number
    LIMIT :limit OFFSET :offset
    """, nativeQuery = true)
List<Object[]> findInvalidRecordsHopDong(
    @Param("sheetId") UUID sheetId,
    @Param("limit") int limit,
    @Param("offset") int offset
);

// Similar for staging_hsbg_cif and staging_hsbg_tap
```

#### **Service Layer Changes:**
```java
@Service
public class MigrationErrorService {

    public List<MigrationErrorResponse> getJobErrors(UUID jobId, int page, int size) {
        // Query all 3 staging tables with UNION
        List<Object[]> rawErrors = errorRepository.findAllInvalidRecordsByJobId(
            jobId,
            size,
            page * size
        );

        // Parse JSONB validation_errors column
        // Group by sheet
        // Return structured response
    }

    public MigrationErrorResponse getSheetErrors(UUID sheetId, int page, int size) {
        // Determine sheet type (HopDong, Cif, or Tap)
        MigrationSheet sheet = sheetRepository.findById(sheetId).orElseThrow();

        List<Object[]> rawErrors;
        if (sheet.getSheetName().contains("hop_dong")) {
            rawErrors = errorRepository.findInvalidRecordsHopDong(sheetId, size, page * size);
        } else if (sheet.getSheetName().contains("CIF")) {
            rawErrors = errorRepository.findInvalidRecordsCif(sheetId, size, page * size);
        } else if (sheet.getSheetName().contains("tap")) {
            rawErrors = errorRepository.findInvalidRecordsTap(sheetId, size, page * size);
        }

        // Parse JSONB and build response
    }
}
```

---

### **Option 2: Populate migration_errors Table (REFACTOR)**

**Pros:**
- ✅ Clean separation of concerns
- ✅ Existing endpoints work without changes
- ✅ Better query performance (indexed table)

**Cons:**
- ❌ Need to refactor validation flow
- ❌ Duplicate data storage
- ❌ More complex write path

**Implementation:**

```java
// ExcelMigrationService.java - After validation
if (!validationResult.isValid()) {
    // Save to staging table (existing)
    staging.setValidationErrors(
        objectMapper.writeValueAsString(validationResult.getErrors())
    );

    // NEW: Also save to migration_errors table
    for (ValidationError error : validationResult.getErrors()) {
        MigrationError errorEntity = MigrationError.builder()
            .sheetId(sheetId)
            .rowNumber(rowNumber)
            .errorCode(error.getErrorCode())
            .errorMessage(error.getMessage())
            .validationRule(error.getRule())
            .errorData(objectMapper.writeValueAsString(error.getDetails()))
            .build();

        migrationErrorRepository.save(errorEntity);
    }
}
```

---

### **Option 3: Hybrid Approach (BALANCED)**

**Pros:**
- ✅ Quick win: Fix retrieval immediately (Option 1)
- ✅ Long-term: Populate migration_errors for performance (Option 2)

**Cons:**
- ⚠️ Two-phase implementation

**Implementation:**

**Phase 1 (Immediate Fix):**
- Implement Option 1 native queries
- Deploy to unblock users

**Phase 2 (Optimization):**
- Add background job to sync staging errors → migration_errors
- Switch to querying migration_errors table
- Better performance for large datasets

---

## 📋 **RECOMMENDED SOLUTION**

### **OPTION 1 - Fix Repositories (Immediate)**

**Why:**
1. ✅ Quickest path to working error retrieval
2. ✅ No data migration needed
3. ✅ Errors are already there, just need correct query
4. ✅ Can be deployed immediately

**Implementation Steps:**

1. Create native queries to UNION all 3 staging tables
2. Update `MigrationErrorService` to parse JSONB
3. Update `MigrationErrorResponse` DTO if needed
4. Test with sample data
5. Deploy

**Estimated Effort:** 2-4 hours

---

## 🔧 **DETAILED IMPLEMENTATION PLAN**

### **Step 1: Create New Repository Interface**

```java
// File: MigrationStagingErrorRepository.java
@Repository
public interface MigrationStagingErrorRepository {

    @Query(value = """
        -- UNION query shown above
        """, nativeQuery = true)
    List<Object[]> findAllInvalidRecordsByJobId(...);

    @Query(value = "SELECT COUNT(*) FROM (...)", nativeQuery = true)
    long countInvalidRecordsByJobId(@Param("jobId") UUID jobId);
}
```

### **Step 2: Update MigrationErrorService**

```java
@Service
@RequiredArgsConstructor
public class MigrationErrorService {

    private final MigrationStagingErrorRepository stagingErrorRepo; // NEW
    private final MigrationSheetRepository sheetRepository;
    private final ObjectMapper objectMapper;

    public List<MigrationErrorResponse> getJobErrors(UUID jobId, int page, int size) {
        // Use new repository
        List<Object[]> rawErrors = stagingErrorRepo.findAllInvalidRecordsByJobId(
            jobId, size, page * size
        );

        // Group by sheet and parse errors
        Map<UUID, List<ErrorDetail>> errorsBySheet = new HashMap<>();

        for (Object[] row : rawErrors) {
            UUID sheetId = UUID.fromString((String) row[0]);
            Long rowNumber = ((Number) row[3]).longValue();
            String errorsJson = (String) row[4]; // validation_errors JSONB

            // Parse JSONB
            List<ValidationError> errors = objectMapper.readValue(
                errorsJson,
                new TypeReference<List<ValidationError>>() {}
            );

            // Convert to ErrorDetail
            for (ValidationError ve : errors) {
                ErrorDetail detail = ErrorDetail.builder()
                    .rowNumber(rowNumber)
                    .errorCode(ve.getErrorCode())
                    .errorMessage(ve.getMessage())
                    .validationRule(ve.getRule())
                    .errorData(objectMapper.writeValueAsString(ve.getDetails()))
                    .build();

                errorsBySheet.computeIfAbsent(sheetId, k -> new ArrayList<>())
                    .add(detail);
            }
        }

        // Build response grouped by sheet
        List<MigrationErrorResponse> responses = new ArrayList<>();
        for (Map.Entry<UUID, List<ErrorDetail>> entry : errorsBySheet.entrySet()) {
            MigrationSheet sheet = sheetRepository.findById(entry.getKey()).orElseThrow();

            responses.add(MigrationErrorResponse.builder()
                .jobId(jobId)
                .sheetId(entry.getKey())
                .sheetName(sheet.getSheetName())
                .totalErrors(entry.getValue().size())
                .errors(entry.getValue())
                .build());
        }

        return responses;
    }
}
```

### **Step 3: Test Query Manually**

```sql
-- Test query to verify errors exist in staging tables
SELECT
    s.sheet_name,
    COUNT(*) as error_count,
    MIN(sh.row_number) as first_error_row,
    MAX(sh.row_number) as last_error_row
FROM staging_hsbg_hop_dong sh
JOIN excel_migration_sheets s ON sh.sheet_id = s.id
WHERE sh.validation_status = 'INVALID'
GROUP BY s.sheet_name

UNION ALL

SELECT
    s.sheet_name,
    COUNT(*) as error_count,
    MIN(sc.row_number) as first_error_row,
    MAX(sc.row_number) as last_error_row
FROM staging_hsbg_cif sc
JOIN excel_migration_sheets s ON sc.sheet_id = s.id
WHERE sc.validation_status = 'INVALID'
GROUP BY s.sheet_name

UNION ALL

SELECT
    s.sheet_name,
    COUNT(*) as error_count,
    MIN(st.row_number) as first_error_row,
    MAX(st.row_number) as last_error_row
FROM staging_hsbg_tap st
JOIN excel_migration_sheets s ON st.sheet_id = s.id
WHERE st.validation_status = 'INVALID'
GROUP BY s.sheet_name;
```

---

## 🧪 **TESTING CHECKLIST**

### **Before Fix:**
```bash
# Upload file with known validation errors
curl -X POST .../upload -F "file=@test_with_errors.xlsx"

# Check progress (should show invalidRows > 0)
curl .../jobs/{jobId}/progress
# Response: {"sheets": [{"invalidRows": 150}]}

# Try to get errors (BROKEN - returns [])
curl .../jobs/{jobId}/errors
# Response: []  ← WRONG!
```

### **After Fix:**
```bash
# Same file
curl -X POST .../upload -F "file=@test_with_errors.xlsx"

# Check progress
curl .../jobs/{jobId}/progress
# Response: {"sheets": [{"invalidRows": 150}]}  ← Same

# Get errors (FIXED - returns actual errors)
curl .../jobs/{jobId}/errors
# Response: [
#   {
#     "sheetName": "HSBG_HopDong",
#     "totalErrors": 150,
#     "errors": [
#       {"rowNumber": 10, "errorMessage": "Email không hợp lệ"},
#       {"rowNumber": 25, "errorMessage": "Thiếu số hợp đồng"}
#     ]
#   }
# ]  ← CORRECT!
```

---

## ⚠️ **MIGRATION NOTES**

### **No Data Migration Required**

✅ Errors are already in staging tables
✅ Just need to query correct tables
✅ Can deploy without downtime
✅ Backward compatible (existing data works)

### **Cleanup Options (Future)**

Once Option 1 is working, consider:
1. Remove unused `migration_errors` table (if never populated)
2. OR: Add background job to populate it for performance
3. OR: Keep both for redundancy

---

## 📊 **PERFORMANCE CONSIDERATIONS**

### **UNION Query Performance:**

| Records | Staging Tables | Query Time | Index Needed |
|---------|---------------|------------|--------------|
| 1k errors | 3 tables | 10-20ms | validation_status |
| 10k errors | 3 tables | 50-100ms | validation_status + row_number |
| 50k errors | 3 tables | 200-500ms | Same + consider pagination |

### **Recommended Indexes:**

```sql
-- Add indexes for fast error queries
CREATE INDEX idx_staging_hopdong_invalid
    ON staging_hsbg_hop_dong(sheet_id, validation_status, row_number)
    WHERE validation_status = 'INVALID';

CREATE INDEX idx_staging_cif_invalid
    ON staging_hsbg_cif(sheet_id, validation_status, row_number)
    WHERE validation_status = 'INVALID';

CREATE INDEX idx_staging_tap_invalid
    ON staging_hsbg_tap(sheet_id, validation_status, row_number)
    WHERE validation_status = 'INVALID';
```

---

## ✅ **ACTION ITEMS**

### **Immediate (Priority 1):**
- [ ] Implement UNION native query in new repository
- [ ] Update MigrationErrorService to use new repository
- [ ] Add JSONB parsing logic
- [ ] Test with sample data containing errors
- [ ] Deploy fix

### **Short-term (Priority 2):**
- [ ] Add partial indexes on validation_status='INVALID'
- [ ] Add performance monitoring for error queries
- [ ] Document JSONB structure in staging tables

### **Long-term (Priority 3):**
- [ ] Evaluate Option 2 (dedicated migration_errors table)
- [ ] Add background sync job if needed
- [ ] Consider materialized view for error queries

---

**Last Updated:** January 2025
**Status:** ⚠️ Critical Issue - Fix Required
**Priority:** HIGH
