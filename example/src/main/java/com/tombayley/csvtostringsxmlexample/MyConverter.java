package com.tombayley.csvtostringsxmlexample;

import com.tombayley.csvtostringsxml.CsvToStringsXml;

public class MyConverter {

    public static void main(String[] args) {
        new MyConverter();
    }

    public MyConverter() {
        CsvToStringsXml.DocType docType = new CsvToStringsXml.DocTypeBuilder()
                .addItem("appname", "My Cool App")
                .build();

        CsvToStringsXml.CsvToStringsXmlBuilder builder
                = new CsvToStringsXml.CsvToStringsXmlBuilder()
                .setExistingResourcesDirPath("example/src/main/resources/android_strings")
                .setSpreadsheetPath("example/src/main/resources/translations.tsv")
                .setOutputPath("example/src/main/resources/NEW_android_strings")
                .setSpreadsheetDelimiter('\t')
                .setDefaultLocaleCode("en")
                .setDocType(docType)
                .setStringIdColumnTitle("Name");

        CsvToStringsXml csvToStringsXml = new CsvToStringsXml(builder);
        csvToStringsXml.start();
    }

}
