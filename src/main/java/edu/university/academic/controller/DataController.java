package edu.university.academic.controller;

import edu.university.academic.utils.DBUtil;
import edu.university.academic.utils.RowProcessor;
import org.springframework.web.bind.annotation.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * 提供数据存在性检查与简单预览接口，供前端决定是否需要重新上传 Excel。
 */
@RestController
@RequestMapping("/api")
@CrossOrigin
public class DataController {

    /**
     * 返回各关键表的行数（students, teachers, sign, award, conflict）
     */
    @GetMapping("/data-status")
    public Map<String, Object> dataStatus() {
        Map<String, Integer> counts = new HashMap<>();
        counts.put("students", getCount("SELECT COUNT(*) AS cnt FROM students"));
        counts.put("teachers", getCount("SELECT COUNT(*) AS cnt FROM teachers"));
        counts.put("sign", getCount("SELECT COUNT(*) AS cnt FROM sign"));
        counts.put("award", getCount("SELECT COUNT(*) AS cnt FROM award"));
        counts.put("conflict", getCount("SELECT COUNT(*) AS cnt FROM conflict"));
        return Map.of("success", true, "counts", counts);
    }

    private int getCount(String sql) {
        try {
            List<Integer> res = DBUtil.query(sql, new Object[]{}, new RowProcessor<Integer>() {
                @Override
                public Integer process(ResultSet rs) throws SQLException {
                    return rs.getInt("cnt");
                }
            });
            return (res == null || res.isEmpty()) ? 0 : res.get(0);
        } catch (Exception ex) {
            ex.printStackTrace();
            return 0;
        }
    }

    /**
     * 预览数据库中已有的数据（前 N 条），用于前端显示。
     * GET /api/preview/{type}?limit=50
     * 当 limit <= 0 或 不传 limit 时，返回全部（不加 LIMIT）
     */
    @GetMapping("/preview/{type}")
    public Map<String, Object> preview(@PathVariable("type") String type,
                                       @RequestParam(value = "limit", required = false) Integer limit) {
        String baseSql;
        switch (type) {
            case "students":
                baseSql = "SELECT stuNo, stuName, stuGender, department, major, currentGrade, stuClass FROM students";
                break;
            case "teachers":
                baseSql = "SELECT teacherNo, departmentNo, teacherName, departmentName FROM teachers";
                break;
            case "sign":
                baseSql = "SELECT id, stuNo, stuName, department, subject, mentor FROM sign";
                break;
            case "award":
                baseSql = "SELECT id, year, competitionName, awardLevel, stuName, subject, award, inFinal FROM award";
                break;
            default:
                return Map.of("success", false, "message", "未知的 type: " + type);
        }

        String sql = baseSql;
        if (limit != null && limit > 0) {
            sql = baseSql + " LIMIT " + limit;
        }

        List<Map<String, Object>> rows = DBUtil.query(sql, new Object[]{}, new RowProcessor<Map<String, Object>>() {
            @Override
            public Map<String, Object> process(ResultSet rs) throws SQLException {
                Map<String, Object> m = new LinkedHashMap<>();
                int colCount = rs.getMetaData().getColumnCount();
                for (int i = 1; i <= colCount; i++) {
                    String col = rs.getMetaData().getColumnLabel(i);
                    m.put(col, rs.getObject(i));
                }
                return m;
            }
        });

        return Map.of("success", true, "rows", rows, "count", rows.size());
    }
}
