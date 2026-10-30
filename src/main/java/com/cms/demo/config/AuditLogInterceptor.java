package com.cms.demo.config;

import com.cms.demo.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuditLogInterceptor implements HandlerInterceptor {
    private static final Logger log = LoggerFactory.getLogger(AuditLogInterceptor.class);
    private static final String ACTOR_ATTRIBUTE = AuditLogInterceptor.class.getName() + ".employeeCode";
    private static final String PATH_ATTRIBUTE = AuditLogInterceptor.class.getName() + ".path";

    private final AuditLogService auditLogService;

    public AuditLogInterceptor(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = appPath(request);
        if (isExcluded(path)) return true;
        if (request.getUserPrincipal() != null) {
            request.setAttribute(ACTOR_ATTRIBUTE, request.getUserPrincipal().getName());
            request.setAttribute(PATH_ATTRIBUTE, path);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception exception) {
        Object actor = request.getAttribute(ACTOR_ATTRIBUTE);
        if (!(actor instanceof String employeeCode) || employeeCode.isBlank()) return;

        String path = (String) request.getAttribute(PATH_ATTRIBUTE);
        if (response.getStatus() >= 400) return;
        try {
            String action = action(request.getMethod(), path);
            if (action == null) return;
            Long entityId = entityId(path, request);
            if (entityId == null) entityId = idFromLocation(response.getHeader("Location"));
            auditLogService.record(employeeCode, entityType(path), entityId,
                    action, request.getMethod(), path,
                    response.getStatus(), request.getRemoteAddr());
        } catch (RuntimeException ex) {
            log.warn("Could not write audit entry for {} {}", request.getMethod(), path, ex);
        }
    }

    private String appPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        return path.isEmpty() ? "/" : path;
    }

    private boolean isExcluded(String path) {
        return path.equals("/audit") || path.startsWith("/audit/")
                || path.equals("/error") || path.equals("/login")
                || path.equals("/favicon.ico") || path.startsWith("/css/")
                || path.startsWith("/js/") || path.startsWith("/images/")
                || path.startsWith("/uploads/");
    }

    private String entityType(String path) {
        String[] parts = path.split("/");
        if (path.contains("/import-batches")) return "IMPORT_BATCH";
        int index = parts.length > 1 && "api".equalsIgnoreCase(parts[1]) ? 2 : 1;
        if (index >= parts.length || parts[index].isBlank()) return "SYSTEM";
        String root = parts[index].toLowerCase(Locale.ROOT);
        return switch (root) {
            case "contracts", "contract" -> "CONTRACT";
            case "users" -> "USER";
            case "master-setup" -> "MASTER_SETUP";
            case "import-batches" -> "IMPORT_BATCH";
            case "change-password", "profile" -> "USER";
            case "payments" -> "PAYMENT";
            case "approvals" -> "APPROVAL";
            default -> root.replace('-', '_').toUpperCase(Locale.ROOT);
        };
    }

    private Long entityId(String path, HttpServletRequest request) {
        for (String part : path.split("/")) {
            if (part.matches("\\d+")) {
                try { return Long.valueOf(part); } catch (NumberFormatException ignored) { return null; }
            }
        }
        for (String key : new String[] {"id", "contractId", "batchId", "userId"}) {
            String value = request.getParameter(key);
            if (value != null && value.matches("\\d+")) {
                try { return Long.valueOf(value); } catch (NumberFormatException ignored) { return null; }
            }
        }
        return null;
    }

    private Long idFromLocation(String location) {
        if (location == null) return null;
        for (String part : location.split("/")) {
            if (part.matches("\\d+")) {
                try { return Long.valueOf(part); } catch (NumberFormatException ignored) { return null; }
            }
        }
        return null;
    }

    private String action(String method, String path) {
        String upperPath = path.toUpperCase(Locale.ROOT);
        if (upperPath.contains("/DEACTIVATE")) return "UPDATE";
        return switch (method.toUpperCase(Locale.ROOT)) {
            case "POST" -> upperPath.endsWith("/DELETE") ? "DELETE"
                    : upperPath.endsWith("/UPDATE") || upperPath.matches(".*/\\d+")
                            || upperPath.contains("/RETRY") || upperPath.contains("CHANGE-PASSWORD")
                            || upperPath.contains("PROFILE/PASSWORD") ? "UPDATE" : "CREATE";
            case "PUT", "PATCH" -> "UPDATE";
            case "DELETE" -> "DELETE";
            default -> null;
        };
    }
}
