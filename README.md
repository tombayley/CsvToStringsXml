# TranslationsToXml

A script to convert Android translations stored in Google Sheets to Android string xml files.
Useful if you don't want to pay some of the high prices websites offering collaborative translations provide.

Disclaimer: I am not responsible for any errors in translations this library may cause. It is up to you to check translations are correct.

## Usage

... how to include module / use jar


```java
CsvToStringsXml.CsvToAndroidStringsXmlBuilder builder
        = new CsvToStringsXml.CsvToAndroidStringsXmlBuilder()
        .setResourcesDirPath("example/src/main/resources/android_strings")
        .setSpreadsheetPath("example/src/main/resources/translations.tsv")
        .setOutputPath("example/src/main/resources/NEW_android_strings")
        .setSpreadsheetDelimiter('\t')
        .setDefaultLocaleCode("en")
        .setStringIdColumnTitle("Name");

CsvToStringsXml csvToStringsXml = new CsvToStringsXml(builder);
csvToStringsXml.start();
```

## Workflow
1. Ensure Google Sheets is in following format:

| Name | en | cs | de | es | ... |
| --- | --- | --- | --- | --- | --- |
| hello_text | Hello | Ahoj | Hallo | Hola | ... |
| bye_text | Bye | Sbohem | Tschüss | Adiós | ... |
| ... | ... | ... | ... | ... | ... |

2. Download the Google Sheet as a .tsv tab separated value file (can also use csv but may cause conflicts with some languages)
3. 


## Features to add:
- Web interface
- User interface
