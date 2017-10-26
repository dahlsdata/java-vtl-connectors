package no.ssb.vtl.connectors;

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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import no.ssb.jsonstat.v2.DatasetBuildable;
import no.ssb.vtl.connectors.util.IdentifierConverter;
import no.ssb.vtl.model.Dataset;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;

import static java.lang.String.*;

public class PxApiConnector extends JsonStatConnector{
    
    private static final String BASE_URL = "http://data.ssb.no/api/v0/no/table/";
    
    @Override
    public boolean canHandle(String identifier) {
        return identifier.startsWith(BASE_URL);
    }
    
    @Override
    public Dataset getDataset(String identifier) throws ConnectorException {
    
        ParameterizedTypeReference<Map<String, DatasetBuildable>> ref = new ParameterizedTypeReference<Map<String, DatasetBuildable>>() {
            // Just a reference.
        };
    
        try {
        
            if (identifier.startsWith(BASE_URL)) {
                identifier = identifier.replace(BASE_URL, "");
            }
    
            int urlParameterIndex = identifier.indexOf("?");
            String query = identifier.substring(urlParameterIndex +1);
            identifier = identifier.substring(0, urlParameterIndex);
    
            RequestEntity requestEntity = RequestEntity.post(new URI(BASE_URL + "/" + identifier))
                    .accept(MediaType.APPLICATION_JSON).body(IdentifierConverter.toJson(query));
    
            ResponseEntity<Map<String, DatasetBuildable>> exchange = getRestTemplate().exchange(
                    BASE_URL +"/{id}",
                    HttpMethod.POST,
                    requestEntity, ref, identifier);
            
    
            if (!exchange.getBody().values().iterator().hasNext()) {
                throw new NotFoundException(format("empty dataset returned for the identifier %s", identifier));
            }

            return buildDataset(exchange);
        
        } catch ( URISyntaxException | RestClientException rce) {
            String statusCode = "";
            if (rce instanceof HttpStatusCodeException) {
                statusCode = String.valueOf(((HttpStatusCodeException) rce).getStatusCode().value());
            }
            throw new ConnectorException(
                    format("%s error when accessing the dataset with id %s", statusCode, identifier),
                    rce
            );
        }
    }
    
}