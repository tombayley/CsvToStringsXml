import com.tombayley.csvtostringsxml.CsvToStringsXml;

public class MyConverter {

    public static void main(String args[]) {
        CsvToStringsXml.DocType docType = new CsvToStringsXml.DocTypeBuilder()
                .addItem("appname", "Bottom Quick Settings", 14)
                .addItem("miui_appname", "MIUI-ify", 2)
                .build();

        CsvToStringsXml.CsvToAndroidStringsXmlBuilder builder
                = new CsvToStringsXml.CsvToAndroidStringsXmlBuilder()
                .setResourcesDirPath("example/src/main/resources/android_strings")
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
