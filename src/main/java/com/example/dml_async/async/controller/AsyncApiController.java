package com.example.dml_async.async.controller;

import com.example.dml_async.aop.Comments;
import com.example.dml_async.async.dto.AsyncEventDto;
import com.example.dml_async.async.service.AsyncFacadeService;
import com.example.dml_async.common.code.ResponseCode;
import com.example.dml_async.common.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/dmlProcess")
public class AsyncApiController {

    private final AsyncFacadeService asyncFacadeService;

    // application.yml 의 api.key 값을 주입
    @Value("${api.key}")
    private String apiKey;

    @Comments("DML_API_REQUEST")
    @PostMapping("/dml")
    public ResponseEntity<Map<String, Object>> updateAsync(
            @RequestHeader("X-API-KEY") String requestApiKey,
            @RequestBody AsyncEventDto request) {

        if (!apiKey.equals(requestApiKey)) {
            throw new CustomException(ResponseCode.C910);
        }

        try {
            asyncFacadeService.publishAsyncEvent(request);
        } catch (Exception e) {
            throw new CustomException(ResponseCode.A100);
        }

        return ResponseEntity.ok()
                .body(Map.of(
                        "message", "비동기 이벤트가 등록되었습니다. (jobName=" + request.getJobName() + ")"
                ));
    }
}