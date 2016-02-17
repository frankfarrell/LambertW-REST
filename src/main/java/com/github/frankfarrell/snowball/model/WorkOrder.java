package com.github.frankfarrell.snowball.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

/**
 * Created by Frank on 12/02/2016.
 */
public class WorkOrder {

    @Min(value=1)
    @Max(value= Long.MAX_VALUE) //2^63-1 or 9223372036854775807
    private final Long id;
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
    public Long getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WorkOrder workOrder = (WorkOrder) o;

        if (id != workOrder.id) return false;
        return timeStamp.equals(workOrder.timeStamp);

    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + timeStamp.hashCode();
        return result;
    }
}
