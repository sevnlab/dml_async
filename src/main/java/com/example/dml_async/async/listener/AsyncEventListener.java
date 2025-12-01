package com.example.dml_async.async.listener;


import com.example.dml_async.config.ChunkProcessor;
import com.example.dml_async.async.dto.AsyncEventDto;
import com.example.dml_async.async.dto.ResultDto;
import com.example.dml_async.async.service.DmlService;
import com.example.dml_async.common.util.SystemUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 비동기 이벤트 처리, 알림, 로그, 배치 트리거
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncEventListener {
    private final ChunkProcessor chunkProcessor;
    private final DmlService dmlService;

    // 빈 직접 주입 스레드 풀 상태 확인용
    private final ThreadPoolTaskExecutor asyncExecutor;

    // 대량 업데이트 대상 파일 경로
    // private static final Path FILE_PATH = Path.of("/dat/seven/service/proc/seven-tx/updateFile.txt");

    // 청크 크기 (한 번에 업데이트할 단위)
    private static final int CHUNK_SIZE = 300;

    /** Async => Controller 응답이 즉시 리턴 */
    @Async("asyncExecutor")
    @EventListener
    public void handleEvent(AsyncEventDto event) {

        int totalCount = 0;

        log.info("비동기 이벤트 수신");
        log.info("===================================");
        log.info("[AsyncService] asyncExecutor Thread Pool Info");
        log.info("CorePoolSize : {}", asyncExecutor.getCorePoolSize());
        log.info("MaxPoolSize : {}", asyncExecutor.getMaxPoolSize());
        log.info("PoolSize(현재): {}", asyncExecutor.getPoolSize());
        log.info("ActiveCount(실행중): {}", asyncExecutor.getActiveCount());
        log.info("QueueSize(대기): {}", asyncExecutor.getThreadPoolExecutor().getQueue().size());
        log.info("===================================");

        SystemUtil.printStatus("비동기 업데이트 시작");
        Instant start = Instant.now();

        Path filePath = Path.of(event.getFilePath());
        log.info("파일 경로 : {}", filePath);

        // Files.lines()는 내부적으로 BufferedReader를 사용, 공백 제거, "" 필터링
//        try (Stream<String> lines = Files.lines(Path.of(event.getFilePath()))) {
        try (Stream<String> lines = Files.lines(filePath)) {

            // [1] 파일 전체 읽기 (1회)
            List<String> allLines = lines.map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            totalCount = allLines.size();
            log.info("총 {}건의 데이터 확인됨", totalCount);
            SystemUtil.printStatus("파일 읽기 완료");

            // [2] 비동기 청크 처리 시작
            submitChunksInBatches(allLines, totalCount, event);

        } catch (IOException e) {
            log.error("파일 읽기 실패: {}", filePath, e);
        }

        Instant end = Instant.now();
        log.info("전체 작업 등록 완료 (총 소요: {}초)", Duration.between(start, end).toSeconds());
    }

    /**
     * 파일 데이터를 CHUNK_SIZE 단위로 나누어 비동기 DML 수행
     */
    private void submitChunksInBatches(List<String> allLines, int totalCount, AsyncEventDto eventDto) throws IOException {
        int chunkCount = (int) Math.ceil((double) totalCount / CHUNK_SIZE);
        log.info("총 {}개의 청크로 분할하여 DML 시작", chunkCount);

        for (int i = 0; i < totalCount; i += CHUNK_SIZE) {
            int end = Math.min(i + CHUNK_SIZE, totalCount);

            /** new ArrayList<>(subList()) 로 참조 복사 → GC가 원본 리스트를 유지하지 않도록 함 */
            List<String> chunk = new ArrayList<>(allLines.subList(i, end));

            /** 각 청크를 완전히 별도 스레드로 실행 */
            chunkProcessor.processChunkAsync(chunk, eventDto);
        }

        if(eventDto.getDmlType().equals("SELECT")) {
            List<ResultDto> resultDtoList = dmlService.getTotalResult();

            // 저장
            log.info("저장될 데이터 건수 : {}", resultDtoList.size());

            // txt 로 저장
            BufferedWriter bw = new BufferedWriter(new FileWriter("C:\\Users\\tekim\\Desktop\\updateFile.txt"));

            for(ResultDto d : resultDtoList) {
                bw.write(d.toString());
                bw.newLine();
            }
            bw.close();
        }
    }


}
