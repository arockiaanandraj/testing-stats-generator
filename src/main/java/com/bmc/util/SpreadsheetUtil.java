package com.bmc.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings({ "java:S106", "java:S125" })
public class SpreadsheetUtil {

    private File spreadsheet;
    private Sheet currentSheet;
    private Map<String, Integer> columns;

    public SpreadsheetUtil(File file) {
        spreadsheet = file;
        columns = new HashMap<>();
    }

    public Sheet switchToSheet(String name) {
        try (Workbook workbooks = WorkbookFactory.create(spreadsheet)) {
            currentSheet = workbooks.getSheet(name);
            currentSheet.getRow(9).forEach(cell -> columns.put(getCellDataAsString(cell), cell.getColumnIndex()));
            // System.out.println(columns);
        } catch (Exception e) {
            System.out.println(name + " sheet is not present in this file.");
        }
        return currentSheet;
    }

    public String getCellData(String column, int row) {
        Row dataRow = currentSheet.getRow(row);
        Integer cellNum = columns.get(column);
        if(cellNum == null) {
            return "";
        }
        return getCellDataAsString(dataRow.getCell(cellNum));
    }

    private String getCellDataAsString(Cell cell) {
        if (cell != null) {
            switch (cell.getCellType()) {
                case STRING:
                    return cell.getStringCellValue();
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        return cell.getDateCellValue().toString();
                    }
                    return String.valueOf((int) cell.getNumericCellValue());
                case FORMULA:
                    if (cell.getCachedFormulaResultType() == CellType.BOOLEAN) {
                        return String.valueOf(cell.getBooleanCellValue());
                    } else if (cell.getCachedFormulaResultType() == CellType.NUMERIC) {
                        return String.valueOf(cell.getNumericCellValue());
                    } else if (cell.getCachedFormulaResultType() == CellType.STRING) {
                        return cell.getStringCellValue();
                    }
                    return "";
                default:
                    return "";
            }

        }
        return "";
    }
}