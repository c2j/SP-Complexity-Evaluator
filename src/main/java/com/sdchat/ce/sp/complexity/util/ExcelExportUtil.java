package com.sdchat.ce.sp.complexity.util;

import com.sdchat.ce.sp.complexity.model.ComplexityMetrics;
import com.sdchat.ce.sp.complexity.model.ProcedureCallMetric;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Utility class for exporting complexity metrics to Excel format.
 */
public class ExcelExportUtil {

    /**
     * Convert a list of ComplexityMetrics to Excel format.
     *
     * @param metricsList The list of complexity metrics to convert
     * @return The Excel file as a byte array
     * @throws IOException If an error occurs during Excel generation
     */
    public static byte[] convertToExcel(List<ComplexityMetrics> metricsList)
        throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Complexity Metrics");

            // Create header row
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = createHeaderStyle(workbook);

            // Specify only the required columns in specified order
            String[] columns = {
                "Package Name",
                "Procedure Name",
                "Line Count",
                "Overall Score",
                "Loop Count",
                "Max Loop Nesting Level",
                "Custom Function Count",
                "Custom Function List",
                "High Weight Table Count",
                "High Weight Table List",
                "High Weight Procedure Count",
                "High Weight Procedure List",
                "Subquery Count",
                "Table Count",
                "Table List",
                "Has Exceptions",
                "Failed Statements",
                "Subtransaction Count",
                "Max Subtransaction Nesting Level",
                "Subtransaction Details",
                "Procedure Call Count",
                "Procedure Call Details",
            };

            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // Create data rows
            int rowNum = 1;
            for (ComplexityMetrics metrics : metricsList) {
                Row row = sheet.createRow(rowNum++);

                // Get package name from file name if available
                String packageName = "";
                if (metrics.getFileName() != null) {
                    String fileName = metrics.getFileName();
                    int lastSlash = fileName.lastIndexOf('/');
                    int lastDot = fileName.lastIndexOf('.');
                    if (lastSlash >= 0 && lastDot > lastSlash) {
                        packageName = fileName.substring(
                            lastSlash + 1,
                            lastDot
                        );
                    } else if (lastDot > 0) {
                        packageName = fileName.substring(0, lastDot);
                    } else {
                        packageName = fileName;
                    }
                }

                // Add cells in the specified order
                int colIndex = 0;
                row.createCell(colIndex++).setCellValue(packageName);
                row
                    .createCell(colIndex++)
                    .setCellValue(
                        metrics.getProcedureName() != null
                            ? metrics.getProcedureName()
                            : ""
                    );
                row.createCell(colIndex++).setCellValue(metrics.getLineCount());
                row
                    .createCell(colIndex++)
                    .setCellValue(metrics.getOverallScore());
                row.createCell(colIndex++).setCellValue(metrics.getLoopCount());
                row
                    .createCell(colIndex++)
                    .setCellValue(metrics.getMaxLoopNestingLevel());
                row
                    .createCell(colIndex++)
                    .setCellValue(metrics.getCustomFunctionCount());
                row
                    .createCell(colIndex++)
                    .setCellValue(
                        listToString(metrics.getCustomFunctionList())
                    );
                row
                    .createCell(colIndex++)
                    .setCellValue(metrics.getHighWeightTableCount());
                row
                    .createCell(colIndex++)
                    .setCellValue(
                        listToString(metrics.getHighWeightTableList())
                    );
                row
                    .createCell(colIndex++)
                    .setCellValue(metrics.getHighWeightProcedureCount());
                row
                    .createCell(colIndex++)
                    .setCellValue(
                        listToString(metrics.getHighWeightProcedureList())
                    );
                row
                    .createCell(colIndex++)
                    .setCellValue(metrics.getSubqueryCount());
                row
                    .createCell(colIndex++)
                    .setCellValue(metrics.getTableCount());
                row
                    .createCell(colIndex++)
                    .setCellValue(listToString(metrics.getTableList()));
                row
                    .createCell(colIndex++)
                    .setCellValue(metrics.isHasExceptions());
                row
                    .createCell(colIndex++)
                    .setCellValue(listToString(metrics.getFailedStatements()));

                row
                    .createCell(colIndex++)
                    .setCellValue(
                        metrics.getSubtransactionCount() != null
                            ? metrics.getSubtransactionCount()
                            : 0
                    );
                row
                    .createCell(colIndex++)
                    .setCellValue(
                        metrics.getMaxSubtransactionNestingLevel() != null
                            ? metrics.getMaxSubtransactionNestingLevel()
                            : 0
                    );
                row
                    .createCell(colIndex++)
                    .setCellValue(
                        metrics.getSubtransactionDetails() != null
                            ? metrics.getSubtransactionDetails()
                            : ""
                    );

                row
                    .createCell(colIndex++)
                    .setCellValue(metrics.getProcedureCallCount());
                row
                    .createCell(colIndex++)
                    .setCellValue(
                        formatProcedureCallDetails(
                            metrics.getProcedureCallDetails()
                        )
                    );
            }

            // Resize all columns to fit to content size
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Resize all columns to fit the content size
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Write to ByteArrayOutputStream
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * Create a cell style for the header row.
     *
     * @param workbook The workbook to create the style in
     * @return The header cell style
     */
    private static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);

        return headerStyle;
    }

    /**
     * Convert a list of strings to a comma-separated string.
     *
     * @param list The list to convert
     * @return The comma-separated string
     */
    private static String listToString(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        return String.join(", ", list);
    }

    /**
     * Format procedure call details for Excel export.
     * Format: "PROC_A:3, PROC_B:2(in loop)"
     *
     * @param details The list of procedure call details
     * @return The formatted string
     */
    private static String formatProcedureCallDetails(
        List<ProcedureCallMetric> details
    ) {
        if (details == null || details.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (ProcedureCallMetric detail : details) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb
                .append(detail.getProcedureName())
                .append(":")
                .append(detail.getCallCount());
            if (detail.isCalledInLoop()) {
                sb.append("(in loop)");
            }
        }
        return sb.toString();
    }
}
