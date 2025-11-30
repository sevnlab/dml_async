package com.example.dml_async.controller;

import com.example.dml_async.config.datasource.DatabaseContextHolder;
import com.example.dml_async.service.DbCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/dmlProcess")
@RequiredArgsConstructor
public class updateController {
//    private final AsyncFacadeService asyncFacadeService;
private final DbCheckService dbCheckService;

    // 비동기 업데이트 실행
    @PostMapping("/update-async")
//    public ResponseEntity<Map<String, Object>> updateAsync(@RequestBody AsyncEventDto request) {
    public ResponseEntity<Map<String, Object>> updateAsync() {

        // 현재 스레드에 DB 키 저장
        DatabaseContextHolder.set("serviceA");

        try {
//            if ("테스트검증".equals(request.getAuth())) {
//                try {
//                    asyncFacadeService.publishAsyncEvent(request);
//                } catch (Exception e) {
//                    log.info("비동기 이벤트 처리 중 오류 발생() => {}", e.getMessage());
//                }
//                return ResponseEntity.ok()
//                        .body(Map.of(
//                                "message", "비동기 마이그레이션 이벤트가 발행되었습니다. (jobName=" + request.getJobName() + ")"
//                        ));
//            }else{
//                throw new ApiException(ResponseCode);
//            }
            return null;
        } finally {
            // API 스레드 정리
            DatabaseContextHolder.clear();
        }
    }

    @GetMapping("/check")
    public String check(@RequestParam String service) {

        // ThreadLocal에 serviceA / serviceB 저장
//        DatabaseContextHolder.set(service);
        DatabaseContextHolder.set("serviceC");
        String result = dbCheckService.getCurrentDbInfo();
        DatabaseContextHolder.clear();

        return result;
    }
}