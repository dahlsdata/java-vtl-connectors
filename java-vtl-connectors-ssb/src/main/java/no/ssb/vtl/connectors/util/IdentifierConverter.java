package no.ssb.vtl.connectors.util;

/*-
 * ========================LICENSE_START=================================
 * Java VTL Utility connectors
 * %%
 * Copyright (C) 2017 Statistics Norway and contributors
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;

public class IdentifierConverter {
    
    public static String toQueryString(String json) throws IOException {
        StringBuilder sb = new StringBuilder();
        JsonNode jsonNode = new ObjectMapper().readTree(json);
        JsonNode query = jsonNode.get("query");
        if (query.size() > 0) {
            for (int q = 0; q < query.size(); q++) {
                if (q > 0) {
                    sb.append("&");
                }
                JsonNode queryPart = query.get(q);
                sb.append(trimQuotes(queryPart.get("code")));
                
                sb.append("=");
                JsonNode selection = queryPart.get("selection");
                JsonNode values = selection.get("values");
                StringBuilder valuesBuilder = new StringBuilder();
                for (int v = 0; v < values.size(); v++) {
                    if (v > 0) {
                        valuesBuilder.append("+");
                    }
                    JsonNode value = values.get(v);
                    valuesBuilder.append(trimQuotes(value));
                    
                }
                JsonNode filter = selection.get("filter");
                String format = "%s";
                if (trimQuotes(filter).equals("all")) {
                    format = "all(%s)";
                } else if (trimQuotes(filter).equals("top")) {
                    format = "top(%s)";
                }
                sb.append(String.format(format, valuesBuilder.toString()));
            }
        }
        return sb.toString();
        
        
    }
    
    
    
    public static JsonNode toJson(String query) {
        JsonNodeFactory factory = JsonNodeFactory.instance;
        ObjectNode json = factory.objectNode();
        ArrayNode array = json.putArray("query");
        String[] parameters = query.split("&");
        for (String parameter : parameters) {
            ObjectNode queryPart = array.addObject();
            String[] keyValue = parameter.split("=");
            String key = keyValue[0];
            String value = keyValue[1];
            queryPart.put("code", key);
            ObjectNode selection = queryPart.putObject("selection");
            if (value.matches("all\\(.*\\)")) {
                selection.put("filter", "all");
                value = value.substring(4, value.length() - 1);
            } else if (value.matches("top\\(.*\\)")) {
                selection.put("filter", "top");
                value = value.substring(4, value.length() - 1);
            } else {
                selection.put("filter", "item");
            }
            ArrayNode valuesNode = selection.putArray("values");
            String[] values = value.split("\\+");
            for (String value1 : values) {
                valuesNode.add(value1);
            }
            
            
        }
        ObjectNode response = json.putObject("response");
        response.put("format", "json-stat");
        return json;
    }
    
    private static String trimQuotes(JsonNode node) {
        return node.toString().replaceAll("\"", "");
    }
    
}























