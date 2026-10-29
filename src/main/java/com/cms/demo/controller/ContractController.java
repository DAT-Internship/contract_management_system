package com.cms.demo.controller;

import com.cms.demo.dto.AttachmentResponse;
import com.cms.demo.dto.ContractRequest;
import com.cms.demo.dto.ContractResponse;
import com.cms.demo.entity.ContractAttachment;
import com.cms.demo.service.ContractService;
import com.cms.demo.service.FileStorageService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/contract")
public class ContractController {

    private final ContractService contractService;
    private final FileStorageService fileStorageService;

    public ContractController(ContractService contractService, FileStorageService fileStorageService) {
        this.contractService = contractService;
        this.fileStorageService = fileStorageService;
    }

    @PostMapping
    public ResponseEntity<ContractResponse> create(
            @Valid @RequestBody ContractRequest request,
            Principal principal) {
        ContractResponse response = contractService.create(request, principal.getName());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.contractId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public List<ContractResponse> findAll() {
        return contractService.findAll();
    }

    @GetMapping("/{id}")
    public ContractResponse findById(@PathVariable Long id) {
        return contractService.findById(id);
    }

    @PutMapping("/{id}")
    public ContractResponse update(
            @PathVariable Long id,
            @Valid @RequestBody ContractRequest request,
            Principal principal) {
        return contractService.update(id, request, principal.getName());
    }

    @PostMapping(value = "/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AttachmentResponse> uploadAttachment(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            Principal principal) {
        AttachmentResponse response = contractService.addAttachment(id, file, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/attachments")
    public List<AttachmentResponse> getAttachments(@PathVariable Long id) {
        return contractService.getAttachmentResponses(id);
    }

    @GetMapping("/{contractId}/attachments/{attachmentId}/download")
    public ResponseEntity<Resource> downloadAttachment(
            @PathVariable Long contractId,
            @PathVariable Long attachmentId) {
        ContractAttachment attachment = contractService.getAttachment(attachmentId);
        if (!attachment.getContract().getContractId().equals(contractId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attachment mismatch");
        }
        Resource resource = fileStorageService.loadFileAsResource(attachment.getFilePath());
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(attachment.getMimeType());
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + attachment.getFileName() + "\"")
                .body(resource);
    }
}
