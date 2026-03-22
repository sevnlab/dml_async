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
