package com.github.frankfarrell.snowball.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by Frank on 14/02/2016.
 */
@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum WorkOrderClass {
    @JsonProperty("normal")
    NOMRAL("normal"),

    @JsonProperty("priority")
    PRIORITY("priority"),

    @JsonProperty("vip")
    VIP("vip"),

    @JsonProperty("management")
    MANAGEMENT_OVERRIDE("management");

    private final String orderClass;

    private WorkOrderClass(String s) {
        orderClass = s;
    }

    public boolean equalsName(String otherName) {
        return (otherName == null) ? false : orderClass.equals(otherName);
    }

    public String toString() {
        return this.orderClass;
    }
}
