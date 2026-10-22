package com.cms.demo.entity;


import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@NoArgsConstructor
@Entity
@Data
@Table(name = "contract_attachments")
public class ContractAttachment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attachment_id")
    private Long attachmentId;

    @Column(name = "file_name", length=255, nullable=false)
    private String fileName;

    @Column(name = "file_path", length=500, nullable=false)
    private String filePath;

    @Column(name = "file_size", nullable=false)
    private Long fileSize;

    @Column(name = "mime_type", length=80, nullable=false)
    private String mimeType;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = true)
    private Contract contract;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = true)
    private User uploadedBy;

}
