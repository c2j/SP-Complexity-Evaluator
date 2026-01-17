package com.sdchat.ce.sp.complexity.service;

import com.sdchat.ce.sp.complexity.model.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Slf4j
@Service
public class ExcelExportService {

    private static final String SUMMARY_SHEET = "Summary";
    private static final String HINTS_SHEET = "Hints";
    private static final String STATEMENTS_SHEET = "Statements";

    public byte[] exportAnalysisToExcel(HintAnalysisSummary analysis, String procedureName) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            createSummarySheet(workbook, analysis, procedureName);
            createHintsSheet(workbook, analysis);
            createStatementsSheet(workbook, analysis);

            workbook.write(outputStream);
            log.debug("Exported analysis to Excel: {} valid hints, {} invalid hints",
                    analysis.getValidCount(), analysis.getInvalidCount());

            return outputStream.toByteArray();
        }
    }

    private void createSummarySheet(Workbook workbook, HintAnalysisSummary analysis, String procedureName) {
        Sheet sheet = workbook.createSheet(SUMMARY_SHEET);

        Row titleRow = sheet.createRow(0);
        titleRow.createCell(0).setCellValue("Hint Validation Summary");
        CellStyle titleStyle = createBoldStyle(workbook);
        titleRow.getCell(0).setCellStyle(titleStyle);

        int row = 2;
        row = createDetailRow(sheet, row, "Procedure Name", procedureName != null ? procedureName : "N/A");
        row = createDetailRow(sheet, row, "Total Hints Found", String.valueOf(analysis.getTotalHints()));
        row = createDetailRow(sheet, row, "Valid Hints", String.valueOf(analysis.getValidCount()));
        row = createDetailRow(sheet, row, "Invalid Hints", String.valueOf(analysis.getInvalidCount()));

        row += 2;
        if (analysis.getCategoryBreakdown() != null && !analysis.getCategoryBreakdown().isEmpty()) {
            Row categoryHeader = sheet.createRow(row++);
            categoryHeader.createCell(0).setCellValue("By Category");
            categoryHeader.getCell(0).setCellStyle(titleStyle);

            for (var entry : analysis.getCategoryBreakdown().entrySet()) {
                row = createDetailRow(sheet, row, entry.getKey(), String.valueOf(entry.getValue()));
            }
        }

        row += 2;
        if (analysis.getErrorTypeBreakdown() != null && !analysis.getErrorTypeBreakdown().isEmpty()) {
            Row errorHeader = sheet.createRow(row++);
            errorHeader.createCell(0).setCellValue("By Error Type");
            errorHeader.getCell(0).setCellStyle(titleStyle);

            for (var entry : analysis.getErrorTypeBreakdown().entrySet()) {
                row = createDetailRow(sheet, row, entry.getKey(), String.valueOf(entry.getValue()));
            }
        }

        for (int i = 0; i <= 2; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createHintsSheet(Workbook workbook, HintAnalysisSummary analysis) {
        Sheet sheet = workbook.createSheet(HINTS_SHEET);

        String[] headers = {"Hint Text", "Status", "Category", "Line", "Offset", "Hint Name", "Parameters", "Error Type", "Error Message"};
        Row headerRow = sheet.createRow(0);
        CellStyle headerStyle = createHeaderStyle(workbook);

        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
            headerRow.getCell(i).setCellStyle(headerStyle);
        }

        int row = 1;
        CellStyle validStyle = createValidStyle(workbook);
        CellStyle invalidStyle = createInvalidStyle(workbook);

        for (HintValidationResult hint : analysis.getValidHints()) {
            row = fillHintRow(sheet, row++, hint, validStyle);
        }

        for (HintValidationResult hint : analysis.getInvalidHints()) {
            row = fillHintRow(sheet, row++, hint, invalidStyle);
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private int fillHintRow(Sheet sheet, int rowNum, HintValidationResult hint, CellStyle style) {
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(hint.getHintText());
        row.createCell(1).setCellValue(hint.getValidationStatus());
        row.createCell(2).setCellValue(hint.getCategory() != null ? hint.getCategory() : "");
        row.createCell(3).setCellValue(hint.getLineNumber() != null ? hint.getLineNumber() : 0);
        row.createCell(4).setCellValue(hint.getCharOffset() != null ? hint.getCharOffset() : 0);
        row.createCell(5).setCellValue(hint.getHintName() != null ? hint.getHintName() : "");
        row.createCell(6).setCellValue(hint.getParameters() != null ? hint.getParameters() : "");
        row.createCell(7).setCellValue(hint.getErrorType() != null ? hint.getErrorType() : "");
        row.createCell(8).setCellValue(hint.getErrorMessage() != null ? hint.getErrorMessage() : "");

        row.setRowStyle(style);
        return rowNum;
    }

    private void createStatementsSheet(Workbook workbook, HintAnalysisSummary analysis) {
        Sheet sheet = workbook.createSheet(STATEMENTS_SHEET);

        Row headerRow = sheet.createRow(0);
        String[] headers = {"Statement", "Hints Count", "Valid", "Invalid"};
        CellStyle headerStyle = createHeaderStyle(workbook);

        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
            headerRow.getCell(i).setCellStyle(headerStyle);
        }

        int row = 1;
        for (HintValidationResult hint : analysis.getValidHints()) {
            Row dataRow = sheet.createRow(row++);
            dataRow.createCell(0).setCellValue("Statement " + hint.getLineNumber());
            dataRow.createCell(1).setCellValue("1");
            dataRow.createCell(2).setCellValue("1");
            dataRow.createCell(3).setCellValue("0");
        }

        for (HintValidationResult hint : analysis.getInvalidHints()) {
            Row dataRow = sheet.createRow(row++);
            dataRow.createCell(0).setCellValue("Statement " + hint.getLineNumber());
            dataRow.createCell(1).setCellValue("1");
            dataRow.createCell(2).setCellValue("0");
            dataRow.createCell(3).setCellValue("1");
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private int createDetailRow(Sheet sheet, int rowNum, String label, String value) {
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value);
        return rowNum + 1;
    }

    private CellStyle createBoldStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle createValidStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle createInvalidStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.CORAL.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }
}
