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
package com.blackducksoftware.integration.hub.dataservice.notification.transformer;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.blackducksoftware.integration.hub.api.item.MetaService;
import com.blackducksoftware.integration.hub.api.notification.NotificationRequestService;
import com.blackducksoftware.integration.hub.api.policy.PolicyRequestService;
import com.blackducksoftware.integration.hub.api.project.version.ProjectVersionRequestService;
import com.blackducksoftware.integration.hub.api.version.VersionBomPolicyRequestService;
import com.blackducksoftware.integration.hub.dataservice.ItemTransform;
import com.blackducksoftware.integration.hub.dataservice.notification.model.FullProjectVersionView;
import com.blackducksoftware.integration.hub.dataservice.notification.model.NotificationContentItem;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.blackducksoftware.integration.hub.exception.HubItemTransformException;
import com.blackducksoftware.integration.hub.service.HubRequestService;

import io.swagger.client.model.ComponentVersionView;
import io.swagger.client.model.NotificationView;
import io.swagger.client.model.ProjectVersionView;

public abstract class AbstractNotificationTransformer implements ItemTransform<List<NotificationContentItem>, NotificationView> {
	private final NotificationRequestService notificationService;

	private final ProjectVersionRequestService projectVersionService;

	private final PolicyRequestService policyService;

	private final VersionBomPolicyRequestService bomVersionPolicyService;

	private final HubRequestService hubRequestService;

	private final MetaService metaService;

	public AbstractNotificationTransformer(final NotificationRequestService notificationService, final ProjectVersionRequestService projectVersionService, final PolicyRequestService policyService,
			final VersionBomPolicyRequestService bomVersionPolicyService, final HubRequestService hubRequestService, final MetaService metaService) {
		this.notificationService = notificationService;
		this.projectVersionService = projectVersionService;
		this.policyService = policyService;
		this.bomVersionPolicyService = bomVersionPolicyService;
		this.hubRequestService = hubRequestService;
		this.metaService = metaService;
	}

	public NotificationRequestService getNotificationService() {
		return notificationService;
	}

	public ProjectVersionRequestService getProjectVersionService() {
		return projectVersionService;
	}

	public PolicyRequestService getPolicyService() {
		return policyService;
	}

	public VersionBomPolicyRequestService getBomVersionPolicyService() {
		return bomVersionPolicyService;
	}

	public HubRequestService getHubRequestService() {
		return hubRequestService;
	}

	@Override
	public abstract List<NotificationContentItem> transform(NotificationView item) throws HubItemTransformException;

	protected FullProjectVersionView createFullProjectVersion(final String projectVersionUrl, final String projectName, final String versionName) throws HubIntegrationException {
		ProjectVersionView item;
		try {
			item = getHubRequestService().getItem(projectVersionUrl, ProjectVersionView.class);
		} catch (final HubIntegrationException e) {
			final String msg = "Error getting the full ProjectVersion for this affected project version URL: " + projectVersionUrl + ": " + e.getMessage();
			throw new HubIntegrationException(msg, e);
		}
		final FullProjectVersionView fullProjectVersion = new FullProjectVersionView();
		fullProjectVersion.setProjectName(projectName);
		fullProjectVersion.setProjectVersionName(versionName);
		fullProjectVersion.setDistribution(item.getDistribution());
		fullProjectVersion.setLicense(item.getLicense());
		fullProjectVersion.setNickname(item.getNickname());
		fullProjectVersion.setPhase(item.getPhase());
		fullProjectVersion.setReleaseComments(item.getReleaseComments());
		fullProjectVersion.setReleasedOn(item.getReleasedOn());
		fullProjectVersion.setSource(item.getSource());

		fullProjectVersion.setUrl(metaService.getHref(item));
		fullProjectVersion.setCodeLocationsLink((metaService.getFirstLinkSafely(item, MetaService.CODE_LOCATION_LINK)));
		fullProjectVersion.setComponentsLink((metaService.getFirstLinkSafely(item, MetaService.COMPONENTS_LINK)));
		fullProjectVersion.setPolicyStatusLink((metaService.getFirstLinkSafely(item, MetaService.POLICY_STATUS_LINK)));
		fullProjectVersion.setProjectLink((metaService.getFirstLinkSafely(item, MetaService.PROJECT_LINK)));
		fullProjectVersion.setRiskProfileLink((metaService.getFirstLinkSafely(item, MetaService.RISK_PROFILE_LINK)));
		fullProjectVersion.setVersionReportLink((metaService.getFirstLinkSafely(item, MetaService.VERSION_REPORT_LINK)));
		fullProjectVersion.setVulnerableComponentsLink((metaService.getFirstLinkSafely(item, MetaService.VULNERABLE_COMPONENTS_LINK)));

		return fullProjectVersion;
	}

	public MetaService getMetaService() {
		return metaService;
	}

	protected ComponentVersionView getComponentVersion(final String componentVersionLink) throws HubIntegrationException {
		ComponentVersionView componentVersion = null;
		if (!StringUtils.isBlank(componentVersionLink)) {
			componentVersion = getHubRequestService().getItem(componentVersionLink, ComponentVersionView.class);
		}
		return componentVersion;
	}

	protected String getComponentVersionName(final String componentVersionLink) throws HubIntegrationException {
		String componentVersionName = "";
		if (!StringUtils.isBlank(componentVersionLink)) {
			final ComponentVersionView compVersion = getComponentVersion(componentVersionLink);
			if (compVersion != null) {
				componentVersionName = compVersion.getVersionName();
			}
		}
		return componentVersionName;
	}
}
