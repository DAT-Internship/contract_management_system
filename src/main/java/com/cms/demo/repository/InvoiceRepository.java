package com.cms.demo.repository;

import com.cms.demo.entity.Invoice;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    @EntityGraph(attributePaths = {"contract", "party", "currency", "status", "updatedBy"})
    @Query("""
            select i from Invoice i
            where i.contract.contractId = :contractId
            order by i.dueDate asc, i.invoiceId asc
            """)
    List<Invoice> findByContractId(@Param("contractId") Long contractId);

    @EntityGraph(attributePaths = {"contract", "party", "currency", "status", "updatedBy"})
    @Query("""
            select i from Invoice i
            where i.contract.contractId in :contractIds
            order by i.dueDate asc, i.invoiceId asc
            """)
    List<Invoice> findByContractIdIn(@Param("contractIds") Collection<Long> contractIds);

    @Query("""
            select case when count(i) > 0 then true else false end
            from Invoice i
            where lower(i.invoiceNo) = lower(:invoiceNo)
            """)
    boolean existsByInvoiceNo(@Param("invoiceNo") String invoiceNo);

    @Query("""
            select case when count(i) > 0 then true else false end
            from Invoice i
            where lower(i.invoiceNo) = lower(:invoiceNo)
              and i.invoiceId <> :invoiceId
            """)
    boolean existsByInvoiceNoAndIdNot(@Param("invoiceNo") String invoiceNo,
            @Param("invoiceId") Long invoiceId);

    @Query("select i from Invoice i left join fetch i.status where i.dueDate < :today "
            + "and (i.status is null or upper(i.status.code) not in ('PAID', 'OVERDUE'))")
    List<Invoice> findUnpaidPastDue(@Param("today") LocalDate today);
}
