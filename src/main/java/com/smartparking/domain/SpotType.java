package com.smartparking.domain;

/** Physical size class of a parking spot. */
public enum SpotType {
    SMALL, MEDIUM, LARGE;

    public String code() {
        return name().substring(0, 1);
    }
}
