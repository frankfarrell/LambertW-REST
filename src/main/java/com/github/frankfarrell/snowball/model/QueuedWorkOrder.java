package com.github.frankfarrell.snowball.model;


import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

/**
 * Created by Frank on 14/02/2016.
 */

/**
 * Representation of a work order that is enqueued
 */
public class QueuedWorkOrder extends WorkOrder{

    private long positionInQueue;
    private Duration durationInQueue;
    private WorkOrderClass workOrderClass;

    public QueuedWorkOrder(long id, OffsetDateTime timeStamp, Duration durationInQueue, long positionInQueue, WorkOrderClass workOrderClass) {
        super(id, timeStamp);
        this.durationInQueue = durationInQueue;
        this.positionInQueue = positionInQueue;
        this.workOrderClass = workOrderClass;
    }

    @JsonSerialize(using = DurationSerializer.class)
    public Duration getDurationInQueue() {
        return durationInQueue;
    }

    public long getPositionInQueue() {
        return positionInQueue;
    }

    public WorkOrderClass getWorkOrderClass() {
        return workOrderClass;
    }

    public static class DurationSerializer extends JsonSerializer<Duration> {
        @Override
        public void serialize(Duration duration, JsonGenerator json, SerializerProvider provider) throws IOException, JsonProcessingException {
            json.writeNumber(duration.getSeconds());
        }
    }

}
