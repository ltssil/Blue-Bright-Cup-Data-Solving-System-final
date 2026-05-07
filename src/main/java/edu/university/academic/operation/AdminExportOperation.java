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
public class AdminExportOperation {

    /**
     * 生成冲突表为 Excel 的字节数组。
     * header: ID, 学生名, 科目, 奖项, 年份
     */
    public static byte[] generateConflictExcelBytes(Integer year) {
        try {
            List<Map<String, Object>> rows = DBUtil.query(
                    "SELECT id, stuName, subject, award, year FROM conflict ORDER BY id ASC",
                    new Object[]{},
                    new RowProcessor<Map<String, Object>>() {
                        @Override
                        public Map<String, Object> process(ResultSet rs) throws SQLException {
                            Map<String, Object> m = new LinkedHashMap<>();
                            m.put("id", rs.getObject("id"));
                            m.put("stuName", rs.getString("stuName"));
                            m.put("subject", rs.getString("subject"));
                            m.put("award", rs.getString("award"));
                            m.put("year", year);
                            return m;
                        }
                    }
            );

            try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                Sheet sheet = wb.createSheet("冲突表");

                String[] headers = new String[] { "ID", "学生名", "科目", "奖项", "年份" };

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
                    sheet.setColumnWidth(i, 20 * 256);
                }

                // body style
                CellStyle bodyStyle = wb.createCellStyle();
                bodyStyle.setVerticalAlignment(VerticalAlignment.CENTER);
                bodyStyle.setWrapText(true);
                bodyStyle.setBorderTop(BorderStyle.THIN);
                bodyStyle.setBorderBottom(BorderStyle.THIN);
                bodyStyle.setBorderLeft(BorderStyle.THIN);
                bodyStyle.setBorderRight(BorderStyle.THIN);

                int r = 1;
                for (Map<String, Object> row : rows) {
                    Row rr = sheet.createRow(r++);
                    int cidx = 0;

                    Cell c0 = rr.createCell(cidx++);
                    c0.setCellStyle(bodyStyle);
                    CellValueOperation.setCellValue(c0, row.get("id"));

                    Cell c1 = rr.createCell(cidx++);
                    c1.setCellStyle(bodyStyle);
                    CellValueOperation.setCellValue(c1, row.get("stuName"));

                    Cell c2 = rr.createCell(cidx++);
                    c2.setCellStyle(bodyStyle);
                    CellValueOperation.setCellValue(c2, row.get("subject"));

                    Cell c3 = rr.createCell(cidx++);
                    c3.setCellStyle(bodyStyle);
                    CellValueOperation.setCellValue(c3, row.get("award"));

                    Cell c4 = rr.createCell(cidx++);
                    c4.setCellStyle(bodyStyle);
                    CellValueOperation.setCellValue(c4, row.get("year"));

                    if (year != null) {
                        CellValueOperation.setCellValue(c4, year);
                    } else {
                        CellValueOperation.setCellValue(c4, "");
                    }
                }

                // auto size columns
                for (int i = 0; i < headers.length; i++) {
                    try {
                        sheet.autoSizeColumn(i);
                        int cur = sheet.getColumnWidth(i);
                        int min = 10 * 256;
                        if (cur < min) sheet.setColumnWidth(i, min);
                    } catch (Exception ignored) {}
                }

                wb.write(out);
                return out.toByteArray();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return new byte[0];
        }
    }
}
