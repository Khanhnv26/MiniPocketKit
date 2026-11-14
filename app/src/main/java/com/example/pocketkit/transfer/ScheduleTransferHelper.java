package com.example.pocketkit.transfer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.pocketkit.model.ScheduleSlot;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public final class ScheduleTransferHelper {

    public enum TransferFormat {
        XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
        CSV("csv", "text/csv"),
        JSON("json", "application/json");

        public final String extension;
        public final String mimeType;

        TransferFormat(String extension, String mimeType) {
            this.extension = extension;
            this.mimeType = mimeType;
        }
    }

    public enum IssueType {
        INVALID_DAY,
        INVALID_TIME,
        INVALID_RANGE,
        EMPTY_SUBJECT
    }

    public static final class RowIssue {
        public final int rowNumber;
        public final IssueType issueType;

        public RowIssue(int rowNumber, @NonNull IssueType issueType) {
            this.rowNumber = rowNumber;
            this.issueType = issueType;
        }
    }

    public static final class ImportResult {
        public final TransferFormat detectedFormat;
        public final int totalRows;
        public final List<ScheduleSlot> validSlots;
        public final List<RowIssue> issues;

        public ImportResult(
                @NonNull TransferFormat detectedFormat,
                int totalRows,
                @NonNull List<ScheduleSlot> validSlots,
                @NonNull List<RowIssue> issues
        ) {
            this.detectedFormat = detectedFormat;
            this.totalRows = totalRows;
            this.validSlots = validSlots;
            this.issues = issues;
        }
    }

    public static final class LegacyXlsException extends IOException {
        public LegacyXlsException() {
            super("Legacy .xls binary format is not supported");
        }
    }

    private static final String[] XLSX_HEADERS = new String[]{
            "Thứ (2-7)",
            "Giờ bắt đầu",
            "Giờ kết thúc",
            "Môn học",
            "Phòng học",
            "Giảng viên",
            "Ghi chú"
    };

    private static final int[] XLSX_COL_WIDTHS = new int[]{11, 14, 14, 30, 18, 24, 28};

    private static final Pattern NUMBER_PATTERN = Pattern.compile("^-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?$");
    private static final Pattern ANY_NUMBER_TOKEN_PATTERN = Pattern.compile("\\d+");
    private static final Pattern CELL_COL_PATTERN = Pattern.compile("^([A-Za-z]+)");

    private static final int COL_DAY = 0;
    private static final int COL_START = 1;
    private static final int COL_END = 2;
    private static final int COL_SUBJECT = 3;
    private static final int COL_ROOM = 4;
    private static final int COL_LECTURER = 5;
    private static final int COL_NOTE = 6;
    private static final int COL_TIME_RANGE = 7;

    private static final class ScheduleJsonPayload {
        int version;
        String exportedAt;
        List<ScheduleJsonItem> schedules;
    }

    private static final class ScheduleJsonItem {
        int dayOfWeek;
        String startTime;
        String endTime;
        String subject;
        String room;
        String lecturer;
        String note;
    }

    private ScheduleTransferHelper() {
    }

    public static void exportSchedules(
            @NonNull List<ScheduleSlot> slots,
            @NonNull TransferFormat format,
            @NonNull OutputStream outputStream
    ) throws IOException {
        switch (format) {
            case XLSX:
                writeXlsx(slots, outputStream);
                break;
            case CSV:
                writeCsv(slots, outputStream);
                break;
            case JSON:
                writeJson(slots, outputStream);
                break;
        }
    }

    public static List<ScheduleSlot> buildSampleSlotsFromSeedRows(@NonNull String[] seedRows, int limit) {
        List<ScheduleSlot> list = new ArrayList<>();
        int count = 0;
        for (String row : seedRows) {
            if (count >= limit) {
                break;
            }
            String[] parts = row.split("\\|", -1);
            if (parts.length < 7) {
                continue;
            }
            try {
                int day = Integer.parseInt(parts[0].trim());
                list.add(ScheduleSlot.builder()
                        .dayOfWeek(day)
                        .startTime(parts[1].trim())
                        .endTime(parts[2].trim())
                        .subject(parts[3].trim())
                        .room(parts[4].trim())
                        .lecturer(parts[5].trim())
                        .note(parts[6].trim())
                        .build());
                count++;
            } catch (NumberFormatException ignored) {
            }
        }
        return list;
    }

    public static ImportResult parseSchedules(
            @NonNull InputStream inputStream,
            @Nullable String fileName
    ) throws IOException {
        byte[] bytes = readAllBytes(inputStream);
        return parseSchedules(bytes, fileName);
    }

    public static ImportResult parseSchedules(
            @NonNull byte[] bytes,
            @Nullable String fileName
    ) throws IOException {
        TransferFormat format = detectFormat(bytes, fileName);
        switch (format) {
            case XLSX:
                return parseXlsx(bytes);
            case JSON:
                return parseJson(bytes);
            case CSV:
            default:
                return parseCsv(bytes);
        }
    }

    public static TransferFormat detectFormat(@NonNull byte[] bytes, @Nullable String fileName) throws IOException {
        if (bytes.length >= 8
                && (bytes[0] & 0xFF) == 0xD0
                && (bytes[1] & 0xFF) == 0xCF
                && (bytes[2] & 0xFF) == 0x11
                && (bytes[3] & 0xFF) == 0xE0) {
            throw new LegacyXlsException();
        }
        if (bytes.length >= 4
                && bytes[0] == 0x50
                && bytes[1] == 0x4B
                && bytes[2] == 0x03
                && bytes[3] == 0x04) {
            return TransferFormat.XLSX;
        }
        String textPreview = stripBom(new String(bytes, StandardCharsets.UTF_8)).trim();
        if (textPreview.startsWith("{") || textPreview.startsWith("[")) {
            return TransferFormat.JSON;
        }
        if (fileName != null) {
            String lower = fileName.toLowerCase(Locale.ROOT).trim();
            if (lower.endsWith(".xlsx")) {
                return TransferFormat.XLSX;
            }
            if (lower.endsWith(".xls")) {
                throw new LegacyXlsException();
            }
            if (lower.endsWith(".json")) {
                return TransferFormat.JSON;
            }
        }
        return TransferFormat.CSV;
    }

    private static void writeXlsx(@NonNull List<ScheduleSlot> slots, @NonNull OutputStream outputStream) throws IOException {
        ZipOutputStream zos = new ZipOutputStream(outputStream, StandardCharsets.UTF_8);

        writeZipEntry(zos, "[Content_Types].xml", buildContentTypesXml());
        writeZipEntry(zos, "_rels/.rels", buildRootRelsXml());
        writeZipEntry(zos, "xl/workbook.xml", buildWorkbookXml());
        writeZipEntry(zos, "xl/_rels/workbook.xml.rels", buildWorkbookRelsXml());
        writeZipEntry(zos, "xl/styles.xml", buildStylesXml());
        writeZipEntry(zos, "xl/worksheets/sheet1.xml", buildSheetXml(slots));

        zos.finish();
        zos.flush();
    }

    private static void writeZipEntry(ZipOutputStream zos, String entryName, String content) throws IOException {
        ZipEntry entry = new ZipEntry(entryName);
        zos.putNextEntry(entry);
        zos.write(content.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    private static String buildContentTypesXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
                + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
                + "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>"
                + "</Types>";
    }

    private static String buildRootRelsXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
                + "</Relationships>";
    }

    private static String buildWorkbookXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" "
                + "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
                + "<sheets>"
                + "<sheet name=\"ThoiKhoaBieu\" sheetId=\"1\" r:id=\"rId1\"/>"
                + "</sheets>"
                + "</workbook>";
    }

    private static String buildWorkbookRelsXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
                + "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>"
                + "</Relationships>";
    }

    private static String buildStylesXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
                + "<fonts count=\"2\">"
                + "<font><sz val=\"11\"/><color rgb=\"FF0F172A\"/><name val=\"Calibri\"/></font>"
                + "<font><b/><sz val=\"11\"/><color rgb=\"FF0F172A\"/><name val=\"Calibri\"/></font>"
                + "</fonts>"
                + "<fills count=\"3\">"
                + "<fill><patternFill patternType=\"none\"/></fill>"
                + "<fill><patternFill patternType=\"gray125\"/></fill>"
                + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFE8F1FB\"/><bgColor indexed=\"64\"/></patternFill></fill>"
                + "</fills>"
                + "<borders count=\"2\">"
                + "<border><left/><right/><top/><bottom/><diagonal/></border>"
                + "<border>"
                + "<left style=\"thin\"><color rgb=\"FFD9E5F2\"/></left>"
                + "<right style=\"thin\"><color rgb=\"FFD9E5F2\"/></right>"
                + "<top style=\"thin\"><color rgb=\"FFD9E5F2\"/></top>"
                + "<bottom style=\"thin\"><color rgb=\"FF94A3B8\"/></bottom>"
                + "<diagonal/>"
                + "</border>"
                + "</borders>"
                + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
                + "<cellXfs count=\"2\">"
                + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyAlignment=\"1\"><alignment vertical=\"center\" wrapText=\"1\"/></xf>"
                + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"center\" vertical=\"center\"/></xf>"
                + "</cellXfs>"
                + "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>"
                + "</styleSheet>";
    }

    private static String buildSheetXml(@NonNull List<ScheduleSlot> slots) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        sb.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">");
        sb.append("<cols>");
        for (int i = 0; i < XLSX_COL_WIDTHS.length; i++) {
            int colNum = i + 1;
            sb.append("<col min=\"").append(colNum)
                    .append("\" max=\"").append(colNum)
                    .append("\" width=\"").append(XLSX_COL_WIDTHS[i])
                    .append("\" customWidth=\"1\"/>");
        }
        sb.append("</cols>");
        sb.append("<sheetData>");

        sb.append("<row r=\"1\" ht=\"24\" customHeight=\"1\">");
        for (int col = 0; col < XLSX_HEADERS.length; col++) {
            appendInlineStringCell(sb, colIndexToLetter(col) + "1", 1, XLSX_HEADERS[col]);
        }
        sb.append("</row>");

        for (int i = 0; i < slots.size(); i++) {
            ScheduleSlot slot = slots.get(i);
            int rowNum = i + 2;
            sb.append("<row r=\"").append(rowNum).append("\">");
            appendNumericCell(sb, "A" + rowNum, 0, slot.getDayOfWeek());
            appendInlineStringCell(sb, "B" + rowNum, 0, safeString(slot.getStartTime()));
            appendInlineStringCell(sb, "C" + rowNum, 0, safeString(slot.getEndTime()));
            appendInlineStringCell(sb, "D" + rowNum, 0, safeString(slot.getSubject()));
            appendInlineStringCell(sb, "E" + rowNum, 0, safeString(slot.getRoom()));
            appendInlineStringCell(sb, "F" + rowNum, 0, safeString(slot.getLecturer()));
            appendInlineStringCell(sb, "G" + rowNum, 0, safeString(slot.getNote()));
            sb.append("</row>");
        }

        sb.append("</sheetData>");
        sb.append("</worksheet>");
        return sb.toString();
    }

    private static void appendNumericCell(StringBuilder sb, String ref, int styleId, int value) {
        sb.append("<c r=\"").append(ref).append("\" s=\"").append(styleId).append("\">")
                .append("<v>").append(value).append("</v></c>");
    }

    private static void appendInlineStringCell(StringBuilder sb, String ref, int styleId, String value) {
        sb.append("<c r=\"").append(ref).append("\" s=\"").append(styleId).append("\" t=\"inlineStr\">")
                .append("<is><t xml:space=\"preserve\">")
                .append(escapeXml(value))
                .append("</t></is></c>");
    }

    private static String escapeXml(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            if (ch == '&') {
                sb.append("&amp;");
            } else if (ch == '<') {
                sb.append("&lt;");
            } else if (ch == '>') {
                sb.append("&gt;");
            } else if (ch == '"') {
                sb.append("&quot;");
            } else if (ch == '\'') {
                sb.append("&apos;");
            } else if (ch >= 0x20 || ch == '\t' || ch == '\n' || ch == '\r') {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    private static String colIndexToLetter(int index) {
        StringBuilder sb = new StringBuilder();
        int current = index;
        while (current >= 0) {
            sb.insert(0, (char) ('A' + (current % 26)));
            current = (current / 26) - 1;
        }
        return sb.toString();
    }

    private static void writeCsv(@NonNull List<ScheduleSlot> slots, @NonNull OutputStream outputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        buffer.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < XLSX_HEADERS.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escapeCsv(XLSX_HEADERS[i]));
        }
        sb.append("\r\n");

        for (ScheduleSlot slot : slots) {
            sb.append(escapeCsv(String.valueOf(slot.getDayOfWeek()))).append(',');
            sb.append(escapeCsv(safeString(slot.getStartTime()))).append(',');
            sb.append(escapeCsv(safeString(slot.getEndTime()))).append(',');
            sb.append(escapeCsv(safeString(slot.getSubject()))).append(',');
            sb.append(escapeCsv(safeString(slot.getRoom()))).append(',');
            sb.append(escapeCsv(safeString(slot.getLecturer()))).append(',');
            sb.append(escapeCsv(safeString(slot.getNote()))).append("\r\n");
        }

        buffer.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        outputStream.write(buffer.toByteArray());
        outputStream.flush();
    }

    private static String escapeCsv(String value) {
        String cleaned = value == null ? "" : value;
        boolean needsQuotes = cleaned.indexOf(',') >= 0
                || cleaned.indexOf(';') >= 0
                || cleaned.indexOf('"') >= 0
                || cleaned.indexOf('\n') >= 0
                || cleaned.indexOf('\r') >= 0;
        if (!needsQuotes) {
            return cleaned;
        }
        return "\"" + cleaned.replace("\"", "\"\"") + "\"";
    }

    private static void writeJson(@NonNull List<ScheduleSlot> slots, @NonNull OutputStream outputStream) throws IOException {
        ScheduleJsonPayload payload = new ScheduleJsonPayload();
        payload.version = 1;
        payload.exportedAt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(new Date());
        payload.schedules = new ArrayList<>();
        for (ScheduleSlot slot : slots) {
            ScheduleJsonItem item = new ScheduleJsonItem();
            item.dayOfWeek = slot.getDayOfWeek();
            item.startTime = safeString(slot.getStartTime());
            item.endTime = safeString(slot.getEndTime());
            item.subject = safeString(slot.getSubject());
            item.room = safeString(slot.getRoom());
            item.lecturer = safeString(slot.getLecturer());
            item.note = safeString(slot.getNote());
            payload.schedules.add(item);
        }
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        outputStream.write(gson.toJson(payload).getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }

    private static ImportResult parseXlsx(@NonNull byte[] bytes) throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    entries.put(normalizeZipPath(entry.getName()), readAllBytes(zis));
                }
                zis.closeEntry();
            }
        }

        List<String> sharedStrings = new ArrayList<>();
        byte[] sharedBytes = entries.get("xl/sharedstrings.xml");
        if (sharedBytes != null) {
            sharedStrings = parseSharedStringsXml(sharedBytes);
        }

        byte[] sheetBytes = resolveFirstWorksheetBytes(entries);
        if (sheetBytes == null) {
            throw new IOException("Worksheet XML not found in xlsx package");
        }

        List<List<String>> rows = parseWorksheetRows(sheetBytes, sharedStrings);
        return parseTabularRows(TransferFormat.XLSX, rows);
    }

    @Nullable
    private static byte[] resolveFirstWorksheetBytes(Map<String, byte[]> entries) {
        byte[] workbookBytes = entries.get("xl/workbook.xml");
        byte[] relsBytes = entries.get("xl/_rels/workbook.xml.rels");
        if (workbookBytes != null && relsBytes != null) {
            try {
                Map<String, String> relTargets = parseRelationships(relsBytes);
                Document doc = parseXmlDocument(workbookBytes);
                List<Element> sheetNodes = collectElementsByLocalName(doc.getDocumentElement(), "sheet");
                if (!sheetNodes.isEmpty()) {
                    Element firstSheet = sheetNodes.get(0);
                    String rId = firstSheet.getAttribute("r:id");
                    if (rId.isEmpty()) {
                        rId = firstSheet.getAttribute("id");
                    }
                    String target = relTargets.get(rId);
                    if (target != null) {
                        String normalized = normalizeSheetTarget(target);
                        if (entries.containsKey(normalized)) {
                            return entries.get(normalized);
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if (entries.containsKey("xl/worksheets/sheet1.xml")) {
            return entries.get("xl/worksheets/sheet1.xml");
        }
        for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
            if (entry.getKey().startsWith("xl/worksheets/sheet") && entry.getKey().endsWith(".xml")) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static String normalizeZipPath(String path) {
        String p = path.replace('\\', '/').toLowerCase(Locale.ROOT);
        while (p.startsWith("/")) {
            p = p.substring(1);
        }
        return p;
    }

    private static String normalizeSheetTarget(String target) {
        String clean = target.replace('\\', '/');
        while (clean.startsWith("/")) {
            clean = clean.substring(1);
        }
        if (!clean.toLowerCase(Locale.ROOT).startsWith("xl/")) {
            clean = "xl/" + clean;
        }
        return clean.toLowerCase(Locale.ROOT);
    }

    private static Map<String, String> parseRelationships(byte[] relsBytes) throws IOException {
        Map<String, String> map = new HashMap<>();
        try {
            Document doc = parseXmlDocument(relsBytes);
            List<Element> nodes = collectElementsByLocalName(doc.getDocumentElement(), "Relationship");
            for (Element el : nodes) {
                String id = el.getAttribute("Id");
                String target = el.getAttribute("Target");
                if (!id.isEmpty() && !target.isEmpty()) {
                    map.put(id, target);
                }
            }
        } catch (Exception e) {
            throw new IOException("Failed to parse workbook relationships", e);
        }
        return map;
    }

    private static List<String> parseSharedStringsXml(byte[] sharedBytes) throws IOException {
        List<String> strings = new ArrayList<>();
        try {
            Document doc = parseXmlDocument(sharedBytes);
            List<Element> siList = collectElementsByLocalName(doc.getDocumentElement(), "si");
            for (Element si : siList) {
                StringBuilder textBuilder = new StringBuilder();
                collectTextNodesByTag(si, "t", textBuilder);
                strings.add(textBuilder.toString());
            }
        } catch (Exception e) {
            throw new IOException("Failed to parse sharedStrings.xml", e);
        }
        return strings;
    }

    private static List<List<String>> parseWorksheetRows(byte[] sheetBytes, List<String> sharedStrings) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        try {
            Document doc = parseXmlDocument(sheetBytes);
            List<Element> rowNodes = collectElementsByLocalName(doc.getDocumentElement(), "row");
            for (Element rowNode : rowNodes) {
                List<String> rowValues = new ArrayList<>();
                NodeList children = rowNode.getChildNodes();
                int nextSequentialCol = 0;
                for (int c = 0; c < children.getLength(); c++) {
                    Node child = children.item(c);
                    if (!(child instanceof Element) || !"c".equals(localName(child))) {
                        continue;
                    }
                    Element cellEl = (Element) child;
                    String ref = cellEl.getAttribute("r");
                    int colIndex = ref.isEmpty() ? nextSequentialCol : cellRefToColIndex(ref);
                    if (colIndex < 0) {
                        colIndex = nextSequentialCol;
                    }
                    nextSequentialCol = colIndex + 1;

                    while (rowValues.size() <= colIndex) {
                        rowValues.add("");
                    }
                    rowValues.set(colIndex, readCellValue(cellEl, sharedStrings));
                }
                rows.add(rowValues);
            }
        } catch (Exception e) {
            throw new IOException("Failed to parse worksheet XML", e);
        }
        return rows;
    }

    private static String readCellValue(Element cellEl, List<String> sharedStrings) {
        String type = cellEl.getAttribute("t");
        if ("inlineStr".equals(type)) {
            Node isNode = findDirectChild(cellEl, "is");
            if (isNode != null) {
                StringBuilder sb = new StringBuilder();
                collectTextNodesByTag(isNode, "t", sb);
                return sb.toString();
            }
            return "";
        }
        Node valueNode = findDirectChild(cellEl, "v");
        String raw = valueNode != null ? valueNode.getTextContent() : "";
        if ("s".equals(type)) {
            try {
                int idx = Integer.parseInt(raw.trim());
                if (idx >= 0 && idx < sharedStrings.size()) {
                    return sharedStrings.get(idx);
                }
            } catch (NumberFormatException ignored) {
            }
            return "";
        }
        return raw != null ? raw : "";
    }

    private static Document parseXmlDocument(byte[] bytes) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setExpandEntityReferences(false);
        try {
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        } catch (ParserConfigurationException ignored) {
        }
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new ByteArrayInputStream(bytes));
    }

    @Nullable
    private static Node findDirectChild(Element parent, String tagName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element && tagName.equals(localName(node))) {
                return node;
            }
        }
        return null;
    }

    private static void collectTextNodesByTag(Node root, String targetTag, StringBuilder out) {
        if (root instanceof Element && targetTag.equals(localName(root))) {
            out.append(root.getTextContent());
            return;
        }
        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            collectTextNodesByTag(children.item(i), targetTag, out);
        }
    }

    private static String localName(Node node) {
        String name = node.getNodeName();
        int colon = name.indexOf(':');
        return colon >= 0 ? name.substring(colon + 1) : name;
    }

    private static List<Element> collectElementsByLocalName(@Nullable Element root, String targetName) {
        List<Element> result = new ArrayList<>();
        if (root == null) {
            return result;
        }
        if (targetName.equals(localName(root))) {
            result.add(root);
        }
        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element) {
                result.addAll(collectElementsByLocalName((Element) child, targetName));
            }
        }
        return result;
    }

    private static int cellRefToColIndex(String ref) {
        Matcher matcher = CELL_COL_PATTERN.matcher(ref.trim());
        if (!matcher.find()) {
            return -1;
        }
        String letters = matcher.group(1).toUpperCase(Locale.US);
        int col = 0;
        for (int i = 0; i < letters.length(); i++) {
            col = col * 26 + (letters.charAt(i) - 'A' + 1);
        }
        return col - 1;
    }

    private static ImportResult parseCsv(@NonNull byte[] bytes) {
        String content = stripBom(new String(bytes, StandardCharsets.UTF_8));
        char delimiter = detectCsvDelimiter(content);
        List<List<String>> rows = parseCsvRows(content, delimiter);
        return parseTabularRows(TransferFormat.CSV, rows);
    }

    private static char detectCsvDelimiter(String content) {
        int commaCount = 0;
        int semicolonCount = 0;
        boolean inQuotes = false;
        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);
            if (ch == '"') {
                inQuotes = !inQuotes;
            } else if (!inQuotes) {
                if (ch == ',') {
                    commaCount++;
                } else if (ch == ';') {
                    semicolonCount++;
                } else if (ch == '\n' || ch == '\r') {
                    if (commaCount > 0 || semicolonCount > 0) {
                        break;
                    }
                }
            }
        }
        return semicolonCount > commaCount ? ';' : ',';
    }

    private static List<List<String>> parseCsvRows(String content, char delimiter) {
        List<List<String>> rows = new ArrayList<>();
        List<String> currentRow = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < content.length() && content.charAt(i + 1) == '"') {
                        cell.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cell.append(ch);
                }
            } else {
                if (ch == '"') {
                    inQuotes = true;
                } else if (ch == delimiter) {
                    currentRow.add(cell.toString());
                    cell.setLength(0);
                } else if (ch == '\r' || ch == '\n') {
                    if (ch == '\r' && i + 1 < content.length() && content.charAt(i + 1) == '\n') {
                        i++;
                    }
                    currentRow.add(cell.toString());
                    cell.setLength(0);
                    rows.add(currentRow);
                    currentRow = new ArrayList<>();
                } else {
                    cell.append(ch);
                }
            }
        }
        if (cell.length() > 0 || !currentRow.isEmpty()) {
            currentRow.add(cell.toString());
            rows.add(currentRow);
        }
        return rows;
    }

    private static ImportResult parseJson(@NonNull byte[] bytes) throws IOException {
        String text = stripBom(new String(bytes, StandardCharsets.UTF_8)).trim();
        if (text.isEmpty()) {
            return new ImportResult(TransferFormat.JSON, 0, new ArrayList<>(), new ArrayList<>());
        }
        JsonElement root;
        try {
            root = JsonParser.parseString(text);
        } catch (Exception e) {
            throw new IOException("Invalid JSON schedule content", e);
        }

        JsonArray array = null;
        if (root.isJsonArray()) {
            array = root.getAsJsonArray();
        } else if (root.isJsonObject()) {
            JsonObject obj = root.getAsJsonObject();
            if (obj.has("schedules") && obj.get("schedules").isJsonArray()) {
                array = obj.getAsJsonArray("schedules");
            } else if (obj.has("items") && obj.get("items").isJsonArray()) {
                array = obj.getAsJsonArray("items");
            } else if (obj.has("data") && obj.get("data").isJsonArray()) {
                array = obj.getAsJsonArray("data");
            }
        }
        if (array == null) {
            throw new IOException("JSON does not contain schedule list");
        }

        List<ScheduleSlot> valid = new ArrayList<>();
        List<RowIssue> issues = new ArrayList<>();
        int rowNumber = 0;

        for (JsonElement element : array) {
            rowNumber++;
            if (!element.isJsonObject()) {
                issues.add(new RowIssue(rowNumber, IssueType.EMPTY_SUBJECT));
                continue;
            }
            JsonObject obj = element.getAsJsonObject();
            String rawDay = getJsonField(obj, "dayOfWeek", "day_of_week", "day", "thu");
            String rawStart = getJsonField(obj, "startTime", "start_time", "start", "gioBatDau");
            String rawEnd = getJsonField(obj, "endTime", "end_time", "end", "gioKetThuc");
            String rawSubject = getJsonField(obj, "subject", "monHoc", "course", "title");
            String rawRoom = getJsonField(obj, "room", "phongHoc", "location");
            String rawLecturer = getJsonField(obj, "lecturer", "giangVien", "teacher");
            String rawNote = getJsonField(obj, "note", "ghiChu", "notes");

            validateAndCollect(rowNumber, rawDay, rawStart, rawEnd, rawSubject, rawRoom, rawLecturer, rawNote, valid, issues);
        }
        return new ImportResult(TransferFormat.JSON, rowNumber, valid, issues);
    }

    private static String getJsonField(JsonObject obj, String... keys) {
        for (String key : keys) {
            if (obj.has(key) && !obj.get(key).isJsonNull()) {
                JsonElement el = obj.get(key);
                if (el.isJsonPrimitive()) {
                    return el.getAsString();
                }
            }
        }
        return "";
    }

    private static ImportResult parseTabularRows(TransferFormat format, List<List<String>> rows) {
        int headerIndex = -1;
        for (int i = 0; i < rows.size(); i++) {
            if (!isRowEmpty(rows.get(i))) {
                headerIndex = i;
                break;
            }
        }
        if (headerIndex < 0) {
            return new ImportResult(format, 0, new ArrayList<>(), new ArrayList<>());
        }

        Map<Integer, Integer> columnMapping = detectHeaderMapping(rows.get(headerIndex));
        boolean hasHeader = columnMapping != null;
        if (!hasHeader) {
            columnMapping = defaultColumnMapping();
        }

        List<ScheduleSlot> valid = new ArrayList<>();
        List<RowIssue> issues = new ArrayList<>();
        int startDataRow = hasHeader ? headerIndex + 1 : headerIndex;
        int totalDataRows = 0;

        for (int r = startDataRow; r < rows.size(); r++) {
            List<String> row = rows.get(r);
            if (isRowEmpty(row)) {
                continue;
            }
            totalDataRows++;
            int displayRowNumber = r + 1;

            String rawDay = getCell(row, columnMapping.get(COL_DAY));
            String rawStart = getCell(row, columnMapping.get(COL_START));
            String rawEnd = getCell(row, columnMapping.get(COL_END));
            String rawTimeRange = getCell(row, columnMapping.get(COL_TIME_RANGE));
            if ((rawStart.isEmpty() || rawEnd.isEmpty()) && !rawTimeRange.isEmpty()) {
                String[] split = rawTimeRange.split("[-–—]");
                if (split.length >= 2) {
                    if (rawStart.isEmpty()) {
                        rawStart = split[0].trim();
                    }
                    if (rawEnd.isEmpty()) {
                        rawEnd = split[1].trim();
                    }
                }
            }
            String rawSubject = getCell(row, columnMapping.get(COL_SUBJECT));
            String rawRoom = getCell(row, columnMapping.get(COL_ROOM));
            String rawLecturer = getCell(row, columnMapping.get(COL_LECTURER));
            String rawNote = getCell(row, columnMapping.get(COL_NOTE));

            validateAndCollect(displayRowNumber, rawDay, rawStart, rawEnd, rawSubject, rawRoom, rawLecturer, rawNote, valid, issues);
        }

        return new ImportResult(format, totalDataRows, valid, issues);
    }

    private static void validateAndCollect(
            int rowNumber,
            String rawDay,
            String rawStart,
            String rawEnd,
            String rawSubject,
            String rawRoom,
            String rawLecturer,
            String rawNote,
            List<ScheduleSlot> valid,
            List<RowIssue> issues
    ) {
        int day = parseDayOfWeek(rawDay);
        if (day < ScheduleSlot.DAY_MIN || day > ScheduleSlot.DAY_MAX) {
            issues.add(new RowIssue(rowNumber, IssueType.INVALID_DAY));
            return;
        }
        String start = normalizeTime(rawStart);
        String end = normalizeTime(rawEnd);
        if (start == null || end == null) {
            issues.add(new RowIssue(rowNumber, IssueType.INVALID_TIME));
            return;
        }
        int startMin = ScheduleSlot.toMinutes(start);
        int endMin = ScheduleSlot.toMinutes(end);
        if (startMin < 0 || endMin <= startMin) {
            issues.add(new RowIssue(rowNumber, IssueType.INVALID_RANGE));
            return;
        }
        String subject = safeString(rawSubject);
        if (subject.isEmpty()) {
            issues.add(new RowIssue(rowNumber, IssueType.EMPTY_SUBJECT));
            return;
        }

        valid.add(ScheduleSlot.builder()
                .dayOfWeek(day)
                .startTime(start)
                .endTime(end)
                .subject(subject)
                .room(safeString(rawRoom))
                .lecturer(safeString(rawLecturer))
                .note(safeString(rawNote))
                .build());
    }

    @Nullable
    private static Map<Integer, Integer> detectHeaderMapping(List<String> row) {
        Map<Integer, Integer> mapping = new HashMap<>();
        int recognized = 0;

        for (int col = 0; col < row.size(); col++) {
            String key = normalizeHeaderKey(row.get(col));
            if (key.isEmpty()) {
                continue;
            }
            if ((key.contains("thu") && !key.contains("ketthuc")) || key.equals("day") || key.equals("dayofweek") || key.equals("weekday")) {
                if (!mapping.containsKey(COL_DAY)) {
                    mapping.put(COL_DAY, col);
                    recognized++;
                }
            } else if (key.contains("batdau") || key.contains("starttime") || key.equals("start") || key.equals("from")) {
                if (!mapping.containsKey(COL_START)) {
                    mapping.put(COL_START, col);
                    recognized++;
                }
            } else if (key.contains("ketthuc") || key.contains("endtime") || key.equals("end") || key.equals("to")) {
                if (!mapping.containsKey(COL_END)) {
                    mapping.put(COL_END, col);
                    recognized++;
                }
            } else if (key.contains("khunggio") || key.contains("thoigian") || key.equals("time") || key.equals("timerange")) {
                if (!mapping.containsKey(COL_TIME_RANGE)) {
                    mapping.put(COL_TIME_RANGE, col);
                    recognized++;
                }
            } else if (key.contains("monhoc") || key.equals("mon") || key.contains("subject") || key.contains("course")) {
                if (!mapping.containsKey(COL_SUBJECT)) {
                    mapping.put(COL_SUBJECT, col);
                    recognized++;
                }
            } else if (key.contains("phong") || key.contains("room") || key.contains("location")) {
                if (!mapping.containsKey(COL_ROOM)) {
                    mapping.put(COL_ROOM, col);
                    recognized++;
                }
            } else if (key.contains("giangvien") || key.equals("gv") || key.contains("lecturer") || key.contains("teacher") || key.contains("instructor")) {
                if (!mapping.containsKey(COL_LECTURER)) {
                    mapping.put(COL_LECTURER, col);
                    recognized++;
                }
            } else if (key.contains("ghichu") || key.contains("note")) {
                if (!mapping.containsKey(COL_NOTE)) {
                    mapping.put(COL_NOTE, col);
                    recognized++;
                }
            }
        }

        return recognized >= 2 ? mapping : null;
    }

    private static Map<Integer, Integer> defaultColumnMapping() {
        Map<Integer, Integer> map = new HashMap<>();
        map.put(COL_DAY, 0);
        map.put(COL_START, 1);
        map.put(COL_END, 2);
        map.put(COL_SUBJECT, 3);
        map.put(COL_ROOM, 4);
        map.put(COL_LECTURER, 5);
        map.put(COL_NOTE, 6);
        return map;
    }

    private static String normalizeHeaderKey(String raw) {
        if (raw == null) {
            return "";
        }
        String replaced = raw.replace('đ', 'd').replace('Đ', 'D');
        String decomposed = Normalizer.normalize(replaced, Normalizer.Form.NFD);
        String stripped = decomposed.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return stripped.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    public static int parseDayOfWeek(@Nullable String raw) {
        if (raw == null) {
            return -1;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return -1;
        }
        if (NUMBER_PATTERN.matcher(trimmed).matches()) {
            try {
                double val = Double.parseDouble(trimmed);
                int intVal = (int) Math.round(val);
                if (Math.abs(val - intVal) < 0.001 && intVal >= ScheduleSlot.DAY_MIN && intVal <= ScheduleSlot.DAY_MAX) {
                    return intVal;
                }
                return -1;
            } catch (NumberFormatException e) {
                return -1;
            }
        }

        String norm = normalizeHeaderKey(trimmed);
        Matcher numMatcher = ANY_NUMBER_TOKEN_PATTERN.matcher(norm);
        if (numMatcher.find()) {
            try {
                int parsed = Integer.parseInt(numMatcher.group());
                if (!numMatcher.find() && parsed >= ScheduleSlot.DAY_MIN && parsed <= ScheduleSlot.DAY_MAX) {
                    return parsed;
                }
            } catch (NumberFormatException ignored) {
            }
            return -1;
        }

        if (norm.equals("thuhai") || norm.equals("hai") || norm.equals("mon") || norm.equals("monday")) {
            return 2;
        }
        if (norm.equals("thuba") || norm.equals("ba") || norm.equals("tue") || norm.equals("tuesday")) {
            return 3;
        }
        if (norm.equals("thutu") || norm.equals("tu") || norm.equals("wed") || norm.equals("wednesday")) {
            return 4;
        }
        if (norm.equals("thunam") || norm.equals("nam") || norm.equals("thu") || norm.equals("thursday")) {
            return 5;
        }
        if (norm.equals("thusau") || norm.equals("sau") || norm.equals("fri") || norm.equals("friday")) {
            return 6;
        }
        if (norm.equals("thubay") || norm.equals("bay") || norm.equals("sat") || norm.equals("saturday")) {
            return 7;
        }
        return -1;
    }

    @Nullable
    public static String normalizeTime(@Nullable String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        if (NUMBER_PATTERN.matcher(trimmed).matches()) {
            try {
                double num = Double.parseDouble(trimmed);
                if (num > 0 && num < 1.0) {
                    return fractionToTime(num);
                }
                if (num > 100.0) {
                    double frac = num - Math.floor(num);
                    if (frac > 0.0) {
                        return fractionToTime(frac);
                    }
                }
            } catch (NumberFormatException ignored) {
            }
            return null;
        }

        String normalized = trimmed.toLowerCase(Locale.ROOT).replace('h', ':');
        String[] parts = normalized.split(":");
        if (parts.length < 2 || parts.length > 3) {
            return null;
        }
        try {
            int hour = Integer.parseInt(parts[0].trim());
            int minute = Integer.parseInt(parts[1].trim());
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                return null;
            }
            return String.format(Locale.US, "%02d:%02d", hour, minute);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String fractionToTime(double fraction) {
        int totalMinutes = (int) Math.round(fraction * 1440.0) % 1440;
        if (totalMinutes < 0) {
            totalMinutes += 1440;
        }
        int hour = totalMinutes / 60;
        int minute = totalMinutes % 60;
        return String.format(Locale.US, "%02d:%02d", hour, minute);
    }

    private static boolean isRowEmpty(List<String> row) {
        for (String cell : row) {
            if (cell != null && !cell.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static String getCell(List<String> row, @Nullable Integer index) {
        if (index == null || index < 0 || index >= row.size()) {
            return "";
        }
        String val = row.get(index);
        return val != null ? val.trim() : "";
    }

    private static String safeString(@Nullable String s) {
        return s == null ? "" : s.trim();
    }

    private static String stripBom(String text) {
        if (!text.isEmpty() && text.charAt(0) == '\uFEFF') {
            return text.substring(1);
        }
        return text;
    }

    private static byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int read;
        while ((read = is.read(chunk)) != -1) {
            buffer.write(chunk, 0, read);
        }
        return buffer.toByteArray();
    }
}
