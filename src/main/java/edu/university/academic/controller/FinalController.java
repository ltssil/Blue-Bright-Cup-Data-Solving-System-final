package edu.university.academic.controller;

import edu.university.academic.operation.FinalReportOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class FinalController {

    @Autowired
    private FinalReportOperation finalReportOperation;

    /**
     * 合成预览：返回合成好的 JSON 行集合
     */
    @PostMapping("/compose-final")
    public Map<String, Object> composeFinal(@RequestBody Map<String, Object> body) {
        Integer year = body.get("year") == null ? null : Integer.valueOf(body.get("year").toString());
        String competitionName = body.get("competitionName") == null ? null : body.get("competitionName").toString();
        String awardLevel = body.get("awardLevel") == null ? null : body.get("awardLevel").toString();
        String major = body.get("major") == null ? null : body.get("major").toString();

        List<Map<String, Object>> rows = finalReportOperation.composeFinalRows(year, competitionName, awardLevel, major);
        return Map.of("success", true, "rows", rows, "count", rows.size());
    }

    /**
     * 生成并返回 Excel 文件（xlsx），使用 composeFinalRows 的数据
     */
    @PostMapping("/generate-final-excel")
    public ResponseEntity<byte[]> generateFinalExcel(@RequestBody Map<String, Object> body) {
        Integer year = body.get("year") == null ? null : Integer.valueOf(body.get("year").toString());
        String competitionName = body.get("competitionName") == null ? null : body.get("competitionName").toString();
        String awardLevel = body.get("awardLevel") == null ? null : body.get("awardLevel").toString();
        String major = body.get("major") == null ? null : body.get("major").toString();

        byte[] bytes = finalReportOperation.generateFinalExcelBytes(year, competitionName, awardLevel, major);

        String rawFilename = "Final_Report_" + (competitionName == null ? "比赛" : competitionName) + "_" + (year == null ? "" : year) + ".xlsx";
        String encoded;
        try {
            encoded = URLEncoder.encode(rawFilename, StandardCharsets.UTF_8.toString()).replaceAll("\\+", "%20");
        } catch (Exception ex) {
            encoded = rawFilename;
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    /**
     * 将合成结果持久化到数据库。
     */
    @PostMapping("/save-final-to-db")
    public Map<String, Object> saveFinalToDb(@RequestBody Map<String, Object> body) {
        Integer year = body.get("year") == null ? null : Integer.valueOf(body.get("year").toString());
        String competitionName = body.get("competitionName") == null ? null : body.get("competitionName").toString();
        String awardLevel = body.get("awardLevel") == null ? null : body.get("awardLevel").toString();
        String major = body.get("major") == null ? null : body.get("major").toString();
        boolean overwrite = body.get("overwrite") == null ? true : Boolean.parseBoolean(body.get("overwrite").toString());

        int inserted = finalReportOperation.persistFinalRows(year, competitionName, awardLevel, major, overwrite);
        if (inserted >= 0) {
            return Map.of("success", true, "inserted", inserted);
        } else {
            return Map.of("success", false, "message", "持久化失败，请查看后端日志");
        }
    }

    /**
     * 从 finalreport 表读取已保存的合成结果（教师/学生/前端使用）
     */
    @GetMapping("/final-rows-db") // @RequestMapping(methods = Request.GET)
    public Map<String, Object> finalRowsDb(@RequestParam(value = "mentor", required = false) String mentor,
                                           @RequestParam(value = "stuNo", required = false) String stuNo,
                                           @RequestParam(value = "limit", required = false) Integer limit) {
        List<Map<String, Object>> rows = finalReportOperation.listFinalReportRows(mentor, stuNo, limit);
        return Map.of("success", true, "rows", rows, "count", rows.size());
    }

    /**
     * 保留的视图查询接口 按 mentor / stuNo / limit 从 vw_final_base 查询即时视图结果
     */
    @GetMapping("/final-rows")
    public Map<String, Object> finalRows(@RequestParam(value = "mentor", required = false) String mentor,
                                         @RequestParam(value = "stuNo", required = false) String stuNo,
                                         @RequestParam(value = "limit", required = false) Integer limit) {
        List<Map<String, Object>> rows = finalReportOperation.queryFinalRows(mentor, stuNo, limit);
        return Map.of("success", true, "rows", rows, "count", rows.size());
    }

    /**
     * 返回比赛名列表（供前端下拉）
     */
    @GetMapping("/competitions")
    public Map<String, Object> competitions() {
        List<String> comps = finalReportOperation.listCompetitions();
        return Map.of("success", true, "competitions", comps);
    }

    /**
     * 返回年份列表（供前端下拉）
     */
    @GetMapping("/years")
    public Map<String, Object> years() {
        List<Integer> years = finalReportOperation.listYears();
        return Map.of("success", true, "years", years);
    }

    /**
     * 返回专业列表（供前端复选/下拉）
     */
    @GetMapping("/majors")
    public Map<String, Object> majors() {
        List<String> majors = finalReportOperation.listMajors();
        return Map.of("success", true, "majors", majors);
    }
}
