package com.github.frankfarrell.snowball.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

/**
 * Created by Frank on 12/02/2016.
 */
public class WorkOrder {

    private final long id;
    private final OffsetDateTime timeStamp;

    //Uses These annotations because object is immutable and setters are not available
    @JsonCreator
    public WorkOrder(@JsonProperty("id") long id, @JsonProperty("timeStamp") OffsetDateTime timeStamp) {
        this.id = id;
        this.timeStamp = timeStamp;
    }

    @ApiModelProperty(value="ISO Timestamp", allowableValues = "2016-02-15T11:23:18+01:00", notes = "The timestamp the order was placed, in ISO format, eg \"2016-02-15T11:23:18+01:00\"", required = true)
    public OffsetDateTime getTimeStamp() {
        return timeStamp;
    }

    @ApiModelProperty(notes = "The id of the user who place the order", required = true)
    public long getId() {
        return id;
    }
}
