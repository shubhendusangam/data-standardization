package com.datastd.ingestion.service;

import com.datastd.common.dto.ParseWarning;
import com.datastd.ingestion.dto.FileParseResult;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class FileParserServiceTest {

    private final FileParserService fileParserService = new FileParserService();

    // ─── CSV Tests ──────────────────────────────────────────────────

    @Test
    void parseCsv_allValidRows_shouldReturnNoWarnings() throws IOException {
        String csv = "name,age,city\nAlice,30,NYC\nBob,25,LA\n";
        InputStream is = new ByteArrayInputStream(csv.getBytes());

        FileParseResult result = fileParserService.parseCsv(is);

        assertThat(result.getParsedRowCount()).isEqualTo(2);
        assertThat(result.getSkippedRowCount()).isEqualTo(0);
        assertThat(result.getWarnings()).isEmpty();
        assertThat(result.isWarningsTruncated()).isFalse();
    }

    @Test
    void parseCsv_oneMalformedRow_shouldHaveOneWarning() throws IOException {
        // Row 2 has fewer columns than the header
        String csv = "name,age,city\nAlice,30,NYC\nBob\nCharlie,35,Chicago\n";
        InputStream is = new ByteArrayInputStream(csv.getBytes());

        FileParseResult result = fileParserService.parseCsv(is);

        // Bob row has data (1 column) so it's still parsed, but a warning is added
        assertThat(result.getSkippedRowCount()).isEqualTo(1);
        assertThat(result.getWarnings()).hasSize(1);

        ParseWarning warning = result.getWarnings().get(0);
        assertThat(warning.getRowIndex()).isEqualTo(3); // 1-based: header=1, Alice=2, Bob=3
        assertThat(warning.getReason()).contains("columns, expected");
    }

    @Test
    void parseCsv_150BadRows_shouldCapWarningsAt100() throws IOException {
        StringBuilder csv = new StringBuilder("col1,col2,col3\n");
        // 1 valid row
        csv.append("a,b,c\n");
        // 150 rows with fewer columns than header
        for (int i = 0; i < 150; i++) {
            csv.append("only_one_col\n");
        }
        InputStream is = new ByteArrayInputStream(csv.toString().getBytes());

        FileParseResult result = fileParserService.parseCsv(is);

        assertThat(result.getSkippedRowCount()).isEqualTo(150);
        assertThat(result.getWarnings()).hasSize(100);
        assertThat(result.isWarningsTruncated()).isTrue();
    }

    @Test
    void parseCsv_emptyFile_shouldReturnEmpty() throws IOException {
        String csv = "";
        InputStream is = new ByteArrayInputStream(csv.getBytes());

        FileParseResult result = fileParserService.parseCsv(is);

        assertThat(result.getParsedRowCount()).isEqualTo(0);
        assertThat(result.getSkippedRowCount()).isEqualTo(0);
        assertThat(result.getWarnings()).isEmpty();
    }

    // ─── Excel Tests ────────────────────────────────────────────────

    @Test
    void parseExcel_allValidRows_shouldReturnNoWarnings() throws IOException {
        byte[] excelBytes = createExcelBytes(new String[]{"name", "age"},
                new Object[][]{{"Alice", 30}, {"Bob", 25}});

        FileParseResult result = fileParserService.parseExcel(new ByteArrayInputStream(excelBytes));

        assertThat(result.getParsedRowCount()).isEqualTo(2);
        assertThat(result.getSkippedRowCount()).isEqualTo(0);
        assertThat(result.getWarnings()).isEmpty();
        assertThat(result.isWarningsTruncated()).isFalse();
    }

    @Test
    void parseExcel_withMergedCellRange_shouldWarnAboutMergedCell() throws IOException {
        byte[] excelBytes;
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet();

            // Header
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("name");
            headerRow.createCell(1).setCellValue("city");
            headerRow.createCell(2).setCellValue("status");

            // Data row 1
            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("Alice");
            row1.createCell(1).setCellValue("NYC");
            row1.createCell(2).setCellValue("active");

            // Data row 2 — merge cells B2:B3
            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("Bob");
            row2.createCell(1).setCellValue("LA");
            row2.createCell(2).setCellValue("inactive");

            Row row3 = sheet.createRow(3);
            row3.createCell(0).setCellValue("Charlie");
            // cell 1 in row 3 is part of a merged region
            row3.createCell(1).setCellValue("");
            row3.createCell(2).setCellValue("active");

            // Merge B3:B4 (rows 2 and 3, column 1)
            sheet.addMergedRegion(new CellRangeAddress(2, 3, 1, 1));

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            excelBytes = out.toByteArray();
        }

        FileParseResult result = fileParserService.parseExcel(new ByteArrayInputStream(excelBytes));

        assertThat(result.getParsedRowCount()).isEqualTo(3);
        // Should have at least one warning about the merged cell
        assertThat(result.getWarnings()).isNotEmpty();
        assertThat(result.getWarnings().stream()
                .anyMatch(w -> w.getReason().contains("Merged cell"))).isTrue();
    }

    @Test
    void parseExcel_emptyWorkbook_shouldReturnEmpty() throws IOException {
        byte[] excelBytes;
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            workbook.createSheet(); // empty sheet
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            excelBytes = out.toByteArray();
        }

        FileParseResult result = fileParserService.parseExcel(new ByteArrayInputStream(excelBytes));

        assertThat(result.getParsedRowCount()).isEqualTo(0);
        assertThat(result.getWarnings()).isEmpty();
    }

    // ─── Helper ─────────────────────────────────────────────────────

    private byte[] createExcelBytes(String[] headers, Object[][] data) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet();

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            for (int r = 0; r < data.length; r++) {
                Row row = sheet.createRow(r + 1);
                for (int c = 0; c < data[r].length; c++) {
                    Cell cell = row.createCell(c);
                    if (data[r][c] instanceof Number) {
                        cell.setCellValue(((Number) data[r][c]).doubleValue());
                    } else {
                        cell.setCellValue(String.valueOf(data[r][c]));
                    }
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }
}

