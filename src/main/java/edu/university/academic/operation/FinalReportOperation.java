package edu.university.academic.operation;

import edu.university.academic.utils.DBUtil;
import edu.university.academic.utils.RowProcessor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Service
public class FinalReportOperation {

    /**
     * composeFinalRows
     * 使用 vw_final_base（视图）作为数据源，严格依赖视图列名：
     * award_id, stuNo, stuName, major, stuGrade, stuClass, mentor,
     * subject, awardRank, inFinal, awardYear, competitionName, awardLevel
     *
     * 前端传入的 year/competitionName/awardLevel 会注入/覆盖输出。
     */
    public List<Map<String, Object>> composeFinalRows(Integer year, String competitionName, String awardLevel, String major) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT b.award_id, COALESCE(b.stuNo, s.stuNo) AS stuNo, b.stuName, b.major, b.stuGrade, b.stuClass, ");
        sql.append("b.mentor, b.subject, b.awardRank, b.inFinal, b.awardYear, b.competitionName, b.awardLevel ");
        sql.append("FROM vw_final_base b ");
        sql.append("LEFT JOIN (SELECT stuName, MIN(stuNo) AS stuNo FROM students WHERE COALESCE(stuName,'') <> '' GROUP BY stuName) s ");
        sql.append("ON COALESCE(s.stuName,'') = COALESCE(b.stuName,'') ");
        sql.append("WHERE COALESCE(b.stuName,'') <> '' ");
        sql.append("AND COALESCE(b.stuName,'') NOT IN (SELECT COALESCE(stuName,'') FROM conflict) ");

        List<Object> params = new ArrayList<>();
        if (major != null && !major.trim().isEmpty()) {
            sql.append(" AND COALESCE(b.major,'') = ? ");
            params.add(major.trim());
        }

        sql.append(" ORDER BY awardYear DESC, awardLevel, awardRank, stuNo");

        List<Map<String, Object>> rows = DBUtil.query(sql.toString(), params.toArray(), new RowProcessor<Map<String, Object>>() {
            @Override
            public Map<String, Object> process(ResultSet rs) throws SQLException {
                Map<String, Object> r = new LinkedHashMap<>();

                // 视图中的原始值
                Object dbYear = rs.getObject("awardYear");
                String dbCompetition = rs.getString("competitionName");
                String dbLevel = rs.getString("awardLevel");

                // 将前端注入值覆盖视图值
                r.put("year", year != null ? year : dbYear);
                r.put("competitionName", (competitionName != null && !competitionName.trim().isEmpty()) ? competitionName : (dbCompetition == null ? "" : dbCompetition));
                r.put("awardLevel", (awardLevel != null && !awardLevel.trim().isEmpty()) ? awardLevel : (dbLevel == null ? "" : dbLevel));

                r.put("subject", rs.getString("subject"));
                r.put("award", rs.getString("awardRank"));
                r.put("stuName", rs.getString("stuName"));
                r.put("major", rs.getString("major"));
                r.put("stuGrade", rs.getString("stuGrade"));
                r.put("stuClass", rs.getString("stuClass"));
                r.put("mentor", rs.getString("mentor"));
                r.put("inFinal", rs.getString("inFinal"));
                r.put("stuNo", rs.getObject("stuNo"));
                // 视图award_id，安全读取
                r.put("source_award_id", rs.getObject("award_id"));
                return r;
            }
        });

        return rows;
    }

    /**
     * queryFinalRows
     * 从 finalreport 查询最终已合成结果
     * 供 教师 / 学生 使用
     *
     * mentor  : 指导教师姓名
     * stuNo   : 学号
     * limit   : 返回条数<=0 或 null 表示不限制
     */
    public List<Map<String, Object>> queryFinalRows(String mentor, String stuNo, Integer limit) {

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        sql.append(" id, ");
        sql.append(" source_award_id, ");
        sql.append(" stuNo, stuName, ");
        sql.append(" major, stuGrade, stuClass, ");
        sql.append(" mentor, ");
        sql.append(" subject, ");
        sql.append(" award, ");
        sql.append(" inFinal, ");
        sql.append(" year, ");
        sql.append(" competitionName, ");
        sql.append(" awardLevel ");
        sql.append("FROM finalreport ");
        sql.append("WHERE 1 = 1 ");

        List<Object> params = new ArrayList<>();

        // 教师端：按指导教师查询
        if (mentor != null && !mentor.trim().isEmpty()) {
            sql.append(" AND mentor = ? ");
            params.add(mentor.trim());
        }

        // 学生端：按学号查询
        if (stuNo != null && !stuNo.trim().isEmpty()) {
            sql.append(" AND CAST(stuNo AS CHAR) = ? ");
            params.add(stuNo.trim());
        }

        sql.append(" ORDER BY year DESC, awardLevel, award, stuNo ");

        if (limit != null && limit > 0) {
            sql.append(" LIMIT ").append(limit);
        }

        return DBUtil.query(
                sql.toString(),
                params.toArray(),
                new RowProcessor<Map<String, Object>>() {
                    @Override
                    public Map<String, Object> process(ResultSet rs) throws SQLException {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("id", rs.getLong("id"));
                        r.put("award_id", rs.getObject("source_award_id"));
                        r.put("stuNo", rs.getObject("stuNo"));
                        r.put("stuName", rs.getString("stuName"));
                        r.put("major", rs.getString("major"));
                        r.put("stuGrade", rs.getString("stuGrade"));
                        r.put("stuClass", rs.getString("stuClass"));
                        r.put("mentor", rs.getString("mentor"));
                        r.put("subject", rs.getString("subject"));
                        r.put("award", rs.getString("award"));
                        r.put("inFinal", rs.getString("inFinal"));
                        r.put("year", rs.getInt("year"));
                        r.put("competitionName", rs.getString("competitionName"));
                        r.put("awardLevel", rs.getString("awardLevel"));
                        return r;
                    }
                }
        );
    }


    /**
     * persistFinalRows - 将合成结果持久化到 finalreport 表
     * overwrite=true 时根据前端提供的 year/competitionName/awardLevel 删除相同上下文的旧数据（若都未提供则默认清空）。
     *
     * 返回插入行数（或 -1 表示异常）。
     */
    public int persistFinalRows(Integer year, String competitionName, String awardLevel, String major, boolean overwrite) {
        try {
            if (overwrite) {
                StringBuilder delSql = new StringBuilder("DELETE FROM finalreport WHERE 1=1 ");
                List<Object> delParams = new ArrayList<>();
                if (year != null) {
                    delSql.append(" AND year = ? ");
                    delParams.add(year);
                }
                if (competitionName != null && !competitionName.trim().isEmpty()) {
                    delSql.append(" AND competitionName = ? ");
                    delParams.add(competitionName.trim());
                }
                if (awardLevel != null && !awardLevel.trim().isEmpty()) {
                    delSql.append(" AND awardLevel = ? ");
                    delParams.add(awardLevel.trim());
                }
                if (delParams.isEmpty()) {
                    // 若用户明确要求覆盖但未指定上下文，默认清空 finalreport
                    DBUtil.update("DELETE FROM finalreport", new Object[]{});
                } else {
                    DBUtil.update(delSql.toString(), delParams.toArray());
                }
            }

            StringBuilder insertSql = new StringBuilder();
            insertSql.append("INSERT INTO finalreport (year, competitionName, awardLevel, subject, award, stuName, major, stuGrade, stuClass, mentor, inFinal, stuNo, source_award_id) ");
            insertSql.append("SELECT COALESCE(?, awardYear) AS year, COALESCE(NULLIF(?,''), competitionName) AS competitionName, COALESCE(NULLIF(?,''), awardLevel) AS awardLevel, ");
            insertSql.append("b.subject, b.awardRank AS award, b.stuName, b.major, b.stuGrade, b.stuClass, b.mentor, b.inFinal, COALESCE(b.stuNo, s.stuNo), b.award_id ");
            insertSql.append("FROM vw_final_base b ");
            insertSql.append("LEFT JOIN (SELECT stuName, MIN(stuNo) AS stuNo FROM students WHERE COALESCE(stuName,'') <> '' GROUP BY stuName) s ");
            insertSql.append("ON COALESCE(s.stuName,'') = COALESCE(b.stuName,'') ");
            insertSql.append("WHERE COALESCE(b.stuName,'') <> '' AND COALESCE(b.stuName,'') NOT IN (SELECT COALESCE(stuName,'') FROM conflict) ");

            List<Object> insertParams = new ArrayList<>();
            insertParams.add(year); // param1 COALESCE(?, awardYear)
            insertParams.add(competitionName == null ? "" : competitionName); // param2
            insertParams.add(awardLevel == null ? "" : awardLevel); // param3

            if (major != null && !major.trim().isEmpty()) {
                insertSql.append(" AND COALESCE(b.major,'') = ? ");
                insertParams.add(major.trim());
            }

            int inserted = DBUtil.update(insertSql.toString(), insertParams.toArray());
            return inserted;
        } catch (Exception ex) {
            ex.printStackTrace();
            return -1;
        }
    }

    /**
     * listFinalReportRows - 从 finalreport 表读取已持久化的合成结果供教师/学生稳定查看
     */
    public List<Map<String, Object>> listFinalReportRows(String mentor, String stuNo, Integer limit) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT id, year, competitionName, awardLevel, subject, award, stuName, major, stuGrade, stuClass, mentor, inFinal, stuNo, source_award_id, created_time ");
        sql.append("FROM finalreport ");
        sql.append("WHERE COALESCE(stuName,'') <> '' ");

        List<Object> params = new ArrayList<>();
        if (mentor != null && !mentor.trim().isEmpty()) {
            sql.append(" AND COALESCE(mentor,'') = ? ");
            params.add(mentor.trim());
        }
        if (stuNo != null && !stuNo.trim().isEmpty()) {
            sql.append(" AND COALESCE(CAST(stuNo AS CHAR),'') = ? ");
            params.add(stuNo.trim());
        }

        sql.append(" ORDER BY year DESC, awardLevel, award, stuNo ");

        if (limit != null && limit > 0) {
            sql.append(" LIMIT ").append(limit);
        }

        List<Map<String, Object>> rows = DBUtil.query(sql.toString(), params.toArray(), new RowProcessor<Map<String, Object>>() {
            @Override
            public Map<String, Object> process(ResultSet rs) throws SQLException {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("id", rs.getObject("id"));
                r.put("year", rs.getObject("year"));
                r.put("competitionName", rs.getString("competitionName"));
                r.put("awardLevel", rs.getString("awardLevel"));
                r.put("subject", rs.getString("subject"));
                r.put("award", rs.getString("award"));
                r.put("stuName", rs.getString("stuName"));
                r.put("major", rs.getString("major"));
                r.put("stuGrade", rs.getString("stuGrade"));
                r.put("stuClass", rs.getString("stuClass"));
                r.put("mentor", rs.getString("mentor"));
                r.put("inFinal", rs.getString("inFinal"));
                r.put("stuNo", rs.getObject("stuNo"));
                r.put("source_award_id", rs.getObject("source_award_id"));
                r.put("created_time", rs.getObject("created_time"));
                return r;
            }
        });

        return rows;
    }

    /**
     * generateFinalExcelBytes - 复用 composeFinalRows 的输出生成 Excel
     */
    public byte[] generateFinalExcelBytes(Integer year, String competitionName, String awardLevel, String major) {
        List<Map<String, Object>> rows = composeFinalRows(year, competitionName, awardLevel, major);

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("最终表");

            String[] headers = new String[] {
                    "年份", "获奖级别", "比赛名", "科目名称", "奖项等级", "学生姓名", "学号", "专业", "年级", "班级", "指导老师", "是否进入决赛"
            };

            // header style
            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setWrapText(true);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            Row hRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell c = hRow.createCell(i);
                CellValueOperation.setCellValue(c, headers[i]);
                c.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 18 * 256);
            }

            // body style
            CellStyle bodyStyle = wb.createCellStyle();
            bodyStyle.setAlignment(HorizontalAlignment.CENTER);
            bodyStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            bodyStyle.setWrapText(true);
            bodyStyle.setBorderTop(BorderStyle.THIN);
            bodyStyle.setBorderBottom(BorderStyle.THIN);
            bodyStyle.setBorderLeft(BorderStyle.THIN);
            bodyStyle.setBorderRight(BorderStyle.THIN);

            DataFormatter df = new DataFormatter();

            int rnum = 1;
            for (Map<String, Object> row : rows) {
                Row rr = sheet.createRow(rnum++);
                int cidx = 0;

                Cell c0 = rr.createCell(cidx++);
                c0.setCellStyle(bodyStyle);
                CellValueOperation.setCellValue(c0, row.get("year") == null ? "" : row.get("year").toString());

                Cell c1 = rr.createCell(cidx++);
                c1.setCellStyle(bodyStyle);
                CellValueOperation.setCellValue(c1, row.get("awardLevel") == null ? "" : row.get("awardLevel"));

                Cell c2 = rr.createCell(cidx++);
                c2.setCellStyle(bodyStyle);
                CellValueOperation.setCellValue(c2, row.get("competitionName") == null ? "" : row.get("competitionName"));

                Cell c3 = rr.createCell(cidx++);
                c3.setCellStyle(bodyStyle);
                CellValueOperation.setCellValue(c3, row.get("subject"));

                Cell c4 = rr.createCell(cidx++);
                c4.setCellStyle(bodyStyle);
                CellValueOperation.setCellValue(c4, row.get("award"));

                Cell c5 = rr.createCell(cidx++);
                c5.setCellStyle(bodyStyle);
                CellValueOperation.setCellValue(c5, row.get("stuName"));

                Cell c6 = rr.createCell(cidx++);
                c6.setCellStyle(bodyStyle);
                Object value = row.get("stuNo");
                c6.setCellValue(value == null ? "" : String.valueOf(value));

                Cell c7 = rr.createCell(cidx++);
                c7.setCellStyle(bodyStyle);
                CellValueOperation.setCellValue(c7, row.get("major"));

                Cell c8 = rr.createCell(cidx++);
                c8.setCellStyle(bodyStyle);
                CellValueOperation.setCellValue(c8, row.get("stuGrade"));

                Cell c9 = rr.createCell(cidx++);
                c9.setCellStyle(bodyStyle);
                CellValueOperation.setCellValue(c9, row.get("stuClass"));

                Cell c10 = rr.createCell(cidx++);
                c10.setCellStyle(bodyStyle);
                CellValueOperation.setCellValue(c10, row.get("mentor"));

                Cell c11 = rr.createCell(cidx++);
                c11.setCellStyle(bodyStyle);
                CellValueOperation.setCellValue(c11, row.get("inFinal"));
            }

            sheet.createFreezePane(0, 1);

            for (int i = 0; i < headers.length; i++) {
                try {
                    sheet.autoSizeColumn(i);
                    int current = sheet.getColumnWidth(i);
                    int minWidth = 12 * 256;
                    if (i == 2 || i == 3 || i == 10) minWidth = 28 * 256;
                    if (i == 5) minWidth = 16 * 256;
                    if (current < minWidth) sheet.setColumnWidth(i, minWidth);
                    int maxWidth = 80 * 256;
                    if (sheet.getColumnWidth(i) > maxWidth) sheet.setColumnWidth(i, maxWidth);
                } catch (Exception ignored) {}
            }

            int lastRow = sheet.getLastRowNum();
            for (int ri = 1; ri <= lastRow; ri++) {
                Row rr = sheet.getRow(ri);
                if (rr == null) continue;
                int maxLines = 1;
                for (int ci = 0; ci < headers.length; ci++) {
                    Cell cell = rr.getCell(ci);
                    String text = (cell == null) ? "" : df.formatCellValue(cell);
                    int colWidthChars = sheet.getColumnWidth(ci) / 256;
                    if (colWidthChars <= 0) colWidthChars = 30;
                    int lines = (int) Math.ceil((double) text.length() / (double) colWidthChars);
                    if (lines < 1) lines = 1;
                    maxLines = Math.max(maxLines, lines);
                }
                float height = (float) (maxLines * 15.0);
                rr.setHeightInPoints(height);
            }

            wb.write(out);
            return out.toByteArray();
        } catch (Exception ex) {
            ex.printStackTrace();
            return new byte[0];
        }
    }

    // 辅助方法majors/competitions/years
    public List<String> listMajors() {
        String sql = "SELECT DISTINCT COALESCE(major,'') AS major FROM students WHERE COALESCE(major,'') <> ''";
        return DBUtil.query(sql, new Object[]{}, rs -> rs.getString("major"));
    }

    public List<String> listCompetitions() {
        String sql = "SELECT DISTINCT COALESCE(competitionName, '') AS competitionName FROM award WHERE COALESCE(competitionName,'') <> ''";
        return DBUtil.query(sql, new Object[]{}, new RowProcessor<String>() {
            @Override
            public String process(ResultSet rs) throws SQLException {
                return rs.getString("competitionName");
            }
        });
    }

    public List<Integer> listYears() {
        String sql = "SELECT DISTINCT year FROM award WHERE year IS NOT NULL ORDER BY year DESC";
        return DBUtil.query(sql, new Object[]{}, new RowProcessor<Integer>() {
            @Override
            public Integer process(ResultSet rs) throws SQLException {
                return rs.getInt("year");
            }
        });
    }
}
