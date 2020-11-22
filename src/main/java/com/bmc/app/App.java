package com.bmc.app;

import java.io.File;
import java.io.FileWriter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.json.JSONObject;

public final class App {
    private static String empNameColumnHeading = "Employee Name";
    private static String ventSerialNbrColumnHeading = "Ventilator Serial Number";
    private static String sentForReworkColumnHeading = "Sent for Rework?";
    private static String electricalSafetyCheck = "Electrical Safety Check";
    private static String sysCalibTestProc = "Sys Calibration Test Procedure";
    private static String systemFinalTesting = "Systems Final Testing";
    private static List<String> testSheets = Arrays.asList(electricalSafetyCheck, sysCalibTestProc, systemFinalTesting);

    private static Map<String, List<String>> ventilators = new HashMap<>();

    private static int test1Count = 0;
    private static int test2Count = 0;
    private static int test3Count = 0;

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        int choice = 0;
        do {
            System.out.println("\n**** Ventilator Testing Stats ****");
            System.out.println("1. Generate Reports");
            System.out.println("2. Generate Stats");
            System.out.println("3. Exit");
            System.out.print("Choose an option: ");
            choice = Integer.parseInt(in.nextLine());
            if (choice == 1) {
                System.out.println("Enter folder path to generate reports:");
                String directoryPath = in.nextLine();
                System.out.println("Enter employee name:");
                String empName = in.nextLine();
                generateReport(directoryPath, empName);
            } else if (choice == 2) {
                System.out.println("Enter consolidated report filepath:");
                String consolidatedReportPath = in.nextLine();
                generateStats(consolidatedReportPath);
            }
        } while (choice != 3);
        // in.close();
    }

    public static void generateReport(String directoryPath, String empName) {
        List<File> files = FilesBrowser.getFiles(directoryPath);
        ConsolidatedReport consolidatedReport = new ConsolidatedReport();
        consolidatedReport.setEmpName(empName);
        int fileCount = 0;
        for (File file : files) {
            SpreadsheetUtil ssUtil = new SpreadsheetUtil(file);
            String jobNbr = file.getParentFile().getName();
            System.out.println("Processing - " + jobNbr + " - File Count = " + ++fileCount);
            for (String sheet : testSheets) {
                consolidatedReport.getReports().add(collectResults(ssUtil, sheet, empName, jobNbr));
            }
        }
        writeStats();
        writeConsolidatedReport(consolidatedReport);
    }

    public static Report collectResults(SpreadsheetUtil ssUtil, String sheetName, String empName, String jobNbr) {
        Report report = new Report();
        report.setJobNbr(jobNbr);
        Sheet currentSheet = ssUtil.switchToSheet(sheetName);
        if (currentSheet == null) {
            return report;
        }
        String currentEmpName;
        String ventilatorSerialNbr;
        Boolean sentForRework;
        int testCount = 0;
        for (Row row : currentSheet) {
            int rowNbr = row.getRowNum();
            if (rowNbr > 9) {
                currentEmpName = ssUtil.getCellData(empNameColumnHeading, rowNbr);
                ventilatorSerialNbr = ssUtil.getCellData(ventSerialNbrColumnHeading, rowNbr);
                sentForRework = ssUtil.getCellData(sentForReworkColumnHeading, rowNbr).equals("YES") ? Boolean.TRUE
                        : Boolean.FALSE;
                // System.out.println(currentEmpName);
                // System.out.println(ventilatorSerialNbr);
                if (currentEmpName.toUpperCase().contains(empName)) {
                    report.setTestName(sheetName);
                    report.setTestCount(++testCount);
                    Ventilator ventilator = new Ventilator();
                    ventilator.setSerialNbr(ventilatorSerialNbr);
                    ventilator.setSentForRework(sentForRework);
                    ventilator.setJobNbr(jobNbr);
                    ventilator.setEmpName(currentEmpName);
                    report.getVentilators().add(ventilator);
                    ventilators.computeIfAbsent(ventilatorSerialNbr, value -> new ArrayList<>());
                    ventilators.get(ventilatorSerialNbr).add(sheetName);
                    ventilators.put(ventilatorSerialNbr, ventilators.get(ventilatorSerialNbr));
                }
            }
        }
        writeReport(report);
        return report;
    }

    public static void generateStats(String consolidatedReportPath) {

        ObjectMapper objectMapper = new ObjectMapper();
        File file = new File(consolidatedReportPath);
        ConsolidatedReport consolidatedReport;

        try {
            consolidatedReport = objectMapper.readValue(file, ConsolidatedReport.class);
            System.out.println(consolidatedReport.getEmpName());
            List<Report> test1Reports = consolidatedReport.getReports().stream()
                    .filter(report -> electricalSafetyCheck.equalsIgnoreCase(report.getTestName()))
                    .collect(Collectors.toList());
            // System.out.println(test1Reports.size());
            test1Reports.forEach(report -> test1Count += report.getTestCount());
            System.out.println("Test 1 = " + test1Count);

            List<Report> test2Reports = consolidatedReport.getReports().stream().filter(report -> (sysCalibTestProc
                    .equalsIgnoreCase(report.getTestName())
                    && !report.getVentilators().isEmpty()
                    && report.getVentilators().stream()
                            .filter(ventilator -> ventilator.getEmpName().split(",").length > 1
                                    && ventilator.getEmpName().split(",")[1].contains(consolidatedReport.getEmpName()))
                            .count() > 0))
                    .collect(Collectors.toList());
            // System.out.println(test2Reports.size());
            test2Reports.forEach(report -> test2Count += report.getTestCount());
            System.out.println("Test 2 = " + test2Count);

            List<Report> test3Reports = consolidatedReport.getReports().stream()
                    .filter(report -> systemFinalTesting.equalsIgnoreCase(report.getTestName()))
                    .collect(Collectors.toList());
            // System.out.println(test3Reports.size());
            test3Reports.forEach(report -> test3Count += report.getTestCount());
            System.out.println("Test 3 = " + test3Count);
        } catch (Exception E) {
            E.printStackTrace();
        }
    }

    private static void writeReport(Report report) {
        JSONObject obj = new JSONObject(report);
        try (FileWriter outFile = new FileWriter("Reports.json", true);) {
            outFile.write(obj.toString(4));
            outFile.write(",\n");
        } catch (Exception E) {
            E.printStackTrace();
        }
    }

    private static void writeStats() {
        try (FileWriter outFile = new FileWriter("VentilatorStats.txt", true);) {
            outFile.write(ventilators.toString());
        } catch (Exception E) {
            E.printStackTrace();
        }
    }

    private static void writeConsolidatedReport(ConsolidatedReport consolidatedReport) {
        JSONObject obj = new JSONObject(consolidatedReport);
        try (FileWriter outFile = new FileWriter("ConsolidatedReport.json");) {
            outFile.write(obj.toString(4));
        } catch (Exception E) {
            E.printStackTrace();
        }
    }

}
