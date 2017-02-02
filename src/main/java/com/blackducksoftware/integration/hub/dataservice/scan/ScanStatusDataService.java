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
package com.blackducksoftware.integration.hub.dataservice.scan;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.blackducksoftware.integration.hub.api.codelocation.CodeLocationRequestService;
import com.blackducksoftware.integration.hub.api.item.MetaService;
import com.blackducksoftware.integration.hub.api.project.ProjectRequestService;
import com.blackducksoftware.integration.hub.api.project.version.ProjectVersionRequestService;
import com.blackducksoftware.integration.hub.api.scan.ScanStatus;
import com.blackducksoftware.integration.hub.api.scan.ScanSummaryRequestService;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.blackducksoftware.integration.hub.exception.HubTimeoutExceededException;
import com.blackducksoftware.integration.hub.rest.RestConnection;
import com.blackducksoftware.integration.hub.service.HubRequestService;
import com.blackducksoftware.integration.log.IntLogger;

import com.blackducksoftware.integration.hub.model.CodeLocationView;
import com.blackducksoftware.integration.hub.model.ProjectVersionView;
import com.blackducksoftware.integration.hub.model.ProjectView;
import com.blackducksoftware.integration.hub.model.ScanSummaryView;
import com.blackducksoftware.integration.hub.model.ScanSummaryView.StatusEnum;

public class ScanStatusDataService extends HubRequestService {
	private static final long FIVE_SECONDS = 5 * 1000;

	private static final long DEFAULT_TIMEOUT = 300000l;

	private final ProjectRequestService projectRequestService;

	private final ProjectVersionRequestService projectVersionRequestService;

	private final CodeLocationRequestService codeLocationRequestService;

	private final ScanSummaryRequestService scanSummaryRequestService;

	private final MetaService metaService;

	private final long timeoutInMilliseconds;

	public ScanStatusDataService(final IntLogger logger, final RestConnection restConnection, final ProjectRequestService projectRequestService, final ProjectVersionRequestService projectVersionRequestService,
			final CodeLocationRequestService codeLocationRequestService, final ScanSummaryRequestService scanSummaryRequestService, final MetaService metaService, final long timeoutInMilliseconds) {
		super(restConnection);
		this.metaService = metaService;
		this.projectRequestService = projectRequestService;
		this.projectVersionRequestService = projectVersionRequestService;
		this.codeLocationRequestService = codeLocationRequestService;
		this.scanSummaryRequestService = scanSummaryRequestService;

		long timeout = timeoutInMilliseconds;
		if (timeoutInMilliseconds <= 0l) {
			timeout = DEFAULT_TIMEOUT;
			logger.alwaysLog(timeoutInMilliseconds + "ms is not a valid BOM wait time, using : " + timeout + "ms instead");
		}
		this.timeoutInMilliseconds = timeout;
	}

	/**
	 * For the provided projectName and projectVersion, wait at most
	 * timeoutInMilliseconds for the project/version to exist and/or
	 * at least one pending bom import scan to begin. Then, wait at most
	 * timeoutInMilliseconds for all discovered pending scans to
	 * complete.
	 *
	 * If the timeouts are exceeded, a HubTimeoutExceededException will be
	 * thrown.
	 *
	 */
	public void assertBomImportScanStartedThenFinished(final String projectName, final String projectVersion) throws HubTimeoutExceededException, HubIntegrationException {
		final List<ScanSummaryView> pendingScans = waitForPendingScansToStart(projectName, projectVersion, timeoutInMilliseconds);
		waitForScansToComplete(pendingScans, timeoutInMilliseconds);
	}

	/**
	 * For the given pendingScans, wait at most
	 * timeoutInMilliseconds for the scans to complete.
	 *
	 * If the timeout is exceeded, a HubTimeoutExceededException will be thrown.
	 *
	 */
	public void assertBomImportScansFinished(final List<ScanSummaryView> pendingScans) throws HubTimeoutExceededException, HubIntegrationException {
		waitForScansToComplete(pendingScans, timeoutInMilliseconds);
	}

	private List<ScanSummaryView> waitForPendingScansToStart(final String projectName, final String projectVersion, final long scanStartedTimeoutInMilliseconds) throws HubIntegrationException {
		List<ScanSummaryView> pendingScans = getPendingScans(projectName, projectVersion);
		final long startedTime = System.currentTimeMillis();
		boolean pendingScansOk = pendingScans.size() > 0;
		while (!done(pendingScansOk, scanStartedTimeoutInMilliseconds, startedTime, "No scan has started within the specified wait time: %d minutes")) {
			try {
				Thread.sleep(FIVE_SECONDS);
			} catch (final InterruptedException e) {
				throw new HubIntegrationException("The thread waiting for the scan to start was interrupted: " + e.getMessage(), e);
			}
			pendingScans = getPendingScans(projectName, projectVersion);
			pendingScansOk = pendingScans.size() > 0;
		}

		return pendingScans;
	}

	private void waitForScansToComplete(List<ScanSummaryView> pendingScans, final long scanStartedTimeoutInMilliseconds) throws HubTimeoutExceededException, HubIntegrationException {
		pendingScans = getPendingScans(pendingScans);
		final long startedTime = System.currentTimeMillis();
		boolean pendingScansOk = pendingScans.isEmpty();
		while (!done(pendingScansOk, scanStartedTimeoutInMilliseconds, startedTime, "The pending scans have not completed within the specified wait time: %d minutes")) {
			try {
				Thread.sleep(FIVE_SECONDS);
			} catch (final InterruptedException e) {
				throw new HubIntegrationException("The thread waiting for the scan to complete was interrupted: " + e.getMessage(), e);
			}
			pendingScans = getPendingScans(pendingScans);
			pendingScansOk = pendingScans.isEmpty();
		}
	}

	private boolean done(final boolean pendingScansOk, final long timeoutInMilliseconds, final long startedTime, final String timeoutMessage) throws HubTimeoutExceededException {
		if (pendingScansOk) {
			return true;
		}

		if (takenTooLong(timeoutInMilliseconds, startedTime)) {
			throw new HubTimeoutExceededException(String.format(timeoutMessage, TimeUnit.MILLISECONDS.toMinutes(timeoutInMilliseconds)));
		}

		return false;
	}

	private boolean takenTooLong(final long timeoutInMilliseconds, final long startedTime) {
		final long elapsed = System.currentTimeMillis() - startedTime;
		return elapsed > timeoutInMilliseconds;
	}

	private List<ScanSummaryView> getPendingScans(final String projectName, final String projectVersion) {
		List<ScanSummaryView> pendingScans = new ArrayList<>();
		try {
			final ProjectView projectItem = projectRequestService.getProjectByName(projectName);
			final ProjectVersionView projectVersionItem = projectVersionRequestService.getProjectVersion(projectItem, projectVersion);
			final String projectVersionUrl = metaService.getHref(projectVersionItem);

			final List<CodeLocationView> allCodeLocations = codeLocationRequestService.getAllCodeLocationsForCodeLocationType(CodeLocationView.TypeEnum.BOM_IMPORT);

			final List<String> allScanSummariesLinks = new ArrayList<>();
			for (final CodeLocationView codeLocationItem : allCodeLocations) {
				final String mappedProjectVersionUrl = codeLocationItem.getMappedProjectVersion();
				if (projectVersionUrl.equals(mappedProjectVersionUrl)) {
					final String scanSummariesLink = metaService.getFirstLink(codeLocationItem, MetaService.SCANS_LINK);
					allScanSummariesLinks.add(scanSummariesLink);
				}
			}

			final List<ScanSummaryView> allScanSummaries = new ArrayList<>();
			for (final String scanSummaryLink : allScanSummariesLinks) {
				allScanSummaries.addAll(scanSummaryRequestService.getAllScanSummaryItems(scanSummaryLink));
			}

			pendingScans = new ArrayList<>();
			for (final ScanSummaryView scanSummaryItem : allScanSummaries) {
				if (isPending(scanSummaryItem.getStatus())) {
					pendingScans.add(scanSummaryItem);
				}
			}
		} catch (final Exception e) {
			pendingScans = new ArrayList<>();
			// ignore, since we might not have found a project or version, etc
			// so just keep waiting until the timeout
		}

		return pendingScans;
	}

	private List<ScanSummaryView> getPendingScans(final List<ScanSummaryView> scanSummaries) throws HubIntegrationException {
		final List<ScanSummaryView> pendingScans = new ArrayList<>();
		for (final ScanSummaryView scanSummaryItem : scanSummaries) {
			final String scanSummaryLink = metaService.getHref(scanSummaryItem);
			final ScanSummaryView currentScanSummaryItem = scanSummaryRequestService.getItem(scanSummaryLink);
			if (isPending(currentScanSummaryItem.getStatus())) {
				pendingScans.add(currentScanSummaryItem);
			} else if (isError(currentScanSummaryItem.getStatus())) {
				throw new HubIntegrationException("There was a problem in the Hub processing the scan(s). Error Status : " + currentScanSummaryItem.getStatus().toString() + ", " + currentScanSummaryItem.getStatusMessage());
			}
		}

		return pendingScans;
	}

	private boolean isPending(StatusEnum statusEnum) throws HubIntegrationException {
		return ScanStatus.getScanStatus(statusEnum.toString()).isPending();
	}

	private boolean isError(StatusEnum statusEnum) throws HubIntegrationException {
		return ScanStatus.getScanStatus(statusEnum.toString()).isError();
	}

}
