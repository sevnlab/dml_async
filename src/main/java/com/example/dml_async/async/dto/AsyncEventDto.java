package com.example.dml_async.async.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AsyncEventDto {

    @NonNull
    private String jobName;

    @NonNull
    private String dmlType;      // UPDATE / SELECT / INSERT

    // 경로 쓸때 참고
    // 윈도우 uploadFilePath 예시: "D:\\test_data\\TB_WALLET_POINT_TRADE_202603211915.txt"
    // 리눅스 uploadFilePath 예시: "/home/seven/data/TB_WALLET_POINT_TRADE_202603211915.txt"
    private String uploadFilePath;
    private String downloadFilePath;

}