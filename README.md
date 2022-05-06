# JsonToCsv
Convert dynamic JsonArray/JsonObject to CSV file, it doesn't depend on Schema.

CSV headers/Column headers will be added as Json hierarchy Key and the Values of dynamic JsonArray/JsonObject will be added as row in the CSV and will be mapped to the right CSV header.

The JsonArray/JsonObject should have one of this valid value (JSON String, Number, Array, Object or token 'null', 'true' or 'false')