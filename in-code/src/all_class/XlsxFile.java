package all_class;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;
import javax.xml.XMLConstants;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

/** 只处理本系统使用的普通单元格；使用 JDK 的 ZIP/XML API，无需另装依赖。 */
final class XlsxFile {
    private static final String NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";
    private static final String REL = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";
    private static final String PKG = "http://schemas.openxmlformats.org/package/2006/relationships";
    private static final String XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
    private static final Pattern ESCAPE = Pattern.compile("_x([0-9A-Fa-f]{4})_");
    private XlsxFile() {}

    static void write(Path path, Map<String, List<List<Object>>> sheets) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(path))) {
            StringBuilder types = new StringBuilder("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>");
            StringBuilder workbook = new StringBuilder("<workbook xmlns=\"" + NS + "\" xmlns:r=\"" + REL + "\"><sheets>");
            StringBuilder rels = new StringBuilder("<Relationships xmlns=\"" + PKG + "\"><Relationship Id=\"styles\" Type=\"" + REL + "/styles\" Target=\"styles.xml\"/>");
            int index = 0;
            for (Map.Entry<String, List<List<Object>>> sheet : sheets.entrySet()) {
                index++;
                String target = "worksheets/sheet" + index + ".xml";
                types.append("<Override PartName=\"/xl/").append(target).append("\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");
                workbook.append("<sheet name=\"").append(escape(sheet.getKey())).append("\" sheetId=\"").append(index).append("\" r:id=\"rId").append(index).append("\"/>");
                rels.append("<Relationship Id=\"rId").append(index).append("\" Type=\"").append(REL).append("/worksheet\" Target=\"").append(target).append("\"/>");
                put(zip, "xl/" + target, worksheet(sheet.getValue()));
            }
            put(zip, "[Content_Types].xml", types.append("</Types>").toString());
            put(zip, "xl/workbook.xml", workbook.append("</sheets></workbook>").toString());
            put(zip, "xl/_rels/workbook.xml.rels", rels.append("</Relationships>").toString());
            put(zip, "_rels/.rels", "<Relationships xmlns=\"" + PKG + "\"><Relationship Id=\"rId1\" Type=\"" + REL + "/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>");
            put(zip, "xl/styles.xml", "<styleSheet xmlns=\"" + NS + "\">"
                    + "<numFmts count=\"1\"><numFmt numFmtId=\"164\" formatCode=\"yyyy-mm-dd hh:mm:ss.000\"/></numFmts>"
                    + "<fonts count=\"2\"><font><sz val=\"11\"/><name val=\"Microsoft YaHei\"/></font><font><b/><sz val=\"11\"/><color rgb=\"FFFFFFFF\"/><name val=\"Microsoft YaHei\"/></font></fonts>"
                    + "<fills count=\"3\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill><fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF245870\"/><bgColor indexed=\"64\"/></patternFill></fill></fills>"
                    + "<borders count=\"1\"><border/></borders><cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
                    + "<cellXfs count=\"4\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/><xf numFmtId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\"/><xf numFmtId=\"164\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\"/><xf numFmtId=\"2\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\"/></cellXfs>"
                    + "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles></styleSheet>");
        }
    }

    private static String worksheet(List<List<Object>> rows) throws IOException {
        if (rows.isEmpty() || rows.size() > 1048576) throw new IOException("Sheet 行数无效");
        int columns = rows.get(0).size();
        StringBuilder xml = new StringBuilder("<worksheet xmlns=\"" + NS + "\"><sheetViews><sheetView workbookViewId=\"0\"><pane ySplit=\"1\" topLeftCell=\"A2\" activePane=\"bottomLeft\" state=\"frozen\"/></sheetView></sheetViews><cols>");
        for (int c = 0; c < columns; c++) {
            int width = 16;
            for (List<Object> row : rows) {
                Object value = row.get(c);
                if (value instanceof Date) width = Math.max(width, 27);
                else if (value != null) {
                    int length = value.toString().codePoints().map(cp -> cp > 255 ? 2 : 1).sum();
                    width = Math.max(width, Math.min(45, length + 2));
                }
            }
            xml.append("<col min=\"").append(c + 1).append("\" max=\"").append(c + 1).append("\" width=\"").append(width).append("\" customWidth=\"1\"/>");
        }
        xml.append("</cols><sheetData>");
        for (int r = 0; r < rows.size(); r++) {
            xml.append("<row r=\"").append(r + 1).append("\">");
            for (int c = 0; c < columns; c++) {
                Object value = rows.get(r).get(c);
                int style = r == 0 ? 1 : value instanceof Date ? 2 : value instanceof Double ? 3 : 0;
                xml.append("<c r=\"").append(column(c)).append(r + 1).append("\" s=\"").append(style).append('"');
                if (value instanceof Number || value instanceof Date) {
                    double number = value instanceof Date ? excelDate((Date) value) : ((Number) value).doubleValue();
                    if (!Double.isFinite(number)) throw new IOException("不能保存非有限数值");
                    xml.append("><v>").append(value instanceof Date ? number : value).append("</v></c>");
                } else if (value instanceof Boolean) {
                    xml.append(" t=\"b\"><v>").append((Boolean) value ? 1 : 0).append("</v></c>");
                } else {
                    String text = value == null ? "" : value.toString();
                    if (text.length() > 32767) throw new IOException("单元格内容超过 Excel 长度限制");
                    // 明确写为文本，以 = 开头的名称或密码不会变成公式。
                    xml.append(" t=\"inlineStr\"><is><t xml:space=\"preserve\">").append(escape(text)).append("</t></is></c>");
                }
            }
            xml.append("</row>");
        }
        return xml.append("</sheetData><autoFilter ref=\"A1:").append(column(columns - 1)).append(rows.size()).append("\"/></worksheet>").toString();
    }

    // Excel 显示北京时间，固定 UTC+8，避免更换电脑时区后改变实际时间。
    private static double excelDate(Date date) { return (date.getTime() + 28800000L) / 86400000.0 + 25569; }
    static Date date(String value) {
        double serial = Double.parseDouble(value);
        if (!Double.isFinite(serial)) throw new IllegalArgumentException("日期无效");
        return new Date(Math.round((serial - 25569) * 86400000) - 28800000L);
    }

    static Map<String, List<List<String>>> read(Path path) throws IOException {
        try (ZipFile zip = new ZipFile(path.toFile(), StandardCharsets.UTF_8)) {
            Document workbook = document(zip, "xl/workbook.xml");
            NodeList properties = workbook.getElementsByTagNameNS(NS, "workbookPr");
            if (properties.getLength() > 0 && Set.of("1", "true").contains(((Element) properties.item(0)).getAttribute("date1904")))
                throw new IOException("不支持 1904 日期系统，请保留原工作簿的日期设置");
            Map<String, String> targets = new HashMap<>();
            NodeList rels = document(zip, "xl/_rels/workbook.xml.rels").getElementsByTagNameNS(PKG, "Relationship");
            String sharedPath = null;
            for (int i = 0; i < rels.getLength(); i++) {
                Element rel = (Element) rels.item(i);
                if (rel.getAttribute("TargetMode").equals("External")) throw new IOException("不允许外部工作表");
                String target = rel.getAttribute("Target");
                String name = target.startsWith("/") ? target.substring(1) : "xl/" + target;
                name = Path.of(name).normalize().toString().replace('\\', '/');
                if (!name.startsWith("xl/")) throw new IOException("工作表路径无效");
                targets.put(rel.getAttribute("Id"), name);
                if (rel.getAttribute("Type").equals(REL + "/sharedStrings")) sharedPath = name;
            }
            List<String> shared = new ArrayList<>();
            if (sharedPath != null) {
                NodeList strings = document(zip, sharedPath).getElementsByTagNameNS(NS, "si");
                for (int i = 0; i < strings.getLength(); i++) shared.add(text((Element) strings.item(i), "t"));
            }
            Map<String, List<List<String>>> result = new LinkedHashMap<>();
            NodeList sheets = workbook.getElementsByTagNameNS(NS, "sheet");
            for (int i = 0; i < sheets.getLength(); i++) {
                Element sheet = (Element) sheets.item(i);
                String target = targets.get(sheet.getAttributeNS(REL, "id"));
                if (target == null) throw new IOException("工作表关联缺失");
                List<List<String>> rows = new ArrayList<>();
                NodeList data = document(zip, target).getElementsByTagNameNS(NS, "row");
                for (int r = 0; r < data.getLength(); r++) {
                    List<String> values = new ArrayList<>();
                    NodeList cells = ((Element) data.item(r)).getElementsByTagNameNS(NS, "c");
                    int previous = -1;
                    for (int c = 0; c < cells.getLength(); c++) {
                        Element cell = (Element) cells.item(c);
                        String ref = cell.getAttribute("r");
                        if (!ref.matches("[A-Z]{1,3}[1-9][0-9]*")) throw new IOException("单元格位置无效");
                        int col = 0;
                        for (char ch : ref.toCharArray()) { if (!Character.isLetter(ch)) break; col = col * 26 + ch - 'A' + 1; }
                        if (col > 16384 || col <= previous) throw new IOException("单元格位置重复或越界");
                        previous = col;
                        while (values.size() < col) values.add("");
                        if (cell.getElementsByTagNameNS(NS, "f").getLength() > 0) throw new IOException("数据单元格不能使用公式：" + ref);
                        String value;
                        String raw = text(cell, "v");
                        switch (cell.getAttribute("t")) {
                            case "inlineStr": value = text(cell, "t"); break;
                            case "s": value = shared.get(Integer.parseInt(raw)); break;
                            case "b":
                                if (!raw.equals("1") && !raw.equals("0")) throw new IOException("布尔值无效");
                                value = raw.equals("1") ? "true" : "false"; break;
                            case "str": value = raw; break;
                            case "": case "n": value = raw.isEmpty() ? "" : new BigDecimal(raw).stripTrailingZeros().toPlainString(); break;
                            default: throw new IOException("不支持的单元格类型：" + ref);
                        }
                        values.set(col - 1, value);
                    }
                    if (values.stream().anyMatch(v -> !v.isEmpty())) rows.add(values);
                }
                if (result.put(sheet.getAttribute("name"), rows) != null) throw new IOException("Sheet 名称重复");
            }
            return result;
        } catch (ParserConfigurationException | SAXException | RuntimeException e) {
            throw new IOException("Excel 文件格式错误：" + path.getFileName(), e);
        }
    }

    private static Document document(ZipFile zip, String name) throws IOException, ParserConfigurationException, SAXException {
        ZipEntry entry = zip.getEntry(name);
        if (entry == null) throw new IOException("Excel 缺少 " + name);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        try (InputStream input = zip.getInputStream(entry)) { return factory.newDocumentBuilder().parse(input); }
    }

    private static String text(Element parent, String tag) {
        StringBuilder result = new StringBuilder();
        NodeList nodes = parent.getElementsByTagNameNS(NS, tag);
        for (int i = 0; i < nodes.getLength(); i++) result.append(nodes.item(i).getTextContent());
        Matcher matcher = ESCAPE.matcher(result);
        StringBuffer decoded = new StringBuffer();
        while (matcher.find()) matcher.appendReplacement(decoded, Matcher.quoteReplacement(String.valueOf((char) Integer.parseInt(matcher.group(1), 16))));
        return matcher.appendTail(decoded).toString();
    }

    private static String escape(String value) {
        String text = ESCAPE.matcher(value).replaceAll("_x005F_x$1_");
        StringBuilder out = new StringBuilder();
        for (char ch : text.toCharArray()) {
            if (ch < 32 || ch == 0xFFFE || ch == 0xFFFF) out.append(String.format(Locale.ROOT, "_x%04X_", (int) ch));
            else if (ch == '&') out.append("&amp;");
            else if (ch == '<') out.append("&lt;");
            else if (ch == '>') out.append("&gt;");
            else if (ch == '"') out.append("&quot;");
            else out.append(ch);
        }
        return out.toString();
    }

    private static String column(int index) {
        StringBuilder name = new StringBuilder();
        do { name.insert(0, (char) ('A' + index % 26)); index = index / 26 - 1; } while (index >= 0);
        return name.toString();
    }

    private static void put(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write((XML + content).getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}
