package com.example.dml_async.common.code;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum JobName {
    TB_TEST_TABLE("TB_TEST_TABLE"),
    TB_NEW_TABLE("TB_WALLET_POINT_TRADE_COPY")
    ;

    private final String jobName;
}
