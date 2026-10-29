package com.cms.demo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService(tempDir.toString());
    }

    @Test
    void testStoreFileSuccessfully() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "contract_agreement.pdf",
                "application/pdf",
                "Sample PDF content".getBytes());

        FileStorageService.StoredFile stored = fileStorageService.storeFile(file, "contracts/1");

        assertNotNull(stored);
        assertEquals("contract_agreement.pdf", stored.originalFileName());
        assertTrue(stored.relativePath().startsWith("contracts/1/"));
        assertTrue(stored.storedFileName().endsWith(".pdf"));
        assertEquals("application/pdf", stored.mimeType());
        assertEquals("Sample PDF content".length(), stored.size());
        assertTrue(stored.absolutePath().toFile().exists());

        // Test reading back as resource
        Resource resource = fileStorageService.loadFileAsResource(stored.relativePath());
        assertTrue(resource.exists());
        assertTrue(resource.isReadable());
        try (InputStream is = resource.getInputStream()) {
            assertEquals("Sample PDF content", new String(is.readAllBytes()));
        }

        // Test deletion
        boolean deleted = fileStorageService.deleteFile(stored.relativePath());
        assertTrue(deleted);
        assertFalse(stored.absolutePath().toFile().exists());
    }

    @Test
    void testStoreEmptyFileThrowsException() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", new byte[0]);

        assertThrows(ResponseStatusException.class, () -> fileStorageService.storeFile(emptyFile, "contracts/1"));
    }

    @Test
    void testPathTraversalThrowsException() {
        MockMultipartFile badFile = new MockMultipartFile(
                "file", "../../../etc/passwd", "text/plain", "malicious".getBytes());

        assertThrows(ResponseStatusException.class, () -> fileStorageService.storeFile(badFile, "contracts/1"));
    }
}
