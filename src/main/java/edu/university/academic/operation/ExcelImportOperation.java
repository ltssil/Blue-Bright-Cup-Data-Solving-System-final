package edu.university.academic.operation;

import edu.university.academic.utils.DBUtil;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExcelImportOperation {

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

    private static String pickFirst(Map<String, Integer> headerIndexMap, Row row, String... keys) {
        if (headerIndexMap == null || row == null || keys == null) return null;
        for (String key : keys) {
            String value = getByHeader(row, headerIndexMap, key);
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    public static Map<String, Object> importStudents(InputStream is) {
        Map<String, Object> resp = new HashMap<>();
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
            String sql = "INSERT INTO students (stuNo, stuName, stuGender, department, major, currentGrade, stuClass) VALUES (?, ?, ?, ?, ?, ?, ?)";
            List<Object[]> paramsList = new ArrayList<>();

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                try {
                    Object[] params = new Object[]{
                            pickFirst(headerIndexMap, row, "学号", "stuNo"),
                            pickFirst(headerIndexMap, row, "姓名", "学生姓名", "stuName"),
                            pickFirst(headerIndexMap, row, "性别", "stuGender"),
                            pickFirst(headerIndexMap, row, "院系", "department"),
                            pickFirst(headerIndexMap, row, "专业", "major"),
                            pickFirst(headerIndexMap, row, "现在年级", "年级", "currentGrade"),
                            pickFirst(headerIndexMap, row, "班级", "stuClass")
                    };

                    if (isBlank(params[0]) || isBlank(params[1])) {
                        errors.add("第 " + (r + 1) + " 行：学号或姓名为空，跳过");
                        continue;
                    }

                    paramsList.add(params);
                } catch (Exception ex) {
                    errors.add("第 " + (r + 1) + " 行：异常 - " + ex.getMessage());
                }
            }

            int successCount = countBatchSuccess(DBUtil.executeBatchUpdate(sql, paramsList));
            resp.put("success", true);
            resp.put("message", "成功导入 " + successCount + " 条学生记录");
            resp.put("errors", errors);
            return resp;
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    public static Map<String, Object> importTeachers(InputStream is) {
        Map<String, Object> resp = new HashMap<>();
        List<String> errors = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                return Map.of("success", false, "message", "Excel 中没有 sheet");
            }

            Row headerRow = sheet.getRow(0);
            Map<String, Integer> headerIndexMap = buildHeaderIndex(headerRow);

            String sql = "INSERT INTO teachers (teacherNo, departmentNo, teacherName, departmentName) VALUES (?, ?, ?, ?)";
            List<Object[]> paramsList = new ArrayList<>();

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                try {
                    Object[] params = new Object[]{
                            pickFirst(headerIndexMap, row, "工号", "teacherNo"),
                            pickFirst(headerIndexMap, row, "部门编号", "departmentNo"),
                            pickFirst(headerIndexMap, row, "姓名", "教师姓名", "teacherName"),
                            pickFirst(headerIndexMap, row, "部门名称", "departmentName")
                    };

                    if (isBlank(params[0])) {
                        errors.add("第 " + (r + 1) + " 行：工号为空，跳过");
                        continue;
                    }

                    paramsList.add(params);
                } catch (Exception ex) {
                    errors.add("第 " + (r + 1) + " 行：异常 - " + ex.getMessage());
                }
            }

            int successCount = countBatchSuccess(DBUtil.executeBatchUpdate(sql, paramsList));
            resp.put("success", true);
            resp.put("message", "成功导入 " + successCount + " 条教师记录");
            resp.put("errors", errors);
            return resp;
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    public static Map<String, Object> importSign(InputStream is) {
        Map<String, Object> resp = new HashMap<>();
        int successCount = 0;
        List<String> errors = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                return Map.of("success", false, "message", "Excel 中没有 sheet");
            }

            int headerRowNum = -1;
            for (int i = 0; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String value = CellValueOperation.getCellValue(row.getCell(0));
                if ("学号".equals(value) || "stuNo".equalsIgnoreCase(value) || "学号/学号".equalsIgnoreCase(value)) {
                    headerRowNum = i;
                    break;
                }
            }
            if (headerRowNum == -1) {
                return Map.of("success", false, "message", "未找到报名表表头行（学号）");
            }

            Row headerRow = sheet.getRow(headerRowNum);
            Map<String, Integer> headerIndexMap = buildHeaderIndex(headerRow);

            List<Object[]> allRows = new ArrayList<>();
            Map<String, Integer> nameCount = new HashMap<>();

            for (int r = headerRowNum + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                try {
                    String stuNo = pickFirst(headerIndexMap, row, "学号", "stuNo");
                    String stuName = pickFirst(headerIndexMap, row, "学生姓名", "姓名", "stuName");
                    String department = pickFirst(headerIndexMap, row, "院系", "department");
                    String subject = pickFirst(headerIndexMap, row, "科目名称", "科目", "subject");
                    String mentor = pickFirst(headerIndexMap, row, "指导老师", "指导教师", "mentor");

                    if (isBlank(stuNo) || isBlank(subject)) {
                        errors.add("第 " + (r + 1) + " 行：学号或科目为空，跳过");
                        continue;
                    }

                    stuNo = stuNo.trim();
                    stuName = stuName == null ? "" : stuName.trim();

                    allRows.add(new Object[]{
                            stuNo,
                            stuName,
                            department == null ? null : department.trim(),
                            subject == null ? null : subject.trim(),
                            mentor == null ? null : mentor.trim()
                    });

                    if (!stuName.isEmpty()) {
                        nameCount.put(stuName, nameCount.getOrDefault(stuName, 0) + 1);
                    }
                } catch (Exception ex) {
                    errors.add("第 " + (r + 1) + " 行：解析异常 - " + ex.getMessage());
                }
            }

            List<Object[]> signParams = new ArrayList<>();
            List<Object[]> conflictParams = new ArrayList<>();

            for (Object[] rowArr : allRows) {
                String stuName = (String) rowArr[1];
                String subject = (String) rowArr[3];

                if (stuName != null && !stuName.trim().isEmpty() && nameCount.getOrDefault(stuName, 0) > 1) {
                    conflictParams.add(new Object[]{stuName, subject, null});
                } else {
                    signParams.add(rowArr);
                }
            }

            String sqlConflict = "INSERT INTO conflict (stuName, subject, award) VALUES (?, ?, ?)";
            int conflictInserted = countBatchSuccess(DBUtil.executeBatchUpdate(sqlConflict, conflictParams));
            resp.put("conflictInserted", conflictInserted);

            String sqlSign = "INSERT INTO sign (stuNo, stuName, department, subject, mentor) VALUES (?, ?, ?, ?, ?)";
            successCount = countBatchSuccess(DBUtil.executeBatchUpdate(sqlSign, signParams));

            resp.put("success", true);
            resp.put("message", "导入完成：成功写入报名表 " + successCount + " 条；写入冲突 " + conflictInserted + " 条");
            resp.put("errors", errors);
            return resp;
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("success", false, "message", e.getMessage());
        }
    }

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
            List<Object[]> paramsList = new ArrayList<>();

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                try {
                    String name = pickFirst(headerIndexMap, row, "考生姓名", "姓名", "stuName", "学生姓名");
                    String subject = pickFirst(headerIndexMap, row, "科目名称", "科目", "subject");
                    String prize = pickFirst(headerIndexMap, row, "奖项", "award");
                    String inFinal = pickFirst(headerIndexMap, row, "是否进入决赛", "inFinal");
                    String yearStr = pickFirst(headerIndexMap, row, "年份", "year");
                    String comp = pickFirst(headerIndexMap, row, "比赛名", "比赛名称", "competitionName");
                    String level = pickFirst(headerIndexMap, row, "获奖级别", "awardLevel");

                    Integer yearVal = null;
                    if (yearStr != null) {
                        try {
                            yearVal = Integer.valueOf(yearStr);
                        } catch (Exception ignored) {
                            yearVal = null;
                        }
                    }

                    if (isBlank(name)) {
                        errors.add("第 " + (r + 1) + " 行：姓名为空，跳过");
                        continue;
                    }
                    if (isBlank(subject)) {
                        errors.add("第 " + (r + 1) + " 行：科目为空，跳过");
                        continue;
                    }

                    paramsList.add(new Object[]{
                            yearVal,
                            comp,
                            level,
                            name,
                            subject,
                            prize,
                            inFinal
                    });
                } catch (Exception ex) {
                    errors.add("第 " + (r + 1) + " 行：解析异常 - " + ex.getMessage());
                }
            }

            String sqlInsertAward = "INSERT INTO award (year, competitionName, awardLevel, stuName, subject, award, inFinal) VALUES (?, ?, ?, ?, ?, ?, ?)";
            awardInserted = countBatchSuccess(DBUtil.executeBatchUpdate(sqlInsertAward, paramsList));

            List<String> duplicateNames = DBUtil.query(
                    "SELECT stuName FROM award GROUP BY stuName HAVING COUNT(*) > 1",
                    new Object[]{},
                    rs -> rs.getString("stuName")
            );

            if (duplicateNames != null && !duplicateNames.isEmpty()) {
                try (Connection conn = DBUtil.getConnection();
                     PreparedStatement psInsert = conn.prepareStatement(
                             "INSERT INTO conflict (stuName, subject, award) SELECT stuName, subject, award FROM award WHERE stuName = ?"
                     );
                     PreparedStatement psDelete = conn.prepareStatement(
                             "DELETE FROM award WHERE stuName = ?"
                     )) {
                    conn.setAutoCommit(false);

                    for (String name : duplicateNames) {
                        if (isBlank(name)) continue;
                        psInsert.setString(1, name);
                        conflictInserted += psInsert.executeUpdate();

                        psDelete.setString(1, name);
                        awardInserted -= psDelete.executeUpdate();
                    }

                    conn.commit();
                } catch (Exception ex) {
                    return Map.of("success", false, "message", "处理 award->conflict 失败：" + ex.getMessage());
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

    private static boolean isBlank(Object value) {
        return value == null || value.toString().trim().isEmpty();
    }

    private static int countBatchSuccess(int[] results) {
        int count = 0;
        for (int value : results) {
            if (value > 0) {
                count += value;
            } else if (value == Statement.SUCCESS_NO_INFO) {
                count++;
            }
        }
        return count;
    }
}
