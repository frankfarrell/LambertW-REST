package com.github.frankfarrell.snowball.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
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

    private final Long positionInQueue;
    private final Long durationInQueue; //In seconds
    private final WorkOrderClass workOrderClass;

    private Double rank;

    public QueuedWorkOrder(long id, OffsetDateTime timeStamp, long durationInQueue, long positionInQueue, WorkOrderClass workOrderClass) {
        super(id, timeStamp);
        this.durationInQueue = durationInQueue;
        this.positionInQueue = positionInQueue;
        this.workOrderClass = workOrderClass;
    }

    public long getDurationInQueue() {
        return durationInQueue;
    }

    public long getPositionInQueue() {
        return positionInQueue;
    }

    public WorkOrderClass getWorkOrderClass() {
        return workOrderClass;
    }

    @JsonIgnore
    public Double getRank() {
        return rank;
    }

    public void setRank(Double rank) {
        this.rank = rank;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        QueuedWorkOrder that = (QueuedWorkOrder) o;

        if (!positionInQueue.equals(that.positionInQueue)) return false;
        if (!durationInQueue.equals(that.durationInQueue)) return false;
        return workOrderClass == that.workOrderClass;

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + positionInQueue.hashCode();
        result = 31 * result + durationInQueue.hashCode();
        result = 31 * result + workOrderClass.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "QueuedWorkOrder{" +
                "id="+ getId() +
                "timeStamp+"+getTimeStamp().toString()+
                "positionInQueue=" + positionInQueue +
                ", durationInQueue=" + durationInQueue +
                ", workOrderClass=" + workOrderClass +
                ", rank=" + rank +
                '}';
    }
}
