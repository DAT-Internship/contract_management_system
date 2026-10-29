package com.cms.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private final Path baseUploadLocation;

    public record StoredFile(
            String originalFileName,
            String storedFileName,
            String relativePath,
            Path absolutePath,
            long size,
            String mimeType) {
    }

    public FileStorageService(@Value("${app.upload.dir:src/main/resources/static/uploads}") String uploadDir) {
        this.baseUploadLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.baseUploadLocation);
            log.info("File upload storage location initialized at: {}", this.baseUploadLocation);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create base upload directory: " + this.baseUploadLocation, e);
        }
    }

    public Path getBaseUploadLocation() {
        return this.baseUploadLocation;
    }

    public StoredFile storeFile(MultipartFile file, String subFolder) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot store empty file");
        }

        String rawFileName = StringUtils.cleanPath(Objects.requireNonNullElse(file.getOriginalFilename(), "attachment"));
        if (rawFileName.contains("..")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File name contains invalid path sequence: " + rawFileName);
        }

        // Keep extension and sanitize base name
        String extension = "";
        int dotIndex = rawFileName.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = rawFileName.substring(dotIndex);
        }

        String baseName = dotIndex >= 0 ? rawFileName.substring(0, dotIndex) : rawFileName;
        String sanitizedBaseName = baseName.replaceAll("[^a-zA-Z0-9_\\-\\.]", "_");
        if (sanitizedBaseName.length() > 60) {
            sanitizedBaseName = sanitizedBaseName.substring(0, 60);
        }

        String storedFileName = UUID.randomUUID().toString() + "_" + sanitizedBaseName + extension;

        String cleanSubFolder = (subFolder == null) ? "" : subFolder.trim().replace('\\', '/').replaceAll("^/+", "").replaceAll("/+$", "");
        Path targetDir = cleanSubFolder.isEmpty() ? this.baseUploadLocation : this.baseUploadLocation.resolve(cleanSubFolder).normalize();

        try {
            Files.createDirectories(targetDir);
            Path targetFilePath = targetDir.resolve(storedFileName).normalize();

            // Security check: ensure targetFilePath is within baseUploadLocation
            if (!targetFilePath.startsWith(this.baseUploadLocation)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot store file outside target upload directory");
            }

            Files.copy(file.getInputStream(), targetFilePath, StandardCopyOption.REPLACE_EXISTING);

            String relativePath = cleanSubFolder.isEmpty()
                    ? storedFileName
                    : cleanSubFolder + "/" + storedFileName;

            String mimeType = file.getContentType();
            if (mimeType == null || mimeType.isBlank()) {
                mimeType = "application/octet-stream";
            }

            return new StoredFile(
                    rawFileName,
                    storedFileName,
                    relativePath,
                    targetFilePath,
                    file.getSize(),
                    mimeType);

        } catch (IOException ex) {
            log.error("Failed to store file: {}", rawFileName, ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store file: " + rawFileName, ex);
        }
    }

    public Resource loadFileAsResource(String relativePath) {
        try {
            String cleanRelative = relativePath.trim().replace('\\', '/')
                    .replaceFirst("^/uploads/", "")
                    .replaceFirst("^/", "");
            Path filePath = this.baseUploadLocation.resolve(cleanRelative).normalize();

            if (!filePath.startsWith(this.baseUploadLocation)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file path");
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found: " + cleanRelative);
            }
        } catch (MalformedURLException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found", ex);
        }
    }

    public boolean deleteFile(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return false;
        }
        try {
            String cleanRelative = relativePath.trim().replace('\\', '/')
                    .replaceFirst("^/uploads/", "")
                    .replaceFirst("^/", "");
            Path filePath = this.baseUploadLocation.resolve(cleanRelative).normalize();
            if (filePath.startsWith(this.baseUploadLocation)) {
                return Files.deleteIfExists(filePath);
            }
        } catch (IOException e) {
            log.warn("Could not delete file at: {}", relativePath, e);
        }
        return false;
    }
}
