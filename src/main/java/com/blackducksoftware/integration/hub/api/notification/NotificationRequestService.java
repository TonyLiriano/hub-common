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
package com.blackducksoftware.integration.hub.api.notification;

import static com.blackducksoftware.integration.hub.api.UrlConstants.SEGMENT_API;
import static com.blackducksoftware.integration.hub.api.UrlConstants.SEGMENT_NOTIFICATIONS;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import com.blackducksoftware.integration.hub.api.item.MetaService;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.blackducksoftware.integration.hub.model.NotificationView;
import com.blackducksoftware.integration.hub.model.UserView;
import com.blackducksoftware.integration.hub.request.HubPagedRequest;
import com.blackducksoftware.integration.hub.rest.RestConnection;
import com.blackducksoftware.integration.hub.service.HubParameterizedRequestService;
import com.blackducksoftware.integration.log.IntLogger;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class NotificationRequestService extends HubParameterizedRequestService<NotificationView> {
	private static final List<String> NOTIFICATIONS_SEGMENTS = Arrays.asList(SEGMENT_API, SEGMENT_NOTIFICATIONS);

	private final Map<String, Class<? extends NotificationView>> typeMap = new HashMap<>();

	private final MetaService metaService;

	public NotificationRequestService(final IntLogger logger, final RestConnection restConnection, final MetaService metaService) {
		super(restConnection, NotificationView.class);
		this.metaService = metaService;
		typeMap.put("VULNERABILITY", VulnerabilityNotificationItem.class);
		typeMap.put("RULE_VIOLATION", RuleViolationNotificationItem.class);
		typeMap.put("POLICY_OVERRIDE", PolicyOverrideNotificationItem.class);
		typeMap.put("RULE_VIOLATION_CLEARED", RuleViolationClearedNotificationItem.class);
	}

	public List<NotificationView> getAllNotifications(final Date startDate, final Date endDate) throws HubIntegrationException {
		final SimpleDateFormat sdf = new SimpleDateFormat(RestConnection.JSON_DATE_FORMAT);
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		final String startDateString = sdf.format(startDate);
		final String endDateString = sdf.format(endDate);

		final HubPagedRequest hubPagedRequest = getHubRequestFactory().createGetPagedRequest(100, NOTIFICATIONS_SEGMENTS);
		hubPagedRequest.addQueryParameter("startDate", startDateString);
		hubPagedRequest.addQueryParameter("endDate", endDateString);

		final List<NotificationView> allNotificationItems = getAllItems(hubPagedRequest);
		return allNotificationItems;
	}

	public List<NotificationView> getUserNotifications(final Date startDate, final Date endDate, final UserView user) throws HubIntegrationException {
		final SimpleDateFormat sdf = new SimpleDateFormat(RestConnection.JSON_DATE_FORMAT);
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		final String startDateString = sdf.format(startDate);
		final String endDateString = sdf.format(endDate);
		final String url = metaService.getFirstLink(user, MetaService.NOTIFICATIONS_LINK);

		final HubPagedRequest hubPagedRequest = getHubRequestFactory().createGetPagedRequest(100, url);
		hubPagedRequest.addQueryParameter("startDate", startDateString);
		hubPagedRequest.addQueryParameter("endDate", endDateString);

		final List<NotificationView> allNotificationItems = getAllItems(hubPagedRequest);
		return allNotificationItems;
	}

	@Override
	public List<NotificationView> getItems(final JsonObject jsonObject) {
		final JsonArray jsonArray = jsonObject.get("items").getAsJsonArray();
		final List<NotificationView> allNotificationItems = new ArrayList<>(jsonArray.size());
		for (final JsonElement jsonElement : jsonArray) {
			final String type = jsonElement.getAsJsonObject().get("type").getAsString();
			Class<? extends NotificationView> clazz = NotificationView.class;
			if (typeMap.containsKey(type)) {
				clazz = typeMap.get(type);
			}
			allNotificationItems.add(getRestConnection().getGson().fromJson(jsonElement, clazz));
		}

		return allNotificationItems;
	}

}
