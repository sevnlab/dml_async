package com.example.dml_async.config.async;

import com.example.dml_async.config.datasource.DatabaseContextHolder;
import org.springframework.core.task.TaskDecorator;

/**
 * 비동기(@Async) 작업 실행 시
 * ThreadLocal 값(DB 선택 key)을
 * worker thread로 전파하는 클래스
 *
 * ※ 전파가 없으면 worker thread는 DB 키를 알 수 없어서
 *    default DB로만 붙는 문제가 발생함.
 */
public class ContextCopyingDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {

        // API thread 에서 설정된 DB key 가져오기
        String dbKey = DatabaseContextHolder.get();

        return () -> {
            try {
                // worker thread 에 같은 DB key 셋팅
                DatabaseContextHolder.set(dbKey);

                // 실제 비즈니스 로직 실행
                runnable.run();

            } finally {
                // worker thread 값 정리
                DatabaseContextHolder.clear();
            }
        };
    }
}