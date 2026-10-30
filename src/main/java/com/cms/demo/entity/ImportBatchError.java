package com.cms.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
@Entity
@Table(name = "import_batch_errors")
public class ImportBatchError {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "error_id")
    private Long errorId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private ImportBatch batch;

    @Column(name = "source_row", nullable = false)
    private Integer rowNumber;

    @Column(name = "field_name", length = 80)
    private String fieldName;

    @Column(name = "error_message", nullable = false, columnDefinition = "text")
    private String errorMessage;
}
