package com.cms.demo.controller;

import com.cms.demo.entity.ImportBatch;
import com.cms.demo.entity.ImportBatchError;
import com.cms.demo.repository.ImportBatchErrorRepository;
import com.cms.demo.repository.ImportBatchRepository;
import com.cms.demo.repository.UserRepository;
import com.cms.demo.service.ContractExcelImportService;
import com.cms.demo.service.FileStorageService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.io.ByteArrayOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/import-batches")
public class ImportBatchApiController {
    private final ImportBatchRepository batches;
    private final UserRepository users;
    private final FileStorageService files;
    private final ContractExcelImportService contractImporter;
    private final ImportBatchErrorRepository batchErrors;

    public ImportBatchApiController(ImportBatchRepository batches, UserRepository users, FileStorageService files,
            ContractExcelImportService contractImporter, ImportBatchErrorRepository batchErrors) {
        this.batches = batches;
        this.users = users;
        this.files = files;
        this.contractImporter = contractImporter;
        this.batchErrors = batchErrors;
    }

    @GetMapping
    public Map<String, Object> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) String uploadedBy,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "25") int pageSize) {
        pageSize = List.of(10, 25, 50, 100).contains(pageSize) ? pageSize : 25;
        String query = lower(q), typeFilter = lower(type), statusFilter = lower(status), userFilter = lower(uploadedBy);
        List<ImportBatch> filtered = batches.findAll().stream()
                .filter(batch -> query.isEmpty() || lower(batch.getFileName()).contains(query) || String.valueOf(batch.getBatchId()).contains(query))
                .filter(batch -> typeFilter.isEmpty() || lower(batch.getBatchType()).contains(typeFilter))
                .filter(batch -> statusFilter.isEmpty() || lower(batch.getStatus()).equals(statusFilter))
                .filter(batch -> from == null || (batch.getCreatedAt() != null && !batch.getCreatedAt().toLocalDate().isBefore(from)))
                .filter(batch -> to == null || (batch.getCreatedAt() != null && !batch.getCreatedAt().toLocalDate().isAfter(to)))
                .filter(batch -> userFilter.isEmpty() || (batch.getUploadedBy() != null && lower(batch.getUploadedBy().getFullName()).contains(userFilter)))
                .sorted(Comparator.comparing(ImportBatch::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        int total = filtered.size();
        int totalPages = Math.max(1, (int) Math.ceil(total / (double) pageSize));
        page = Math.max(1, Math.min(page, totalPages));
        int start = Math.min((page - 1) * pageSize, total);
        List<BatchView> data = filtered.subList(start, Math.min(start + pageSize, total)).stream().map(this::view).toList();
        Map<String, Object> summary = Map.of(
                "total", filtered.size(),
                "completed", filtered.stream().filter(b -> "COMPLETED".equalsIgnoreCase(b.getStatus())).count(),
                "failed", filtered.stream().filter(b -> "FAILED".equalsIgnoreCase(b.getStatus())).count(),
                "inProgress", filtered.stream().filter(b -> List.of("PENDING", "PROCESSING").contains(upper(b.getStatus()))).count());
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("data", data); response.put("total", total); response.put("page", page);
        response.put("pageSize", pageSize); response.put("totalPages", totalPages); response.put("summary", summary);
        return response;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> upload(@RequestParam String type, @RequestParam("file") MultipartFile file,
            java.security.Principal principal) {
        String normalizedType = upper(type);
        if (!List.of("USER", "CONTRACT").contains(normalizedType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Import type must be user or contract");
        }
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        if (!filename.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose an .xlsx workbook");
        }
        var actor = users.findByEmployeeCode(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
        var stored = files.storeFile(file, "imports");
        ImportBatch batch = new ImportBatch();
        batch.setBatchType(normalizedType);
        batch.setFileName(stored.originalFileName());
        batch.setFilePath("/uploads/" + stored.relativePath());
        batch.setTotalRows(0); batch.setSuccessRows(0);
        batch.setStatus("PENDING");
        batch.setCreatedAt(LocalDateTime.now());
        batch.setUploadedBy(actor);
        batch = batches.save(batch);
        if ("CONTRACT".equals(normalizedType)) {
            try {
                applyContractImport(batch, file.getInputStream(), principal.getName(), false);
            } catch (Exception ex) {
                batch.setStatus("FAILED"); batch.setTotalRows(1); batch.setSuccessRows(0); batches.save(batch);
                saveError(batch, 1, "workbook", ex.getMessage() == null ? "Could not process workbook" : ex.getMessage());
            }
        }
        Map<String, Object> response = batchResponse(batch);
        response.put("errors", batchErrors.findByBatchBatchIdOrderByRowNumberAsc(batch.getBatchId()).stream().map(this::errorView).toList());
        response.put("message", "CONTRACT".equals(normalizedType)
                ? batch.getSuccessRows() + " contracts added; " + (batch.getTotalRows() - batch.getSuccessRows()) + " rows failed."
                : "User workbook uploaded. User-row processing is not configured yet.");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/templates/{type}")
    public ResponseEntity<byte[]> template(@PathVariable String type) {
        String normalizedType = upper(type);
        String fileName;
        List<String> headers;
        if ("USER".equals(normalizedType)) {
            fileName = "users-template.xlsx";
            headers = List.of("username", "email", "role", "department", "status");
        } else if ("CONTRACT".equals(normalizedType)) {
            fileName = "contracts-template.xlsx";
            headers = List.of("contract_no", "customer", "project_id", "category", "status", "department", "value", "currency", "start_date", "end_date");
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Template not found");
        }
        byte[] workbook = makeXlsxTemplate(headers);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(workbook);
    }

    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable Long id) {
        ImportBatch batch = find(id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("batchId", batch.getBatchId()); result.put("fileName", batch.getFileName());
        result.put("batchType", batch.getBatchType()); result.put("status", batch.getStatus());
        result.put("totalRows", batch.getTotalRows()); result.put("successRows", batch.getSuccessRows());
        result.put("failedRows", Math.max(0, (batch.getTotalRows() == null ? 0 : batch.getTotalRows()) - (batch.getSuccessRows() == null ? 0 : batch.getSuccessRows())));
        result.put("createdAt", batch.getCreatedAt()); result.put("errors", batchErrors.findByBatchBatchIdOrderByRowNumberAsc(id).stream().map(this::errorView).toList());
        return result;
    }

    @GetMapping("/{id}/errors")
    public ResponseEntity<byte[]> errors(@PathVariable Long id) {
        find(id);
        String csv = "row,field,error\r\n" + batchErrors.findByBatchBatchIdOrderByRowNumberAsc(id).stream()
                .map(error -> error.getRowNumber() + "," + csv(error.getFieldName()) + "," + csv(error.getErrorMessage()) + "\r\n")
                .reduce("", String::concat);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"batch-" + id + "-errors.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    @PostMapping("/{id}/retry")
    public BatchView retry(@PathVariable Long id, java.security.Principal principal) {
        ImportBatch batch = find(id);
        if ("CONTRACT".equalsIgnoreCase(batch.getBatchType())) {
            try {
                batchErrors.deleteByBatchBatchId(id);
                applyContractImport(batch, files.loadFileAsResource(batch.getFilePath()).getInputStream(), principal.getName(), true);
            } catch (Exception ex) {
                batch.setStatus("FAILED"); batch.setTotalRows(1); batch.setSuccessRows(0);
                saveError(batch, 1, "workbook", ex.getMessage() == null ? "Could not process workbook" : ex.getMessage());
            }
            return view(batches.save(batch));
        }
        batch.setStatus("PENDING");
        return view(batches.save(batch));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        ImportBatch batch = find(id); batchErrors.deleteByBatchBatchId(id); batches.delete(batch); files.deleteFile(batch.getFilePath());
        return ResponseEntity.noContent().build();
    }

    private ImportBatch find(Long id) {
        return batches.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Import batch not found"));
    }
    private void applyContractImport(ImportBatch batch, java.io.InputStream input, String employeeCode, boolean skipExisting) {
        var result = contractImporter.importWorkbook(input, employeeCode, skipExisting);
        batch.setTotalRows(result.total()); batch.setSuccessRows(result.successful());
        batch.setStatus(result.errors().isEmpty() ? "COMPLETED" : result.successful() > 0 ? "PARTIALLY COMPLETED" : "FAILED");
        batches.save(batch);
        result.errors().forEach(error -> saveError(batch, error.rowNumber(), error.field(), error.message()));
    }
    private void saveError(ImportBatch batch, int row, String field, String message) {
        ImportBatchError error = new ImportBatchError(); error.setBatch(batch); error.setRowNumber(row);
        error.setFieldName(field); error.setErrorMessage(message); batchErrors.save(error);
    }
    private Map<String, Object> batchResponse(ImportBatch batch) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("batchId", batch.getBatchId()); data.put("batchType", batch.getBatchType()); data.put("fileName", batch.getFileName());
        data.put("totalRows", batch.getTotalRows()); data.put("successRows", batch.getSuccessRows());
        data.put("failedRows", Math.max(0, batch.getTotalRows() - batch.getSuccessRows())); data.put("status", batch.getStatus());
        data.put("uploadedByName", batch.getUploadedBy().getFullName()); data.put("createdAt", batch.getCreatedAt());
        return data;
    }
    private String csv(String value) { return "\"" + (value == null ? "" : value.replace("\"", "\"\"")) + "\""; }
    private Map<String, Object> errorView(ImportBatchError error) {
        return Map.of("rowNumber", error.getRowNumber(), "fieldName", error.getFieldName() == null ? "" : error.getFieldName(), "errorMessage", error.getErrorMessage());
    }
    private byte[] makeXlsxTemplate(List<String> headers) {
        StringBuilder cells = new StringBuilder();
        for (int i = 0; i < headers.size(); i++) {
            cells.append("<c r=\"").append(columnName(i + 1)).append("1\" t=\"inlineStr\"><is><t>")
                    .append(headers.get(i)).append("</t></is></c>");
        }
        String sheet = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><dimension ref=\"A1:"
                + columnName(headers.size()) + "1\"/><sheetData><row r=\"1\">" + cells + "</row></sheetData></worksheet>";
        Map<String, String> parts = Map.of(
                "[Content_Types].xml", "<?xml version=\"1.0\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/></Types>",
                "_rels/.rels", "<?xml version=\"1.0\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>",
                "xl/workbook.xml", "<?xml version=\"1.0\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"Template\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>",
                "xl/_rels/workbook.xml.rels", "<?xml version=\"1.0\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/></Relationships>",
                "xl/worksheets/sheet1.xml", sheet);
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream(); ZipOutputStream zip = new ZipOutputStream(bytes)) {
            for (Map.Entry<String, String> part : parts.entrySet()) {
                zip.putNextEntry(new ZipEntry(part.getKey())); zip.write(part.getValue().getBytes(StandardCharsets.UTF_8)); zip.closeEntry();
            }
            zip.finish(); return bytes.toByteArray();
        } catch (java.io.IOException ex) { throw new IllegalStateException("Could not build Excel template", ex); }
    }
    private String columnName(int value) {
        StringBuilder column = new StringBuilder();
        while (value > 0) { value--; column.insert(0, (char) ('A' + value % 26)); value /= 26; }
        return column.toString();
    }
    private BatchView view(ImportBatch batch) {
        return new BatchView(batch.getBatchId(), batch.getBatchType(), batch.getFileName(), batch.getTotalRows(),
                batch.getSuccessRows(), batch.getTotalRows() == null || batch.getSuccessRows() == null ? 0 : Math.max(0, batch.getTotalRows() - batch.getSuccessRows()),
                batch.getStatus(), batch.getUploadedBy() == null ? "—" : batch.getUploadedBy().getFullName(), batch.getCreatedAt());
    }
    private String lower(String value) { return value == null ? "" : value.trim().toLowerCase(Locale.ROOT); }
    private String upper(String value) { return value == null ? "" : value.trim().toUpperCase(Locale.ROOT); }

    public record BatchView(Long batchId, String batchType, String fileName, Integer totalRows,
            Integer successRows, Integer failedRows, String status, String uploadedByName, LocalDateTime createdAt) { }
}
