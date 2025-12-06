package com.example.dml_async.common.code;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ResponseCode {

    // --- 성공 ---
    SUCCESS("0000", "요청이 성공적으로 처리되었습니다."),

    // --- 인증 / 권한 ---
    C910("C910", "인증 정보가 올바르지 않습니다."),
    C911("C911", "권한이 없습니다."),

    // --- 비동기 처리 ---
    A100("A100", "비동기 이벤트 처리 중 오류가 발생했습니다."),
    A101("A101", "비동기 작업이 아직 완료되지 않았습니다."),
    A102("A102", "비동기 작업이 존재하지 않습니다."),

    // --- 공통 에러 ---
    E400("E400", "잘못된 요청입니다."),
    E500("E500", "서버 내부 오류가 발생했습니다.");

    private final String code;
    private final String message;
}
