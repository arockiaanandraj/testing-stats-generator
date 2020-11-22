package com.bmc.app;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

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
            currentSheet.getRow(9).forEach(cell -> {
                columns.put(getCellDataAsString(cell), cell.getColumnIndex());
            });
            // System.out.println(columns);
        } catch (Exception e) {
            System.out.println(name+" sheet is not present in this file.");
        }
        return currentSheet;
    }

    public String getCellData(String column, int row) {
        Row dataRow = currentSheet.getRow(row);
        return getCellDataAsString(dataRow.getCell(columns.get(column)));
    }

    private String getCellDataAsString(Cell cell) {
        if (cell != null) {
            switch (cell.getCellType()) {
                case STRING:
                    return cell.getStringCellValue();
                case NUMERIC:
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