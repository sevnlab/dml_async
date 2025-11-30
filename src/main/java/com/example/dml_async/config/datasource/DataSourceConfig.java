package com.example.dml_async.config.datasource;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * 멀티 DataSource 설정 클래스
 *
 * - dbA(), dbB() 를 생성
 * - RoutingDataSource에 두 개를 등록
 * - ThreadLocal(DB 키)에 따라 하나 선택됨
 */
@Configuration
public class DataSourceConfig {

    /**
     * A DB 연결 정보
     */
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.service-a")
    @Qualifier("serviceA")
    public DataSource serviceA() {
        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .build();
    }

    /**
     * B DB 연결 정보
     */
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.service-b")
    @Qualifier("serviceB")
    public DataSource serviceB() {
        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .build();
    }

    /**
     * RoutingDataSource 생성
     *
     * - "A" → dbA
     * - "B" → dbB
     */
    @Bean
    public ServiceRoutingDataSource routingDataSource(
            @Qualifier("serviceA") DataSource dbA,
            @Qualifier("serviceB") DataSource dbB
    ) {
        ServiceRoutingDataSource routing = new ServiceRoutingDataSource();

        Map<Object, Object> dataSourceMap = new HashMap<>();
        dataSourceMap.put("serviceA", dbA);
        dataSourceMap.put("serviceB", dbB);

        routing.setTargetDataSources(dataSourceMap);
        routing.setDefaultTargetDataSource(dbA); // serviceName 없을 때 fallback

        return routing;
    }

    /**
     * 트랜잭션 매니저는 routingDataSource 기준으로 동작
     */
    @Bean
    public DataSourceTransactionManager transactionManager(DataSource routingDataSource) {
        return new DataSourceTransactionManager(routingDataSource);
    }
}