package com.example.dml_async.aop;

import java.lang.annotation.*;

/**
 * AOP 증적 로깅용 커스텀 애너테이션
 *
 * 사용법: @Comments("작업명") → 해당 메서드 실행 전후로 audit.log에 증적 기록
 *
 * @Target   : 이 애너테이션을 메서드에만 붙일 수 있도록 제한
 * @Retention: 런타임 시점까지 애너테이션 정보 유지 (AOP가 런타임에 읽으므로 필수)
 * @Documented: JavaDoc에 포함
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Comments {
    /**
     * 증적 로그에 남길 작업 이름
     * 예: @Comments("INSERT_CHUNK"), @Comments("API_REQUEST")
     */
    String value() default "";
}