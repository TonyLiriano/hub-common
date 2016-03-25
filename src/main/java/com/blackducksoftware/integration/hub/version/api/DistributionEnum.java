package com.blackducksoftware.integration.hub.version.api;


public enum DistributionEnum {

    EXTERNAL("External")
    ,
    SAAS("SaaS")
    ,
    INTERNAL("Internal")
    ,
    OPENSOURCE("Open Source")
    ,
    UNKNOWNDISTRIBUTION("Unknown Distribution");

    private final String displayValue;

    private DistributionEnum(String displayValue) {
        this.displayValue = displayValue;
    }

    public String getDisplayValue() {
        return displayValue;
    }

    public static DistributionEnum getDistributionByDisplayValue(String displayValue) {
        for (DistributionEnum currentEnum : DistributionEnum.values()) {
            if (currentEnum.getDisplayValue().equalsIgnoreCase(displayValue)) {
                return currentEnum;
            }
        }
        return DistributionEnum.UNKNOWNDISTRIBUTION;
    }

    public static DistributionEnum getDistributionEnum(String distribution) {
        if (distribution == null) {
            return DistributionEnum.UNKNOWNDISTRIBUTION;
        }
        DistributionEnum distributionEnum;
        try {
            distributionEnum = DistributionEnum.valueOf(distribution.toUpperCase());
        } catch (IllegalArgumentException e) {
            // ignore expection
            distributionEnum = UNKNOWNDISTRIBUTION;
        }
        return distributionEnum;
    }
}