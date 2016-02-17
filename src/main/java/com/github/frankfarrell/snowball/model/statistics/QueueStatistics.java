package com.github.frankfarrell.snowball.model.statistics;

/**
 * Created by Frank on 17/02/2016.
 */
public class QueueStatistics {

    private final long queueSize;

    private double averageDurationInQueue;

    public QueueStatistics(long queueSize) {
        this.queueSize = queueSize;
    }

    public double getAverageDurationInQueue() {
        return averageDurationInQueue;
    }

    public long getQueueSize() {
        return queueSize;
    }

    public void setAverageDurationInQueue(double averageDurationInQueue) {
        this.averageDurationInQueue = averageDurationInQueue;
    }

    //Extend this with further statistics
    //private double maxWait;
    //ExpectedWaitPerType
    //variance
}
