package edu.university.academic.test;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class POITest {
    public static void main(String[] args) {
        XSSFWorkbook wb = new XSSFWorkbook();
        wb.createSheet("test");
        System.out.println("POI 正常");
    }
}
