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
    private static ArrayList<String> listToCsvHeader;
    private static HashMap<String, String> listToCsvMap;
    private static ArrayList<HashMap<String, String>> listToCsvArrayList;

    public static void main(String[] args) throws IOException {

        listToCsvArrayList = new ArrayList<>();

        String json1File = "src/main/resources/json1.json";
        String json1Str = readFileAsString(json1File);
        /* Passing json1.json JsonObject*/
        JsonObject json1Object = new JsonObject(json1Str);

        listToCsvHeader = new ArrayList<>();
        listToCsvMap = new HashMap<>();
        processJsonObject(json1Object);
        for (int i =0 ; i < 500 ; i++)
            listToCsvArrayList.add(listToCsvMap);

        String json2File = "src/main/resources/json2.json";
        String json2Str = readFileAsString(json2File);
        /* Passing json2.json JsonArray*/
        JsonArray json2Array = new JsonArray(json2Str);

        listToCsvHeader = new ArrayList<>();
        listToCsvMap = new HashMap<>();
        processJsonArray(json2Array);
        for (int i =0 ; i < 500 ; i++)
            listToCsvArrayList.add(listToCsvMap);

        convertJsontoCsv(listToCsvArrayList);
    }


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
     */
    private static void processJsonArray(JsonArray jsonArray) {
        for (int i = 0; i < jsonArray.size(); i++) {
            processJsonObject(jsonArray.getJsonObject(i));
        }
    }

    /**
     * @param jsonObject Process @param jsonObject and store as Key - Value in HashMap @param listToCsvMap
     *                   Key is @headerKey
     *                   Value is @param a.getValue()
     */
    private static void processJsonObject(JsonObject jsonObject) {
        jsonObject.forEach(a -> {
                    listToCsvHeader.add(a.getKey() + ":");
                    if (isJsonArrayValid(String.valueOf(a.getValue()))) {
                        processJsonArray(new JsonArray(String.valueOf(a.getValue())));
                        listToCsvHeader.remove(listToCsvHeader.size() - 1);
                    } else if (isJsonObjectValid(String.valueOf(a.getValue()))) {
                        processJsonObject(new JsonObject(String.valueOf(a.getValue())));
                        listToCsvHeader.remove(listToCsvHeader.size() - 1);
                    } else {
                        /* @headerKey is the key, @a.getValue() is the value */
                        StringBuilder headerKey = new StringBuilder();
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
     */
    private static void convertJsontoCsv(ArrayList<HashMap<String, String>> listToCsvArrayList) throws IOException {

        Path fileToDeletePath = Paths.get("src/main/resources/CsvList.csv");
        Files.deleteIfExists(fileToDeletePath);
        File listFile = new File("src/main/resources/CsvList.csv");
        FileWriter listFileWriter = new FileWriter(listFile, true);
        CSVWriter csvWriter = new CSVWriter(listFileWriter);
        Set<String> headerStrSet = new HashSet<>();

            /*
              Read all HashMap Keys from ArrayList, store in a Set @headerStrSet
              and add as a header in CSV.
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
            for (String value : headerStrSet) {
                mapValueArray[j++] = listToCsvArrayList.get(i).get(value);
            }
            csvWriter.writeNext(mapValueArray);
        }
        csvWriter.close();
    }

    private static String readFileAsString(String file) {
        try {
            return new String(Files.readAllBytes(Paths.get(file)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "readFileAsString failed";
    }
}