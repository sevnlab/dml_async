package com.example.dml_async.async.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AsyncEventDto {
    @NonNull
    private String auth;

    @NonNull
    private String jobName;

    @NonNull
    private String dmlType;

    // 윈도우에서 읽을때 "filePath":"C:\\Users\\tekim\\Desktop\\updateFile.txt"
    @NonNull
    private String filePath;


}
