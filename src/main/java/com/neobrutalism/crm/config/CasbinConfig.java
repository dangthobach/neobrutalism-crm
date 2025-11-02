package com.neobrutalism.crm.config;

import org.casbin.jcasbin.main.Enforcer;
import org.casbin.jcasbin.persist.Adapter;
import org.casbin.adapter.JDBCAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.sql.DataSource;

/**
 * Casbin Configuration
 * Cấu hình Casbin Enforcer với JDBC Adapter
 */
@Configuration
public class CasbinConfig {

    /**
     * Tạo Casbin Enforcer bean
     * Sử dụng JDBC Adapter để lưu trữ policies trong database
     */
    @Bean
    public Enforcer enforcer(DataSource dataSource) {
        try {
            // Load model từ classpath
            String modelPath = new ClassPathResource("casbin/model.conf").getURL().getPath();

            // Tạo JDBC Adapter
            Adapter adapter = new JDBCAdapter(dataSource);

            // Tạo Enforcer
            Enforcer enforcer = new Enforcer(modelPath, adapter);

            // Load policies từ database
            enforcer.loadPolicy();

            // Enable auto-save (tự động lưu khi thêm/xóa policy)
            enforcer.enableAutoSave(true);

            return enforcer;
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Casbin Enforcer", e);
        }
    }
}
