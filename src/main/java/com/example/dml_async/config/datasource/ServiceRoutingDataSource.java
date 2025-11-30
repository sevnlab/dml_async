package com.example.dml_async.config.datasource;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;

/**
 * 스프링이 DB 커넥션을 가져올 때
 * 어떤 DB를 선택할지 판단하는 클래스
 *
 * - DatabaseContextHolder.get() 값을 기준으로
 *   설정된 DataSource 중 하나를 반환
 */
public class ServiceRoutingDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        // ThreadLocal에서 현재 DB 키를 가져와 라우팅 결정
        String key = DatabaseContextHolder.get();

        // 지정한 서비스만 허용, 서비스를 찾지못하면 첫번째 DB설정을 자동으로 바라봄
        if (!"serviceA".equals(key) && !"serviceB".equals(key)) {
            throw new IllegalArgumentException("Unknown service key: " + key);
        }

        return key;
    }


    /**
     * 스프링 내부에서 매 SQL 실행마다 자동으로 호출
     */
    public DataSource getResolvedDataSource() {
        return determineTargetDataSource();
    }
}