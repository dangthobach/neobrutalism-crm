# 🚀 Excel Migration Implementation Guide

## Quick Start

### 1. Database Setup

Run migration:
```bash
mvn flyway:migrate
```

### 2. Create DTOs for Each Sheet Type

```java
// HSBG_theo_hop_dong DTO
@Data
@ExcelSheet("HSBG_theo_hop_dong")
public class HSBGHopDongDTO {
    @ExcelColumn("Kho VPBank")
    private String khoVpbank;
    
    @ExcelColumn("Mã đơn vị")
    private String maDonVi;
    
    @ExcelColumn("Số hợp đồng")
    private String soHopDong;
    
    // ... all other columns
}
```

### 3. Implement Validators

```java
@Component
public class HSBGHopDongValidator implements SheetValidator<HSBGHopDongDTO> {
    
    @Override
    public ValidationResult validate(HSBGHopDongDTO dto, int rowNumber) {
        ValidationResult result = new ValidationResult();
        
        // Implement all CT1-CT8 rules
        // ...
        
        return result;
    }
}
```

### 4. Start Migration

```java
@RestController
@RequestMapping("/api/migration")
public class MigrationController {
    
    @PostMapping("/upload")
    public ResponseEntity<MigrationJob> uploadFile(
            @RequestParam("file") MultipartFile file) {
        MigrationJob job = migrationService.createMigrationJob(file);
        migrationService.startMigration(job.getId());
        return ResponseEntity.ok(job);
    }
    
    @GetMapping("/{jobId}/progress")
    public Flux<ServerSentEvent<JobProgressInfo>> streamProgress(
            @PathVariable UUID jobId) {
        return Flux.interval(Duration.ofSeconds(1))
            .map(seq -> ServerSentEvent.<JobProgressInfo>builder()
                .id(String.valueOf(seq))
                .event("progress")
                .data(progressService.getJobProgress(jobId))
                .build())
            .takeWhile(event -> 
                !event.getData().getStatus().isTerminal());
    }
}
```

## Next Steps

1. Implement all 3 DTOs (HSBG_theo_hop_dong, HSBG_theo_CIF, HSBG_theo_tap)
2. Implement validators with all business rules
3. Implement data normalizer
4. Implement duplicate detection
5. Implement master data writer
6. Set up monitoring dashboard

See `EXCEL_MIGRATION_PLAN.md` for complete architecture and implementation details.

