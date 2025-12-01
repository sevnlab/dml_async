package com.example.dml_async.common.code;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum JobName {
    TB_TEST_TABLE("TB_TEST_TABLE")
    ;

    private final String jobName;
}
