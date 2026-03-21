package com.example.dml_async.async.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.List;

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

    // 윈도우에서 읽을때 예시::"C:\\Users\\tekim\\Desktop\\updateFile.txt"
    // 리눅스에서 읽을때 예시: 
    @NonNull
    private String uploadFilePath;

    private String downloadFilePath;

    // INSERT 전용 - 대상 테이블명
    // 예: "TB_TARGET_TABLE"
    private String tableName;

    // INSERT 전용 - TXT 파일 컬럼 순서와 동일하게 작성
    // 예: ["COL1", "COL2", "COL3"]
    private List<String> columnNames;
}