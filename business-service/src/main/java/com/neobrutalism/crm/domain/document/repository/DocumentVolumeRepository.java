package com.neobrutalism.crm.domain.document.repository;

import com.neobrutalism.crm.domain.document.model.DocumentVolume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentVolumeRepository extends JpaRepository<DocumentVolume, UUID> {
    
    @Query("SELECT dv FROM DocumentVolume dv WHERE dv.boxCode = :boxCode AND dv.tenantId = :tenantId AND dv.isDeleted = false")
    List<DocumentVolume> findByBoxCodeAndTenantId(
        @Param("boxCode") String boxCode, 
        @Param("tenantId") UUID tenantId
    );
    
    @Query("SELECT CASE WHEN COUNT(dv) > 0 THEN true ELSE false END FROM DocumentVolume dv " +
           "WHERE dv.volumeName = :volumeName AND dv.boxCode = :boxCode AND dv.tenantId = :tenantId AND dv.isDeleted = false")
    boolean existsByVolumeNameAndBoxCodeAndTenantId(
        @Param("volumeName") String volumeName,
        @Param("boxCode") String boxCode,
        @Param("tenantId") UUID tenantId
    );
}
