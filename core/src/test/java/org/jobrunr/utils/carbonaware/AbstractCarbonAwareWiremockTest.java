package org.jobrunr.utils.carbonaware;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.jobrunr.utils.carbonaware.CarbonAwareConfiguration.DEFAULT_CLIENT_API_CONNECT_TIMEOUT;
import static org.jobrunr.utils.carbonaware.CarbonAwareConfiguration.DEFAULT_CLIENT_API_READ_TIMEOUT;
import static org.jobrunr.utils.carbonaware.CarbonAwareConfigurationReader.getCarbonAwareApiUrlPath;

public class AbstractCarbonAwareWiremockTest {

    private static WireMockServer wireMockServer;
    private static JsonMapper jsonMapper;

    @BeforeAll
    static void beforeAll() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        jsonMapper = new JacksonJsonMapper(mapper);
        wireMockServer = new WireMockServer(10000);
        wireMockServer.start();
    }

    @BeforeEach
    void setUp() {
        wireMockServer.resetAll();
    }

    @AfterAll
    static void tearDown() {
        wireMockServer.stop();
    }

    protected CarbonAwareApiClient createCarbonAwareApiClient() {
        return new CarbonAwareApiClient(CarbonAwareConfigurationReader.getCarbonAwareApiUrl("http://localhost:10000"), DEFAULT_CLIENT_API_CONNECT_TIMEOUT, DEFAULT_CLIENT_API_READ_TIMEOUT, jsonMapper);
    }

    protected void mockResponseWhenRequestingArea(String area, String response) {
        String url = String.format(getCarbonAwareApiUrlPath() + "?area=%s", area);
        wireMockServer.stubFor(WireMock.get(urlEqualTo(url))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(response)));
    }
}
