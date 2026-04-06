package com.example.dml_async.async.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

//@CustomLog
@Repository
@RequiredArgsConstructor
public class AsyncRepository {
    private static final Logger log = LoggerFactory.getLogger("INFO");
    private static final Logger sqlLog = LoggerFactory.getLogger("SQL");

    private final JdbcTemplate jdbcTemplate;

    @PersistenceContext
    private EntityManager entityManager;


    // 파싱 부하로 인해 실행횟수가 많은 SQL final 선언
    private static final String SQL_UPDATE_TEST_TABLE = """
        UPDATE TB_TEST_TABLE
           SET NAME = NULL
         WHERE ROWID IN (:pkList)
    """;


    private static final String SQL_SELECT_TEST_TABLE = """
        SELECT ROWID    
        FROM TB_TEST_TABLE2   
        WHERE AGENT_KEY IN (:pkList)
    """;

    private static final String SQL_SELECT_ALL_TEST_TABLE = """
        SELECT ('ROWID = ' || ROWID || ', SEQ_NO = ' || SEQ_NO) AS VAL    
        FROM TB_TEST_TABLE3   
        WHERE AGENT_KEY IN (:pkList)
    """;

    public void bulkUpdateByPkList(List<String> pkList, String jobName) {
        if (pkList == null || pkList.isEmpty()) return;

        String sql = "";
        String inClauses = (String)pkList.stream().map((s) -> {
            return "'" + s + "'";
        }).collect(Collectors.joining(","));
        String logSql = "";

        /** JOBNAME 별로 비즈니스로직 구분 */
        if(jobName.equals("TB_TEST_TABLE")) {
            sql = SQL_UPDATE_TEST_TABLE;
        }

        sqlLog.info(logSql);
        Query query = this.entityManager.createNativeQuery(sql);
        query.setParameter("pkList", pkList);
        int updated = query.executeUpdate();
        log.debug("업데이트 완료: {}건", updated);

    }

    /**
     * 대용량 멀티 INSERT
     * - JdbcTemplate.batchUpdate 사용 (JDBC 레벨 배치, EntityManager보다 고속)
     * - 한 번 호출에 rows.size()건 일괄 INSERT
     */
    // "yyyy-MM-dd" 또는 "yyyy-MM-dd HH:mm:ss..." 패턴만 날짜로 인식
    // 정규식으로 먼저 걸러내어 Exception 생성 비용 제거
    private static final java.util.regex.Pattern DATE_PATTERN =
            java.util.regex.Pattern.compile("^\\d{4}-\\d{2}-\\d{2}([ T]\\d{2}:\\d{2}:\\d{2}.*)?$");

    private static final DateTimeFormatter DT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter D_FORMAT  = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private Timestamp tryParseDate(String val) {
        // 날짜처럼 생기지 않은 값은 즉시 null 반환 (Exception 비용 없음)
        if (!DATE_PATTERN.matcher(val).matches()) return null;

        try {
            return Timestamp.valueOf(LocalDateTime.parse(val.length() > 10 ? val.substring(0, 19) : val + " 00:00:00", DT_FORMAT));
        } catch (Exception ignored) {
            return null;
        }
    }

    public void bulkInsert(List<String[]> rows, String tableName, List<String> columnNames) {
        if (rows == null || rows.isEmpty()) return;

        String cols = String.join(", ", columnNames);
        // list 는 map, filter 같은 가공 메서드를 못 써서 .stream() 으로 변환후 값들을 ? 처리 후 하나의 스트링으로 처리
        String placeholders = columnNames.stream().map(c -> "?").collect(Collectors.joining(", "));

        // ? 로 넣는 이유는 DB 가 ? 를 바인딩 파라미터로 인식하기 때문(파싱부하를 피하기 위해)
        String sql = "INSERT INTO " + tableName + " (" + cols + ") VALUES (" + placeholders + ")";

        // EntityManager는 쿼리 파싱, 캐싱 오버헤드 있음. JDBC 직접 호출이 대용량에서는 빠름
        // 행수x컬럼수 = 수억번 호출되므로 정규식으로 사전 차단
        // batchUpdate 가 내부적으로 ? 로 넘긴것을 어떻게 넣을지 모르기 때문에,
        // 규칙(BatchPreparedStatementSetter) 을 같이 넘긴다.
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {

            // 규칙1
            // 각 행에 값을 어떻게 채울지
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {

                String[] row = rows.get(i);
                for (int j = 0; j < columnNames.size(); j++) {
                    String val = (j < row.length) ? row[j].trim() : null;

                    // 빈 문자열이나 "NULL" 문자열은 DB NULL 로 처리
                    if (val == null || val.isEmpty() || val.equalsIgnoreCase("NULL")) {
                        ps.setNull(j + 1, java.sql.Types.VARCHAR);  // 빈값/NULL 문자열 → DB NULL
                    } else {
                        // 날짜 형식이면 Timestamp 로 변환, 아니면 문자열 그대로
                        Timestamp ts = tryParseDate(val);
                        if (ts != null) {
                            ps.setTimestamp(j + 1, ts);
                        } else {
                            ps.setString(j + 1, val);
                        }
                    }
                }
            }

            // 규칙2
            // 총 몇행인지
            @Override
            public int getBatchSize() {
                return rows.size();
            }
        });
    }

    /**
     * 테이블 전체 조회 후 ^ 구분자 형식의 라인 목록 반환
     * - 첫 번째 원소: 컬럼명 헤더 (COL1^COL2^...)
     * - 이후 원소:   데이터 행 (val1^val2^...)
     */
    public List<String> selectAllFromTable(String tableName) {
        String sql = "SELECT * FROM " + tableName;
        log.info("[SelectAll] 실행 SQL: {}", sql);

        return jdbcTemplate.query(sql, rs -> {
            List<String> lines = new ArrayList<>();
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();

            // 헤더 라인
            StringBuilder header = new StringBuilder();
            for (int i = 1; i <= colCount; i++) {
                if (i > 1) header.append("^");
                header.append(meta.getColumnName(i));
            }
            lines.add(header.toString());

            // 데이터 라인
            while (rs.next()) {
                StringBuilder row = new StringBuilder();
                for (int i = 1; i <= colCount; i++) {
                    if (i > 1) row.append("^");
                    Object val = rs.getObject(i);
                    row.append(val != null ? val.toString() : "");
                }
                lines.add(row.toString());
            }
            return lines;
        });
    }

    public List<String> bulkSelect(List<String> pkList, String jobName) {

        String sql = "";

        if(jobName.equals("TB_TEST_TABLE2")) {
            sql = SQL_SELECT_TEST_TABLE;
        } else if(jobName.equals("TB_TEST_TABLE3")) {
            sql = SQL_SELECT_ALL_TEST_TABLE;
        }

        String inClauses = (String)pkList.stream().map((s) -> {
            return "'" + s + "'";
        }).collect(Collectors.joining(","));
        String logSql = "SELECT ROWID FROM TB_QPAY_EID1_INFO WHERE EID_KEY IN (" + inClauses + ")";
        sqlLog.info(logSql);
        Query query = this.entityManager.createNativeQuery(sql);
        query.setParameter("pkList", pkList);
        List<String> result = query.getResultList();
        return result.stream().map(Object::toString).toList();
    }

    public void truncateTable(String tableName) {
        String sql = "TRUNCATE TABLE " + tableName;
        log.info("[Delete] 실행 SQL: {}", sql);
        jdbcTemplate.execute(sql);
    }

}
