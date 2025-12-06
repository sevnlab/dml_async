package com.example.dml_async.async.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

//@CustomLog
@Repository
@RequiredArgsConstructor
public class AsyncRepository {
    private static final Logger log = LoggerFactory.getLogger("INFO");
    private static final Logger sqlLog = LoggerFactory.getLogger("SQL");

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

}
