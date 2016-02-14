package com.github.frankfarrell.snowball.model;


import java.time.Duration;
import java.time.Instant;

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

    public QueuedWorkOrder(long id, Instant timeStamp, Duration durationInQueue, long positionInQueue, WorkOrderClass workOrderClass) {
        super(id, timeStamp);
        this.durationInQueue = durationInQueue;
        this.positionInQueue = positionInQueue;
        this.workOrderClass = workOrderClass;
    }

    public Duration getDurationInQueue() {
        return durationInQueue;
    }

    public long getPositionInQueue() {
        return positionInQueue;
    }

    public WorkOrderClass getWorkOrderClass() {
        return workOrderClass;
    }
}
