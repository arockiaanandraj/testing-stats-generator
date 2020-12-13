package com.bmc.app;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bmc.dto.ConsolidatedReport;
import com.bmc.dto.Report;
import com.bmc.dto.Ventilator;
import com.bmc.util.FilesBrowserUtil;
import com.bmc.util.Helper;
import com.bmc.util.SpreadsheetUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.json.JSONObject;

@SuppressWarnings({ "java:S106", "java:S125", "java:S3776", "java:S1643" })
public final class App {
    private static final String EMP_NAME_COL_HEADING = "Employee Name";
    private static final String TEST1_EMP_NAME_COL_HEADING = "Test 1 Operator ";
    private static final String VENT_SERIAL_NBR_COL_HEADING = "Ventilator Serial Number";
    private static final String SENT_FOR_REWORK_COL_HEADING = "Sent for Rework?";
    private static final String DATE_COL_HEADING = "Date";
    private static final String ASSEMBLY1_SHEET_NAME = "Assembly 1";
    private static final String ASSEMBLY2_SHEET_NAME = "Assembly 2";
    private static final String ASSEMBLY3_SHEET_NAME = "Assembly 3";
    private static final String ELECTRICAL_SAFETY_CHECK_SHEET_NAME = "Electrical Safety Check";
    private static final String SYSTEM_CALIB_TEST_SHEET_NAME = "Sys Calibration Test Procedure";
    private static final String SYSTEM_FINAL_TESTING_SHEET_NAME = "Systems Final Testing";
    private static final List<String> assemblySheets = Arrays.asList(ASSEMBLY1_SHEET_NAME, ASSEMBLY2_SHEET_NAME,
            ASSEMBLY3_SHEET_NAME);
    private static final List<String> testSheets = Arrays.asList(ELECTRICAL_SAFETY_CHECK_SHEET_NAME,
            SYSTEM_CALIB_TEST_SHEET_NAME, SYSTEM_FINAL_TESTING_SHEET_NAME);
    private static final List<String> allSheets = Stream.of(assemblySheets, testSheets).flatMap(List<String>::stream)
            .collect(Collectors.toList());

    private static Map<String, List<String>> ventilators = new HashMap<>();

    private static int assembly1Count = 0;
    private static int assembly2Count = 0;
    private static int assembly3Count = 0;

    private static int test1Count = 0;
    private static int test2Count = 0;
    private static int test3Count = 0;

    private static int sentForReworkCount = 0;

    public static void main(String[] args) {
        try (Scanner in = new Scanner(System.in)) {
            int choice = 0;
            do {
                System.out.println("\n**** Ventilator Testing Stats ****");
                System.out.println("1. Generate Reports");
                System.out.println("2. Generate Stats");
                System.out.println("3. Exit");
                System.out.print("Choose an option: ");
                choice = Integer.parseInt(in.nextLine());
                if (choice == 1) {
                    ventilators = new HashMap<>();
                    System.out.println("Enter folder path to generate reports:");
                    String directoryPath = in.nextLine();
                    System.out.println("Enter employee name:");
                    String empName = in.nextLine();
                    generateReport(directoryPath, empName);
                } else if (choice == 2) {
                    assembly1Count = 0;
                    assembly2Count = 0;
                    assembly3Count = 0;
                    test1Count = 0;
                    test2Count = 0;
                    test3Count = 0;
                    sentForReworkCount = 0;
                    System.out.println("Enter consolidated report filepath:");
                    String consolidatedReportPath = in.nextLine();
                    generateStats(consolidatedReportPath);
                }
            } while (choice != 3);
        } catch (Exception E) {
            E.printStackTrace();
        }
    }

    public static void generateReport(String directoryPath, String empName) {
        List<File> files = FilesBrowserUtil.getFiles(directoryPath);
        ConsolidatedReport consolidatedReport = new ConsolidatedReport();
        consolidatedReport.setEmpName(empName);
        int fileCount = 0;
        for (File file : files) {
            SpreadsheetUtil ssUtil = new SpreadsheetUtil(file);
            String jobNbr = file.getParentFile().getName();
            System.out.println("Processing - " + jobNbr + " - File Count = " + ++fileCount);
            for (String sheet : allSheets) {
                consolidatedReport.getReports().add(collectResults(ssUtil, sheet, empName, jobNbr));
            }
        }
        writeStats(empName);
        writeConsolidatedReport(consolidatedReport, empName);
    }

    public static Report collectResults(SpreadsheetUtil ssUtil, String sheetName, String empName, String jobNbr) {
        Report report = new Report();
        report.setJobNbr(jobNbr);
        Sheet currentSheet = ssUtil.switchToSheet(sheetName);
        if (currentSheet == null) {
            return report;
        }
        String currentEmpName;
        String test1Operator;
        String ventilatorSerialNbr;
        Boolean sentForRework = Boolean.FALSE;
        LocalDateTime testDate;
        int testCount = 0;
        for (Row row : currentSheet) {
            int rowNbr = row.getRowNum();
            if (rowNbr > 9) {
                ventilatorSerialNbr = ssUtil.getCellData(VENT_SERIAL_NBR_COL_HEADING, rowNbr);
                testDate = Helper.convertToLocalDateTime(ssUtil.getCellData(DATE_COL_HEADING, rowNbr));
                if (!assemblySheets.contains(sheetName)) {
                    sentForRework = "YES".equalsIgnoreCase(ssUtil.getCellData(SENT_FOR_REWORK_COL_HEADING, rowNbr))
                            ? Boolean.TRUE
                            : Boolean.FALSE;
                }
                currentEmpName = ssUtil.getCellData(EMP_NAME_COL_HEADING, rowNbr);
                if (SYSTEM_CALIB_TEST_SHEET_NAME.equals(sheetName)) {
                    test1Operator = ssUtil.getCellData(TEST1_EMP_NAME_COL_HEADING, rowNbr);
                    if (!test1Operator.isEmpty() && !currentEmpName.isEmpty() && Boolean.FALSE.equals(sentForRework)) {
                        currentEmpName = test1Operator + "," + currentEmpName;
                    } else if (!test1Operator.isEmpty()
                            && (currentEmpName.isEmpty() || Boolean.TRUE.equals(sentForRework))) {
                        currentEmpName = test1Operator;
                    }
                }
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
                    ventilator.setTestDate(testDate);
                    report.getVentilators().add(ventilator);
                    ventilators.computeIfAbsent(ventilatorSerialNbr, value -> new ArrayList<>());
                    ventilators.get(ventilatorSerialNbr).add(sheetName);
                    ventilators.put(ventilatorSerialNbr, ventilators.get(ventilatorSerialNbr));
                }
            }
        }
        writeReport(report, empName);
        return report;
    }

    public static void generateStats(String consolidatedReportPath) {

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        File file = new File(consolidatedReportPath);
        ConsolidatedReport consolidatedReport;

        try {
            consolidatedReport = objectMapper.readValue(file, ConsolidatedReport.class);
            System.out.println(consolidatedReport.getEmpName());

            List<Report> assembly1Reports = consolidatedReport.getReports().stream()
                    .filter(report -> ASSEMBLY1_SHEET_NAME.equalsIgnoreCase(report.getTestName()))
                    .collect(Collectors.toList());
            // System.out.println(assembly1Reports.size());
            assembly1Reports.forEach(report -> assembly1Count += report.getTestCount());
            System.out.println("Assembly 1 = " + assembly1Count);

            List<Report> assembly2Reports = consolidatedReport.getReports().stream()
                    .filter(report -> ASSEMBLY2_SHEET_NAME.equalsIgnoreCase(report.getTestName()))
                    .collect(Collectors.toList());
            // System.out.println(assembly2Reports.size());
            assembly2Reports.forEach(report -> assembly2Count += report.getTestCount());
            System.out.println("Assembly 2 = " + assembly2Count);

            List<Report> assembly3Reports = consolidatedReport.getReports().stream()
                    .filter(report -> ASSEMBLY3_SHEET_NAME.equalsIgnoreCase(report.getTestName()))
                    .collect(Collectors.toList());
            // System.out.println(assembly3Reports.size());
            assembly3Reports.forEach(report -> assembly3Count += report.getTestCount());
            System.out.println("Assembly 3 = " + assembly3Count);

            Set<String> vents = new TreeSet<>();
            consolidatedReport.getReports().forEach(report -> {
                if (ELECTRICAL_SAFETY_CHECK_SHEET_NAME.equalsIgnoreCase(report.getTestName())
                        && !report.getVentilators().isEmpty()) {
                    report.getVentilators().forEach(ventilator -> {
                        vents.add(ventilator.getSerialNbr());
                        test1Count++;
                    });
                }
                if (SYSTEM_CALIB_TEST_SHEET_NAME.equalsIgnoreCase(report.getTestName())
                        && !report.getVentilators().isEmpty()) {
                    report.getVentilators().forEach(ventilator -> {
                        if ((!vents.contains(ventilator.getSerialNbr()))) {
                            String tempStr = ventilator.getEmpName();
                            if (tempStr.contains("\n") && !tempStr.contains(",")) {
                                tempStr = tempStr.replace("\n", ",");
                            } else if (tempStr.contains("\n") && tempStr.contains(",")) {
                                tempStr = tempStr.replace("\n", "");
                            }
                            if ((tempStr.contains(",")
                                    && (tempStr.split(",").length > 1 && tempStr.split(",")[0].toUpperCase()
                                            .contains(consolidatedReport.getEmpName()))
                                    || (tempStr.split(",").length > 1 && tempStr.split(",")[0].isEmpty()
                                            && Boolean.TRUE.equals(ventilator.getSentForRework())))
                                    || (!tempStr.contains(",") && Boolean.TRUE.equals(ventilator.getSentForRework()))) {
                                vents.add(ventilator.getSerialNbr());
                                test1Count++;
                            }
                        }
                    });
                }
            });
            // System.out.println(vents);
            System.out.println("Test 1 = " + test1Count);

            consolidatedReport.getReports().forEach(report -> {
                if (SYSTEM_CALIB_TEST_SHEET_NAME.equalsIgnoreCase(report.getTestName())
                        && !report.getVentilators().isEmpty()) {
                    report.getVentilators().forEach(ventilator -> {
                        String tempStr = ventilator.getEmpName();
                        if (tempStr.contains("\n") && !tempStr.contains(",")) {
                            tempStr = tempStr.replace("\n", ",");
                        } else if (tempStr.contains("\n") && tempStr.contains(",")) {
                            tempStr = tempStr.replace("\n", "");
                        }
                        if (tempStr.split(",").length > 1
                                && tempStr.split(",")[1].toUpperCase().contains(consolidatedReport.getEmpName())) {
                            test2Count++;
                        }
                    });
                }
            });
            System.out.println("Test 2 = " + test2Count);

            consolidatedReport.getReports().forEach(report -> {
                if (SYSTEM_FINAL_TESTING_SHEET_NAME.equalsIgnoreCase(report.getTestName())
                        && !report.getVentilators().isEmpty()) {
                    report.getVentilators().forEach(ventilator -> {
                        String tempStr = ventilator.getEmpName();
                        if (tempStr.contains("\n") && !tempStr.contains(",")) {
                            tempStr = tempStr.replace("\n", ",");
                        } else if (tempStr.contains("\n") && tempStr.contains(",")) {
                            tempStr = tempStr.replace("\n", "");
                        }
                        // System.out.println(tempStr);
                        if (tempStr.split(",").length > 1
                                && tempStr.split(",")[1].toUpperCase().contains(consolidatedReport.getEmpName())) {
                            test3Count++;
                        }
                    });
                }
            });
            System.out.println("Test 3 = " + test3Count);

            consolidatedReport.getReports().forEach(report -> {
                if (!report.getVentilators().isEmpty()) {
                    report.getVentilators().forEach(ventilator -> {
                        if (Boolean.TRUE.equals(ventilator.getSentForRework())) {
                            sentForReworkCount++;
                        }
                    });
                }
            });
            System.out.println("Sent for Rework = " + sentForReworkCount);

        } catch (Exception E) {
            E.printStackTrace();
        }
    }

    private static void writeReport(Report report, String empName) {
        JSONObject obj = new JSONObject(report);
        try (FileWriter outFile = new FileWriter("reports\\Reports" + "-" + empName + ".json", true);) {
            outFile.write(obj.toString(4));
            outFile.write(",\n");
        } catch (Exception E) {
            E.printStackTrace();
        }
    }

    private static void writeStats(String empName) {
        try (FileWriter outFile = new FileWriter("reports\\VentilatorStats" + "-" + empName + ".txt");) {
            outFile.write(ventilators.toString());
        } catch (Exception E) {
            E.printStackTrace();
        }
    }

    private static void writeConsolidatedReport(ConsolidatedReport consolidatedReport, String empName) {
        JSONObject obj = new JSONObject(consolidatedReport);
        try (FileWriter outFile = new FileWriter("reports\\ConsolidatedReport" + "-" + empName + ".json");) {
            outFile.write(obj.toString(4));
        } catch (Exception E) {
            E.printStackTrace();
        }
    }

}
