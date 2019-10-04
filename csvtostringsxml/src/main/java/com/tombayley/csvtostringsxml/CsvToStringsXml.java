package com.tombayley.csvtostringsxml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.RFC4180Parser;
import com.opencsv.RFC4180ParserBuilder;

import org.apache.commons.lang3.StringUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class CsvToStringsXml {

    // ==========================================
    // Config
    // ==========================================
    protected String existingResourcesDirPath;
    protected String spreadsheetPath;
    protected String outputPath;
    protected char spreadsheetDelimiter;
    protected String defaultLocaleCode;
    protected DocType docType;
    protected String encoding;
    protected String stringIdColumnTitle;

    // ==========================================
    // Misc
    // ==========================================
    protected static final int CSV_STRING_ID_COL = 0;
    protected static final int CSV_LOCALE_CODE_ROW = 0;

    // ==========================================
    // Constants
    // ==========================================
    protected static final String STRING_VALUES_DIR_PREFIX = "values";
    protected static final String STRINGS_XML = "strings.xml";
    protected static final String STRINGS_XML_ROOT_NODE = "resources";
    protected static final String STRINGS_XML_NODE_STRING = "string";
    protected static final String STRINGS_XML_ATTRIBUTE_NAME = "name";
    protected static final String STRINGS_XML_ATTRIBUTE_TRANSLATABLE = "translatable";

    protected static final String DOC_TYPE_TEMPLATE = "&%s;";

    protected boolean isBuilderValid = false;

    public CsvToStringsXml(CsvToStringsXmlBuilder builder) {
        existingResourcesDirPath = builder.existingResourcesDirPath;

        if (builder.spreadsheetPath == null) {
            print("spreadsheetPath not set");
            return;
        }
        spreadsheetPath = builder.spreadsheetPath;

        if (builder.outputPath == null){
            print("outputPath not set");
            return;
        }
        outputPath = builder.outputPath;

        if (builder.stringIdColumnTitle == null){
            print("stringIdColumnTitle not set");
            return;
        }
        stringIdColumnTitle = builder.stringIdColumnTitle;

        spreadsheetDelimiter = builder.spreadsheetDelimiter;
        defaultLocaleCode = builder.defaultLocaleCode;
        encoding = builder.encoding;
        docType = builder.docType;

        isBuilderValid = true;
    }

    public void start() {
        if (!isBuilderValid) {
            print("Builder not valid");
            return;
        }

        List<String[]> newTranslationsCsv;
        try {
            InputStream inputStream = new FileInputStream(new File(spreadsheetPath));
            newTranslationsCsv = readCsv(new InputStreamReader(inputStream, encoding));
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        HashMap<String, LinkedHashMap<String, String>> newTranslations = convertCsvListToHashMap(newTranslationsCsv);
        removeEmptyTranslations(newTranslations);

        if (existingResourcesDirPath != null) {
            HashMap<String, Document> stringDocumentXmls;
            try {
                stringDocumentXmls = readResStringFiles(new File(existingResourcesDirPath));
            } catch (IOException | ParserConfigurationException | SAXException e) {
                e.printStackTrace();
                return;
            }

            HashMap<String, LinkedHashMap<String, String>> existingTranslations = convertStringXmlsToHashMap(stringDocumentXmls);
            addExistingStringXmlTranslationsToNewTranslations(existingTranslations, newTranslations);
        }

        fixTranslations(newTranslations);
        saveTranslations(newTranslations);

        print("DONE");

        printDocTypeOccurrences();
    }

    protected void printDocTypeOccurrences() {
        String template = "%s: %s/%s,\t";

        print("\n==========================");
        print("DocType keys with unexpected occurrences per locale (Format: expected/actual):");

        for (Map.Entry<String, DocType.Item> entity : docType.entities.entrySet()) {
            String docTypeKey = entity.getKey();
            DocType.Item docTypeItem = entity.getValue();
            int expectedOccurrences = docTypeItem.expectedOccurrences;
            if (expectedOccurrences == DocType.NOT_SET) continue;

            StringBuilder stringBuilder = new StringBuilder();

            for (Map.Entry<String, Integer> localeOccurrences : docTypeItem.actualOccurrencesPerLocale.entrySet()) {
                String localeCode = localeOccurrences.getKey();
                Integer occurrences = localeOccurrences.getValue();

                if (occurrences == expectedOccurrences) continue;
                stringBuilder.append(String.format(template, localeCode, occurrences, expectedOccurrences));
            }

            String occurrenceText = stringBuilder.toString();
            if (occurrenceText.isEmpty()) continue;

            print(docTypeKey);
            print("\t" + occurrenceText);
        }

        print("==========================\n");
    }

    protected void saveTranslations(HashMap<String, LinkedHashMap<String, String>> translations) {
        for (Map.Entry<String, LinkedHashMap<String, String>> translation : translations.entrySet()) {
            File dir = new File(
                    outputPath
                            + File.separator
                            + STRING_VALUES_DIR_PREFIX
                            + (translation.getKey().equals(defaultLocaleCode) ? "" : "-" + translation.getKey())
            );
            dir.mkdirs();

            File newStringXmlFile = new File(dir, STRINGS_XML);
            try {
                newStringXmlFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                Document document = createDocumentFromTranslation(translation.getValue());
                saveDocToFile(document, newStringXmlFile);
            } catch (IOException | TransformerException | ParserConfigurationException e) {
                e.printStackTrace();
            }
        }
    }

    protected void fixTranslations(HashMap<String, LinkedHashMap<String, String>> translations) {
        for (Map.Entry<String, LinkedHashMap<String, String>> translation : translations.entrySet()) {
            String localeCode = translation.getKey();
            LinkedHashMap<String, String> strings = translation.getValue();

            if (!localeCode.equals(defaultLocaleCode)) {
                strings.remove("app_name");
            }

            for (Map.Entry<String, String> string : strings.entrySet()) {
                String stringKey = string.getKey();
                String stringText = string.getValue();

                stringText = escapeString(stringText);
                stringText = replaceDocType(localeCode, stringText);

                stringText = stringText.trim();

                strings.put(stringKey, stringText);
            }
        }
    }

    protected String escapeString(String stringText) {
        // Replace ellipsis (u2026 is …)
        return stringText.replace("...", "&#8230;")
                .replace("\u2026", "&#8230;")

                // Add a \ before and single/double quotes if there isn't one already
                .replace("\\\"", "\"")
                .replace("\"", "\\\"")
                .replace("\\'", "'")
                .replace("'", "\\'")

                .replace(" & ", " &amp; ")
                ;
    }

    protected String replaceDocType(String localeCode, String stringText) {
        for (Map.Entry<String, DocType.Item> docTypeEntry : docType.entities.entrySet()) {
            String docTypeKey = docTypeEntry.getKey();
            DocType.Item docTypeItem = docTypeEntry.getValue();

            if (!stringText.contains(docTypeItem.value)) continue;
            String fullDoctTypeText = String.format(DOC_TYPE_TEMPLATE, docTypeEntry.getKey());
            stringText = stringText.replace(docTypeItem.value, fullDoctTypeText);

            int occurrences = StringUtils.countMatches(stringText, fullDoctTypeText);
            Integer currentOccurrences = docTypeItem.actualOccurrencesPerLocale.get(localeCode);
            if (currentOccurrences == null) currentOccurrences = 0;
            docTypeItem.actualOccurrencesPerLocale.put(localeCode, currentOccurrences + occurrences);
        }

        return stringText;
    }

    protected Document createDocumentFromTranslation(LinkedHashMap<String, String> translation) throws ParserConfigurationException {
        Document document = createNewXmlFile();
        Element root = document.createElement(STRINGS_XML_ROOT_NODE);

        Node disableEscaping = document.createProcessingInstruction(StreamResult.PI_DISABLE_OUTPUT_ESCAPING, "&");
        root.appendChild(disableEscaping);

        for (Map.Entry<String, String> string : translation.entrySet()) {
            String stringId = string.getKey();
            String stringText = string.getValue();

            Element stringNode = document.createElement(STRINGS_XML_NODE_STRING);
            stringNode.setAttribute(STRINGS_XML_ATTRIBUTE_NAME, stringId);
            if (stringId.contains("app_name")) stringNode.setAttribute(STRINGS_XML_ATTRIBUTE_TRANSLATABLE, "false");
            stringNode.setTextContent(stringText);

            root.appendChild(stringNode);
        }

        document.appendChild(root);

//        Node enableEscaping = document.createProcessingInstruction(StreamResult.PI_ENABLE_OUTPUT_ESCAPING, "&");
//        root.appendChild(enableEscaping);

        return document;
    }

    protected Document createNewXmlFile() throws ParserConfigurationException {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        return docBuilder.newDocument();
    }

    protected void saveDocToFile(Document doc, File file) throws IOException, TransformerException {
        doc.setXmlStandalone(true);

        DOMSource source = new DOMSource(doc);
        FileOutputStream out = new FileOutputStream(file);
        StreamResult result = new StreamResult(out);

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        // transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        transformer.setOutputProperty(OutputKeys.ENCODING, encoding);
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        out.write(("<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>\n").getBytes(encoding));
        out.write(docType.docTypeText.getBytes(encoding));
        transformer.transform(source, result);

        out.close();
    }

    protected void addExistingStringXmlTranslationsToNewTranslations(
            HashMap<String, LinkedHashMap<String, String>> existingTranslations,
            HashMap<String, LinkedHashMap<String, String>> newTranslations
    ) {
        for (Map.Entry<String, LinkedHashMap<String, String>> existingStringXml : existingTranslations.entrySet()) {
            String localeCode = existingStringXml.getKey();
            LinkedHashMap<String, String> existingTranslation = existingStringXml.getValue();

            LinkedHashMap<String, String> newTranslation = newTranslations.get(localeCode);
            if (newTranslation == null || newTranslation.isEmpty()) {
                print(new Exception("newTranslation == null").getStackTrace());
                continue;
            }

            addAllIfAbsent(existingTranslation, newTranslation);
        }
    }

    protected void removeEmptyTranslations(HashMap<String, LinkedHashMap<String, String>> translations) {
        for (Map.Entry<String, LinkedHashMap<String, String>> stringXml : translations.entrySet()) {
            LinkedHashMap<String, String> translation = stringXml.getValue();
            translation.values().removeIf(Objects::isNull);
            translation.values().removeIf(String::isEmpty);
        }
    }

    protected void addAllIfAbsent(LinkedHashMap<String, String> toBeAdded, LinkedHashMap<String, String> target) {
        LinkedHashMap<String, String> tmp = new LinkedHashMap<>(toBeAdded);
        tmp.keySet().removeAll(target.keySet());
        target.putAll(tmp);
    }



    protected HashMap<String, LinkedHashMap<String, String>> convertStringXmlsToHashMap(HashMap<String, Document> stringXmls) {
        HashMap<String, LinkedHashMap<String, String>> hashMap = new HashMap<>();

        for (Map.Entry<String, Document> stringXml : stringXmls.entrySet()) {
            LinkedHashMap<String, String> translations = new LinkedHashMap<>();

            try {
                Document doc = stringXml.getValue();
                NodeList nodeList = doc.getElementsByTagName(STRINGS_XML_NODE_STRING);
                for (int i = 0; i < nodeList.getLength(); i++) {
                    Node node = nodeList.item(i);

                    if (node.getNodeType() != Node.ELEMENT_NODE) continue;

                    Element element = (Element) node;
                    String stringId = element.getAttribute(STRINGS_XML_ATTRIBUTE_NAME);

                    translations.put(stringId, element.getTextContent());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            hashMap.put(stringXml.getKey(), translations);
        }

        return hashMap;
    }


    protected HashMap<String, LinkedHashMap<String, String>> convertCsvListToHashMap(List<String[]> csv) {
        HashMap<String, LinkedHashMap<String, String>> hashMap = new HashMap<>();

        List<String> stringIds = getFirstColumnOfCsv(csv);

        HashMap<Integer, String> localePositions = new HashMap<>();

        int terminatingLocaleColNum = 999;
        int rowNum = 0;

        for (String[] row : csv) {
            int colNum = 0;

            for (String cell : row) {
                if (colNum >= terminatingLocaleColNum) continue;

                if (rowNum == 0) {
                    if (cell.isEmpty()) {
                        terminatingLocaleColNum = colNum;
                        break;
                    }

                    hashMap.put(cell, new LinkedHashMap<>());
                    localePositions.put(colNum, cell);
                } else {
                    String localeCode = localePositions.get(colNum);
                    LinkedHashMap<String, String> localeStrings = hashMap.get(localeCode);
                    localeStrings.put(stringIds.get(rowNum - 1), cell);
                }
                colNum++;
            }
            rowNum++;
        }

        hashMap.remove(stringIdColumnTitle);

        return hashMap;
    }

    protected List<String> getFirstColumnOfCsv(List<String[]> csv) {
        List<String> firstCol = new ArrayList<>();

        for (int i = 0; i < csv.size(); i++) {
            if (i == CSV_LOCALE_CODE_ROW) continue;

            firstCol.add(csv.get(i)[CSV_STRING_ID_COL]);
        }

        return firstCol;
    }


    protected void print(Object o) {
        System.out.println(o);
    }

    protected List<String[]> readCsv(Reader reader) throws IOException {
        RFC4180Parser rfc4180Parser = new RFC4180ParserBuilder().withSeparator('\t').build();
//        CSVParser csvParser = new CSVParserBuilder().withSeparator('\t').build();

        CSVReader csvReader = new CSVReaderBuilder(reader)
                .withCSVParser(rfc4180Parser)
                .build();

        return csvReader.readAll();
    }

    protected HashMap<String, Document> readResStringFiles(File parentDir) throws IOException, ParserConfigurationException, SAXException {
        HashMap<String, Document> documents = new HashMap<>();
        File[] resDirs = parentDir.listFiles();

        for (File resDir : resDirs) {
            if (!resDir.getName().startsWith(STRING_VALUES_DIR_PREFIX)) continue;

            File[] localeResFiles = resDir.listFiles();

            for (File localeResFile : localeResFiles) {
                if (!localeResFile.getName().equals(STRINGS_XML)) continue;

                String valuesDirName = resDir.getName();
                String localeCode;
                if (valuesDirName.equals(STRING_VALUES_DIR_PREFIX)) {
                    localeCode = defaultLocaleCode;
                } else {
                    localeCode = valuesDirName.replace(STRING_VALUES_DIR_PREFIX + "-", "");
                }

                Document doc = readXmlFile(localeResFile);
                removeNonTranslatableStrings(doc);

                documents.put(localeCode, doc);
                break;
            }
        }

        return documents;
    }

    protected void removeNonTranslatableStrings(Document doc) {
        Element root = doc.getDocumentElement();
        NodeList nodeList = doc.getElementsByTagName(STRINGS_XML_NODE_STRING);

        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);

            if (node.getNodeType() != Node.ELEMENT_NODE) {
                root.removeChild(node);
                print("Removed non-element node with content: " + node.getTextContent());
                continue;
            }

            Element element = (Element) node;

            if (!element.hasAttribute(STRINGS_XML_ATTRIBUTE_TRANSLATABLE)) continue;
            String attributeValue = element.getAttribute(STRINGS_XML_ATTRIBUTE_TRANSLATABLE);
            if (attributeValue.equals("false")) {
                root.removeChild(node);
//                print("Removed non-translatable node with content: " + element.getTextContent());
            }
        }
    }

    protected Document readXmlFile(File file) throws IOException, ParserConfigurationException, SAXException {
        InputStream inputStream= new FileInputStream(file);
        Reader reader = new InputStreamReader(inputStream, encoding);
        InputSource is = new InputSource(reader);
        is.setEncoding(encoding);

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(is);
        doc.getDocumentElement().normalize();

        inputStream.close();
        reader.close();

        return doc;
    }

    public static class DocTypeBuilder {
        HashMap<String, DocType.Item> entities = new HashMap<>();

        public DocTypeBuilder addItem(String key, String value) {
            entities.put(key, new DocType.Item(value));
            return this;
        }

        public DocTypeBuilder addItem(String key, String value, int expectedOccurrences) {
            entities.put(key, new DocType.Item(value, expectedOccurrences));
            return this;
        }

        public DocType build() {
            return new DocType(entities);
        }
    }

    public static class DocType {
        HashMap<String, DocType.Item> entities = new HashMap<>();
        String docTypeText = "";

        static final int NOT_SET = -1;

        public DocType(HashMap<String, Item> entities) {
            final String docTypeTemplate = "<!DOCTYPE resources [%s\n]>\n";
            final String entityTemplate = "\n    <!ENTITY %s \"%s\">";

            if (entities.size() == 0) return;

            docTypeText = docTypeTemplate;

            StringBuilder entitiesBuilder = new StringBuilder();
            for (Map.Entry<String, Item> entity : entities.entrySet()) {
                entitiesBuilder.append(String.format(entityTemplate, entity.getKey(), entity.getValue().value));
            }

            docTypeText = String.format(docTypeText, entitiesBuilder.toString());
            this.entities = entities;
        }

        public static class Item {
            String value;
            int expectedOccurrences = NOT_SET;
            HashMap<String, Integer> actualOccurrencesPerLocale = new HashMap<>();

            public Item(String value) {
                this.value = value;
            }
            public Item(String value, int expectedOccurrences) {
                this.value = value;
                this.expectedOccurrences = expectedOccurrences;
            }
        }
    }

    public static class CsvToStringsXmlBuilder {
        String existingResourcesDirPath = null;
        String spreadsheetPath = null;
        String outputPath = null;
        char spreadsheetDelimiter = ',';
        String defaultLocaleCode = "en";
        DocType docType = null;
        String encoding = "UTF-8";
        String stringIdColumnTitle = "";

        public CsvToStringsXmlBuilder setExistingResourcesDirPath(String path) {
            existingResourcesDirPath = path;
            return this;
        }

        public CsvToStringsXmlBuilder setSpreadsheetPath(String path) {
            spreadsheetPath = path;
            return this;
        }

        public CsvToStringsXmlBuilder setOutputPath(String path) {
            outputPath = path;
            return this;
        }

        public CsvToStringsXmlBuilder setSpreadsheetDelimiter(char delimiter) {
            spreadsheetDelimiter = delimiter;
            return this;
        }

        public CsvToStringsXmlBuilder setDefaultLocaleCode(String code) {
            defaultLocaleCode = code;
            return this;
        }

        public CsvToStringsXmlBuilder setDocType(DocType docType) {
            this.docType = docType;
            return this;
        }

        public CsvToStringsXmlBuilder setEncoding(String encoding) {
            this.encoding = encoding;
            return this;
        }

        public CsvToStringsXmlBuilder setStringIdColumnTitle(String title) {
            stringIdColumnTitle = title;
            return this;
        }
    }

}
