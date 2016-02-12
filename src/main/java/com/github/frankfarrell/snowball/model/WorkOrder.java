package com.github.frankfarrell.snowball.model;

import io.swagger.annotations.ApiModelProperty;

import java.time.Instant;

/**
 * Created by Frank on 12/02/2016.
 */
public class WorkOrder {

    private final long id;
    private final Instant timeStamp;

    public WorkOrder(long id, Instant timeStamp) {
        this.id = id;
        this.timeStamp = timeStamp;
    }

    @ApiModelProperty(notes = "The timestamp the order was placed", required = true)
    public Instant getTimeStamp() {
        return timeStamp;
    }

    @ApiModelProperty(notes = "The id of the user who place the order", required = true)
    public long getId() {
        return id;
    }
}
