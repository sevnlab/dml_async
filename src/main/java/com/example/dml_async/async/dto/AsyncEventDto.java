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

    // 윈도우에서 읽을때 예시::"D:\\Users\\tekim\\Desktop\\updateFile.txt"
    // 윈도우에서 읽을때 예시::"D:\\test_data\\TB_WALLET_POINT_TRADE_202603211915_CHANG2.txt"
    // 리눅스에서 읽을때 예시:
    @NonNull
    private String uploadFilePath;
    private String downloadFilePath;

}