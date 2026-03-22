package com.example.dml_async.async.listener;


import com.example.dml_async.aop.Comments;
import com.example.dml_async.config.ChunkProcessor;
import com.example.dml_async.async.dto.AsyncEventDto;
import com.example.dml_async.async.service.DmlService;
import com.example.dml_async.common.util.SystemUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.util.StringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 비동기 이벤트 처리, 알림, 로그, 예외처리
 */
@Component
@RequiredArgsConstructor
public class AsyncEventListener {

    private static final Logger log = LoggerFactory.getLogger("INFO");

    private final ChunkProcessor chunkProcessor;
    private final DmlService dmlService;

    // 현재 실행 중인 스레드 풀 상태 확인
    private final ThreadPoolTaskExecutor asyncExecutor;

    // 대용량 INSERT 전용 멀티스레드 executor
    private final ThreadPoolTaskExecutor bulkInsertExecutor;

    // UPDATE/SELECT 청크 크기 (현 환경 프로젝트에 맞게 조정)
    private static final int CHUNK_SIZE = 300;

    // INSERT 전용 청크 크기 (배치 INSERT는 크게 잡을수록 효율적)
    private static final int INSERT_CHUNK_SIZE = 5000;

    /** Async => Controller 요청에 대한 처리 */
    @Async("asyncExecutor")
    @EventListener
    public void handleEvent(AsyncEventDto event) {

        // INSERT 는 전용 고속 경로로 분기 (기존 UPDATE/SELECT 로직 불변)
        if ("INSERT".equals(event.getDmlType())) {
            handleBulkInsert(event);
            return;
        }

        int totalCount = 0;

        log.info("비동기 이벤트 처리 시작");
        log.info("===================================");
        log.info("[AsyncService] asyncExecutor Thread Pool Info");
        log.info("CorePoolSize : {}", asyncExecutor.getCorePoolSize());
        log.info("MaxPoolSize : {}", asyncExecutor.getMaxPoolSize());
        log.info("PoolSize(현재): {}", asyncExecutor.getPoolSize());
        log.info("ActiveCount(활성화): {}", asyncExecutor.getActiveCount());
        log.info("QueueSize(대기): {}", asyncExecutor.getThreadPoolExecutor().getQueue().size());
        log.info("===================================");

        SystemUtil.printStatus("비동기 프로세스 시작");
        Instant start = Instant.now();

        Path filePath = Path.of(event.getUploadFilePath());
        log.info("파일 경로 : {}", filePath);

        try (Stream<String> lines = Files.lines(filePath)) {

            // 파일 읽기
            List<String> allLines = lines.map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            totalCount = allLines.size();
            log.info("파일 개수 = " + totalCount);
            SystemUtil.printStatus("파일 읽기 완료");

            submitChunksInBatches(allLines, totalCount, event);

        } catch (IOException | InterruptedException e) {
            log.error("파일 읽기 오류 = " + filePath, e);
        }

        Instant end = Instant.now();
        log.info("전체 작업 완료 (소요시간: {}초)", Duration.between(start, end).toSeconds());
    }

    /**
     * 대용량 INSERT 고속 처리
     * - 파일 스트리밍 (40M건도 메모리 안전)
     * - INSERT_CHUNK_SIZE 단위로 잘라 bulkInsertExecutor 에 병렬 제출
     * - CallerRunsPolicy 로 자동 backpressure (큐 초과 시 읽기 스레드가 직접 처리)
     * - Thread.sleep 없음
     */
    private void handleBulkInsert(AsyncEventDto event) {
        log.info("===================================");
        log.info("[BulkInsert] 대용량 INSERT 시작");
        log.info("[BulkInsert] 테이블: {}", event.getJobName());
        log.info("[BulkInsert] bulkInsertExecutor ThreadPool - Core: {}, Max: {}",
                bulkInsertExecutor.getCorePoolSize(), bulkInsertExecutor.getMaxPoolSize());
        log.info("===================================");

        SystemUtil.printStatus("대용량 INSERT 시작");

        Instant start = Instant.now();

        Path filePath = Path.of(event.getUploadFilePath());
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        AtomicInteger totalInserted = new AtomicInteger(0);
        AtomicInteger chunkIndex = new AtomicInteger(0);

        // Files.lines() 로 읽으면 전체행수 x 라인크기에 해당하는 많은 메모리가 필요하여,
        // 스트림으로 5000건씩만 메모리에 올림
        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {

            // 첫 줄 = DBeaver 헤더 → 컬럼명으로 파싱
            String headerLine = reader.readLine();

            if (headerLine == null || headerLine.trim().isEmpty()) {
                log.error("[BulkInsert] 파일 첫 줄(헤더)이 없습니다: {}", filePath);
                throw new RuntimeException("[BulkInsert] 파일 첫 줄(헤더)이 없습니다: " + filePath);
            }
            String delimiter = "^";
            List<String> columnNames = List.of(headerLine.split(java.util.regex.Pattern.quote(delimiter), -1))
                    .stream().map(String::trim).collect(Collectors.toList());
            log.info("[BulkInsert] 파싱된 컬럼명 ({}개): {}", columnNames.size(), columnNames);

            List<String> chunk = new ArrayList<>(INSERT_CHUNK_SIZE);
            String line;

            // 한 줄씩 읽으면서 5000건 모이면 즉시 제출
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                chunk.add(line);

                if (chunk.size() >= INSERT_CHUNK_SIZE) {
                    // 복사후 제출
                    // 원본 chunk 를 바로 넘기면 chunk.clear() 시 이미 제출된 작업 데이터도 사라짐, 방어적 복사
                    submitInsertChunk(new ArrayList<>(chunk), event, columnNames, futures, totalInserted, chunkIndex);
                    chunk.clear(); // 즉시 비움
                }
            }

            // 마지막 나머지 청크
            if (!chunk.isEmpty()) {
                submitInsertChunk(new ArrayList<>(chunk), event, columnNames, futures, totalInserted, chunkIndex);
            }

        } catch (IOException e) {
            log.error("[BulkInsert] 파일 읽기 오류: {}", filePath, e);
            return;
        }

        log.info("[BulkInsert] 총 {}개 청크 제출 완료, 모든 INSERT 완료 대기 중...", chunkIndex.get());

        // 모든 INSERT 완료까지 대기
        // 모든 청크 Future 가 완료될 때까지 대기. 총 소요시간 측정 및 완료 로그 출력 가능
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        Instant end = Instant.now();
        log.info("[BulkInsert] 완료 - 총 {}건, 소요시간: {}초",
                totalInserted.get(), Duration.between(start, end).toSeconds());
        SystemUtil.printStatus("대용량 INSERT 완료");
    }

    // 청크 비동기 제출
    private void submitInsertChunk(List<String> chunk, AsyncEventDto event, List<String> columnNames,
                                   List<CompletableFuture<Void>> futures,
                                   AtomicInteger totalInserted, AtomicInteger chunkIndex) {

        int idx = chunkIndex.incrementAndGet();

        // future 를 리스트에 저장해서 allOf() 에서 전체 완료 추적
        futures.add(CompletableFuture.runAsync(() -> {
            try {
                dmlService.processInsert(chunk, event, columnNames);
                int done = totalInserted.addAndGet(chunk.size());
                log.info("[BulkInsert] 청크 #{} 완료 ({}건, 누적: {}건)", idx, chunk.size(), done);
            } catch (Exception e) {
                log.error("[BulkInsert] 청크 #{} INSERT 실패 (size={})", idx, chunk.size(), e);
            }
        }, bulkInsertExecutor));
    }

    private void submitChunksInBatches(List<String> allLines, int totalCount, AsyncEventDto eventDto) throws IOException, InterruptedException {
        int chunkCount = (int) Math.ceil((double) totalCount / CHUNK_SIZE);
        log.info("총 {}개의 청크로 분리하여 DML 실행", chunkCount);

        if(eventDto.getDmlType().equals("SELECT")) {
            dmlService.initSelectProcess(chunkCount);
        }

        for (int i = 0; i < totalCount; i += CHUNK_SIZE) {
            int end = Math.min(i + CHUNK_SIZE, totalCount);

            List<String> chunk = new ArrayList<>(allLines.subList(i, end));
            Thread.sleep(4000);

            chunkProcessor.processChunkAsync(chunk, eventDto);
        }

    }
}