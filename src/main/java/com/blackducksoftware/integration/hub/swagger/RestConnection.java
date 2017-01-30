/**
 * Hub Common
 *
 * Copyright (C) 2017 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.blackducksoftware.integration.hub.swagger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.blackducksoftware.integration.hub.global.HubProxyInfo;
import com.blackducksoftware.integration.log.IntLogger;
import com.blackducksoftware.integration.log.LogLevel;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.squareup.okhttp.Authenticator;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

/**
 * The parent class of all Hub connections.
 */
public abstract class RestConnection {
    public static final String JSON_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSX";

    private final URL baseUrl;

    private final HubProxyInfo hubProxyInfo;

    private final Gson gson = new GsonBuilder().setDateFormat(JSON_DATE_FORMAT).create();

    private final JsonParser jsonParser = new JsonParser();

    private OkHttpClient client;

    private IntLogger logger;

    private int timeout = 120;

    public static Date parseDateString(final String dateString) throws ParseException {
        final SimpleDateFormat sdf = new SimpleDateFormat(JSON_DATE_FORMAT);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.parse(dateString);
    }

    public static String formatDate(final Date date) {
        final SimpleDateFormat sdf = new SimpleDateFormat(JSON_DATE_FORMAT);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(date);
    }

    public RestConnection(final URL baseUrl) {
        this(null, baseUrl, null);
    }

    public RestConnection(final IntLogger logger, final URL baseUrl, final HubProxyInfo hubProxyInfo) {
        if (logger != null) {
            setLogger(logger);
        }
        this.baseUrl = baseUrl;
        this.hubProxyInfo = hubProxyInfo;
        client = new OkHttpClient();
        client.setSslSocketFactory(null);
        // just in case setTimeout() is never called
        setTimeout(timeout);
    }

    public IntLogger getLogger() {
        return logger;
    }

    public void setLogger(final IntLogger logger) {
        this.logger = logger;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(final int timeout) {
        if (timeout <= 0) {
            throw new IllegalArgumentException("Timeout must be greater than zero.");
        }
        this.timeout = timeout;
        logMessage(LogLevel.DEBUG, "Setting connectTimeout to: " + timeout + "s on client context");
    }

    public void connect() throws HubIntegrationException {
        addBuilderConnectionTimes();
        addBuilderProxyInformation();
        addBuilderAuthentication();
        //TODO: Build the client first. Kind of already are
        //setClient(client);
        
        clientAuthenticate();
    }

    public abstract void addBuilderAuthentication() throws HubIntegrationException;

    public abstract void clientAuthenticate() throws HubIntegrationException;

    public HttpUrl createHttpUrl() {
        return HttpUrl.get(getBaseUrl()).newBuilder().build();
    }

    public HttpUrl createHttpUrl(final URL providedUrl) {
        final HttpUrl.Builder urlBuilder = HttpUrl.get(providedUrl).newBuilder();
        return urlBuilder.build();
    }

    public HttpUrl createHttpUrl(final String providedUrl) {
        final HttpUrl.Builder urlBuilder = HttpUrl.parse(providedUrl).newBuilder();
        return urlBuilder.build();
    }

    public HttpUrl createHttpUrl(final List<String> urlSegments) {
        return createHttpUrl(urlSegments, null);
    }

    public HttpUrl createHttpUrl(final List<String> urlSegments,
            final Map<String, String> queryParameters) {
        return createHttpUrl(getBaseUrl().toString(), urlSegments, queryParameters);
    }

    public HttpUrl createHttpUrl(final String providedUrl, final List<String> urlSegments,
            final Map<String, String> queryParameters) {
        final HttpUrl.Builder urlBuilder = HttpUrl.parse(providedUrl).newBuilder();
        urlBuilder.scheme("http");
        if (urlSegments != null) {
            for (final String urlSegment : urlSegments) {
                urlBuilder.addPathSegment(urlSegment);
            }
        }
        if (queryParameters != null) {
            for (final Entry<String, String> queryParameter : queryParameters.entrySet()) {
                urlBuilder.addQueryParameter(queryParameter.getKey(), queryParameter.getValue());
            }
        }
        return urlBuilder.build();
    }

    private void addBuilderConnectionTimes() throws HubIntegrationException {
        client.setConnectTimeout(timeout, TimeUnit.SECONDS);
        client.setWriteTimeout(timeout, TimeUnit.SECONDS);
        client.setReadTimeout(timeout, TimeUnit.SECONDS);
    }

    private void addBuilderProxyInformation() throws HubIntegrationException {
        if (getHubProxyInfo() != null) {
            client.setProxy(getHubProxyInfo().getProxy(getBaseUrl()));
            String password;
            try {
                password = getHubProxyInfo().getDecryptedPassword();
                if (StringUtils.isNotBlank(getHubProxyInfo().getUsername()) && StringUtils.isNotBlank(password)) {
                    client.setAuthenticator(
                            (Authenticator) new com.blackducksoftware.integration.hub.proxy.OkAuthenticator(getHubProxyInfo().getUsername(),
                                    password));
                }
            } catch (final Exception e) {
                throw new HubIntegrationException(e.getMessage(), e);
            }
        }
    }

    public RequestBody createJsonRequestBody(final String content) {
        return createJsonRequestBody("application/json", content);
    }

    public RequestBody createJsonRequestBody(final String mediaType, final String content) {
        return RequestBody.create(MediaType.parse(mediaType), content);
    }

    public RequestBody createEncodedRequestBody(final Map<String, String> content) {
        final FormEncodingBuilder builder = new FormEncodingBuilder();
        for (final Entry<String, String> contentEntry : content.entrySet()) {
        	// TODO: try addEncoded if that doesnt work
        	builder.addEncoded(contentEntry.getKey(), contentEntry.getValue());
        }
        return builder.build();
    }

    public Request createGetRequest(final HttpUrl httpUrl) {
        return createGetRequest(httpUrl, "application/json");
    }

    public Request createGetRequest(final HttpUrl httpUrl, final String mediaType) {
        final Map<String, String> headers = new HashMap<>();
        headers.put("Accept", mediaType);
        return createGetRequest(httpUrl, headers);
    }

    public Request createGetRequest(final HttpUrl httpUrl, final Map<String, String> headers) {
        final Request.Builder requestBuilder = new Request.Builder();
        for (final Entry<String, String> header : headers.entrySet()) {
            requestBuilder.addHeader(header.getKey(), header.getValue());
        }
        return requestBuilder
                .url(httpUrl).get().build();
    }

    public Request createPostRequest(final HttpUrl httpUrl, final RequestBody request) {
        return new Request.Builder()
                .url(httpUrl)
                .post(request).build();
    }

    public Request createPutRequest(final HttpUrl httpUrl, final RequestBody body) {
        return new Request.Builder()
                .url(httpUrl)
                .put(body).build();
    }

    public Request createDeleteRequest(final HttpUrl httpUrl) {
        return new Request.Builder()
                .url(httpUrl).delete().build();
    }

    public Response handleExecuteClientCall(final Request request) throws IOException, HubIntegrationException {
        return handleExecuteClientCall(request, 0);
    }

    private Response handleExecuteClientCall(final Request request, final int retryCount) throws IOException, HubIntegrationException {
        if (getClient() != null) {
            logRequestHeaders(request);
            final Call call = getClient().newCall(request);
            //call.
            final Response response = call.execute();
            if (!response.isSuccessful()) {
                if (response.code() == 401 && retryCount < 2) {
                    connect();
                    return handleExecuteClientCall(request, retryCount + 1);
                } else {
                    try {
						throw new HubIntegrationException(
						        "There was a problem trying to " + request.method() + " this item : " + request.url().toURI().toString() + ". Error : "
						                + response.message());
					} catch (URISyntaxException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                }
            }
            logResponseHeaders(response);
            return response;
        } else {
            connect();
            return handleExecuteClientCall(request, retryCount);
        }
    }

    private void logMessage(final LogLevel level, final String txt) {
        if (logger != null) {
            if (level == LogLevel.ERROR) {
                logger.error(txt);
            } else if (level == LogLevel.WARN) {
                logger.warn(txt);
            } else if (level == LogLevel.INFO) {
                logger.info(txt);
            } else if (level == LogLevel.DEBUG) {
                logger.debug(txt);
            } else if (level == LogLevel.TRACE) {
                logger.trace(txt);
            }
        }
    }

    private boolean isDebugLogging() {
        return logger != null && logger.getLogLevel() == LogLevel.TRACE;
    }

    private void logRequestHeaders(final Request request) {
        if (isDebugLogging()) {
            final String requestName = request.getClass().getSimpleName();
            logMessage(LogLevel.TRACE, requestName + " : " + request.toString());
            logHeaders(requestName, request.headers());
        }
    }

    private void logResponseHeaders(final Response response) {
        if (isDebugLogging()) {
            final String responseName = response.getClass().getSimpleName();
            logMessage(LogLevel.TRACE, responseName + " : " + response.toString());
            logHeaders(responseName, response.headers());
        }
    }

    private void logHeaders(final String requestOrResponseName, final Headers headers) {
        if (headers != null && headers.size() > 0) {
            logMessage(LogLevel.TRACE, requestOrResponseName + " headers : ");
            for (final Entry<String, List<String>> headerEntry : headers.toMultimap().entrySet()) {
                final String key = headerEntry.getKey();
                String value = "null";
                if (headerEntry.getValue() != null && !headerEntry.getValue().isEmpty()) {
                    value = StringUtils.join(headerEntry.getValue(), System.lineSeparator());
                }
                logMessage(LogLevel.TRACE, String.format("Header %s : %s", key, value));
            }
        } else {
            logMessage(LogLevel.TRACE, requestOrResponseName + " does not have any headers.");
        }
    }

    @Override
    public String toString() {
        return "RestConnection [baseUrl=" + baseUrl + "]";
    }

//    protected OkHttpClient.Builder getBuilder() {
//        return builder;
//    }

    public OkHttpClient getClient() {
        return client;
    }

    public void setClient(final OkHttpClient client) {
        this.client = client;
    }

    public URL getBaseUrl() {
        return baseUrl;
    }

    public Gson getGson() {
        return gson;
    }

    public JsonParser getJsonParser() {
        return jsonParser;
    }

    public HubProxyInfo getHubProxyInfo() {
        return hubProxyInfo;
    }

}
