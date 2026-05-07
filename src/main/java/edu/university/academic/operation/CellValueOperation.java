package edu.university.academic.operation;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;

public class CellValueOperation {

    public static java.lang.Object getCelValue(Cell cell) {
        if(cell == null)    return null ;
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue() ;
            case NUMERIC: return cell.getNumericCellValue() ;
            case BOOLEAN: return cell.getBooleanCellValue() ;
            case FORMULA: return cell.getCellFormula() ;
            default: return null ;
        }
    }

    public static void setCellValue(Cell cell, Object value) {
        if(value == null)   return ;
        if(value instanceof String) {
            cell.setCellValue((String) value);
        } else if(value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if(value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else {
            cell.setCellValue(value.toString());
        }
    }

    public static String getCellValue(Cell cell) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.STRING) return cell.getStringCellValue().trim();
        if (cell.getCellType() == CellType.NUMERIC) return String.valueOf((long)cell.getNumericCellValue());
        return cell.toString().trim();
    }
}