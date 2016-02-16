package com.github.frankfarrell.snowball.model;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Created by Frank on 14/02/2016.
 */
@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum WorkOrderClass {
    NOMRAL("normal"),
    PRIORITY("priority"),
    VIP("vip"),
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
