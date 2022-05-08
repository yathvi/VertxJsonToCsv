import com.opencsv.CSVWriter;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class JsonToCsv {
    /* CSV Header */
    private static ArrayList<String> listToCsvHeader;

    /* JSON Key-Value, Keys added as header in CSV, Values added as row in CSV  */
    private static HashMap<String, String> listToCsvMap;

    /* List of JSON Key-Value, Keys added as header in CSV, Values added as row in CSV */
    private static ArrayList<HashMap<String, String>> listToCsvArrayList;
    private static ArrayList<HashMap<String, String>> listToCsvArrayList_2;

    public static void main(String[] args) throws IOException {

        addDifferentJsonsToCsv();
        mergeDifferentJsonsAndAddInCsv();
    }

    private static void addDifferentJsonsToCsv() throws IOException {
        listToCsvArrayList = new ArrayList<>();

        String json1File = "src/main/resources/json1.json";
        String json1Str = readFileAsString(json1File);
        /* Append Json1 Keys*/
        //String appendJson1Key = "json1_";
        String appendJson1Key = "";
        /* Passing json1.json JsonObject*/
        JsonObject json1Object = new JsonObject(json1Str);

        listToCsvHeader = new ArrayList<>();
        listToCsvMap = new HashMap<>();
        processJsonObject(json1Object,appendJson1Key);
        for (int i =0 ; i < 200 ; i++)
            listToCsvArrayList.add(listToCsvMap);

        String json2File = "src/main/resources/json2.json";
        String json2Str = readFileAsString(json2File);
        /* Append Json2 Keys*/
        //String appendJson2Key = "json2_";
        String appendJson2Key = "";
        /* Passing json2.json JsonArray*/
        JsonArray json2Array = new JsonArray(json2Str);

        listToCsvHeader = new ArrayList<>();
        listToCsvMap = new HashMap<>();
        processJsonArray(json2Array,appendJson2Key);
        for (int i =0 ; i < 200 ; i++)
            listToCsvArrayList.add(listToCsvMap);

        convertJsontoCsv(listToCsvArrayList);
    }

    private static void mergeDifferentJsonsAndAddInCsv() throws IOException {
        listToCsvArrayList = new ArrayList<>();
        listToCsvArrayList_2 = new ArrayList<>();

        String json1File = "src/main/resources/json1.json";
        String json1Str = readFileAsString(json1File);
        /* Append Json1 Keys*/
        String appendJson1Key = "json1_";
        /* Passing json1.json JsonObject*/
        JsonObject json1Object = new JsonObject(json1Str);

        listToCsvHeader = new ArrayList<>();
        listToCsvMap = new HashMap<>();
        processJsonObject(json1Object,appendJson1Key);
        for (int i =0 ; i < 10 ; i++)
            listToCsvArrayList.add(listToCsvMap);

        String json2File = "src/main/resources/json2.json";
        String json2Str = readFileAsString(json2File);
        /* Append Json2 Keys*/
        String appendJson2Key = "json2_";
        /* Passing json2.json JsonArray*/
        JsonArray json2Array = new JsonArray(json2Str);

        listToCsvHeader = new ArrayList<>();
        listToCsvMap = new HashMap<>();
        processJsonArray(json2Array,appendJson2Key);
        for (int i =0 ; i < 10 ; i++)
            listToCsvArrayList_2.add(listToCsvMap);

        mergeJsontoCsv(listToCsvArrayList,listToCsvArrayList_2);
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
     * @param jsonArrayInString Check whether valid JsonArray format
     * @return Return true if valid else false
     */

    private static boolean isJsonArrayValid(String jsonArrayInString) {
        try {
            JsonArray localJsonArray = new JsonArray(jsonArrayInString);
            for (int i = 0; i < localJsonArray.size(); i++) {
                if (!isJsonObjectValid(localJsonArray.getJsonObject(i).toString()))
                    return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * @param jsonArray Process each Json in the array
     * @param json if different json has same key name, use this param which will append string to differentiate them.
     */
    private static void processJsonArray(JsonArray jsonArray, String json) {
        for (int i = 0; i < jsonArray.size(); i++) {
            processJsonObject(jsonArray.getJsonObject(i),json);
        }
    }

    /**
     * @param jsonObject Process @param jsonObject and store as Key - Value in HashMap @param listToCsvMap
     *                   Key is @headerKey
     *                   Value is @param a.getValue()
     * @param json if different json has same key name, use this param which will append string to differentiate them.
     */
    private static void processJsonObject(JsonObject jsonObject, String json) {
        jsonObject.forEach(a -> {
                    listToCsvHeader.add(a.getKey() + ":");
                    if (isJsonArrayValid(String.valueOf(a.getValue()))) {
                        processJsonArray(new JsonArray(String.valueOf(a.getValue())),json);
                        listToCsvHeader.remove(listToCsvHeader.size() - 1);
                    } else if (isJsonObjectValid(String.valueOf(a.getValue()))) {
                        processJsonObject(new JsonObject(String.valueOf(a.getValue())),json);
                        listToCsvHeader.remove(listToCsvHeader.size() - 1);
                    } else {
                        /* @headerKey is the key, @a.getValue() is the value */
                        StringBuilder headerKey = new StringBuilder();
                        headerKey.append(json);
                        for (String str : listToCsvHeader) {
                            headerKey.append(str);
                        }
                        //System.out.print(headerKey+"::-->:::");
                        //System.out.println(a.getValue());
                        listToCsvMap.put(String.valueOf(headerKey), String.valueOf(a.getValue()));
                        listToCsvHeader.remove(listToCsvHeader.size() - 1);
                    }
                }
        );
    }

    /**
     * @param listToCsvArrayList ArrayList of HashMaps(key-values),
     *                              keys are added as header in CSV,
     *                              values will be added as rows in CSV.
     * @throws IOException
     */
    private static void convertJsontoCsv(ArrayList<HashMap<String, String>> listToCsvArrayList) throws IOException {

        Path fileToDeletePath = Paths.get("src/main/resources/CsvList.csv");
        Files.deleteIfExists(fileToDeletePath);
        File listFile = new File("src/main/resources/CsvList.csv");
        FileWriter listFileWriter = new FileWriter(listFile, true);
        CSVWriter csvWriter = new CSVWriter(listFileWriter);
        Set<String> headerStrSet = new HashSet<>();

        /*
         Read all HashMap Keys from ArrayList, store in a Set @headerStrSet and add as a header in CSV.
         */

        /*listToCsvArrayList.forEach(map-> headerStrSet.addAll(map.keySet()));
          csvWriter.writeNext(headerStrSet.stream().toArray(String[]::new));*/

        for (int i = 0; i < listToCsvArrayList.size(); i++) {
            for (String headerStr : listToCsvArrayList.get(i).keySet()) {
                headerStrSet.add(headerStr);
            }
        }
        /* Convert Set @headerStrSet to String array @headerStrArray  */
        String[] headerStrArray = new String[headerStrSet.size()];
        int headerCount = 0;
        for (String headerStr : headerStrSet) {
            headerStrArray[headerCount++] = headerStr;
        }
        csvWriter.writeNext(headerStrArray);


        /* Add each HashMap values as a row in CSV. */

           /* listToCsvArrayList.forEach(map->{
                ArrayList<String> stringValueArray = new ArrayList<>();
                headerStrSet.forEach(set->stringValueArray.add(map.get(set)));
                csvWriter.writeNext(stringValueArray.stream().toArray(String[]::new));
            });*/

        for (int i = 0; i < listToCsvArrayList.size(); i++) {
            String[] mapValueArray = new String[headerStrSet.size()];
            int j = 0;
            for (String key : headerStrSet) {
                mapValueArray[j++] = listToCsvArrayList.get(i).get(key);
            }
            csvWriter.writeNext(mapValueArray);
        }
        csvWriter.close();
    }

    /**
     *
     * @param listToCsvArrayList ArrayList of HashMaps(key-values); first Json, keys are added as header in CSV, values will be added as rows in CSV.
     * @param listToCsvArrayList_2 ArrayList of HashMaps(key-values); second Json, keys are added as header in CSV, values will be added as rows in CSV.
     * @throws IOException
     */
    private static void mergeJsontoCsv(ArrayList<HashMap<String, String>> listToCsvArrayList, ArrayList<HashMap<String, String>> listToCsvArrayList_2) throws IOException {

        Path fileToDeletePath = Paths.get("src/main/resources/MergeCsvList.csv");
        Files.deleteIfExists(fileToDeletePath);
        File listFile = new File("src/main/resources/MergeCsvList.csv");
        FileWriter listFileWriter = new FileWriter(listFile, true);
        CSVWriter csvWriter = new CSVWriter(listFileWriter);
        Set<String> headerStrSet = new HashSet<>();

        /*
            Read all HashMap Keys from ArrayList @param listToCsvArrayList, store in a Set @param headerStrSet and add as a header in CSV.
        */

        for (int i = 0; i < listToCsvArrayList.size(); i++) {
            for (String headerStr : listToCsvArrayList.get(i).keySet()) {
                headerStrSet.add(headerStr);
            }
        }

        /*
            Read all HashMap Keys from ArrayList @param listToCsvArrayList_2, store in a Set @param headerStrSet and add as a header in CSV.
        */

        for (int i = 0; i < listToCsvArrayList_2.size(); i++) {
            for (String headerStr : listToCsvArrayList_2.get(i).keySet()) {
                headerStrSet.add(headerStr);
            }
        }

        /* Convert Set @headerStrSet to String array @headerStrArray  */
        String[] headerStrArray = new String[headerStrSet.size()];
        int headerCount = 0;
        for (String headerStr : headerStrSet) {
            headerStrArray[headerCount++] = headerStr;
        }
        csvWriter.writeNext(headerStrArray);


        /* If key10 value of Json1 and Json2 match, then merge Json1 and Json2 as a row in CSV. */

        for (int i = 0; i < listToCsvArrayList.size(); i++) {
            for (int j = 0; j < listToCsvArrayList_2.size(); j++) {
                if(listToCsvArrayList.get(i).containsKey("json1_key2:key3:key6:key10:")
                        &&
                        listToCsvArrayList_2.get(j).containsKey("json2_key5:key6:key10:")
                        &&
                        listToCsvArrayList.get(i).get("json1_key2:key3:key6:key10:")
                                .equals(listToCsvArrayList_2.get(j).get("json2_key5:key6:key10:"))) {

                    String[] mapValueArray = new String[headerStrSet.size()];
                    int k = 0;
                    for (String key : headerStrSet) {
                        if (listToCsvArrayList.get(i).containsKey(key))
                            mapValueArray[k++] = listToCsvArrayList.get(i).get(key);
                        else if (listToCsvArrayList_2.get(j).containsKey(key))
                            mapValueArray[k++] = listToCsvArrayList_2.get(j).get(key);
                    }
                    csvWriter.writeNext(mapValueArray);
                }
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
