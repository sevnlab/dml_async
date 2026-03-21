package com.example.dml_async.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

/**
 * =====================================================
 * [AOP 구조 설명]
 *
 * @Aspect  : 이 클래스가 Aspect(공통 관심사 모음) 임을 선언
 * @Component: Spring Bean으로 등록 (AOP가 동작하려면 Bean이어야 함)
 *
 * [동작 원리]
 * Spring이 @Comments 붙은 메서드를 호출할 때
 * 실제 객체 대신 프록시(Proxy) 객체가 먼저 받아서
 * → logAudit() 실행 → proceed()로 원본 메서드 실행 → 이후 처리
 * =====================================================
 */
@Aspect
@Component
public class CommentsAspect {

    private static final Logger aspectLog = LoggerFactory.getLogger("INFO");

    /**
     * =====================================================
     * [Advice 설명]
     *
     * @Around : 메서드 실행 전후를 모두 감싸는 Advice
     *           proceed() 호출 전 = Before 구간
     *           proceed() 호출 후 = After 구간
     *           try-catch로 예외도 처리 가능
     *
     * "@annotation(comments)" : Pointcut 표현식
     *           → @Comments 애너테이션이 붙은 메서드에만 적용
     *           → 파라미터 이름(comments)과 @Around 표현식 이름이 반드시 일치해야 함
     *
     * ProceedingJoinPoint : 실행 중인 메서드의 정보를 담은 객체
     *           → joinPoint.getSignature()  : 메서드 시그니처
     *           → joinPoint.getArgs()       : 전달된 파라미터
     *           → joinPoint.getTarget()     : 실제 실행 객체
     *           → joinPoint.proceed()       : 원본 메서드 실행 ★ 반드시 호출해야 함
     *
     * Comments comments : @Comments 애너테이션 객체 (value() 값 접근용)
     * =====================================================
     */
    @Around("@annotation(comments)")
    public Object logAudit(ProceedingJoinPoint joinPoint, Comments comments) throws Throwable {

        String className  = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String operation  = comments.value().isEmpty() ? methodName : comments.value();
        Object[] args     = joinPoint.getArgs();

        Instant start = Instant.now();

        // ① 메서드 실행 전 로그
        aspectLog.info("[AUDIT][START] 작업={} | 클래스={}.{}() | 파라미터={}",
                operation, className, methodName, summarizeArgs(args));

        try {
            // ② 원본 메서드 실행
            //    proceed()를 호출하지 않으면 실제 메서드가 실행되지 않음
            Object result = joinPoint.proceed();

            long elapsed = Duration.between(start, Instant.now()).toMillis();

            // ③ 정상 완료 로그
            aspectLog.info("[AUDIT][SUCCESS] 작업={} | 소요={}ms", operation, elapsed);

            return result;

        } catch (Throwable e) {
            long elapsed = Duration.between(start, Instant.now()).toMillis();

            // ④ 예외 발생 로그
            aspectLog.error("[AUDIT][FAIL] 작업={} | 소요={}ms | 에러={}",
                    operation, elapsed, e.getMessage());

            // 예외는 반드시 다시 던져야 원래 흐름(트랜잭션 롤백 등) 유지됨
            throw e;
        }
    }

    /**
     * 파라미터를 문자열로 요약 (너무 길면 200자로 자름)
     * List<String> 형태의 대용량 데이터가 그대로 찍히지 않도록 처리
     */
    private String summarizeArgs(Object[] args) {
        if (args == null || args.length == 0) return "없음";
        String summary = Arrays.toString(args);
        return summary.length() > 200 ? summary.substring(0, 200) + "..." : summary;
    }
}
