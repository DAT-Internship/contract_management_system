package com.cms.demo.repository;

import com.cms.demo.entity.Invoice;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByContractContractId(Long contractId);
}
