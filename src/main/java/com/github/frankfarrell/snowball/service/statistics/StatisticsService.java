package com.github.frankfarrell.snowball.service.statistics;

import com.github.frankfarrell.snowball.model.QueuedWorkOrder;
import com.github.frankfarrell.snowball.model.statistics.QueueStatistics;
import com.github.frankfarrell.snowball.model.statistics.StatisticType;
import com.github.frankfarrell.snowball.model.statistics.StatisticalSummaryRequest;
import com.github.frankfarrell.snowball.service.WorkOrderQueue;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;

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
    public QueueStatistics getStatistics(final StatisticalSummaryRequest statisticalSummary){
        List<QueuedWorkOrder> filteredWorkOrders = this.workOrderQueue
                .getAllWorkOrders()
                .stream()
                .filter(order ->
                {
                    if(statisticalSummary.getFilters().size()>0){
                        return statisticalSummary.getFilters().contains(order.getWorkOrderClass());
                    }
                    else{
                        return true;
                    }
                })
                .collect(Collectors.toList());

        QueueStatistics queueStatistics = new QueueStatistics(filteredWorkOrders.size());

        /*
        Done this way to allow for service to be more extensible in future
         */
        statisticalSummary.getStatistics().forEach(statType->
        {
            switch (statType){
                case AVERAGE_WAIT:
                    queueStatistics.setAverageDurationInQueue(getAverageWaitTime(filteredWorkOrders));
                    break;
                default:
                    break;
            }
        });


        return queueStatistics;
    }

    public double getAverageWaitTime(List<QueuedWorkOrder> workOrders){
        // Expect time from queue, since we have already claculated order there
        return workOrders
                .stream()
                .mapToLong(QueuedWorkOrder::getDurationInQueue)
                .average()
                .getAsDouble();
    }
}
