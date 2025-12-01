package com.example.dml_async.async.controller;

import com.example.dml_async.async.dto.AsyncEventDto;
import com.example.dml_async.async.service.AsyncFacadeService;
import com.example.dml_async.common.code.ResponseCode;
import com.example.dml_async.common.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

//@CustomLog
@RestController
@RequiredArgsConstructor
@RequestMapping("/dmlProcess")
public class AsyncApiController {
    private final AsyncFacadeService asyncFacadeService;

    // 비동기 DML 처리
    // 비동기 업데이트 실행
    @PostMapping("/update-async")
    public ResponseEntity<Map<String, Object>> updateAsync(@RequestBody AsyncEventDto request) {
        if ("TEST_AUTH".equals(request.getAuth())){
            try {
                asyncFacadeService.publishAsyncEvent(request);
            } catch (Exception e) {
                System.out.println("비동기 이벤트 처리 중 오류 발생(" +  e.getMessage() + ")");

            }
            return ResponseEntity.ok()
                    .body(Map.of(
                            "message", "비동기 이벤트가 발행되었습니다. (jobName=" + request.getJobName() + ")"
                    ));
        } else {
            // 에러처리
            throw new CustomException(ResponseCode.C910);
        }
    }
}