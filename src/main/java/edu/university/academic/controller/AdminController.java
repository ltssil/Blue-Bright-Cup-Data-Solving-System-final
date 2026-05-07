package edu.university.academic.controller;

import edu.university.academic.operation.AdminClearOperation;
import edu.university.academic.operation.AdminExportOperation;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    /**
     * 清空业务数据表
     */
    @PostMapping("/clearTables")
    public Map<String, Object> clearTables(@RequestBody Map<String, Object> body) {

        @SuppressWarnings("unchecked")
        List<String> tables = (List<String>) body.get("tables");

        AdminClearOperation.clearTables(tables);

        return Map.of(
                "success", true,
                "message", "数据表清空成功"
        );
    }

    @GetMapping("/export-conflicts")
    public ResponseEntity<byte[]> exportConflicts(@RequestParam(value = "year", required = false) Integer year) {
        try {
            byte[] bytes = AdminExportOperation.generateConflictExcelBytes(year);

            String base = (year != null) ? (year + "_conflicts_" + LocalDate.now().toString() + ".xlsx") : ("conflicts_" + LocalDate.now().toString() + ".xlsx");
            String encoded = URLEncoder.encode(base, StandardCharsets.UTF_8.toString()).replaceAll("\\+", "%20");

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(bytes);
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(new byte[0]);
        }
    }
}
