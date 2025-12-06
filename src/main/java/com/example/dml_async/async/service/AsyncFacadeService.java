package com.example.dml_async.async.service;


import com.example.dml_async.common.code.JobName;
import com.example.dml_async.async.dto.AsyncEventDto;
import com.example.dml_async.common.code.ResponseCode;
import com.example.dml_async.common.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Arrays;

/**
 * 발행자(publisher) 역할 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncFacadeService {

    // 스프링 이벤트 기반 비동기 트리거
    private final ApplicationEventPublisher eventPublisher;

    public void publishAsyncEvent(AsyncEventDto asyncEventDto) {

        // JOB Name 검증
        JobName jobName = Arrays.stream(JobName.values())
                .filter(target -> target.name().equalsIgnoreCase(asyncEventDto.getJobName()))
                .findFirst()
                .orElseThrow(() -> new CustomException(ResponseCode.E400));


        // ---- 요청객체를 실어서 이벤트 발행 ----
        eventPublisher.publishEvent(asyncEventDto);
    }
}
