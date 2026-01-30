/*
 * Copyright 2021-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.extensions.sparkplug.influxdb;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.izettle.metrics.influxdb.InfluxDbHttpSender;
import com.izettle.metrics.influxdb.utils.TimeUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

/**
 * HTTP sender implementation for InfluxDB Cloud using the InfluxDB 2.x API.
 * <p>
 * This sender extends {@link InfluxDbHttpSender} to support InfluxDB Cloud's authentication
 * and API requirements:
 * <ul>
 *     <li>Uses the {@code /api/v2/write} endpoint instead of the legacy write endpoint</li>
 *     <li>Authenticates using Bearer tokens via the {@code Authorization} header</li>
 *     <li>Supports organization and bucket parameters required by InfluxDB 2.x</li>
 *     <li>Compresses data using GZIP for efficient transmission</li>
 * </ul>
 * <p>
 * The sender is thread-safe and can be used concurrently by multiple threads.
 *
 * @author David Sondermann
 * @see InfluxDbHttpSender
 */
public class InfluxDbCloudSender extends InfluxDbHttpSender {

    /**
     * The authentication token for InfluxDB Cloud.
     */
    private final @NotNull String authToken;

    /**
     * Connection timeout in milliseconds.
     */
    private final int connectTimeout;

    /**
     * Read timeout in milliseconds.
     */
    private final int readTimeout;

    /**
     * The fully constructed URL for the InfluxDB Cloud write endpoint.
     */
    private final @NotNull URL url;

    /**
     * Constructs a new InfluxDbCloudSender for sending metrics to InfluxDB Cloud.
     *
     * @param protocol          the protocol to use (http or https)
     * @param host              the InfluxDB Cloud host
     * @param port              the port number
     * @param authToken         the authentication token for InfluxDB Cloud
     * @param timePrecision     the time precision for timestamps
     * @param connectTimeout    the connection timeout in milliseconds
     * @param readTimeout       the read timeout in milliseconds
     * @param measurementPrefix optional prefix for all measurements (may be {@code null})
     * @param organization      the InfluxDB Cloud organization name
     * @param bucket            the InfluxDB Cloud bucket name
     * @throws Exception if the URL cannot be constructed
     */
    public InfluxDbCloudSender(
            final @NotNull String protocol,
            final @NotNull String host,
            final int port,
            final @NotNull String authToken,
            final @NotNull TimeUnit timePrecision,
            final int connectTimeout,
            final int readTimeout,
            final @Nullable String measurementPrefix,
            final @NotNull String organization,
            final @NotNull String bucket) throws Exception {
        super(protocol, host, port, "", authToken, timePrecision, connectTimeout, readTimeout, measurementPrefix);
        this.authToken = authToken;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;

        final var endpoint = new URL(protocol, host, port, "/api/v2/write").toString();
        final var queryPrecision = String.format("precision=%s", TimeUtils.toTimePrecision(timePrecision));
        final var orgParameter = String.format("org=%s", URLEncoder.encode(organization, StandardCharsets.UTF_8));
        final var bucketParameter = String.format("bucket=%s", URLEncoder.encode(bucket, StandardCharsets.UTF_8));
        this.url = new URL(endpoint + "?" + queryPrecision + "&" + orgParameter + "&" + bucketParameter);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Writes the data to InfluxDB Cloud using the v2 API with token authentication.
     * The data is compressed using GZIP before sending.
     *
     * @param line the line protocol data to write
     * @return the HTTP response code (2xx indicates success)
     * @throws Exception if the write fails or the server returns a non-2xx response
     */
    @Override
    protected int writeData(final byte @NotNull [] line) throws Exception {
        final var con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Authorization", "Token " + authToken);
        con.setDoOutput(true);
        con.setConnectTimeout(connectTimeout);
        con.setReadTimeout(readTimeout);
        con.setRequestProperty("Content-Encoding", "gzip");
        try (final var out = con.getOutputStream(); final var gzipOutputStream = new GZIPOutputStream(out)) {
            gzipOutputStream.write(line);
            gzipOutputStream.flush();
            out.flush();
        }
        // check if non 2XX response code
        final var responseCode = con.getResponseCode();
        if (responseCode / 100 != 2) {
            throw new IOException(String.format("Server returned HTTP response code: %d for URL: %s with content: '%s'",
                    responseCode,
                    url,
                    con.getResponseMessage()));
        }
        return responseCode;
    }
}
