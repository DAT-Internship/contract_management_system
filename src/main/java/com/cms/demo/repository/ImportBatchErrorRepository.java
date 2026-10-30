package com.cms.demo.repository;

import com.cms.demo.entity.ImportBatchError;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportBatchErrorRepository extends JpaRepository<ImportBatchError, Long> {
    List<ImportBatchError> findByBatchBatchIdOrderByRowNumberAsc(Long batchId);
    void deleteByBatchBatchId(Long batchId);
}
