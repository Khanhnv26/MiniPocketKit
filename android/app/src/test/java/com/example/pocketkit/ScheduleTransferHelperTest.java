package com.example.pocketkit;

import com.example.pocketkit.model.ScheduleSlot;
import com.example.pocketkit.transfer.ScheduleTransferHelper;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ScheduleTransferHelperTest {

    @Test
    public void xlsxExportAndImportRoundTrip_preservesAllFieldsAndZipParts() throws Exception {
        List<ScheduleSlot> source = Arrays.asList(
                ScheduleSlot.builder()
                        .dayOfWeek(2)
                        .startTime("07:30")
                        .endTime("09:45")
                        .subject("Cơ sở dữ liệu & Giải thuật <Nâng cao>")
                        .room("Phòng A204")
                        .lecturer("Giảng viên Nguyễn Minh Anh")
                        .note("Mang laptop \"cá nhân\"")
                        .build(),
                ScheduleSlot.builder()
                        .dayOfWeek(6)
                        .startTime("13:30")
                        .endTime("15:45")
                        .subject("Lập trình Di động")
                        .room("")
                        .lecturer("")
                        .note("")
                        .build()
        );

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ScheduleTransferHelper.exportSchedules(source, ScheduleTransferHelper.TransferFormat.XLSX, out);
        byte[] xlsxBytes = out.toByteArray();

        Set<String> zipNames = new HashSet<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(xlsxBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                zipNames.add(entry.getName());
                zis.closeEntry();
            }
        }
        assertTrue(zipNames.contains("[Content_Types].xml"));
        assertTrue(zipNames.contains("xl/workbook.xml"));
        assertTrue(zipNames.contains("xl/styles.xml"));
        assertTrue(zipNames.contains("xl/worksheets/sheet1.xml"));

        ScheduleTransferHelper.ImportResult result = ScheduleTransferHelper.parseSchedules(xlsxBytes, "unknown_name");
        assertEquals(ScheduleTransferHelper.TransferFormat.XLSX, result.detectedFormat);
        assertEquals(2, result.totalRows);
        assertEquals(0, result.issues.size());
        assertEquals(2, result.validSlots.size());

        ScheduleSlot first = result.validSlots.get(0);
        assertEquals(2, first.getDayOfWeek());
        assertEquals("07:30", first.getStartTime());
        assertEquals("09:45", first.getEndTime());
        assertEquals("Cơ sở dữ liệu & Giải thuật <Nâng cao>", first.getSubject());
        assertEquals("Phòng A204", first.getRoom());
        assertEquals("Giảng viên Nguyễn Minh Anh", first.getLecturer());
        assertEquals("Mang laptop \"cá nhân\"", first.getNote());

        ScheduleSlot second = result.validSlots.get(1);
        assertEquals(6, second.getDayOfWeek());
        assertEquals("13:30", second.getStartTime());
        assertEquals("15:45", second.getEndTime());
        assertEquals("Lập trình Di động", second.getSubject());
        assertEquals("", second.getRoom());
    }

    @Test
    public void xlsxSharedStringsNumericTimeAndReorderedHeaders_parsesCorrectly() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
            writeEntry(zos, "[Content_Types].xml",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                            + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"/>");
            writeEntry(zos, "xl/workbook.xml",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                            + "<workbook xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
                            + "<sheets><sheet name=\"Sheet1\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");
            writeEntry(zos, "xl/_rels/workbook.xml.rels",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                            + "<Relationship Id=\"rId1\" Target=\"worksheets/sheet1.xml\"/></Relationships>");
            writeEntry(zos, "xl/sharedStrings.xml",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                            + "<sst count=\"7\" uniqueCount=\"7\">"
                            + "<si><t>Môn học</t></si>"
                            + "<si><t>Thứ</t></si>"
                            + "<si><t>Bắt đầu</t></si>"
                            + "<si><t>Kết thúc</t></si>"
                            + "<si><t>Phòng</t></si>"
                            + "<si><t>Hệ điều hành</t></si>"
                            + "<si><t>Thứ 5</t></si>"
                            + "</sst>");
            writeEntry(zos, "xl/worksheets/sheet1.xml",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                            + "<worksheet><sheetData>"
                            + "<row r=\"1\">"
                            + "<c r=\"A1\" t=\"s\"><v>0</v></c>"
                            + "<c r=\"B1\" t=\"s\"><v>1</v></c>"
                            + "<c r=\"C1\" t=\"s\"><v>2</v></c>"
                            + "<c r=\"D1\" t=\"s\"><v>3</v></c>"
                            + "<c r=\"E1\" t=\"s\"><v>4</v></c>"
                            + "</row>"
                            + "<row r=\"2\">"
                            + "<c r=\"A2\" t=\"s\"><v>5</v></c>"
                            + "<c r=\"B2\" t=\"s\"><v>6</v></c>"
                            + "<c r=\"C2\"><v>0.3125</v></c>"
                            + "<c r=\"D2\"><v>0.40625</v></c>"
                            + "</row>"
                            + "</sheetData></worksheet>");
        }

        ScheduleTransferHelper.ImportResult result = ScheduleTransferHelper.parseSchedules(out.toByteArray(), "external.xlsx");
        assertEquals(ScheduleTransferHelper.TransferFormat.XLSX, result.detectedFormat);
        assertEquals(1, result.validSlots.size());
        ScheduleSlot slot = result.validSlots.get(0);
        assertEquals("Hệ điều hành", slot.getSubject());
        assertEquals(5, slot.getDayOfWeek());
        assertEquals("07:30", slot.getStartTime());
        assertEquals("09:45", slot.getEndTime());
        assertEquals("", slot.getRoom());
    }

    @Test
    public void csvRoundTripAndSemicolonImport_parsesCorrectly() throws Exception {
        List<ScheduleSlot> source = Arrays.asList(
                ScheduleSlot.builder()
                        .dayOfWeek(3)
                        .startTime("09:55")
                        .endTime("12:10")
                        .subject("Thiết kế UI, UX")
                        .room("Phòng B301")
                        .lecturer("Giảng viên Lê Thu Hà")
                        .note("Ghi chú, có dấu phẩy")
                        .build()
        );

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ScheduleTransferHelper.exportSchedules(source, ScheduleTransferHelper.TransferFormat.CSV, out);
        byte[] csvBytes = out.toByteArray();

        ScheduleTransferHelper.ImportResult result = ScheduleTransferHelper.parseSchedules(csvBytes, "tkb.csv");
        assertEquals(ScheduleTransferHelper.TransferFormat.CSV, result.detectedFormat);
        assertEquals(1, result.validSlots.size());
        assertEquals("Thiết kế UI, UX", result.validSlots.get(0).getSubject());
        assertEquals("Ghi chú, có dấu phẩy", result.validSlots.get(0).getNote());

        String semicolonCsv = "Thu;Gio bat dau;Gio ket thuc;Mon hoc;Phong\r\nT4;7h30;9h45;Mạng máy tính;A101";
        ScheduleTransferHelper.ImportResult semiResult = ScheduleTransferHelper.parseSchedules(
                semicolonCsv.getBytes(StandardCharsets.UTF_8), "semi.csv");
        assertEquals(1, semiResult.validSlots.size());
        assertEquals(4, semiResult.validSlots.get(0).getDayOfWeek());
        assertEquals("07:30", semiResult.validSlots.get(0).getStartTime());
        assertEquals("09:45", semiResult.validSlots.get(0).getEndTime());
        assertEquals("Mạng máy tính", semiResult.validSlots.get(0).getSubject());
        assertEquals("A101", semiResult.validSlots.get(0).getRoom());
    }

    @Test
    public void jsonRoundTripAndSnakeCaseArray_parsesCorrectly() throws Exception {
        List<ScheduleSlot> source = Arrays.asList(
                ScheduleSlot.builder()
                        .dayOfWeek(7)
                        .startTime("08:00")
                        .endTime("10:00")
                        .subject("Ôn tập tổng hợp")
                        .room("Thư viện")
                        .lecturer("Nhóm 01")
                        .note("")
                        .build()
        );

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ScheduleTransferHelper.exportSchedules(source, ScheduleTransferHelper.TransferFormat.JSON, out);

        ScheduleTransferHelper.ImportResult result = ScheduleTransferHelper.parseSchedules(out.toByteArray(), "backup.json");
        assertEquals(ScheduleTransferHelper.TransferFormat.JSON, result.detectedFormat);
        assertEquals(1, result.validSlots.size());
        assertEquals("Ôn tập tổng hợp", result.validSlots.get(0).getSubject());

        String rawArray = "[{\"day_of_week\":\"Thứ Tư\",\"start_time\":\"13:30\",\"end_time\":\"15:00\",\"subject\":\"Java Web\"}]";
        ScheduleTransferHelper.ImportResult snakeResult = ScheduleTransferHelper.parseSchedules(
                rawArray.getBytes(StandardCharsets.UTF_8), null);
        assertEquals(ScheduleTransferHelper.TransferFormat.JSON, snakeResult.detectedFormat);
        assertEquals(1, snakeResult.validSlots.size());
        assertEquals(4, snakeResult.validSlots.get(0).getDayOfWeek());
        assertEquals("Java Web", snakeResult.validSlots.get(0).getSubject());
    }

    @Test
    public void invalidRowsAndLegacyXls_handledCleanly() throws Exception {
        String mixedCsv = "Thu,Bat dau,Ket thuc,Mon hoc\n"
                + "8,07:30,09:45,Sai thứ\n"
                + "2,25:10,09:45,Sai giờ\n"
                + "2,10:00,09:45,Sai khoảng giờ\n"
                + "2,07:30,09:45,\n"
                + "T5,07:30,09:45,Hợp lệ\n";

        ScheduleTransferHelper.ImportResult result = ScheduleTransferHelper.parseSchedules(
                mixedCsv.getBytes(StandardCharsets.UTF_8), "mixed.csv");
        assertEquals(5, result.totalRows);
        assertEquals(1, result.validSlots.size());
        assertEquals(4, result.issues.size());
        assertEquals(ScheduleTransferHelper.IssueType.INVALID_DAY, result.issues.get(0).issueType);
        assertEquals(ScheduleTransferHelper.IssueType.INVALID_TIME, result.issues.get(1).issueType);
        assertEquals(ScheduleTransferHelper.IssueType.INVALID_RANGE, result.issues.get(2).issueType);
        assertEquals(ScheduleTransferHelper.IssueType.EMPTY_SUBJECT, result.issues.get(3).issueType);

        byte[] legacyXlsHeader = new byte[]{(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1};
        try {
            ScheduleTransferHelper.parseSchedules(legacyXlsHeader, "old.xls");
            fail("Expected LegacyXlsException");
        } catch (ScheduleTransferHelper.LegacyXlsException expected) {
            assertTrue(expected.getMessage().contains(".xls"));
        }
    }

    @Test
    public void dayAndTimeHelpers_parseExpectedFormats() {
        assertEquals(2, ScheduleTransferHelper.parseDayOfWeek("Thứ 2"));
        assertEquals(3, ScheduleTransferHelper.parseDayOfWeek("T3"));
        assertEquals(4, ScheduleTransferHelper.parseDayOfWeek("Thứ Tư"));
        assertEquals(5, ScheduleTransferHelper.parseDayOfWeek("Thursday"));
        assertEquals(6, ScheduleTransferHelper.parseDayOfWeek("6"));
        assertEquals(7, ScheduleTransferHelper.parseDayOfWeek("Thứ Bảy"));
        assertEquals(-1, ScheduleTransferHelper.parseDayOfWeek("CN"));
        assertEquals(-1, ScheduleTransferHelper.parseDayOfWeek("8"));

        assertEquals(450, ScheduleSlot.toMinutes("07:30"));
        assertEquals(-1, ScheduleSlot.toMinutes("24:00"));
    }

    private void writeEntry(ZipOutputStream zos, String name, String xml) throws Exception {
        zos.putNextEntry(new ZipEntry(name));
        zos.write(xml.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }
}
