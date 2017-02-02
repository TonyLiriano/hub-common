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
package com.blackducksoftware.integration.hub.dataservice.notification.model;

import org.apache.commons.lang3.builder.RecursiveToStringStyle;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.joda.time.DateTime;

import com.google.common.base.Joiner;

import com.blackducksoftware.integration.hub.model.ComponentVersionView;

public class NotificationContentItem implements Comparable<NotificationContentItem> {
	private final FullProjectVersionView projectVersion;

	private final String componentName;

	private final ComponentVersionView componentVersion;

	private final String componentVersionUrl;

	// We need createdAt (from the enclosing notificationItem) so we can order
	// them after
	// they are collected multi-threaded
	public final DateTime createdAt;

	public NotificationContentItem(final DateTime createdAt, final FullProjectVersionView projectVersion, final String componentName, final ComponentVersionView componentVersion, final String componentVersionUrl) {
		this.createdAt = createdAt;
		this.projectVersion = projectVersion;
		this.componentName = componentName;
		this.componentVersion = componentVersion;
		this.componentVersionUrl = componentVersionUrl;
	}

	public FullProjectVersionView getProjectVersion() {
		return projectVersion;
	}

	public String getComponentName() {
		return componentName;
	}

	public ComponentVersionView getComponentVersion() {
		return componentVersion;
	}

	public String getComponentVersionUrl() {
		return componentVersionUrl;
	}

	public DateTime getCreatedAt() {
		return createdAt;
	}

	@Override
	public String toString() {
		return ReflectionToStringBuilder.toString(this, RecursiveToStringStyle.JSON_STYLE);
	}

	@Override
	public int compareTo(final NotificationContentItem o) {
		if (equals(o)) {
			return 0;
		}

		final int createdAtComparison = getCreatedAt().compareTo(o.getCreatedAt());
		if (createdAtComparison != 0) {
			// If createdAt times are different, use createdAt to compare
			return createdAtComparison;
		}

		// Identify same-time non-equal items as non-equal
		final Joiner joiner = Joiner.on(":").skipNulls();
		final String thisProjectVersionString = joiner.join(getProjectVersion().getProjectName(), getProjectVersion().getVersionName(), getComponentVersionUrl());
		final String otherProjectVersionString = joiner.join(o.getProjectVersion().getProjectName(), o.getProjectVersion().getVersionName(), o.getComponentVersionUrl().toString());

		return thisProjectVersionString.compareTo(otherProjectVersionString);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getComponentVersionUrl() == null) ? 0 : getComponentVersionUrl().hashCode());
		result = prime * result + ((createdAt == null) ? 0 : createdAt.hashCode());
		result = prime * result + ((projectVersion == null) ? 0 : projectVersion.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final NotificationContentItem other = (NotificationContentItem) obj;
		if (getComponentVersionUrl() == null) {
			if (other.getComponentVersionUrl() != null) {
				return false;
			}
		} else if (!getComponentVersionUrl().equals(other.getComponentVersionUrl())) {
			return false;
		}
		if (createdAt == null) {
			if (other.createdAt != null) {
				return false;
			}
		} else if (!createdAt.equals(other.createdAt)) {
			return false;
		}
		if (projectVersion == null) {
			if (other.projectVersion != null) {
				return false;
			}
		} else if (!projectVersion.equals(other.projectVersion)) {
			return false;
		}
		return true;
	}

}
