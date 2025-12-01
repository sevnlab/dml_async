package com.example.dml_async.async.service;

import com.example.dml_async.async.dto.ResultDto;
import com.example.dml_async.async.repository.AsyncRepository;
import com.example.dml_async.config.ChunkProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DmlService {
    private final AsyncRepository asyncRepository;

    // 비동기 SELECT 결과 누적 저장소
    private static final List<ResultDto> resultStorage = Collections.synchronizedList(new ArrayList<>());
    private final ChunkProcessor chunkProcessor;

    @Transactional
    public void updateChunk(List<String> pkList, String jobName) {
        asyncRepository.bulkUpdateByPkList(pkList, jobName);
    }

    @Transactional(readOnly = true)
    public void processSelect(List<String> pkList, String jobName) {

        List<ResultDto> list = asyncRepository.bulkSelect(pkList, jobName);

        // 결과 누적 저장
        resultStorage.addAll(list);

        log.info("[SELECT] chunk size=" + pkList.size() + " result=" + list.size());
    }

    public List<ResultDto> getTotalResult() {
        return resultStorage;
    }
}
