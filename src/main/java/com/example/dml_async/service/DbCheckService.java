package com.example.dml_async.service;

import com.example.dml_async.config.datasource.DatabaseContextHolder;
import com.example.dml_async.config.datasource.ServiceRoutingDataSource;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
@Service
@RequiredArgsConstructor
public class DbCheckService {

    private final ServiceRoutingDataSource routingDataSource;

    public String getCurrentDbInfo() {
        // 현재 KEY
        String key = DatabaseContextHolder.get();

        // 실제 선택된 DB DataSource
        DataSource resolved = routingDataSource.getResolvedDataSource();

        try {
            HikariDataSource hikari = resolved.unwrap(HikariDataSource.class);

            return """
                현재 선택된 DB 정보:
                - KEY: %s
                - URL: %s
                - USER: %s
                """.formatted(
                    key,
                    hikari.getJdbcUrl(),
                    hikari.getUsername()
            );

        } catch (Exception e) {
            return "Hikari Unwrap 실패 → DataSource 타입: " + resolved.getClass().getName()
                    + "\nmessage=" + e.getMessage();
        }
    }
}