package com.cms.demo.dto;

import java.time.LocalDateTime;

public record AttachmentResponse(
        Long attachmentId,
        String fileName,
        String filePath,
        Long fileSize,
        String mimeType,
        LocalDateTime uploadedAt,
        String uploadedByName) {
}
