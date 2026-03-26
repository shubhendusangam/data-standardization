package com.datastd.ingestion.service;

import com.datastd.common.dto.ParseWarning;
import com.datastd.ingestion.dto.FileParseResult;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

@Service
public class FileParserService {

    private static final Logger log = LoggerFactory.getLogger(FileParserService.class);

    public FileParseResult parseExcel(InputStream inputStream) throws IOException {
        List<Map<String, String>> records = new ArrayList<>();
        List<ParseWarning> warnings = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);

            // Collect merged cell regions for warning detection
            Set<String> mergedCells = new HashSet<>();
            for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
                CellRangeAddress region = sheet.getMergedRegion(i);
                for (int r = region.getFirstRow(); r <= region.getLastRow(); r++) {
                    for (int c = region.getFirstColumn(); c <= region.getLastColumn(); c++) {
                        // Skip the top-left cell of the merge (it holds the value)
                        if (r != region.getFirstRow() || c != region.getFirstColumn()) {
                            mergedCells.add(r + ":" + c);
                        }
                    }
                }
            }

            Iterator<Row> rowIterator = sheet.iterator();

            if (!rowIterator.hasNext()) {
                return new FileParseResult(records, warnings);
            }

            // First row is header
            Row headerRow = rowIterator.next();
            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(getCellValueAsString(cell));
            }

            // Process data rows
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                int rowNum = row.getRowNum() + 1; // 1-based for user display

                try {
                    // Check for merged cells in this row and add warnings
                    for (int i = 0; i < headers.size(); i++) {
                        if (mergedCells.contains(row.getRowNum() + ":" + i)) {
                            String colName = i < headers.size() ? headers.get(i) : String.valueOf(i);
                            warnings.add(new ParseWarning(rowNum,
                                    "Merged cell at column '" + colName + "' — value may be missing",
                                    extractRawRowString(row, headers.size())));
                        }
                    }

                    // Check column count
                    int lastCellNum = row.getLastCellNum();
                    if (lastCellNum > 0 && lastCellNum < headers.size()) {
                        warnings.add(new ParseWarning(rowNum,
                                "Row has " + lastCellNum + " columns, expected " + headers.size(),
                                extractRawRowString(row, headers.size())));
                    }

                    Map<String, String> record = new LinkedHashMap<>();
                    boolean hasData = false;

                    for (int i = 0; i < headers.size(); i++) {
                        Cell cell = row.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);

                        // Check for formula errors
                        if (cell.getCellType() == CellType.ERROR) {
                            String colName = i < headers.size() ? headers.get(i) : String.valueOf(i);
                            warnings.add(new ParseWarning(rowNum,
                                    "Formula error in cell " + colName,
                                    FormulaError.forInt(cell.getErrorCellValue()).getString()));
                            record.put(headers.get(i), "");
                            continue;
                        }

                        String value = getCellValueAsString(cell);
                        record.put(headers.get(i), value);
                        if (!value.isEmpty()) {
                            hasData = true;
                        }
                    }

                    // Skip empty rows silently (not an error)
                    if (hasData) {
                        records.add(record);
                    }
                } catch (Exception e) {
                    warnings.add(new ParseWarning(rowNum, e.getMessage(),
                            extractRawRowString(row, headers.size())));
                }
            }
        }

        if (!warnings.isEmpty()) {
            log.warn("Excel parse completed with warnings: parsedRows={}, skippedRows={}",
                    records.size(), warnings.size());
        }

        return new FileParseResult(records, warnings);
    }

    public FileParseResult parseCsv(InputStream inputStream) throws IOException {
        List<Map<String, String>> records = new ArrayList<>();
        List<ParseWarning> warnings = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new InputStreamReader(inputStream))) {
            List<String[]> allRows = reader.readAll();

            if (allRows.isEmpty()) {
                return new FileParseResult(records, warnings);
            }

            String[] headers = allRows.get(0);

            for (int i = 1; i < allRows.size(); i++) {
                int rowIndex = i + 1; // 1-based, accounting for header at row 1
                try {
                    String[] row = allRows.get(i);

                    // Check for column count mismatch
                    if (row.length < headers.length) {
                        warnings.add(new ParseWarning(rowIndex,
                                "Row has " + row.length + " columns, expected " + headers.length,
                                Arrays.toString(row)));
                    }

                    Map<String, String> record = new LinkedHashMap<>();
                    boolean hasData = false;

                    for (int j = 0; j < headers.length; j++) {
                        String value = j < row.length ? row[j] : "";
                        record.put(headers[j].trim(), value);
                        if (!value.isEmpty()) {
                            hasData = true;
                        }
                    }

                    // Skip empty rows silently (not an error)
                    if (hasData) {
                        records.add(record);
                    }
                } catch (Exception e) {
                    warnings.add(new ParseWarning(rowIndex, e.getMessage(),
                            Arrays.toString(allRows.get(i))));
                }
            }
        } catch (CsvException e) {
            throw new IOException("Failed to parse CSV file: " + e.getMessage(), e);
        }

        if (!warnings.isEmpty()) {
            log.warn("CSV parse completed with warnings: parsedRows={}, skippedRows={}",
                    records.size(), warnings.size());
        }

        return new FileParseResult(records, warnings);
    }

    private String extractRawRowString(Row row, int expectedColumns) {
        StringBuilder sb = new StringBuilder();
        int cols = Math.max(row.getLastCellNum(), expectedColumns);
        for (int i = 0; i < cols; i++) {
            if (i > 0) sb.append(", ");
            Cell cell = row.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            sb.append(getCellValueAsString(cell));
        }
        return sb.toString();
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toString();
                }
                double numVal = cell.getNumericCellValue();
                if (numVal == Math.floor(numVal) && !Double.isInfinite(numVal)) {
                    yield String.valueOf((long) numVal);
                }
                yield String.valueOf(numVal);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield String.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    yield cell.getStringCellValue();
                }
            }
            case ERROR -> "";
            default -> "";
        };
    }
}

