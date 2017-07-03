package no.ssb.vtl.connectors;

/*-
 * ========================LICENSE_START=================================
 * Java VTL
 * %%
 * Copyright (C) 2016 - 2017 Hadrien Kohl
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import no.ssb.vtl.connectors.testutil.ConstantClockSource;
import no.ssb.vtl.connectors.util.TimeUtil;
import no.ssb.vtl.model.Component;
import no.ssb.vtl.model.Dataset;
import no.ssb.vtl.model.VTLObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import java.io.InputStream;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class SsbKlassApiConnectorTest {

    private ObjectMapper mapper;
    private Connector connector;
    private MockRestServiceServer mockServer;

    @Before
    public void setUp() throws Exception {
        this.mapper = new ObjectMapper();
        SsbKlassApiConnector ssbConnector = new SsbKlassApiConnector(this.mapper, SsbKlassApiConnector.PeriodType.YEAR);
        this.connector = ssbConnector;
        mockServer = MockRestServiceServer.createServer(ssbConnector.getRestTemplate());
        TimeUtil.setClockSource(new ConstantClockSource(Instant.parse("2017-01-01T12:00:00.00Z")));
    }

    @After
    public void teardown() {
        TimeUtil.revertClockSource();
    }

    @Test
    public void testCanHandle() throws Exception {

        String testUri = "http://data.ssb.no/api/klass/v1/classifications/131/codes?from=2013-01-01";
        assertThat(this.connector.canHandle(testUri));

        testUri = "http://data.ssb.no/api/v0/dataset/1106.json?lang=en";
        assertThat(!this.connector.canHandle(testUri));

    }

    @Test
    public void testGetDataset() throws Exception {

        InputStream fileStream = Resources.getResource(this.getClass(), "/codes131_from2013.json").openStream();

        mockServer.expect(requestTo("http://data.ssb.no/api/klass/v1/classifications/131/codes?from=2013-01-01"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        new InputStreamResource(fileStream),
                        MediaType.APPLICATION_JSON)
                );

        Dataset dataset = this.connector.getDataset("http://data.ssb.no/api/klass/v1/classifications/131/codes?from=2013-01-01");

        assertThat(dataset.getDataStructure().getRoles()).containsExactly(
                entry("code", Component.Role.IDENTIFIER),
                entry("period", Component.Role.IDENTIFIER),
                entry("name", Component.Role.MEASURE)
        );

        assertThat(dataset.getDataStructure().getTypes()).containsExactly(
                entry("code", String.class),
                entry("period", String.class),
                entry("name", String.class)
        );

        assertThat(dataset.getData())
                .flatExtracting(input -> input)
                .extracting(VTLObject::get)
                .containsSequence(
                        "0101", "2013", "Halden",
                        "0101", "2014", "Halden",
                        "0101", "2015", "Halden",
                        "0101", "2016", "Halden",
                        "0101", "2017", "Halden",
                        "0101", "2018", "Halden",
                        "0104", "2013", "Moss",
                        "0104", "2014", "Moss ny",
                        "0104", "2015", "Moss ny",
                        "0104", "2016", "Moss ny",
                        "0104", "2017", "Moss ny",
                        "0104", "2018", "Moss ny",
                        "0105", "2013", "Sarpsborg",
                        "0105", "2014", "Sarpsborg ny", //valid from 2014-06-01, but year 2014 gets the newest name
                        "0105", "2015", "Sarpsborg ny",
                        "0105", "2016", "Sarpsborg ny",
                        "0105", "2017", "Sarpsborg ny",
                        "0105", "2018", "Sarpsborg ny"
                );

    }

}
