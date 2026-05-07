package edu.university.academic.operation;

import edu.university.academic.utils.DBUtil;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

import java.io.InputStream;
import java.util.*;

/**
 * Excel 导入操作类：接收 InputStream，处理各表导入逻辑。
 * Controller 只负责接收 MultipartFile -> InputStream，
 * 业务逻辑在此类实现，便于测试与复用。
 */
public class ExcelImportOperation {

    // -------------------- 公共帮助方法 --------------------

    private static Map<String, Integer> buildHeaderIndex(Row headerRow) {
        Map<String, Integer> headerIndex = new HashMap<>();
        if (headerRow == null) return headerIndex;
        for (Cell cell : headerRow) {
            String header = cell == null ? "" : CellValueOperation.getCellValue(cell);
            headerIndex.put(header, cell.getColumnIndex());
        }
        return headerIndex;
    }

    private static String getByHeader(Row row, Map<String, Integer> headerIndexMap, String headerName) {
        Integer idx = headerIndexMap.get(headerName);
        if (idx == null) return null;
        Cell cell = row.getCell(idx);
        return CellValueOperation.getCellValue(cell);
    }

    // -------------------- students 导入 --------------------

    public static Map<String, Object> importStudents(InputStream is) {
        Map<String, Object> resp = new HashMap<>();
        int successCount = 0;
        List<String> errors = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                return Map.of("success", false, "message", "Excel 中没有 sheet");
            }

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                return Map.of("success", false, "message", "Excel 第一行必须是表头");
            }

            Map<String, Integer> headerIndexMap = buildHeaderIndex(headerRow);
            Map<String, String> headerMapping = new LinkedHashMap<>();
            headerMapping.put("学号", "stuNo");
            headerMapping.put("姓名", "stuName");
            headerMapping.put("性别", "stuGender");
            headerMapping.put("院系", "department");
            headerMapping.put("专业", "major");
            headerMapping.put("现在年级", "currentGrade");
            headerMapping.put("班级", "stuClass");

            String sql = "INSERT INTO students (stuNo, stuName, stuGender, department, major, currentGrade, stuClass) VALUES (?, ?, ?, ?, ?, ?, ?)";

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                try {
                    Object[] params = new Object[]{
                            getByHeader(row, headerIndexMap, "学号"),
                            getByHeader(row, headerIndexMap, "姓名"),
                            getByHeader(row, headerIndexMap, "性别"),
                            getByHeader(row, headerIndexMap, "院系"),
                            getByHeader(row, headerIndexMap, "专业"),
                            getByHeader(row, headerIndexMap, "现在年级"),
                            getByHeader(row, headerIndexMap, "班级")
                    };

                    // 基本校验：学号与姓名必须存在
                    if (params[0] == null || params[0].toString().trim().isEmpty()
                            || params[1] == null || params[1].toString().trim().isEmpty()) {
                        errors.add("第 " + (r + 1) + " 行：学号或姓名为空，跳过");
                        continue;
                    }

                    int effect = DBUtil.update(sql, params);
                    if (effect > 0) successCount++;
                    else errors.add("第 " + (r + 1) + " 行：插入失败");
                } catch (Exception ex) {
                    errors.add("第 " + (r + 1) + " 行：异常 - " + ex.getMessage());
                }
            }

            resp.put("success", true);
            resp.put("message", "成功导入 " + successCount + " 条学生记录");
            resp.put("errors", errors);
            return resp;

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    // -------------------- teachers 导入 --------------------

    public static Map<String, Object> importTeachers(InputStream is) {
        Map<String, Object> resp = new HashMap<>();
        int successCount = 0;
        List<String> errors = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                return Map.of("success", false, "message", "Excel 中没有 sheet");
            }

            Row headerRow = sheet.getRow(0);
            Map<String, Integer> headerIndexMap = buildHeaderIndex(headerRow);

            String sql = "INSERT INTO teachers (teacherNo, departmentNo, teacherName, departmentName) VALUES (?, ?, ?, ?)";

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                try {
                    Object[] params = new Object[]{
                            getByHeader(row, headerIndexMap, "工号"),
                            getByHeader(row, headerIndexMap, "部门编号"),
                            getByHeader(row, headerIndexMap, "姓名"),
                            getByHeader(row, headerIndexMap, "部门名称")
                    };
                    if (params[0] == null || params[0].toString().trim().isEmpty()) {
                        errors.add("第 " + (r + 1) + " 行：工号为空，跳过");
                        continue;
                    }
                    if (DBUtil.update(sql, params) > 0) successCount++;
                    else errors.add("第 " + (r + 1) + " 行：插入失败");
                } catch (Exception ex) {
                    errors.add("第 " + (r + 1) + " 行：异常 - " + ex.getMessage());
                }
            }

            resp.put("success", true);
            resp.put("message", "成功导入 " + successCount + " 条教师记录");
            resp.put("errors", errors);
            return resp;

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    // -------------------- sign 导入 --------------------

    public static Map<String, Object> importSign(InputStream is) {
        Map<String, Object> resp = new HashMap<>();
        int successCount = 0;
        List<String> errors = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                return Map.of("success", false, "message", "Excel 中没有 sheet");
            }

            // 查找表头行（查找首列为 "学号" 的行）
            int headerRowNum = -1;
            for (int i = 0; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                Cell first = row.getCell(0);
                if (first == null) continue;
                String v = CellValueOperation.getCellValue(first);
                if ("学号".equals(v) || "stuNo".equalsIgnoreCase(v) || "学号/学号".equalsIgnoreCase(v)) {
                    headerRowNum = i;
                    break;
                }
            }
            if (headerRowNum == -1) {
                return Map.of("success", false, "message", "未找到报名表表头行（学号）");
            }

            Row headerRow = sheet.getRow(headerRowNum);
            Map<String, Integer> headerIndexMap = buildHeaderIndex(headerRow);

            // 首先把 Excel 中的数据读出来，按姓名统计出现次数
            // 同时收集每一行用于后续插入
            List<Object[]> allRows = new ArrayList<>();
            Map<String, Integer> nameCount = new HashMap<>();

            for (int r = headerRowNum + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                try {
                    String stuNo = getByHeader(row, headerIndexMap, "学号");
                    String stuName = getByHeader(row, headerIndexMap, "学生姓名");
                    if (stuName == null || stuName.trim().isEmpty()) {
                        // 兼容不同表头命名
                        stuName = getByHeader(row, headerIndexMap, "姓名");
                    }
                    String department = getByHeader(row, headerIndexMap, "院系");
                    String subject = getByHeader(row, headerIndexMap, "科目名称");
                    String mentor = getByHeader(row, headerIndexMap, "指导老师");

                    if (stuNo == null || stuNo.toString().trim().isEmpty() ||
                            subject == null || subject.toString().trim().isEmpty()) {
                        errors.add("第 " + (r + 1) + " 行：学号或科目为空，跳过");
                        continue;
                    }

                    stuNo = stuNo.trim();
                    stuName = stuName == null ? "" : stuName.trim();

                    allRows.add(new Object[]{stuNo, stuName, department == null ? null : department.trim(),
                            subject == null ? null : subject.trim(), mentor == null ? null : mentor.trim()});

                    if (!stuName.isEmpty()) {
                        nameCount.put(stuName, nameCount.getOrDefault(stuName, 0) + 1);
                    }

                } catch (Exception ex) {
                    errors.add("第 " + (r + 1) + " 行：解析异常 - " + ex.getMessage());
                }
            }

            // 把重复姓名>=2次视为冲突：把这些行写入 conflict（来源：sign），其余行写入 sign
            List<Object[]> signParams = new ArrayList<>();
            List<Object[]> conflictParams = new ArrayList<>();

            for (Object[] rowArr : allRows) {
                String stuNo = (String) rowArr[0];
                String stuName = (String) rowArr[1];
                String department = (String) rowArr[2];
                String subject = (String) rowArr[3];
                String mentor = (String) rowArr[4];

                if (stuName != null && !stuName.trim().isEmpty() && nameCount.getOrDefault(stuName, 0) > 1) {
                    // 冲突：插入 conflict（保留学生姓名、科目、来源标记）
                    conflictParams.add(new Object[]{stuName, subject, null});
                } else {
                    // 非冲突：插入 sign
                    signParams.add(new Object[]{stuNo, stuName, department, subject, mentor});
                }
            }

            // 先插入 conflict，再插入 sign——确保顺序一致
            if (!conflictParams.isEmpty()) {
                String sqlConflict = "INSERT INTO conflict (stuName, subject, award) VALUES (?, ?, ?)";
                int[] res = DBUtil.executeBatchUpdate(sqlConflict, conflictParams);
                int added = 0;
                for (int v : res) {
                    if (v >= 0) added += (v > 0 ? v : 1);
                }
                // 记录冲突插入数量作为信息
                resp.put("conflictInserted", added);
            } else {
                resp.put("conflictInserted", 0);
            }

            if (!signParams.isEmpty()) {
                String sqlSign = "INSERT INTO sign (stuNo, stuName, department, subject, mentor) VALUES (?, ?, ?, ?, ?)";
                int[] res = DBUtil.executeBatchUpdate(sqlSign, signParams);
                int added = 0;
                for (int v : res) {
                    if (v >= 0) added += (v > 0 ? v : 1);
                }
                successCount += added;
            }

            resp.put("success", true);
            resp.put("message", "导入完成：成功写入报名表 " + successCount + " 条；写入冲突 " + resp.getOrDefault("conflictInserted", 0) + " 条");
            resp.put("errors", errors);
            return resp;

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("success", false, "message", e.getMessage());
        }
    }


    // -------------------- award 导入（同名 -> conflict，否则 -> award） --------------------

    public static Map<String, Object> importAwardAndExtractConflicts(InputStream is) {

        Map<String, Object> result = new HashMap<>();
        List<String> errors = new ArrayList<>();

        int awardInserted = 0;
        int conflictInserted = 0;

        try (Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                return Map.of("success", false, "message", "Excel 中没有 sheet");
            }

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                return Map.of("success", false, "message", "未找到表头行");
            }

            Map<String, Integer> headerIndexMap = buildHeaderIndex(headerRow);

            // 多种表头命名提高容错
            String[] nameKeys = new String[] { "考生姓名", "姓名", "stuName", "学生姓名" };
            String[] subjectKeys = new String[] { "科目名称", "subject" };
            String[] awardKeys = new String[] { "奖项", "award" };
            String[] inFinalKeys = new String[] { "是否进入决赛", "inFinal" };
            String[] yearKeys = new String[] { "年份", "year" };
            String[] compKeys = new String[] { "比赛名", "competitionName", "比赛名称" };
            String[] levelKeys = new String[] { "获奖级别", "awardLevel" };

            // 收集要插入 award 的行参数
            List<Object[]> paramsList = new ArrayList<>();

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                try {
                    String name = pickFirst(headerIndexMap, row, nameKeys);
                    String subject = pickFirst(headerIndexMap, row, subjectKeys);
                    String prize = pickFirst(headerIndexMap, row, awardKeys);
                    String inFinal = pickFirst(headerIndexMap, row, inFinalKeys);
                    String yearStr = pickFirst(headerIndexMap, row, yearKeys);
                    Integer yearVal = null;
                    if (yearStr != null) {
                        try { yearVal = Integer.valueOf(yearStr); } catch (Exception ignored) { yearVal = null; }
                    }
                    String comp = pickFirst(headerIndexMap, row, compKeys);
                    String level = pickFirst(headerIndexMap, row, levelKeys);

                    if (name == null || name.isEmpty()) {
                        errors.add("第 " + (r + 1) + " 行：姓名为空，跳过");
                        continue;
                    }
                    if (subject == null || subject.isEmpty()) {
                        errors.add("第 " + (r + 1) + " 行：科目为空，跳过");
                        continue;
                    }

                    Object[] params = new Object[] {
                            yearVal,
                            (comp == null ? null : comp),
                            (level == null ? null : level),
                            name,
                            subject,
                            (prize == null ? null : prize),
                            (inFinal == null ? null : inFinal)
                    };

                    paramsList.add(params);

                } catch (Exception ex) {
                    errors.add("第 " + (r + 1) + " 行：解析异常 - " + ex.getMessage());
                }
            }

            // 批量插入 award
            if (!paramsList.isEmpty()) {
                String sqlInsertAward = "INSERT INTO award (year, competitionName, awardLevel, stuName, subject, award, inFinal) VALUES (?, ?, ?, ?, ?, ?, ?)";
                int[] res = DBUtil.executeBatchUpdate(sqlInsertAward, paramsList);
                int sum = 0;
                for (int v : res) {
                    if (v > 0) sum += v;
                    else if (v == 0) sum += 0;
                    else sum += 1;
                }
                awardInserted = sum;
            }

            // 在 award 表内查找重复姓名并迁移到 conflict
            List<String> duplicateNames = DBUtil.query(
                    "SELECT stuName FROM award GROUP BY stuName HAVING COUNT(*) > 1",
                    new Object[]{},
                    rs -> rs.getString("stuName")
            );

            if (duplicateNames != null && !duplicateNames.isEmpty()) {
                Connection conn = null;
                PreparedStatement psInsert = null;
                PreparedStatement psDelete = null;
                try {
                    Class.forName(DBUtil.DRIVER);
                    conn = DriverManager.getConnection(DBUtil.URL, DBUtil.USER, DBUtil.PASS);
                    conn.setAutoCommit(false);

                    psInsert = conn.prepareStatement(
                            "INSERT INTO conflict (stuName, subject, award) SELECT stuName, subject, award FROM award WHERE stuName = ?"
                    );
                    psDelete = conn.prepareStatement(
                            "DELETE FROM award WHERE stuName = ?"
                    );

                    for (String nm : duplicateNames) {
                        if (nm == null || nm.trim().isEmpty()) continue;
                        psInsert.setString(1, nm);
                        int ins = psInsert.executeUpdate();
                        conflictInserted += ins;

                        psDelete.setString(1, nm);
                        int del = psDelete.executeUpdate();
                        awardInserted -= del;
                    }

                    conn.commit();
                } catch (Exception ex) {
                    if (conn != null) {
                        try { conn.rollback(); } catch (Exception ignored) {}
                    }
                    return Map.of("success", false, "message", "处理 award->conflict 失败：" + ex.getMessage());
                } finally {
                    try { if (psInsert != null) psInsert.close(); } catch (Exception ignored) {}
                    try { if (psDelete != null) psDelete.close(); } catch (Exception ignored) {}
                    try { if (conn != null) conn.close(); } catch (Exception ignored) {}
                }
            }

            result.put("success", true);
            result.put("awardInserted", awardInserted);
            result.put("conflictInserted", conflictInserted);
            result.put("errors", errors);
            return result;

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    private static String pickFirst(Map<String, Integer> headerIndexMap, Row row, String[] keys) {
        if (headerIndexMap == null || row == null || keys == null) return null;
        for (String k : keys) {
            Integer idx = headerIndexMap.get(k);
            if (idx != null) {
                Cell c = row.getCell(idx);
                String v = CellValueOperation.getCellValue(c);
                if (v != null && !v.trim().isEmpty()) return v.trim();
            }
        }
        return null;
    }
}
