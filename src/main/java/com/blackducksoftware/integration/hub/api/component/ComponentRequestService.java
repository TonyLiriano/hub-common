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
package com.blackducksoftware.integration.hub.api.component;

import static com.blackducksoftware.integration.hub.api.UrlConstants.SEGMENT_API;
import static com.blackducksoftware.integration.hub.api.UrlConstants.SEGMENT_COMPONENTS;

import java.util.Arrays;
import java.util.List;

import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.blackducksoftware.integration.hub.model.ComponentSearchResultView;
import com.blackducksoftware.integration.hub.request.HubPagedRequest;
import com.blackducksoftware.integration.hub.rest.RestConnection;
import com.blackducksoftware.integration.hub.service.HubParameterizedRequestService;

public class ComponentRequestService extends HubParameterizedRequestService<ComponentSearchResultView> {
	private static final List<String> COMPONENT_SEGMENTS = Arrays.asList(SEGMENT_API, SEGMENT_COMPONENTS);

	public ComponentRequestService(final RestConnection restConnection) {
		super(restConnection, ComponentSearchResultView.class);
	}

	public List<ComponentSearchResultView> getAllComponents(final String namespace, final String groupId, final String artifactId, final String version) throws HubIntegrationException {
		final String componentQuery = String.format("id:%s|%s|%s|%s", namespace, groupId, artifactId, version);
		final HubPagedRequest hubPagedRequest = getHubRequestFactory().createGetPagedRequest(COMPONENT_SEGMENTS, componentQuery);

		final List<ComponentSearchResultView> allComponents = getAllItems(hubPagedRequest);
		return allComponents;
	}

	public ComponentSearchResultView getExactComponentMatch(String namespace, String groupId, String artifactId, String version) throws HubIntegrationException {
		final List<ComponentSearchResultView> allComponents = getAllComponents(namespace, groupId, artifactId, version);
		for (final ComponentSearchResultView componentItem : allComponents) {
			if (componentItem.getOriginId() != null) {
				final String exactMatch = String.format("%s:%s:%s", groupId, artifactId, version);
				if (componentItem.getOriginId().equals(exactMatch)) {
					return componentItem;
				}
			}
		}

		throw new HubIntegrationException("Couldn't find an exact component match.");
	}

}
