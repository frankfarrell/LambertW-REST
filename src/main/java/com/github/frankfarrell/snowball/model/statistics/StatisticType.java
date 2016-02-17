package com.github.frankfarrell.snowball.model.statistics;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by Frank on 17/02/2016.
 */
public enum StatisticType {

    @JsonProperty("averageWait")
    AVERAGE_WAIT("averageWait");

    private final String statType;

    private StatisticType(String s) {
        statType = s;
    }

    public boolean equalsName(String otherName) {
        return (otherName == null) ? false : statType.equals(otherName);
    }

    public String toString() {
        return this.statType;
    }
}
