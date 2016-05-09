/*******************************************************************************
 * Black Duck Software Suite SDK
 * Copyright (C) 2016 Black Duck Software, Inc.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 *******************************************************************************/
package com.blackducksoftware.integration.hub.global;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.blackducksoftware.integration.hub.builder.HubProxyInfoBuilder;
import com.blackducksoftware.integration.hub.builder.ValidationResult;
import com.blackducksoftware.integration.hub.builder.ValidationResults;

public class HubProxyInfoBuilderTest {
	private static final int VALID_PORT = 2303;
	private static final String VALID_HOST = "just need a non-empty string";
	private static final String VALID_PASSWORD = "itsasecret";
	private static final String VALID_USERNAME = "memyselfandi";
	private static final String VALID_IGNORE_HOST_LIST = "google,msn,yahoo";
	private static final String VALID_IGNORE_HOST = "google";
	private static final String INVALID_IGNORE_HOST_LIST = "google,[^-z!,abc";
	private static final String INVALID_IGNORE_HOST = "[^-z!";

	private List<String> expectedMessages;
	private List<String> actualMessages;

	@Before
	public void setUp() {
		expectedMessages = new ArrayList<String>();
		actualMessages = new ArrayList<String>();
	}

	@After
	public void tearDown() {
		assertEquals("Too many/not enough messages expected: \n" + actualMessages.size(), expectedMessages.size(),
				actualMessages.size());

		for (final String expectedMessage : expectedMessages) {
			assertTrue("Did not find the expected message : " + expectedMessage,
					actualMessages.contains(expectedMessage));
		}
	}

	private List<String> getMessages(final ValidationResults<GlobalFieldKey, HubProxyInfo> result) {

		final List<String> messageList = new ArrayList<String>();
		final Map<GlobalFieldKey, List<ValidationResult>> resultMap = result.getResultMap();
		for (final GlobalFieldKey key : resultMap.keySet()) {
			final List<ValidationResult> resultList = resultMap.get(key);

			for (final ValidationResult item : resultList) {
				final String message = item.getMessage();

				if (StringUtils.isNotBlank(message)) {
					messageList.add(item.getMessage());
				}
			}
		}
		return messageList;
	}

	@Test
	public void testValidateProxyConfigHubUrlIgnored() throws Exception {
		final HubProxyInfoBuilder builder = new HubProxyInfoBuilder();
		builder.setHost(VALID_HOST);
		builder.setPort(VALID_PORT);
		builder.setIgnoredProxyHosts(VALID_IGNORE_HOST);

		final HubProxyInfo proxyInfo = builder.build().getConstructedObject();
		final boolean useProxy = proxyInfo.shouldUseProxyForUrl(new URL("https://google.com"));
		assertFalse(useProxy);
	}

	@Test
	public void testValidateProxyConfigHubUrlNotIgnored() throws Exception {
		final HubProxyInfoBuilder builder = new HubProxyInfoBuilder();
		builder.setHost(VALID_HOST);
		builder.setPort(VALID_PORT);
		builder.setIgnoredProxyHosts("test");

		final HubProxyInfo proxyInfo = builder.build().getConstructedObject();
		final boolean useProxy = proxyInfo.shouldUseProxyForUrl(new URL("https://google.com"));
		assertTrue(useProxy);
	}

	@Test
	public void testValidateProxyPort() throws Exception {
		final HubProxyInfoBuilder builder = new HubProxyInfoBuilder();
		builder.setHost(VALID_HOST);
		builder.setPort(VALID_PORT);
		final ValidationResults<GlobalFieldKey, HubProxyInfo> result = new ValidationResults<GlobalFieldKey, HubProxyInfo>();
		builder.validatePort(result);
		assertTrue(result.isSuccess());
	}

	@Test
	public void testValidateProxyPortNoHost() throws Exception {
		expectedMessages.add(HubProxyInfoBuilder.MSG_PROXY_HOST_REQUIRED);
		final HubProxyInfoBuilder builder = new HubProxyInfoBuilder();
		builder.setHost("");
		builder.setPort(VALID_PORT);
		final ValidationResults<GlobalFieldKey, HubProxyInfo> result = new ValidationResults<GlobalFieldKey, HubProxyInfo>();
		builder.validatePort(result);
		assertFalse(result.isSuccess());

		actualMessages = getMessages(result);
	}

	@Test
	public void testValidateCredentialsNoHost() throws Exception {
		expectedMessages.add(HubProxyInfoBuilder.MSG_PROXY_HOST_NOT_SPECIFIED);
		final HubProxyInfoBuilder builder = new HubProxyInfoBuilder();
		builder.setHost("");
		builder.setUsername(VALID_USERNAME);
		builder.setPassword(VALID_PASSWORD);
		final ValidationResults<GlobalFieldKey, HubProxyInfo> result = new ValidationResults<GlobalFieldKey, HubProxyInfo>();
		builder.validateCredentials(result);
		assertTrue(result.hasWarnings());

		actualMessages = getMessages(result);
	}

	@Test
	public void testValidateCredentialsBothEmpty() throws Exception {
		final HubProxyInfoBuilder builder = new HubProxyInfoBuilder();
		builder.setHost(VALID_HOST);
		builder.setUsername("");
		builder.setPassword("");
		final ValidationResults<GlobalFieldKey, HubProxyInfo> result = new ValidationResults<GlobalFieldKey, HubProxyInfo>();
		builder.validateCredentials(result);
		assertTrue(result.isSuccess());
	}

	@Test
	public void testValidateCredentialsBothNotEmpty() throws Exception {
		final HubProxyInfoBuilder builder = new HubProxyInfoBuilder();
		builder.setHost(VALID_HOST);
		builder.setUsername(VALID_USERNAME);
		builder.setPassword(VALID_PASSWORD);
		final ValidationResults<GlobalFieldKey, HubProxyInfo> result = new ValidationResults<GlobalFieldKey, HubProxyInfo>();
		builder.validateCredentials(result);
		assertTrue(result.isSuccess());
	}

	@Test
	public void testValidateCredentialsUserOnly() throws Exception {
		expectedMessages.add(HubProxyInfoBuilder.MSG_CREDENTIALS_INVALID);
		expectedMessages.add(HubProxyInfoBuilder.MSG_CREDENTIALS_INVALID);
		final HubProxyInfoBuilder builder = new HubProxyInfoBuilder();
		builder.setHost(VALID_HOST);
		builder.setUsername(VALID_USERNAME);
		builder.setPassword("");
		final ValidationResults<GlobalFieldKey, HubProxyInfo> result = new ValidationResults<GlobalFieldKey, HubProxyInfo>();
		builder.validateCredentials(result);
		assertFalse(result.isSuccess());

		actualMessages = getMessages(result);
	}

	@Test
	public void testValidateCredentialsPasswordOnly() throws Exception {
		expectedMessages.add(HubProxyInfoBuilder.MSG_CREDENTIALS_INVALID);
		expectedMessages.add(HubProxyInfoBuilder.MSG_CREDENTIALS_INVALID);
		final HubProxyInfoBuilder builder = new HubProxyInfoBuilder();
		builder.setHost(VALID_HOST);
		builder.setUsername("");
		builder.setPassword(VALID_PASSWORD);
		final ValidationResults<GlobalFieldKey, HubProxyInfo> result = new ValidationResults<GlobalFieldKey, HubProxyInfo>();
		builder.validateCredentials(result);
		assertFalse(result.isSuccess());

		actualMessages = getMessages(result);
	}

	@Test
	public void testValidateIgnoreHostNoProxyHost() throws Exception {
		expectedMessages.add(HubProxyInfoBuilder.MSG_PROXY_HOST_NOT_SPECIFIED);
		final HubProxyInfoBuilder builder = new HubProxyInfoBuilder();
		builder.setHost("");
		builder.setIgnoredProxyHosts(VALID_IGNORE_HOST);
		final ValidationResults<GlobalFieldKey, HubProxyInfo> result = new ValidationResults<GlobalFieldKey, HubProxyInfo>();
		builder.validateIgnoreHosts(result);
		assertTrue(result.hasWarnings());

		actualMessages = getMessages(result);
	}

	@Test
	public void testValidateIgnoreHost() throws Exception {
		final HubProxyInfoBuilder builder = new HubProxyInfoBuilder();
		builder.setHost(VALID_HOST);
		builder.setIgnoredProxyHosts(VALID_IGNORE_HOST);
		final ValidationResults<GlobalFieldKey, HubProxyInfo> result = new ValidationResults<GlobalFieldKey, HubProxyInfo>();
		builder.validateIgnoreHosts(result);
		assertTrue(result.isSuccess());
	}

	@Test
	public void testValidateIgnoreHostList() throws Exception {
		final HubProxyInfoBuilder builder = new HubProxyInfoBuilder();
		builder.setHost(VALID_HOST);
		builder.setIgnoredProxyHosts(VALID_IGNORE_HOST_LIST);
		final ValidationResults<GlobalFieldKey, HubProxyInfo> result = new ValidationResults<GlobalFieldKey, HubProxyInfo>();
		builder.validateIgnoreHosts(result);
		assertTrue(result.isSuccess());
	}

	@Test
	public void testValidateIgnoreHostBadPattern() throws Exception {
		expectedMessages.add(HubProxyInfoBuilder.MSG_IGNORE_HOSTS_INVALID);
		final HubProxyInfoBuilder builder = new HubProxyInfoBuilder();
		builder.setHost(VALID_HOST);
		builder.setIgnoredProxyHosts(INVALID_IGNORE_HOST);
		final ValidationResults<GlobalFieldKey, HubProxyInfo> result = new ValidationResults<GlobalFieldKey, HubProxyInfo>();
		builder.validateIgnoreHosts(result);
		assertFalse(result.isSuccess());

		actualMessages = getMessages(result);
	}

	@Test
	public void testValidateIgnoreHostListBadPattern() throws Exception {
		expectedMessages.add(HubProxyInfoBuilder.MSG_IGNORE_HOSTS_INVALID);
		final HubProxyInfoBuilder builder = new HubProxyInfoBuilder();
		builder.setHost(VALID_HOST);
		builder.setIgnoredProxyHosts(INVALID_IGNORE_HOST_LIST);
		final ValidationResults<GlobalFieldKey, HubProxyInfo> result = new ValidationResults<GlobalFieldKey, HubProxyInfo>();
		builder.validateIgnoreHosts(result);
		assertFalse(result.isSuccess());

		actualMessages = getMessages(result);
	}

	@Test
	public void testAssertWithNoHost() throws Exception {
		expectedMessages.add(HubProxyInfoBuilder.MSG_PROXY_HOST_NOT_SPECIFIED);
		expectedMessages.add(HubProxyInfoBuilder.MSG_PROXY_HOST_REQUIRED);
		final HubProxyInfoBuilder builder = new HubProxyInfoBuilder();
		builder.setHost("");
		builder.setPort(VALID_PORT);
		builder.setIgnoredProxyHosts(VALID_IGNORE_HOST_LIST);
		final ValidationResults<GlobalFieldKey, HubProxyInfo> result = builder.assertValid();
		assertFalse(result.isSuccess());

		actualMessages = getMessages(result);
	}

	@Test
	public void testAssertWithInvalidPort() throws Exception {
		expectedMessages.add(HubProxyInfoBuilder.MSG_PROXY_PORT_INVALID);
		final HubProxyInfoBuilder builder = new HubProxyInfoBuilder();
		builder.setHost(VALID_HOST);
		builder.setPort(-1);
		builder.setIgnoredProxyHosts(VALID_IGNORE_HOST_LIST);
		final ValidationResults<GlobalFieldKey, HubProxyInfo> result = builder.assertValid();
		assertFalse(result.isSuccess());

		actualMessages = getMessages(result);
	}

	@Test
	public void testAssertWithInvalidUser() throws Exception {
		expectedMessages.add(HubProxyInfoBuilder.MSG_CREDENTIALS_INVALID);
		expectedMessages.add(HubProxyInfoBuilder.MSG_CREDENTIALS_INVALID);
		final HubProxyInfoBuilder builder = new HubProxyInfoBuilder();
		builder.setHost(VALID_HOST);
		builder.setPort(VALID_PORT);
		builder.setUsername("");
		builder.setPassword(VALID_PASSWORD);
		builder.setIgnoredProxyHosts(VALID_IGNORE_HOST_LIST);
		final ValidationResults<GlobalFieldKey, HubProxyInfo> result = builder.assertValid();
		assertFalse(result.isSuccess());

		actualMessages = getMessages(result);
	}

	@Test
	public void testAssertWithInvalidPassword() throws Exception {
		expectedMessages.add(HubProxyInfoBuilder.MSG_CREDENTIALS_INVALID);
		expectedMessages.add(HubProxyInfoBuilder.MSG_CREDENTIALS_INVALID);
		final HubProxyInfoBuilder builder = new HubProxyInfoBuilder();
		builder.setHost(VALID_HOST);
		builder.setPort(VALID_PORT);
		builder.setUsername(VALID_USERNAME);
		builder.setPassword("");
		builder.setIgnoredProxyHosts(VALID_IGNORE_HOST_LIST);
		final ValidationResults<GlobalFieldKey, HubProxyInfo> result = builder.assertValid();
		assertFalse(result.isSuccess());

		actualMessages = getMessages(result);
	}

	@Test
	public void testAssertWithInvalidIgnoreHost() throws Exception {
		expectedMessages.add(HubProxyInfoBuilder.MSG_IGNORE_HOSTS_INVALID);
		final HubProxyInfoBuilder builder = new HubProxyInfoBuilder();
		builder.setHost(VALID_HOST);
		builder.setPort(VALID_PORT);
		builder.setUsername(VALID_USERNAME);
		builder.setPassword(VALID_PASSWORD);
		builder.setIgnoredProxyHosts(INVALID_IGNORE_HOST);
		final ValidationResults<GlobalFieldKey, HubProxyInfo> result = builder.assertValid();
		assertFalse(result.isSuccess());

		actualMessages = getMessages(result);
	}

	@Test
	public void testAssertWithInvalidIgnoreHostList() throws Exception {
		expectedMessages.add(HubProxyInfoBuilder.MSG_IGNORE_HOSTS_INVALID);
		final HubProxyInfoBuilder builder = new HubProxyInfoBuilder();
		builder.setHost(VALID_HOST);
		builder.setPort(VALID_PORT);
		builder.setUsername(VALID_USERNAME);
		builder.setPassword(VALID_PASSWORD);
		builder.setIgnoredProxyHosts(INVALID_IGNORE_HOST_LIST);
		final ValidationResults<GlobalFieldKey, HubProxyInfo> result = builder.assertValid();
		assertFalse(result.isSuccess());

		actualMessages = getMessages(result);
	}

	@Test
	public void testAssertWithValidInput() throws Exception {

		final HubProxyInfoBuilder builder = new HubProxyInfoBuilder();
		builder.setHost(VALID_HOST);
		builder.setPort(VALID_PORT);
		builder.setUsername(VALID_USERNAME);
		builder.setPassword(VALID_PASSWORD);
		builder.setIgnoredProxyHosts(VALID_IGNORE_HOST_LIST);
		final ValidationResults<GlobalFieldKey, HubProxyInfo> result = builder.assertValid();
		assertTrue(result.isSuccess());
	}

	@Test
	public void testBuildWithValidInput() throws Exception {
		final HubProxyInfoBuilder builder = new HubProxyInfoBuilder();
		builder.setHost(VALID_HOST);
		builder.setPort(VALID_PORT);
		builder.setUsername(VALID_USERNAME);
		builder.setPassword(VALID_PASSWORD);
		builder.setIgnoredProxyHosts(VALID_IGNORE_HOST_LIST);

		final HubProxyInfo proxyInfo = builder.build().getConstructedObject();
		assertNotNull(proxyInfo);
	}

	@Test
	public void testBuildWithEmptyInput() throws Exception {
		final HubProxyInfoBuilder builder = new HubProxyInfoBuilder();

		final ValidationResults<GlobalFieldKey, HubProxyInfo> result = builder.build();
		final HubProxyInfo proxyInfo = result.getConstructedObject();
		assertNotNull(proxyInfo);

		actualMessages = getMessages(result);
	}
}
