package com.example.dml_async.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * DB I/O 허용량 기준으로 병렬도 결정하는 법
     * 1. 싱글 스레드로 1000건 업데이트 시간을 측정.
     * 2. 병렬도를 점차 늘려서 처리속도 향상 비율이 1.5배 이하로 떨어지는 지점이 I/O 허용 한계
     */

    @Bean(name = "asyncExecutor")
//	public Executor updateExecutor() {
    public ThreadPoolTaskExecutor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 싱글 스레드 구성
		executor.setCorePoolSize(1);
		executor.setMaxPoolSize(1);
		executor.setQueueCapacity(1);

         // 동시에 실행할 기본 스레드 수
         // CPU 코어 개수와 비슷하게 하는게 적절
//        executor.setCorePoolSize(4);

        // 큐가 가득 찼을때 추가로 확장 가능한 최대 스레드 수
        // DB 과부하 방지용 제한, CPU 코어 개수 2배정도가 적절
        // DB I/O 여유가 많으면 좀더 높여도 무방, 동시 커밋/락 경쟁이 우려된다면 낮춰야함
//        executor.setMaxPoolSize(8);

        // 대기 큐 용량
        // 해당개수만큼 비동기 작업대기 가능, 너무 크면 메모리 점유율 높아짐
        // 청크 총 개수 ~500개  100~200 권장
        // 청크 총 개수 1000~3000개 300~500 권장
        // 청크 총 개수 1만개 이상 1000 정도(초과는 비효율)
//        executor.setQueueCapacity(500);

        /**
         * 스레드풀이 꽉차면, 다음 요청은 비동기 대신 동기적으로 실행
         * 거절(RejectedExecution) 예외 안 뜨고, 시스템이 스스로 속도를 늦추며 안정적으로 처리
         */
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 로그 확인 시 스레드 식별, 디버깅시 사용
        // 로그에 지정한 이름 [Async-Update-1], [Async-DML-2] 형태로 찍힘
        executor.setThreadNamePrefix("Async-DML-");
        executor.initialize();
        return executor;
    }

}
