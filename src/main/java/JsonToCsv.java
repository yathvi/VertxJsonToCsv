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

        /* Passing json.json present in resources as a string*/
        JsonObject jsonObject = new JsonObject("{\"key\":\"value\",\"key1\":12345,\"key2\":{\"key3\":[{\"key4\":\"\",\"key5\":\"value5\",\"key6\":{\"key7\":\"value7\",\"key8\":\"value8\",\"key9\":\"value9\",\"key10\":\"value10\"},\"key11\":{\"key12\":{\"key13\":\"value13\",\"key14\":\"value14\"},\"key15\":{\"key16\":\"value16\"},\"key17\":{\"key18\":\"value18\"},\"key19\":{\"key20\":[{\"key21\":\"value21\",\"key22\":\"value22\",\"key23\":\"value23\",\"key24\":\"value24\",\"key25\":\"value25\",\"key26\":\"value26\"}],\"key27\":[],\"key28\":{\"key29\":{\"key30\":\"value30\",\"key31\":\"value31\"}}},\"key32\":\"value32\",\"key33\":\"value33\",\"key34\":{\"key35\":\"value35\",\"key36\":\"value36\"}},\"key37\":\"value37\",\"key38\":{\"key39\":{\"key40\":\"value40\",\"key41\":\"value41\"},\"key42\":{\"key43\":\"value43\",\"key44\":\"value44\"},\"key45\":{\"key46\":\"value46\",\"key47\":\"value47\"}},\"key48\":\"value48\",\"key49\":\"value49\",\"key50\":true,\"key51\":false,\"key52\":[{\"key53\":{\"key54\":\"value54\",\"key55\":\"value55\"},\"key56\":\"value56\"}],\"key57\":\"value57\",\"key58\":null,\"key59\":\"value59\",\"key60\":{\"key61\":\"value61\"}}],\"key62\":\"value62\",\"key63\":\"value63\",\"key64\":\"3\",\"key65\":[\"value65-1\",\"value65-2\",\"value65-3\"],\"key66\":\"value66\",\"key67\":false,\"key68\":false,\"key69\":\"value69\",\"key70\":\"value70\",\"key71\":\"value71\",\"key72\":{\"key73\":\"value73\",\"key74\":\"value74\",\"key75\":\"value75\"},\"key76\":{\"key77\":\"value77\",\"key78\":[\"value78\"],\"key79\":null},\"key80\":\"80\"}}");

        listToCsvHeader = new ArrayList<>();
        listToCsvMap = new HashMap<>();
        processJsonObject(jsonObject);
        for (int i =0 ; i < 300 ; i++)
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
}