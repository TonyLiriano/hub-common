/*******************************************************************************
 * Copyright (C) 2016 Black Duck Software, Inc.
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
 *******************************************************************************/
package com.blackducksoftware.integration.hub;

import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.EnumSet;
import java.util.Set;

import org.restlet.resource.ResourceException;

import com.blackducksoftware.integration.hub.api.version.VersionComparison;
import com.blackducksoftware.integration.hub.capabilities.HubCapabilitiesEnum;
import com.blackducksoftware.integration.hub.exception.BDRestException;
import com.blackducksoftware.integration.log.IntLogger;

public class HubSupportHelper implements Serializable {
    private static final long serialVersionUID = 6440466357358359056L;

    private boolean hasBeenChecked = false;

    private final Set<HubCapabilitiesEnum> capabilities = EnumSet.noneOf(HubCapabilitiesEnum.class);

    public boolean isHasBeenChecked() {
        return hasBeenChecked;
    }

    public void setHasBeenChecked(final boolean hasBeenChecked) {
        this.hasBeenChecked = hasBeenChecked;
    }

    /**
     * This will check the Hub server to see which options this version of the
     * Hub supports. You can use the get methods in this class after this method
     * has run to get the supported options.
     */
    public void checkHubSupport(final HubIntRestService service, final IntLogger logger)
            throws IOException, URISyntaxException {
        try {
            final String hubServerVersion = service.getHubVersion();

            if (compareVersion(hubServerVersion, "3.3.1", service)) {
                setHub3_3_1Support();
                setHub3_1Support();
                setHub3_0Support();
            } else if (compareVersion(hubServerVersion, "3.1.0", service)) {
                setHub3_1Support();
                setHub3_0Support();
            } else {
                if (compareVersion(hubServerVersion, "3.0.0", service)) {
                    setHub3_0Support();
                }
            }
            setHasBeenChecked(true);
        } catch (final BDRestException e) {
            ResourceException resEx = null;
            if (e.getCause() != null && e.getCause() instanceof ResourceException) {
                resEx = (ResourceException) e.getCause();
            }
            if (resEx != null) {
                if (logger != null) {
                    logger.error(resEx.getMessage());
                }
            }
            if (logger != null) {
                logger.error(e.getMessage());
            }
        }
    }

    /**
     * This method will check the provided version against the actual version of
     * the Hub server. If the provided version is less than or equal to the
     * server version we return true. If the provided version is greater than
     * the server version we return false.
     */
    private boolean compareVersion(final String hubServerVersion, final String testVersion,
            final HubIntRestService service) throws IOException, BDRestException, URISyntaxException {
        try {
            final String[] splitServerVersion = hubServerVersion.split("\\.");
            final String[] splitTestVersion = testVersion.split("\\.");

            final Integer[] serverVersionParts = new Integer[splitServerVersion.length];
            final Integer[] testVersionParts = new Integer[splitTestVersion.length];
            boolean isServerSnapshot = false;
            for (int i = 0; i < splitServerVersion.length; i++) {
                String currentServerPart = splitServerVersion[i];
                final String currentTestVersionPart = splitTestVersion[i];
                if (currentServerPart.contains("-SNAPSHOT")) {
                    isServerSnapshot = true;
                    currentServerPart = currentServerPart.replace("-SNAPSHOT", "");
                }
                serverVersionParts[i] = Integer.valueOf(currentServerPart);
                testVersionParts[i] = Integer.valueOf(currentTestVersionPart);
            }

            if (serverVersionParts[0] > testVersionParts[0]) {
                // Major part of the server version was greater,
                // so we know it supports whatever feature we are testing for
                return true;
            } else if (serverVersionParts[0] < testVersionParts[0]) {
                // Major part of the server version was less than the one
                // provided,
                // so we know it does not support whatever feature we are
                // testing for
                return false;
            }

            if (serverVersionParts[1] > testVersionParts[1]) {
                // Minor part of the server version was greater,
                // so we know it supports whatever feature we are testing for
                return true;
            } else if (serverVersionParts[1] < testVersionParts[1]) {
                // Minor part of the server version was less than the one
                // provided,
                // so we know it does not support whatever feature we are
                // testing for
                return false;
            }

            if (serverVersionParts[2] > testVersionParts[2]) {
                // Fix version part of the server version was greater,
                // so we know it supports whatever feature we are testing for
                return true;
            } else if (serverVersionParts[2] < testVersionParts[2]) {
                // Fix version part of the server version was less than the one
                // provided,
                // so we know it does not support whatever feature we are
                // testing for
                return false;
            }

            // The versions are identical, check if the server is a SNAPSHOT
            if (isServerSnapshot) {
                // We assume the SNAPSHOT version is less than the released
                // version
                return false;
            }
        } catch (final NumberFormatException e) {
            return fallBackComparison(testVersion, service);
        } catch (final ArrayIndexOutOfBoundsException e) {
            return fallBackComparison(testVersion, service);
        }

        return true;
    }

    public String getHubVersion(final HubIntRestService service)
            throws IOException, BDRestException, URISyntaxException {
        String hubVersion = service.getHubVersion();
        hubVersion = hubVersion.replaceAll("\"", "");
        return hubVersion;
    }

    /**
     * We are parsing the versions incorrectly so we let the Hub server compare
     * the test version to the server version. We return true if the testVersion
     * is less than or equal to the server version We return false if the
     * testVersion is greater than the server version.
     *
     */
    private boolean fallBackComparison(final String testVersion, final HubIntRestService service)
            throws IOException, BDRestException, URISyntaxException {
        final VersionComparison comparison = service.compareWithHubVersion(testVersion);
        if (comparison.getNumericResult() <= 0) {
            return true;
        } else {
            return false;
        }
    }

    public boolean hasCapability(final HubCapabilitiesEnum capability) {
        return capabilities.contains(capability);
    }

    private void setHub3_0Support() {
        capabilities.add(HubCapabilitiesEnum.JRE_PROVIDED);
        capabilities.add(HubCapabilitiesEnum.POLICY_API);
        capabilities.add(HubCapabilitiesEnum.CLI_STATUS_DIRECTORY_OPTION);
    }

    private void setHub3_1Support() {
        capabilities.add(HubCapabilitiesEnum.CLI_PASSWORD_ENVIRONMENT_VARIABLE);
    }

    private void setHub3_3_1Support() {
        capabilities.add(HubCapabilitiesEnum.BOM_FILE_UPLOAD);
    }

}
