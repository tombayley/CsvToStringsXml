# CsvToStringsXml

[![](https://jitpack.io/v/tombayley/CsvToStringsXml.svg)](https://jitpack.io/#tombayley/CsvToStringsXml)
[![Total alerts](https://img.shields.io/lgtm/alerts/g/tombayley/CsvToStringsXml.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/tombayley/CsvToStringsXml/alerts/)
[![Language grade: Java](https://img.shields.io/lgtm/grade/java/g/tombayley/CsvToStringsXml.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/tombayley/CsvToStringsXml/context:java)

A script to convert Android translations stored in Google Sheets to Android string xml files.
Useful if you don't want to pay some of the high prices websites offering collaborative translations provide.

Disclaimer: I am not responsible for any errors in translations this library may cause. It is up to you to check translations are correct.
This library has been tested with a project containing 25 different locales and 400 strings. Unit tests may be added in the future for further testing.



## Usage
Add to your own project using Gradle or create your own script using the JAR file:


##### Using gradle dependency (/example/)
1. Add it in your root build.gradle at the end of repositories:
```
allprojects {
    repositories {
        ...
        maven { url 'https://www.jitpack.io' }
    }
}
```
2. Add the dependency:
```
dependencies {
    implementation 'com.github.tombayley.CsvToStringsXml:csvtostringsxml:1.0.0'
}
```

##### Using JAR file (/example_jar/)
1. To run you program with the jar file:
`java -cp ".;csvtostringsxml-1.0.0.jar" example_jar/MyConverter.java`





### Creating your program
1. Add the following code to your class:
```java
import com.tombayley.csvtostringsxml.CsvToStringsXml;

//...

CsvToStringsXml.CsvToStringsXmlBuilder builder
        = new CsvToStringsXml.CsvToStringsXmlBuilder()
        .setOutputPath("resources_dir/NEW_android_strings")
        .setSpreadsheetPath("resources_dir/translations.tsv")
        .setSpreadsheetDelimiter('\t')
        .setDefaultLocaleCode("en")
        .setStringIdColumnTitle("Name");

CsvToStringsXml csvToStringsXml = new CsvToStringsXml(builder);
csvToStringsXml.start();
```

2. You can customise the CsvToStringsXmlBuilder as you like:

| Config | Explanation |
| --- | --- |
| setExistingResourcesDirPath() | Optional. Path to existing string.xml files (see "Including existing string xml files") |
| setSpreadsheetPath() | Path to csv/tsv file containing translations in the format in the Workflow section |
| setOutputPath() | The output path for generated string.xml files |
| setSpreadsheetDelimiter() | Either ',' for csv or '\t' for tsv |
| setDefaultLocaleCode() | The default locale code for your app. E.g. "en" |
| setDocType() | See "Setting DocType" section |
| setEncoding() | Default "UTF-8". Sets to encoding for files read and written to |
| setStringIdColumnTitle() | The column title for string id's (e.g. "Name" as in example in Workflow section) |



#### Setting DocType
CsvToStringsXml can also generate and populate DocType if you need it.
Useful for if your translations spreadsheet doesn't include DocType variables in it, for example &appname; but instead includes the actual app name "My Cool App".

This might not be useful for many, but helps in some specific scenarios.

```java
CsvToStringsXml.DocType docType = new CsvToStringsXml.DocTypeBuilder()
        .addItem("appname", "My Cool App", 10)
        .build();

CsvToStringsXml.CsvToStringsXmlBuilder builder
        = new CsvToStringsXml.CsvToStringsXmlBuilder()
        //...
        .setDocType(docType);
```

`addItem()` takes the DocType key and value as mandatory arguments.
You can also pass in a integer as a 3rd parameter to specify the expected number of occurrences of each DocType key.
If the actual occurrences doesn't match the expected in any locale, the DocType key and locale code are printed with the actual and expected occurrences.




#### Including existing string xml files
You can combine existing string xml files with the translations in your spreadsheet to produce new updated string xml files.
This is useful if there are translations for strings in your existing string xml files but not in the spreadsheet.

Note: Only existing strings are combined. Existing locales which aren't present in the translations csv will not be combined (yet)

```java
CsvToStringsXml.CsvToStringsXmlBuilder builder
        = new CsvToStringsXml.CsvToStringsXmlBuilder()
        //...
        .setExistingResourcesDirPath("example/src/main/resources/android_strings");
```

Existing strings can simply be copied from your Android project to the directory defined above, as seen in [example/src/main/resources/android_strings](https://github.com/tombayley/CsvToStringsXml/tree/master/example/src/main/resources/android_strings)
An example output of this combination is shown in [example/src/main/resources/NEW_android_strings](https://github.com/tombayley/CsvToStringsXml/tree/master/example/src/main/resources/NEW_android_strings)







## Workflow
1. Ensure your spreadsheet is in following format:

| Name | en | cs | de | es | ... |
| --- | --- | --- | --- | --- | --- |
| hello_text | Hello | Ahoj | Hallo | Hola | ... |
| bye_text | Bye | Sbohem | Tschüss | Adiós | ... |
| ... | ... | ... | ... | ... | ... |

2. Download the sheet as a .tsv tab separated value file (can also use csv but may cause conflicts with some languages)
3. Place the file in a resources directory (along with existing string xmls if needed, see "Including existing string xml files")
4. Configure the CsvToStringsXmlBuilder and run the program.
5. Replace the String XML's in your Android project with the newly generated XML files.
6. Using diff tool (e.g. git diff), make sure changes are as expected





## Features to add:
- Unit testing
- Web interface
- User interface with JAR
