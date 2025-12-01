package com.example.dml_async.async.repository;

import com.example.dml_async.async.dto.ResultDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

//@CustomLog
@Repository
@RequiredArgsConstructor
public class AsyncRepository {
    @PersistenceContext
    private EntityManager entityManager;

    // 파싱 부하로 인해 실행횟수가 많은 SQL final 선언
    private static final String SQL_UPDATE_QUICK_PAY_TOKEN_INFO = """
        UPDATE TB_QUICK_PAY_TOKEN_INFO
           SET SALE_AMT = TOTAL_AMT,
               PROMOTION_AMT = 0
         WHERE ROWID IN (:pkList)
    """;

    private static final String SQL_UPDATE_AGENT_EASY_PAY = """
        UPDATE TB_AGENT_EASY_PAY
           SET VAN_KEY = NULL 
         WHERE ROWID IN (:pkList)
    """;

    private static final String SQL_UPDATE_QUICK_PAY_MAPPING_INFO = """
        UPDATE TB_QUICK_PAY_MAPPING
           SET CARD_CODE = NULL
         WHERE ROWID IN (:pkList)
    """;

    private static final String SQL_UPDATE_QPAY_EID1_INFO = """
        UPDATE TB_QPAY_EID1_INFO
           SET EID_VAL = NULL, USE_YN = 'N'
         WHERE ROWID IN (:pkList)
    """;

    private static final String SQL_SELECT_QPAY_EID1_INFO = """
        SELECT ROWID
        FROM TB_QPAY_EID1_INFO
        WHERE EID_KEY IN (:pkList)
    """;

    public void bulkUpdateByPkList(List<String> pkList, String jobName) {
        if (pkList == null || pkList.isEmpty()) return;

        String sql = "";

        /** JOBNAME 별로 비즈니스로직 구분 */
        if(jobName.equals("TB_QUICK_PAY_TOKEN_INFO")) {
            sql = SQL_UPDATE_QUICK_PAY_TOKEN_INFO;
        } else if(jobName.equals("TB_AGENT_EASY_PAY")) {
            sql = SQL_UPDATE_AGENT_EASY_PAY;
        } else if(jobName.equals("TB_QUICK_PAY_MAPPING")) {
            sql = SQL_UPDATE_QUICK_PAY_MAPPING_INFO;
        } else if(jobName.equals("TB_QPAY_EID1_INFO")) {
            sql = SQL_UPDATE_QPAY_EID1_INFO;
        }

        var query = entityManager.createNativeQuery(sql);
        query.setParameter("pkList", pkList);
        int updated = query.executeUpdate();
//        log.debug("업데이트 완료: {}건", updated);
    }

    public List<ResultDto> bulkSelect(List<String> pkList, String jobName) {
        if (pkList == null || pkList.isEmpty());

        String sql = "";

        /** JOBNAME 별로 비즈니스로직 구분 */
        if(jobName.equals("TB_QPAY_EID1_INFO")) {
            sql = SQL_SELECT_QPAY_EID1_INFO;
        }

        var query = entityManager.createNativeQuery(sql);
        query.setParameter("pkList", pkList);
        List<ResultDto> result = query.getResultList();
//        log.debug("조회 완료 : {}건", result.size());

        return result;
    }

}
