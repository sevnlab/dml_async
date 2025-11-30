package com.example.dml_async.config.datasource;


/**
 * ThreadLocal에 DB 선택 키(serviceName)를 저장하는 클래스
 *
 * - API 진입 스레드에서 set()
 * - RoutingDataSource가 get() 해서 DB 선택
 * - Async Thread로 값을 전파(TaskDecorator로)
 */
public class DatabaseContextHolder {

    // Thread 별로 독립된 값을 저장하는 공간
    private static final ThreadLocal<String> CONTEXT = new ThreadLocal<>();

    // 현재 스레드에 DB 키 저장 ("A" or "B" 등)
    public static void set(String key) {
        CONTEXT.set(key);
    }

    // 현재 스레드에 설정된 값 꺼내기
    public static String get() {
        return CONTEXT.get();
    }

    // 작업 완료 후 값 제거 (메모리 누수 방지)
    public static void clear() {
        CONTEXT.remove();
    }

}
