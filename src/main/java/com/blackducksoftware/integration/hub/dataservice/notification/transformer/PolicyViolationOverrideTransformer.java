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

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.blackducksoftware.integration.hub.api.component.version.ComponentVersionStatus;
import com.blackducksoftware.integration.hub.api.item.MetaService;
import com.blackducksoftware.integration.hub.api.notification.NotificationRequestService;
import com.blackducksoftware.integration.hub.api.notification.PolicyOverrideNotificationItem;
import com.blackducksoftware.integration.hub.api.policy.PolicyRequestService;
import com.blackducksoftware.integration.hub.api.project.version.ProjectVersionRequestService;
import com.blackducksoftware.integration.hub.api.version.VersionBomPolicyRequestService;
import com.blackducksoftware.integration.hub.dataservice.notification.model.FullProjectVersionView;
import com.blackducksoftware.integration.hub.dataservice.notification.model.NotificationContentItem;
import com.blackducksoftware.integration.hub.dataservice.notification.model.PolicyNotificationFilter;
import com.blackducksoftware.integration.hub.dataservice.notification.model.PolicyOverrideContentItem;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.blackducksoftware.integration.hub.exception.HubItemTransformException;
import com.blackducksoftware.integration.hub.service.HubRequestService;

import io.swagger.client.model.BomComponentPolicyStatusView;
import io.swagger.client.model.ComponentVersionView;
import io.swagger.client.model.NotificationView;
import io.swagger.client.model.PolicyRuleView;
import io.swagger.client.model.ProjectVersionView;

public class PolicyViolationOverrideTransformer extends AbstractPolicyTransformer {
	public PolicyViolationOverrideTransformer(final NotificationRequestService notificationService, final ProjectVersionRequestService projectVersionService, final PolicyRequestService policyService,
			final VersionBomPolicyRequestService bomVersionPolicyService, final HubRequestService hubRequestService, final PolicyNotificationFilter policyFilter, final MetaService metaService) {
		super(notificationService, projectVersionService, policyService, bomVersionPolicyService, hubRequestService, policyFilter, metaService);
	}

	@Override
	public List<NotificationContentItem> transform(final NotificationView item) throws HubItemTransformException {
		final List<NotificationContentItem> templateData = new ArrayList<>();
		final ProjectVersionView releaseItem;
		final PolicyOverrideNotificationItem policyOverride = (PolicyOverrideNotificationItem) item;
		final String projectName = policyOverride.getContent().getProjectName();
		final List<ComponentVersionStatus> componentVersionList = new ArrayList<>();
		final ComponentVersionStatus componentStatus = new ComponentVersionStatus();
		componentStatus.setBomComponentVersionPolicyStatusLink(policyOverride.getContent().getBomComponentVersionPolicyStatusLink());
		componentStatus.setComponentName(policyOverride.getContent().getComponentName());
		componentStatus.setComponentVersionLink(policyOverride.getContent().getComponentVersionLink());

		componentVersionList.add(componentStatus);

		try {
			releaseItem = getProjectVersionService().getItem(policyOverride.getContent().getProjectVersionLink());
		} catch (final HubIntegrationException e) {
			throw new HubItemTransformException(e);
		}

		final FullProjectVersionView projectVersion = new FullProjectVersionView();
		projectVersion.setProjectName(projectName);
		projectVersion.setProjectVersionName(releaseItem.getVersionName());
		projectVersion.setUrl(policyOverride.getContent().getProjectVersionLink());

		handleNotification(componentVersionList, projectVersion, item, templateData);
		return templateData;
	}

	@Override
	public void handleNotification(final List<ComponentVersionStatus> componentVersionList, final FullProjectVersionView projectVersion, final NotificationView item, final List<NotificationContentItem> templateData)
			throws HubItemTransformException {

		final PolicyOverrideNotificationItem policyOverrideItem = (PolicyOverrideNotificationItem) item;
		for (final ComponentVersionStatus componentVersion : componentVersionList) {
			try {
				final String componentLink = policyOverrideItem.getContent().getComponentLink();
				final String componentVersionLink = policyOverrideItem.getContent().getComponentVersionLink();
				final ComponentVersionView fullComponentVersion = getComponentVersion(componentVersionLink);

				final String policyStatusUrl = componentVersion.getBomComponentVersionPolicyStatusLink();

				if (StringUtils.isNotBlank(policyStatusUrl)) {
					final BomComponentPolicyStatusView bomComponentVersionPolicyStatus = getBomComponentVersionPolicyStatus(policyStatusUrl);

					List<String> ruleList = getMetaService().getLinks(bomComponentVersionPolicyStatus, MetaService.POLICY_RULE_LINK);

					ruleList = getMatchingRuleUrls(ruleList);
					if (ruleList != null && !ruleList.isEmpty()) {
						final List<PolicyRuleView> policyRuleList = new ArrayList<>();
						for (final String ruleUrl : ruleList) {
							final PolicyRuleView rule = getPolicyRule(ruleUrl);
							policyRuleList.add(rule);
						}
						createContents(projectVersion, componentVersion.getComponentName(), fullComponentVersion, componentLink, componentVersionLink, policyRuleList, item, templateData);
					}
				}
			} catch (final Exception e) {
				throw new HubItemTransformException(e);
			}
		}
	}

	@Override
	public void createContents(final FullProjectVersionView projectVersion, final String componentName, final ComponentVersionView componentVersion, final String componentUrl, final String componentVersionUrl,
			final List<PolicyRuleView> policyRuleList, final NotificationView item, final List<NotificationContentItem> templateData) throws URISyntaxException {
		final PolicyOverrideNotificationItem policyOverride = (PolicyOverrideNotificationItem) item;

		templateData.add(new PolicyOverrideContentItem(item.getCreatedAt(), projectVersion, componentName, componentVersion, componentUrl, componentVersionUrl, policyRuleList, policyOverride.getContent().getFirstName(),
				policyOverride.getContent().getLastName()));
	}

}
