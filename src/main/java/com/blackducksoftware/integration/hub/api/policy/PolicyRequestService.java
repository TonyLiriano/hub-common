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
package com.blackducksoftware.integration.hub.api.policy;

import static com.blackducksoftware.integration.hub.api.UrlConstants.SEGMENT_API;
import static com.blackducksoftware.integration.hub.api.UrlConstants.SEGMENT_POLICY_RULES;

import java.util.Arrays;
import java.util.List;

import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.blackducksoftware.integration.hub.rest.RestConnection;
import com.blackducksoftware.integration.hub.service.HubParameterizedRequestService;

import io.swagger.client.model.PolicyRuleView;

public class PolicyRequestService extends HubParameterizedRequestService<PolicyRuleView> {
	private static final List<String> POLICY_RULE_SEGMENTS = Arrays.asList(SEGMENT_API, SEGMENT_POLICY_RULES);

	public PolicyRequestService(final RestConnection restConnection) {
		super(restConnection, PolicyRuleView.class);
	}

	public List<PolicyRuleView> getAllPolicyRules() throws HubIntegrationException {
		final List<PolicyRuleView> allPolicyRuleItems = getAllItems(POLICY_RULE_SEGMENTS);
		return allPolicyRuleItems;
	}

}
