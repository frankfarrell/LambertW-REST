package com.github.frankfarrell.snowball.service.statistics;

import com.github.frankfarrell.snowball.model.QueuedWorkOrder;
import com.github.frankfarrell.snowball.model.statistics.StatisticalSummary;
import com.github.frankfarrell.snowball.model.statistics.StatisticalSummaryRequest;
import com.github.frankfarrell.snowball.service.WorkOrderQueue;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;

/**
 * Created by Frank on 14/02/2016.
 */
public class StatisticsService {

    private WorkOrderQueue workOrderQueue;

    @Autowired
    public StatisticsService(WorkOrderQueue workOrderQueue) {
        this.workOrderQueue = workOrderQueue;
    }

    /*
    Idea here is that we can return variety of stats
    Eg different statistics
    Filtered on different types, etc
    Liek a sort of query language
     */
    public StatisticalSummary getStatistics(StatisticalSummaryRequest statisticalSummary){
        return new StatisticalSummary();
    }

    public double getAverageWaitTime(){
        // Expect time from queue, since we have already claculated order there
        return this.workOrderQueue
                .getAllWorkOrders()
                .stream()
                .mapToLong( order ->
                        order.getDurationInQueue()
                ).average()
                .getAsDouble();
    }

}
