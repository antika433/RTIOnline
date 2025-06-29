package nic.rti.master.dao.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.Data;
import nic.rti.master.dao.FAAPendingRequestRepository;
import nic.rti.master.dto.PendingRequestDTO;
import nic.rti.master.dto.PendingRequestResponseDTO;
import org.springframework.stereotype.Repository;
import jakarta.persistence.Query;

import java.util.ArrayList;
import java.util.List;

@Data


@Repository
public class FAAPendingRequestRepositoryImpl implements FAAPendingRequestRepository {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<PendingRequestResponseDTO>  fetchUnderProcess(Integer applId, Integer limit, Integer offset){
        String sql= """
                SELECT a.registration_no,
                a.name,
                TO_CHAR(a.recvd_date,'DD/MM/YYYY') AS recv_date,
                TO_CHAR(a.entry_date,'YY-MM-DD"T"HH24:MI:SS"Z"')AS entry_date
                
                FROM RTIMIS.appeal a
                JOIN (
                    SELECT  registration_no, MAX(action_srno) AS max_srno
                    FROM RTIMIS.action_history
                    GROUP BY registration_no
                   ) max_actions ON a.registration_no=max_actions.registration_no
                   JOIN RTIMIS.action_history ah ON ah.registration_no=max_actions.registration_no
                   AND ah.action_srno=max_actions.max_srno
                   
                   WHERE ah.action_status IN ('70','7A','7D','7C')
                   AND a.closing_date IS NULL
                   AND a.applid = :applId
                   ORDER BY a.entry_date DESC
                   LIMIT :limit OFFSET :offset
                """  ;
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("applId",applId);
        query.setParameter("limit",limit);
        query.setParameter("offset",offset);

        @SuppressWarnings("unchecked")
        List<Object[]> rows=query.getResultList();
        List<PendingRequestResponseDTO> dtoList=new ArrayList<>();

        for (Object[] row:rows){
            PendingRequestResponseDTO dto = new PendingRequestResponseDTO();

            dto.setRegistrationNo((String)row[0]);
            dto.setName((String)row[1]);
            dto.setRecvDate((String) row[2]);
            dto.setEntryDate((String) row[3]);
            dtoList.add(dto);

        }
        return dtoList;

    }

    //impliment this temporarily for testing purpose

    public long countUnderProcess(Integer applId) {
        String sql = """
        SELECT COUNT(*) 
        FROM RTIMIS.appeal a
        JOIN (
            SELECT registration_no, MAX(action_srno) AS max_srno
            FROM RTIMIS.action_history
            GROUP BY registration_no
        ) max_actions ON a.registration_no = max_actions.registration_no
        JOIN RTIMIS.action_history ah ON ah.registration_no = max_actions.registration_no
            AND ah.action_srno = max_actions.max_srno
        WHERE ah.action_status IN ('70','7A','7D','7C')
            AND a.closing_date IS NULL
            AND a.applid = :applId
    """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("applId", applId);
        Object result = query.getSingleResult();
        return ((Number) result).longValue();
    }


}
