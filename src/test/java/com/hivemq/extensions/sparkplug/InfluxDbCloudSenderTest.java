/*
 * Copyright 2021 HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.extensions.sparkplug;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;


class InfluxDbCloudSenderTest {
    private InfluxDbCloudSender sender;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.options().dynamicPort());

    @Test
    public void test_write_data() throws Exception {

        sender = new InfluxDbCloudSender("http", "localhost", wireMockRule.port(), "token", TimeUnit.MILLISECONDS, 3000, 3000, "", "testorg", "testbucket");

        wireMockRule.stubFor(post(urlPathEqualTo("/api/v2/write"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("")));

        sender.writeData("line=line".getBytes());

        verify(postRequestedFor(urlEqualTo("/api/v2/write?precision=ms&org=testorg&bucket=testbucket"))
                .withHeader("Authorization", equalTo("Token token"))
                .withRequestBody(equalTo("line=line")));
    }
}