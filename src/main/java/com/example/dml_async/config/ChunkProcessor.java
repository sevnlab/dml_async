package com.example.dml_async.config;

import com.example.dml_async.async.dto.AsyncEventDto;
import com.example.dml_async.async.service.DmlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;


@Service
@RequiredArgsConstructor
public class ChunkProcessor {
    private static final Logger log = LoggerFactory.getLogger("INFO");
    private final DmlService dmlService;

    @Async("asyncExecutor")
    public CompletableFuture<Void> processChunkAsync(List<String> pkList, AsyncEventDto eventDto) {
        try {
            if(eventDto.getDmlType().equals("SELECT")) {
                dmlService.processSelect(pkList, eventDto);
//            } else if(eventDto.getDmlType().equals("SELECTALL")) {
//                dmlService.processSelectAll(pkList, eventDto);
            } else {
                dmlService.updateChunk(pkList, eventDto.getJobName());
            }
        } catch(Exception e) {
            log.error("[{}] ?? ??? ????(size={})", Thread.currentThread().getName(), pkList.size(), e);
        }
        return CompletableFuture.completedFuture(null);
    }
}
