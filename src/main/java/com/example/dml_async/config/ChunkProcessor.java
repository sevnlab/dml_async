package com.example.dml_async.config;

import com.example.dml_async.async.dto.AsyncEventDto;
import com.example.dml_async.async.service.DmlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;


@Service
@RequiredArgsConstructor
@Slf4j
public class ChunkProcessor {
    private final DmlService dmlService;

    @Async("asyncExecutor")
    public CompletableFuture<Void> processChunkAsync(List<String> pkList, AsyncEventDto eventDto) {
//        log.info("[{}] 角青", Thread.currentThread().getName());
        try {
            if(eventDto.getDmlType().equals("SELECT")) {
                dmlService.processSelect(pkList, eventDto.getJobName());
            } else {
                dmlService.updateChunk(pkList, eventDto.getJobName());
            }
        } catch(Exception e) {
            System.out.println(Thread.currentThread().getName());
            System.out.println(e.getMessage());
        }
        return CompletableFuture.completedFuture(null);
    }
}
