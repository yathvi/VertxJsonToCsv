import com.opencsv.CSVWriter;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/***
 * Convert dynamic/nested JsonArray/JsonObject to CSV/Excel file, it doesn't depend on Schema.
 * CSV/Excel headers/Column headers will be added as Json hierarchy Key
 * and the Values of dynamic/nested JsonArray/JsonObject will be added as row in the CSV/Excel
 * and will be mapped to the right CSV/Excel header.
 */
public class JsonToCsvOrExcel {

    private static final Logger logger = LoggerFactory.getLogger(JsonToCsvOrExcel.class);
    /* CSV/Excel Header */
    private static List<String> jsonKeyAsHeader;

    /* JSON Key-Value, Keys added as header in CSV/Excel, Values added as row in CSV/Excel
     *  Using LinkedHashMap to maintain order of insertion  */
    private static Map<String, String> jsonKeyValue;

    /* List of JSON Key-Value, Keys added as header in CSV/Excel, Values added as row in CSV/Excel */
    private static List<Map<String, String>> jsonKeyValueList;

    private static List<List<Map<String, String>>> jsonKeyValueListOfLists;

    private static final int CSV_OR_EXCEL_COLUMNS_SIZE = 16384;

    public static void main(String[] args) throws IOException {
        addDifferentJsonsToCsv();
        mergeDifferentJsonsAndAddInCsv();
    }

    /**
     * Read JsonObject.json; store the key - value in Hashmap @param jsonKeyValue,
     * add this multiple Hashmap @param jsonKeyValue to ArrayList @param jsonKeyValueList.
     *
     * Read JsonArray.json; store the key - value in Hashmap @param jsonKeyValue,
     * add this multiple Hashmap @param jsonKeyValue to ArrayList @param jsonKeyValueList.
     *
     * Trigger jsonToCsvOrExcel by passing ArrayList @param jsonKeyValueList.
     *
     * @throws IOException
     */
    private static void addDifferentJsonsToCsv() throws IOException {
        jsonKeyValueList = new ArrayList<>();
        jsonKeyValueListOfLists = new ArrayList<>();

        String jsonObjectFile = "src/main/resources/json/JsonObject.json";
        String jsonObjectStr = readFileAsString(jsonObjectFile);
        /* Append JsonObject Keys*/
        //String appendJsonObjectKey = "jsonObject_";
        String appendJsonObjectKey = "";
        /* Passing JsonObject.json JsonObject*/
        JsonObject jsonObject = new JsonObject(jsonObjectStr);

        jsonKeyAsHeader = new ArrayList<>();
        jsonKeyValue = new LinkedHashMap<>();
        processJsonObject(jsonObject,appendJsonObjectKey);
        for (int i =0 ; i < 25 ; i++)
            jsonKeyValueList.add(jsonKeyValue);

        String jsonArrayFile = "src/main/resources/json/JsonArray.json";
        String jsonArrayStr = readFileAsString(jsonArrayFile);
        /* Append JsonArray Keys*/
        //String appendJsonArrayKey = "jsonArray_";
        String appendJsonArrayKey = "";
        /* Passing JsonArray.json JsonArray*/
        JsonArray jsonArray = new JsonArray(jsonArrayStr);

        jsonKeyAsHeader = new ArrayList<>();
        jsonKeyValue = new LinkedHashMap<>();
        processJsonArray(jsonArray,appendJsonArrayKey);
        for (int i =0 ; i < 25 ; i++)
            jsonKeyValueList.add(jsonKeyValue);

        jsonKeyValueListOfLists.add(jsonKeyValueList);

        jsonToCsvOrExcel(jsonKeyValueListOfLists,"src/main/resources/csvHeaders/ListHeaders.txt");
    }

    /**
     * Read JsonObject.json; store the key - value in Hashmap @param jsonKeyValue,
     * add this multiple Hashmap @param jsonKeyValue to ArrayList @param jsonKeyValueList.
     *
     * Read JsonArray.json; store the key - value in Hashmap @param jsonKeyValue,
     * add this multiple Hashmap @param jsonKeyValue to ArrayList @param jsonKeyValueList_2.
     *
     * Trigger jsonToCsvOrExcel by passing ArrayList @param jsonKeyValueList and ArrayList @param jsonKeyValueList_2.
     *
     * @throws IOException
     */
    private static void mergeDifferentJsonsAndAddInCsv() throws IOException {

        jsonKeyValueListOfLists = new ArrayList<>();

        String jsonObjectFile = "src/main/resources/json/JsonObject.json";
        String jsonObjectStr = readFileAsString(jsonObjectFile);
        /* Append JsonObject Keys*/
        String appendJsonObjectKey = "jsonObject_";
        jsonKeyValueList = new ArrayList<>();
        for (int i = 1001 ; i <= 1010 ; i++) {
            /* Passing JsonObject.json JsonObject*/
            JsonObject jsonObject = new JsonObject(jsonObjectStr);
            jsonObject.put("uniqueKey",i);
            jsonKeyAsHeader = new ArrayList<>();
            jsonKeyValue = new LinkedHashMap<>();
            processJsonObject(jsonObject,appendJsonObjectKey);
            jsonKeyValueList.add(jsonKeyValue);
        }
        jsonKeyValueListOfLists.add(jsonKeyValueList);

        String jsonArrayFile = "src/main/resources/json/JsonArray.json";
        String jsonArrayStr = readFileAsString(jsonArrayFile);
        /* Append JsonArray Keys*/
        String appendJsonArrayKey = "jsonArray_";
        jsonKeyValueList = new ArrayList<>();
        for (int i = 1001 ; i <= 1010 ; i++) {
            /* Passing JsonArray.json JsonArray*/
            JsonArray jsonArray = new JsonArray(jsonArrayStr);
            jsonArray.getJsonObject(0).put("uniqueKey",i);
            jsonKeyAsHeader = new ArrayList<>();
            jsonKeyValue = new LinkedHashMap<>();
            processJsonArray(jsonArray,appendJsonArrayKey);
            jsonKeyValueList.add(jsonKeyValue);
        }
        jsonKeyValueListOfLists.add(jsonKeyValueList);

        jsonToCsvOrExcel(jsonKeyValueListOfLists,"src/main/resources/csvHeaders/MergeListHeaders.txt");
    }



    /**
     * @param jsonObjectInString Check whether valid JsonObject format
     * @return Return true if valid else false
     */
    private static boolean isJsonObjectValid(String jsonObjectInString) {
        try {
            JsonObject localJsonObject = new JsonObject(jsonObjectInString);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * @param jsonArrayInString Check whether valid JsonArray format, if so process each Json in the array
     * @param serviceName Append serviceName to CSV/Excel Header/Key
     * @return Return true if valid else false
     */
    private static boolean isJsonArrayValid(String jsonArrayInString, String serviceName) {
        try {
            JsonArray jsonArray = new JsonArray(jsonArrayInString);
            return processJsonArray(jsonArray, serviceName);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * @param jsonArray Process each Json in the array
     * @param serviceName Append serviceName to CSV/Excel Header/Key
     * @return return's true if json is processed
     *         return's false if @itsArrayNotJsonArray or if JsonArray has empty values like [], [{}], [{},{}], [{},{"a":"b"},{},{"c":"d"}] ...
     */
    private static boolean processJsonArray(JsonArray jsonArray, String serviceName) {

        /* Check if JsonArray has empty values like [], [{}], [{},{}], [{},{"a":"b"},{},{"c":"d"}] ... */
        int emptyValues = 0;

        /* Example key65": ["jsonObject_value65-1", "jsonObject_value65-2", "jsonObject_value65-3"] */
        boolean itsArrayNotJsonArray = true;

        for (int i = 0; i < jsonArray.size(); i++) {
            try
            {
                if (isJsonObjectValid(Objects.toString(jsonArray.getJsonObject(i)))) {
                    processJsonObject(jsonArray.getJsonObject(i), serviceName);
                    itsArrayNotJsonArray = false;
                    if(jsonArray.getJsonObject(i).size()==0){
                        emptyValues++;
                    }
                }
            } catch (Exception e) {
                /* Ignore this errors if JsonArray doesn't have valid JsonObject */
            }
        }

        if (emptyValues > 0 || itsArrayNotJsonArray) {
            return false;
        }

        return true;
    }

    /**
     * @param jsonObject Process @param jsonObject and store as Key - Value in HashMap @param jsonKeyValue
     *                   Key is @param headerKey
     *                   Value is @param a.getValue()
     * @param serviceName Append serviceName to CSV/Excel Header/Key
     */
    private static void processJsonObject(JsonObject jsonObject, String serviceName) {
        jsonObject.forEach(a -> {
                    jsonKeyAsHeader.add(a.getKey() + ":");
                    /* Check for empty values like [], [{}], [{},{}] ... */
                    if (isJsonArrayValid(Objects.toString(a.getValue()), serviceName) && new JsonArray(Objects.toString(a.getValue())).size()!=0) {
                        jsonKeyAsHeader.remove(jsonKeyAsHeader.size() - 1);
                        /* Check for empty values like {} */
                    } else if (isJsonObjectValid(Objects.toString(a.getValue())) && new JsonObject(Objects.toString(a.getValue())).size()!=0) {
                        processJsonObject(new JsonObject(Objects.toString(a.getValue())), serviceName);
                        jsonKeyAsHeader.remove(jsonKeyAsHeader.size() - 1);
                    } else {
                        /* @headerKey is the key, @a.getValue() is the value */
                        StringBuffer headerKey = new StringBuffer();
                        headerKey.append(serviceName);
                        for (String str : jsonKeyAsHeader) {
                            headerKey.append(str);
                        }
                        /* If key is there in HashMap, add new key with a counter appended. */
                        if(jsonKeyValue.containsKey(Objects.toString(headerKey)))
                        {
                            int count = 1;
                            headerKey = appendHeaderKey(count, serviceName);
                        }
                        //logger.debug(headerKey+"::-->:::");
                        //logger.debug(a.getValue());
                        jsonKeyValue.put(Objects.toString(headerKey), Objects.toString(a.getValue()));
                        jsonKeyAsHeader.remove(jsonKeyAsHeader.size() - 1);
                    }
                }
        );
    }

    /**
     * If JsonArray/JsonObject has same key name, add new key with a counter appended.
     * @param count
     * @param serviceName Append serviceName to CSV/Excel Header/Key
     * @return headerKey
     */
    private static StringBuffer appendHeaderKey(int count, String serviceName) {

        StringBuffer headerKey = new StringBuffer();
        headerKey.append(serviceName);
        for (String str : JsonToCsvOrExcel.jsonKeyAsHeader) {
            headerKey.append(str);
        }
        //logger.debug("Before :" + headerKey);

        headerKey.append(count);
        if(JsonToCsvOrExcel.jsonKeyValue.containsKey(Objects.toString(headerKey)))
        {
            headerKey = appendHeaderKey(++count, serviceName);
        }
        //logger.debug("After :"+headerKey);

        return headerKey;
    }

    /**
     * Merge Different Jsons and add in CSV/Excel as a single row based on common value.
     * @param jsonKeyValueListOfLists List of List of Maps(key-values); keys are added as header in CSV/Excel, values will be added as rows in CSV/Excel.
     * @param keysText Text file that contains Keys which are added as Header in CSV/Excel
     * @throws IOException
     */
    private static void jsonToCsvOrExcel(List<List<Map<String, String>>> jsonKeyValueListOfLists, String keysText) throws IOException {

        Set<String> allServiceHeaderKeySet = new LinkedHashSet<>();
        Set<String> headerKeySet = new LinkedHashSet<>();
        List<Set<String>> listOfHeaderKeySet = new ArrayList<>();
        Map<String, String> mergeJsonKeyValue = new LinkedHashMap<>();
        List<Map<String,String>> mergeJsonKeyValueList = new ArrayList<>();

        /* Read Keys if file is not empty*/
        Scanner sc = new Scanner(new File(keysText));
        while(sc.hasNext()){
            String headerKey = sc.nextLine();
            headerKeySet = buildCSVOrExcelColumns(allServiceHeaderKeySet, headerKeySet, listOfHeaderKeySet, headerKey);
        }
        sc.close();

        /* Add Set @param headerKeySet to List @param listOfHeaderKeySet, if it's not equal to zero and not equal to @param CSV_COLUMNS_SIZE*/
        if (headerKeySet.size() > 1)
        {
            listOfHeaderKeySet.add(headerKeySet);
        }

        if(headerKeySet.isEmpty())
        {
            PrintWriter headerWriter = new PrintWriter(keysText, "UTF-8");
            /*
               Read all HashMap Keys from ArrayList @param jsonKeyValueListOfLists, store in a Set @param headerKeySet and add as a header in CSV/Excel.
            */

            for(int i = 0; i < jsonKeyValueListOfLists.size(); i++) {
                for (int j = 0; j < jsonKeyValueListOfLists.get(i).size(); j++) {
                    for (String headerKey : jsonKeyValueListOfLists.get(i).get(j).keySet()) {

                        headerKeySet = buildCSVOrExcelColumns(allServiceHeaderKeySet, headerKeySet, listOfHeaderKeySet, headerKey);

                    }
                }
            }

            /* Add Set @param headerKeySet to List @param listOfHeaderKeySet, if it's not equal to zero and not equal to @param CSV_COLUMNS_SIZE*/
            if (headerKeySet.size() > 1)
            {
                listOfHeaderKeySet.add(headerKeySet);
            }

            /* Write the keys to the .txt files in csvHeaders folder */
            for (Set<String> headerKeySubSet: listOfHeaderKeySet) {
                for (String headerStr : headerKeySubSet) {
                    headerWriter.println(headerStr);
                }
            }

            headerWriter.close();
        }


        int forLoopCount=0;
        PrintWriter forLoopWriter = new PrintWriter("src/main/resources/text/forLoopCount.txt", "UTF-8");

        /* Recursive call
         *
         * i -> service1
         * j -> service2
         *
         * Compare 2 services
         * i=0 j=1
         *
         * Compare 3 services
         * i=0 j=1
         * i=0 j=2
         * i=1 j=2
         *
         * Compare 5 services
         *  i=0 j=1
         *  i=0 j=2
         *  i=0 j=3
         *  i=0 j=4
         *  i=1 j=2
         *  i=1 j=3
         *  i=1 j=4
         *  i=2 j=3
         *  i=2 j=4
         *  i=3 j=4
         * */
        for(int i = 0; i < jsonKeyValueListOfLists.size(); i++)
        {
            for (int j = i + 1; j < jsonKeyValueListOfLists.size(); j++)
            {
                for (int k = 0; k < jsonKeyValueListOfLists.get(i).size(); k++)
                {
                    for (int l = 0; l < jsonKeyValueListOfLists.get(j).size(); l++)
                    {
                        forLoopWriter.println("i -> "+i +" j -> "+j+ " k -> "+k+" l -> "+l);

                        /*logger.debug("i -> "+i +" j -> "+j+ " k -> "+k+" l -> "+l);
                        logger.debug(Objects.toString(jsonKeyValueListOfLists.get(i).get(k)));
                        logger.debug(Objects.toString(jsonKeyValueListOfLists.get(j).get(l)));*/

                        forLoopCount++;

                        if(StringUtils.equalsIgnoreCase(jsonKeyValueListOfLists.get(i).get(k).get("jsonObject_uniqueKey:"),jsonKeyValueListOfLists.get(j).get(l).get("jsonArray_uniqueKey:")))
                        {
                            /*logger.debug("i -> "+i +" j -> "+j+ " k -> "+k+" l -> "+l);
                            logger.debug("jsonObject_uniqueKey:" + jsonKeyValueListOfLists.get(i).get(k).get("jsonObject_uniqueKey:") + " " + "jsonArray_uniqueKey:" + jsonKeyValueListOfLists.get(j).get(l).get("jsonArray_uniqueKey:"));*/

                            boolean itsNewJsonKeyValue = true;

                            for (Map<String,String> jsonKeyValue : mergeJsonKeyValueList) {
                                if(jsonKeyValue.containsValue(jsonKeyValueListOfLists.get(i).get(k).get("jsonObject_uniqueKey:"))) {
                                    mergeJsonKeyValue = jsonKeyValue;
                                    itsNewJsonKeyValue = false;
                                    break;
                                }
                            }

                            if(itsNewJsonKeyValue)
                                mergeJsonKeyValue = new LinkedHashMap<>();

                            for(String list1Key : jsonKeyValueListOfLists.get(i).get(k).keySet()){
                                mergeJsonKeyValue.put(list1Key,jsonKeyValueListOfLists.get(i).get(k).get(list1Key));
                            }

                            for(String list2Key : jsonKeyValueListOfLists.get(j).get(l).keySet()){
                                mergeJsonKeyValue.put(list2Key,jsonKeyValueListOfLists.get(j).get(l).get(list2Key));
                            }

                            if(itsNewJsonKeyValue)
                                mergeJsonKeyValueList.add(mergeJsonKeyValue);

                        }
                    }

                }
            }
        }

        forLoopWriter.close();

        if(jsonKeyValueListOfLists.size() == 1)
        {
            mergeJsonKeyValueList = jsonKeyValueListOfLists.get(0);
        }

        logger.debug("<----------------------------------------->");

        if(jsonKeyValueListOfLists.size() > 1)
        {
            logger.debug("Total forLoopCount : " + forLoopCount);
        }

        logger.debug("Total number of rows in CSV/Excel : " + mergeJsonKeyValueList.size());
        logger.debug("Total number of Keys : " + allServiceHeaderKeySet.size());

        writeToCsv(listOfHeaderKeySet, mergeJsonKeyValueList);
        writeToExcel(listOfHeaderKeySet, mergeJsonKeyValueList);

    }

    /**
     * @param allServiceHeaderKeySet Set of all CSV/Excel Column Header Keys
     * @param headerKeySet SubSet of CSV/Excel Column Header Keys
     * @param listOfHeaderKeySet List of CSV/Excel Column Header Key Set
     * @param headerKey CSV/Excel Column Header Key
     * @return
     */
    private static Set<String> buildCSVOrExcelColumns(Set<String> allServiceHeaderKeySet, Set<String> headerKeySet, List<Set<String>> listOfHeaderKeySet, String headerKey) {
        if (!allServiceHeaderKeySet.contains(headerKey))
            headerKeySet.add(headerKey);

        allServiceHeaderKeySet.add(headerKey);

        /* Create a new @param headerKeySet object if current @param headerKeySet.size() is equal to @param CSV_COLUMNS_SIZE */
        if (headerKeySet.size() == CSV_OR_EXCEL_COLUMNS_SIZE) {
            listOfHeaderKeySet.add(headerKeySet);
            headerKeySet = new LinkedHashSet<>();
        }
        return headerKeySet;
    }

    /**
     * @param listOfHeaderKeySet Add as Column Header in CSV/Excel.
     * @param mergeJsonKeyValueList Add as Rows in CSV/Excel.
     */
    private static void writeToCsv(List<Set<String>> listOfHeaderKeySet, List<Map<String,String>> mergeJsonKeyValueList ) throws IOException {

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss.SSSZ");
        String csvName = "src/main/resources/csv/csv_" + sdf.format(timestamp) + ".csv";
        File csvFile = new File(csvName);
        FileWriter csvFileWriter = new FileWriter(csvFile, true);
        CSVWriter csvWriter = new CSVWriter(csvFileWriter);

        LocalDateTime startTime = LocalDateTime.now();
        for(int numberOfCsv = 0 ; numberOfCsv < listOfHeaderKeySet.size() ; numberOfCsv++)
        {
            /* Skip for first CSV */
            if(numberOfCsv != 0) {
                csvName = "src/main/resources/csv/csv_" + sdf.format(timestamp) + "_" + numberOfCsv + ".csv";
                csvFile = new File(csvName);
                csvFileWriter = new FileWriter(csvFile, true);
                csvWriter = new CSVWriter(csvFileWriter);
            }

            String[] headerStrArray = new String[listOfHeaderKeySet.get(numberOfCsv).size()];
            int headerCount = 0;
            for (String headerStr : listOfHeaderKeySet.get(numberOfCsv)) {
                if(Objects.isNull(headerStr)) {
                    headerStrArray[headerCount++] = null;
                } else if(Objects.nonNull(headerStr) && headerStr.length() <= 255) {
                    headerStrArray[headerCount++] = headerStr;
                } else {
                    headerStrArray[headerCount++] = headerStr.substring(0,254);
                }
            }
            csvWriter.writeNext(headerStrArray);

            /* Read List of Maps @param mergeJsonKeyValueList and Add as rows in CSV. */
            for (Map<String,String> jsonKeyValue : mergeJsonKeyValueList)
            {
                String[] csvRowArray = new String[listOfHeaderKeySet.get(numberOfCsv).size()];
                int i = 0;
                for (String key : listOfHeaderKeySet.get(numberOfCsv)) {
                    if(jsonKeyValue.containsKey(key)) {
                        if(Objects.isNull(jsonKeyValue.get(key))) {
                            csvRowArray[i++] = jsonKeyValue.get(key);
                        } else if(Objects.nonNull(jsonKeyValue.get(key)) && jsonKeyValue.get(key).length() <= 32767) {
                            csvRowArray[i++] = jsonKeyValue.get(key);
                        } else {
                            csvRowArray[i++] = jsonKeyValue.get(key).substring(0,32766);
                        }
                    } else i++;
                }
                csvWriter.writeNext(csvRowArray);
            }
            csvWriter.close();
        }
        logger.debug("Generated CSV, Total time took: " + startTime.until(LocalDateTime.now(), ChronoUnit.MILLIS));

    }

    /**
     * @param listOfHeaderKeySet Add as Column Header in CSV/Excel.
     * @param mergeJsonKeyValueList Add as Rows in CSV/Excel.
     * @throws IOException
     */
    private static void writeToExcel(List<Set<String>> listOfHeaderKeySet, List<Map<String,String>> mergeJsonKeyValueList ) throws IOException {

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        DateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss.SSSZ");
        Workbook workbook = new SXSSFWorkbook();

        LocalDateTime startTime = LocalDateTime.now();

        for(int sheet = 0 ; sheet < listOfHeaderKeySet.size() ; sheet++)
        {
            int rowCounter = 0;
            Sheet workbookSheet = workbook.createSheet("Sheet" + Objects.toString(sheet+1));
            Row headerRow = workbookSheet.createRow(rowCounter++);
            int rowCount = 0;
            for (String headerKey : listOfHeaderKeySet.get(sheet)) {
                Cell cell = headerRow.createCell(rowCount++);
                if(Objects.isNull(headerKey)) {
                    cell.setCellValue((String) null);
                } else if(Objects.nonNull(headerKey) && headerKey.length() <= 255) {
                    cell.setCellValue(headerKey);
                } else {
                    cell.setCellValue(headerKey.substring(0,254));
                }
            }

            for (Map<String, String> jsonKeyValue : mergeJsonKeyValueList) {
                Row row = workbookSheet.createRow(rowCounter++);
                rowCount = 0;
                for (String key : listOfHeaderKeySet.get(sheet)) {
                    if (jsonKeyValue.containsKey(key)) {
                        Cell cell = row.createCell(rowCount++);
                        if(Objects.isNull(jsonKeyValue.get(key))) {
                            cell.setCellValue(jsonKeyValue.get(key));
                        } else if(Objects.nonNull(jsonKeyValue.get(key)) && jsonKeyValue.get(key).length() <= 32767) {
                            cell.setCellValue(jsonKeyValue.get(key));
                        } else {
                            cell.setCellValue(jsonKeyValue.get(key).substring(0,32766));
                        }
                    } else {
                        row.createCell(rowCount++);
                    }
                }
            }
        }

        FileOutputStream fileOut = new FileOutputStream("src/main/resources/xlsx/xls_" + sdf.format(timestamp) + ".xlsx");
        workbook.write(fileOut);
        fileOut.flush();
        fileOut.close();
        workbook.close();

        logger.debug("Generated Excel, Total time took: " + startTime.until(LocalDateTime.now(), ChronoUnit.MILLIS));
        logger.debug("<----------------------------------------->");

    }

    /**
     * @param file Read File as a String
     * @return
     */
    private static String readFileAsString(String file) {
        try {
            return new String(Files.readAllBytes(Paths.get(file)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "readFileAsString failed";
    }

}
