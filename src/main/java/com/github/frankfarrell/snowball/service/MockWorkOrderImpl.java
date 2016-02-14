package com.github.frankfarrell.snowball.service;

import com.github.frankfarrell.snowball.model.QueuedWorkOrder;
import com.github.frankfarrell.snowball.model.WorkOrder;
import com.github.frankfarrell.snowball.model.WorkOrderClass;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Frank on 12/02/2016.
 */
@Service
public class MockWorkOrderImpl implements WorkOrderQueue{

    /*
    How to implement priority queue with all the features below?
    4 Lists
    4 Priority Queues
    4 Sorted Set of EntrySets, eg an Ordered Hash Map that is unique on key
     */

    

    public List<QueuedWorkOrder> getAllWorkOrders(){

        ArrayList<QueuedWorkOrder> allWorkOrders = new ArrayList<QueuedWorkOrder>(2);

        QueuedWorkOrder workOrder1 = new QueuedWorkOrder(1L, Instant.now(), Duration.ZERO, 5, WorkOrderClass.NOMRAL);
        QueuedWorkOrder workOrder2 = new QueuedWorkOrder(2L, Instant.now(), Duration.ZERO, 5, WorkOrderClass.NOMRAL);

        allWorkOrders.add(workOrder1);
        allWorkOrders.add(workOrder2);

        return allWorkOrders;

    }

    public void removeWorkOrder(long id){

    }

    public QueuedWorkOrder getWorkOrder(long id){
        return new QueuedWorkOrder(id, Instant.now(), Duration.ZERO, 5, WorkOrderClass.NOMRAL);
    }

    public WorkOrder popWorkOrder(){
        return new WorkOrder(1L, Instant.now());
    }

    public QueuedWorkOrder pushWorkOrder(WorkOrder workOrder){
        return new QueuedWorkOrder(workOrder.getId(), workOrder.getTimeStamp(), Duration.ZERO, 5, WorkOrderClass.NOMRAL);
    }

}
