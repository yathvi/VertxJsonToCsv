import com.opencsv.CSVWriter;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

/***
 * Convert dynamic/nested JsonArray/JsonObject to CSV file, it doesn't depend on Schema.
 * CSV headers/Column headers will be added as Json hierarchy Key
 * and the Values of dynamic/nested JsonArray/JsonObject will be added as row in the CSV
 * and will be mapped to the right CSV header.
 */
public class JsonToCsv {
    /* CSV Header */
    private static List<String> jsonKeyAsHeader;

    /* JSON Key-Value, Keys added as header in CSV, Values added as row in CSV
    *  Using LinkedHashMap to maintain order of insertion  */
    private static Map<String, String> jsonKeyValue;

    /* List of JSON Key-Value, Keys added as header in CSV, Values added as row in CSV */
    private static List<Map<String, String>> jsonKeyValueList;

    private static List<List<Map<String, String>>> jsonKeyValueListOfLists;

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
     * Trigger convertJsontoCsv by passing ArrayList @param jsonKeyValueList.
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

        jsonToCsv(jsonKeyValueListOfLists,"src/main/resources/csvHeaders/CsvListHeaders.txt");
    }

    /**
     * Read JsonObject.json; store the key - value in Hashmap @param jsonKeyValue,
     * add this multiple Hashmap @param jsonKeyValue to ArrayList @param jsonKeyValueList.
     *
     * Read JsonArray.json; store the key - value in Hashmap @param jsonKeyValue,
     * add this multiple Hashmap @param jsonKeyValue to ArrayList @param jsonKeyValueList_2.
     *
     * Trigger mergeJsontoCsv by passing ArrayList @param jsonKeyValueList and ArrayList @param jsonKeyValueList_2.
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

        jsonToCsv(jsonKeyValueListOfLists,"src/main/resources/csvHeaders/MergeCsvListHeaders.txt");
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
     * @return Return true if valid else false
     */

    private static boolean isJsonArrayValid(String jsonArrayInString, String json) {
        try {
            JsonArray jsonArray = new JsonArray(jsonArrayInString);
            return processJsonArray(jsonArray,json);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * @param jsonArray Process each Json in the array
     * @param json if different json has same key name, use this param which will append string to differentiate them.
     * @return return's true if json is processed
     *         return's false if @itsArrayNotJsonArray or if JsonArray has empty values like [], [{}], [{},{}], [{},{"a":"b"},{},{"c":"d"}] ...
     */
    private static boolean processJsonArray(JsonArray jsonArray, String json) {

        /* Check if JsonArray has empty values like [], [{}], [{},{}], [{},{"a":"b"},{},{"c":"d"}] ... */
        int emptyValues = 0;

        /* Example key65": ["jsonObject_value65-1", "jsonObject_value65-2", "jsonObject_value65-3"] */
        boolean itsArrayNotJsonArray = true;

        for (int i = 0; i < jsonArray.size(); i++) {
            try
            {
                if (isJsonObjectValid(String.valueOf(jsonArray.getJsonObject(i)))) {
                    processJsonObject(jsonArray.getJsonObject(i),json);
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
     *                   Key is @headerKey
     *                   Value is @param a.getValue()
     * @param json if different json has same key name, use this param which will append string to differentiate them.
     */
    private static void processJsonObject(JsonObject jsonObject, String json) {
        jsonObject.forEach(a -> {
            jsonKeyAsHeader.add(a.getKey() + ":");
            /* Check for empty values like [], [{}], [{},{}] ... */
            if (isJsonArrayValid(String.valueOf(a.getValue()),json) && new JsonArray(String.valueOf(a.getValue())).size()!=0){
                jsonKeyAsHeader.remove(jsonKeyAsHeader.size() - 1);
            /* Check for empty values like {} */
            } else if (isJsonObjectValid(String.valueOf(a.getValue())) && new JsonObject(String.valueOf(a.getValue())).size()!=0) {
                processJsonObject(new JsonObject(String.valueOf(a.getValue())),json);
                jsonKeyAsHeader.remove(jsonKeyAsHeader.size() - 1);
            } else {
                /* @headerKey is the key, @a.getValue() is the value */
                StringBuilder headerKey = new StringBuilder();
                headerKey.append(json);
                for (String str : jsonKeyAsHeader) {
                    headerKey.append(str);
                }
                /* If key is there in HashMap, add new key with a counter appended. */
                if(jsonKeyValue.containsKey(String.valueOf(headerKey)))
                {
                    int count = 1;
                    headerKey = appendHeaderKey(count,json);
                }
                //System.out.print(headerKey+"::-->:::");
                //System.out.println(a.getValue());
                jsonKeyValue.put(String.valueOf(headerKey), String.valueOf(a.getValue()));
                jsonKeyAsHeader.remove(jsonKeyAsHeader.size() - 1);
            }
        });
    }

    /**
     * If JsonArray/JsonObject has same key name, add new key with a counter appended.
     * @param count
     * @return
     */

    private static StringBuilder appendHeaderKey(int count, String json) {
        StringBuilder headerKey = new StringBuilder();
        headerKey.append(json);
        for (String str : jsonKeyAsHeader) {
            headerKey.append(str);
        }
        //System.out.println("Before :" + headerKey);
        
        headerKey.append(count);
        if(jsonKeyValue.containsKey(String.valueOf(headerKey)))
        {
            headerKey = appendHeaderKey(++count,json);
        }
        //System.out.println("After :"+headerKey);
        
        return headerKey;
    }


    /**
     * Merge Different Jsons and add in Csv as a single row based on common value.
     * @param jsonKeyValueListOfLists List of List of Maps(key-values); keys are added as header in CSV, values will be added as rows in CSV.
     * @param keysText Text file that contains Keys which are added as Header in CSV
     * @throws IOException
     */

    private static void jsonToCsv(List<List<Map<String, String>>> jsonKeyValueListOfLists, String keysText) throws IOException {

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss.SSSZ");
        File listFile = new File("src/main/resources/csv/csv_"+sdf.format(timestamp)+".csv");
        FileWriter listFileWriter = new FileWriter(listFile, true);
        CSVWriter csvWriter = new CSVWriter(listFileWriter);
        Set<String> headerStrSet = new LinkedHashSet<>();
        List<Map<String,String>> mergeJsonKeyValueList;

        /* Read Keys if file is not empty*/
        Scanner sc = new Scanner(new File(keysText));
        while(sc.hasNext()){
            String line = sc.nextLine();
            headerStrSet.add(line);
        }
        sc.close();

        if(headerStrSet.isEmpty())
        {
            PrintWriter headerWriter = new PrintWriter(keysText, "UTF-8");
            /*
               Read all HashMap Keys from ArrayList @param jsonKeyValueListOfLists, store in a Set @param headerStrSet and add as a header in CSV.
            */

            for(int i = 0; i < jsonKeyValueListOfLists.size(); i++) {
                for (int j = 0; j < jsonKeyValueListOfLists.get(i).size(); j++) {
                    for (String headerStr : jsonKeyValueListOfLists.get(i).get(j).keySet()) {
                        headerStrSet.add(headerStr);
                    }
                }
            }

            /* Write the keys to the .txt files in csvHeaders folder */
            for (String headerStr : headerStrSet) {
                headerWriter.println(headerStr);
            }
            headerWriter.close();

        }

        /* Convert Set @headerStrSet to String array @headerStrArray and add as a header in CSV. */
        String[] headerStrArray = new String[headerStrSet.size()];
        int headerCount = 0;
        for (String headerStr : headerStrSet) {
            headerStrArray[headerCount++] = headerStr;
        }
        csvWriter.writeNext(headerStrArray);


        Map<String, String> mergeJsonKeyValue = new LinkedHashMap<>();
        mergeJsonKeyValueList = new ArrayList<>();
        int forLoopCount=0;
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
                        /*System.out.println("i -> "+i +" j -> "+j+ " k -> "+k+" l -> "+l);
                        System.out.println(jsonKeyValueListOfLists.get(i).get(k));
                        System.out.println(jsonKeyValueListOfLists.get(j).get(l));
                        System.out.println();*/

                        forLoopCount++;

                        if(StringUtils.equalsIgnoreCase(jsonKeyValueListOfLists.get(i).get(k).get("jsonObject_uniqueKey:"),jsonKeyValueListOfLists.get(j).get(l).get("jsonArray_uniqueKey:")))
                        {
                            System.out.println("i -> "+i +" j -> "+j+ " k -> "+k+" l -> "+l);
                            System.out.println("jsonObject_uniqueKey:" + jsonKeyValueListOfLists.get(i).get(k).get("jsonObject_uniqueKey:") + " " + "jsonArray_uniqueKey:" + jsonKeyValueListOfLists.get(j).get(l).get("jsonArray_uniqueKey:"));
                            System.out.println();
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
        /*System.out.println("total forLoopCount "+ forLoopCount);
        System.out.println(mergeJsonKeyValueList.size());
        System.out.println(mergeJsonKeyValueList);*/


        if(jsonKeyValueListOfLists.size() == 1)
        {
            /* Add each HashMap values as a row in CSV. */

            /*jsonKeyValueListOfLists.get(0).forEach(map->{
                List<String> stringValueArray = new ArrayList<>();
                headerStrSet.forEach(set->stringValueArray.add(map.get(set)));
                csvWriter.writeNext(stringValueArray.stream().toArray(String[]::new));
            });*/

            for (int i = 0; i < jsonKeyValueListOfLists.get(0).size(); i++) {
                String[] csvRowArray = new String[headerStrSet.size()];
                int j = 0;
                for (String key: headerStrSet) {
                    csvRowArray[j++] = jsonKeyValueListOfLists.get(0).get(i).get(key);
                }
                csvWriter.writeNext(csvRowArray);
            }
        }
        else
        {
            System.out.println("total forLoopCount --> " + forLoopCount);
            System.out.println("mergeJsonKeyValueList.size() --> " + mergeJsonKeyValueList.size());

            for (Map<String,String> jsonKeyValue : mergeJsonKeyValueList) {
                String[] csvRowArray = new String[headerStrSet.size()];
                int i = 0;
                for (String key : headerStrSet) {
                    if(jsonKeyValue.containsKey(key)) {
                        csvRowArray[i++] = jsonKeyValue.get(key);
                    }
                    else i++;
                }
                csvWriter.writeNext(csvRowArray);
            }
        }
        csvWriter.close();
    }

    /**
     *
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
