package com.cms.demo.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/** Small dependency-free reader for the first worksheet of an .xlsx workbook. */
public final class XlsxSheetReader {
    private XlsxSheetReader() { }

    public static List<Map<String, String>> read(InputStream input) {
        try {
            Map<String, byte[]> entries = new HashMap<>();
            try (ZipInputStream zip = new ZipInputStream(input)) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    if (!entry.isDirectory()) {
                        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                        zip.transferTo(bytes);
                        entries.put(entry.getName(), bytes.toByteArray());
                    }
                }
            }
            byte[] worksheet = entries.get("xl/worksheets/sheet1.xml");
            if (worksheet == null) throw new IllegalArgumentException("The workbook has no first worksheet");
            List<String> shared = parseSharedStrings(entries.get("xl/sharedStrings.xml"));
            Element root = parse(worksheet).getDocumentElement();
            NodeList rowNodes = root.getElementsByTagNameNS("*", "row");
            if (rowNodes.getLength() < 2) throw new IllegalArgumentException("The worksheet has no data rows");
            List<String> headers = readRow((Element) rowNodes.item(0), shared);
            List<Map<String, String>> rows = new ArrayList<>();
            for (int i = 1; i < rowNodes.getLength(); i++) {
                List<String> values = readRow((Element) rowNodes.item(i), shared);
                Map<String, String> row = new LinkedHashMap<>();
                for (int col = 0; col < headers.size(); col++) {
                    String header = headers.get(col).trim().toLowerCase().replace(' ', '_');
                    if (!header.isBlank() && col < values.size()) row.put(header, values.get(col).trim());
                }
                if (row.values().stream().anyMatch(value -> !value.isBlank())) rows.add(row);
            }
            return rows;
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read the Excel workbook: " + ex.getMessage(), ex);
        }
    }

    private static List<String> parseSharedStrings(byte[] bytes) throws Exception {
        List<String> result = new ArrayList<>();
        if (bytes == null) return result;
        NodeList items = parse(bytes).getElementsByTagNameNS("*", "si");
        for (int i = 0; i < items.getLength(); i++) {
            NodeList texts = ((Element) items.item(i)).getElementsByTagNameNS("*", "t");
            StringBuilder value = new StringBuilder();
            for (int j = 0; j < texts.getLength(); j++) value.append(texts.item(j).getTextContent());
            result.add(value.toString());
        }
        return result;
    }

    private static List<String> readRow(Element row, List<String> shared) {
        NodeList cells = row.getElementsByTagNameNS("*", "c");
        Map<Integer, String> values = new HashMap<>();
        int max = -1;
        for (int i = 0; i < cells.getLength(); i++) {
            Element cell = (Element) cells.item(i);
            int column = columnIndex(cell.getAttribute("r"));
            if (column < 0) continue;
            String type = cell.getAttribute("t");
            String value = "";
            if ("inlineStr".equals(type)) {
                NodeList textNodes = cell.getElementsByTagNameNS("*", "t");
                StringBuilder inline = new StringBuilder();
                for (int j = 0; j < textNodes.getLength(); j++) inline.append(textNodes.item(j).getTextContent());
                value = inline.toString();
            } else {
                NodeList valueNodes = cell.getElementsByTagNameNS("*", "v");
                if (valueNodes.getLength() > 0) value = valueNodes.item(0).getTextContent();
                if ("s".equals(type) && !value.isBlank()) value = shared.get(Integer.parseInt(value));
            }
            values.put(column, value); max = Math.max(max, column);
        }
        List<String> result = new ArrayList<>();
        for (int i = 0; i <= max; i++) result.add(values.getOrDefault(i, ""));
        return result;
    }

    private static int columnIndex(String reference) {
        int result = 0, length = 0;
        for (char ch : reference.toCharArray()) {
            if (!Character.isLetter(ch)) break;
            result = result * 26 + Character.toUpperCase(ch) - 'A' + 1; length++;
        }
        return length == 0 ? -1 : result - 1;
    }

    private static org.w3c.dom.Document parse(byte[] bytes) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
    }
}
