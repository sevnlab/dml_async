package com.example.dml_async.async.service;

import com.example.dml_async.aop.Comments;
import com.example.dml_async.async.dto.AsyncEventDto;
import com.example.dml_async.async.repository.AsyncRepository;
import com.example.dml_async.common.util.SystemUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class DmlService {

    private final AsyncRepository asyncRepository;
    private static final Logger log = LoggerFactory.getLogger("INFO");
    private static final List<String> resultStorage = Collections.synchronizedList(new ArrayList());
    private final AtomicInteger finishChunk = new AtomicInteger(0);
    private int totalChunkCount;


    @Transactional
    public void updateChunk(List<String> pkList, String jobName) {
        this.asyncRepository.bulkUpdateByPkList(pkList, jobName);
    }

    @Transactional(
            readOnly = true
    )
    public void processSelect(List<String> pkList, AsyncEventDto eventDto) {
        List<String> list = this.asyncRepository.bulkSelect(pkList, eventDto.getJobName());
        resultStorage.addAll(list);
        int finishedChunkCount = this.finishChunk.incrementAndGet();
        log.info("완료 청크 개수 : {}", finishedChunkCount);
        if (finishedChunkCount == this.totalChunkCount) {
            this.createFile(resultStorage, eventDto.getDownloadFilePath());
        }

    }

    /**
     * 대용량 INSERT 청크 처리
     * - rawLines: TXT 파일에서 읽은 원본 라인 목록
     * - 구분자로 분리 후 bulkInsert 호출
     */
    @Comments("BULK_INSERT_CHUNK")
    @Transactional
    public void processInsert(List<String> rawLines, AsyncEventDto eventDto, List<String> columnNames) {
        String delimiter = "^";

        // delimiter 에 넣은 값을 리터럴로 처리
        // -1  = 마지막 빈 값도 보존.
        List<String[]> rows = rawLines.stream()
                .map(line -> line.split(Pattern.quote(delimiter), -1))
                .collect(java.util.stream.Collectors.toList());

        asyncRepository.bulkInsert(rows, eventDto.getJobName(), columnNames);
    }

    /**
     * 테이블 전체 SELECT → txt 파일 출력
     * - 첫 줄: 컬럼명 (^ 구분자)
     * - 이후: 데이터 행 (^ 구분자)
     */
    @Transactional(readOnly = true)
    public void processSelectAll(AsyncEventDto eventDto) {
        log.info("[SelectAll] 테이블 조회 시작: {}", eventDto.getJobName());
        List<String> lines = asyncRepository.selectAllFromTable(eventDto.getJobName());
        log.info("[SelectAll] 조회 완료: {}건 (헤더 포함)", lines.size());
        createFile(lines, eventDto.getDownloadFilePath());
    }

    /**
     * INSERT 실패 청크를 별도 파일에 append 저장
     * - 파일명: 원본파일명_FAILED.txt (예: data.txt → data_FAILED.txt)
     * - 헤더는 파일이 새로 생성될 때만 첫 줄에 한 번 기록
     * - 멀티스레드 환경이므로 synchronized 로 파일 쓰기 직렬화
     */
    public synchronized void saveFailedChunk(List<String> rawLines, List<String> columnNames, String uploadFilePath) {
        // 원본 파일 경로에서 _FAILED.txt 파일명 생성
        // 예: D:\data\TB_TRADE.txt → D:\data\TB_TRADE_FAILED.txt
        String failedFilePath;
        int dotIndex = uploadFilePath.lastIndexOf('.');
        if (dotIndex > 0) {
            failedFilePath = uploadFilePath.substring(0, dotIndex) + "_FAILED" + uploadFilePath.substring(dotIndex);
        } else {
            failedFilePath = uploadFilePath + "_FAILED.txt";
        }

        File file = new File(failedFilePath);
        boolean isNew = !file.exists();

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, true))) { // append 모드
            // 파일이 새로 생성되는 경우에만 헤더 기록
            if (isNew) {
                bw.write(String.join("^", columnNames));
                bw.newLine();
            }
            for (String line : rawLines) {
                bw.write(line);
                bw.newLine();
            }
            log.info("[BulkInsert] 실패 데이터 {}건 → {}", rawLines.size(), failedFilePath);
        } catch (IOException e) {
            log.error("[BulkInsert] 실패 파일 저장 오류: {}", failedFilePath, e);
        }
    }

    public void processDeleteAll(AsyncEventDto eventDto) {
        log.info("[Delete] 테이블 전체 삭제 시작: {}", eventDto.getJobName());
        asyncRepository.truncateTable(eventDto.getJobName());
        log.info("[Delete] 테이블 전체 삭제 완료: {}", eventDto.getJobName());
    }

    public void initSelectProcess(int ChunkCount) {
        this.totalChunkCount = ChunkCount;
        this.finishChunk.set(0);
        resultStorage.clear();
    }

    public void createFile(List<String> data, String downloadFilePath) {
        log.info("파일에 저장할 데이터 건수 : {}", data.size());
        File file = new File(downloadFilePath);
        SystemUtil.printStatus("파일 쓰기 시작");

        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(file));

            try {
                Iterator var5 = data.iterator();

                while(var5.hasNext()) {
                    String d = (String)var5.next();
                    bw.write(d);
                    bw.newLine();
                }
            } catch (Throwable var8) {
                try {
                    bw.close();
                } catch (Throwable var7) {
                    var8.addSuppressed(var7);
                }

                throw var8;
            }

            bw.close();
        } catch (IOException var9) {
            log.info("파일 생성 실패 : {}", var9.getMessage());
            throw new RuntimeException(var9);
        }

        SystemUtil.printStatus("파일 쓰기 완료");
    }
}
