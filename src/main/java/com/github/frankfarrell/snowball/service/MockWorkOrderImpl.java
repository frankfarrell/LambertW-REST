package com.github.frankfarrell.snowball.service;

import com.github.frankfarrell.snowball.model.QueuedWorkOrder;
import com.github.frankfarrell.snowball.model.WorkOrder;
import com.github.frankfarrell.snowball.model.WorkOrderClass;
import com.sun.corba.se.impl.ior.WireObjectKeyTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

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

    private List<WorkOrder> normalQueue;
    private List<WorkOrder> priorityQueue;
    private List<WorkOrder> vipQueue;
    private List<WorkOrder> managementQueue;

    public MockWorkOrderImpl() {
        normalQueue = new LinkedList<WorkOrder>();
        priorityQueue = new LinkedList<WorkOrder>();
        vipQueue = new LinkedList<WorkOrder>();
        managementQueue = new LinkedList<WorkOrder>();
    }


    public List<QueuedWorkOrder> getAllWorkOrders(){

        ArrayList<QueuedWorkOrder> allWorkOrders = new ArrayList<QueuedWorkOrder>(2);

        QueuedWorkOrder workOrder1 = new QueuedWorkOrder(1L, Instant.now(), Duration.ZERO, 5, WorkOrderClass.NOMRAL);
        QueuedWorkOrder workOrder2 = new QueuedWorkOrder(2L, Instant.now(), Duration.ZERO, 5, WorkOrderClass.NOMRAL);

        allWorkOrders.add(workOrder1);
        allWorkOrders.add(workOrder2);

        return allWorkOrders;

    }

    public void removeWorkOrder(long id){
        List<WorkOrder> queue = getQueueForId(id);

        //Find the index in the lst
        //queue.remove();
    }

    public QueuedWorkOrder getWorkOrder(long id){

        List<WorkOrder> queue = getQueueForId(id);
        //TODO find item in the queue
        return new QueuedWorkOrder(id, Instant.now(), Duration.ZERO, 5, WorkOrderClass.NOMRAL);
    }

    //TODO Split this out into methods to be reused
    //Eg get lazy stream order
    public WorkOrder popWorkOrder(){
        WorkOrder nextWorkOrder;
        if(managementQueue.size() > 0){
            nextWorkOrder =  managementQueue.get(0);
            managementQueue.remove(0);
        }
        else{
            WorkOrder nextNormal = normalQueue.get(0);
            WorkOrder nextPriority = priorityQueue.get(0);
            WorkOrder nextVip = vipQueue.get(0);

            Instant currentTime = Instant.now();

            long normalTime = nextNormal.getTimeStamp().until(currentTime, ChronoUnit.SECONDS);

            long priorityTime = nextPriority.getTimeStamp().until(currentTime, ChronoUnit.SECONDS);

            double priorityRank = Math.max(3, Math.log(priorityTime)*priorityTime);

            long vipTime = nextVip.getTimeStamp().until(currentTime, ChronoUnit.SECONDS);

            double vipRank = Math.max(4, Math.log(priorityTime)*priorityTime * 2);

            if(vipRank >= priorityRank && vipRank >= normalTime){
                nextWorkOrder = nextVip;
                vipQueue.remove(0);
            }
            else if(priorityRank >= vipRank  && priorityRank >= normalTime){
                nextWorkOrder = nextPriority;
                priorityQueue.remove(0);
            }
            else{
                nextWorkOrder = nextNormal;
                normalQueue.remove(0);
            }

        }
        return nextWorkOrder;

    }

    public QueuedWorkOrder pushWorkOrder(WorkOrder workOrder){

        List<WorkOrder> queue = getQueueForId(workOrder.getId());

        queue.add(workOrder);

        return new QueuedWorkOrder(workOrder.getId(), workOrder.getTimeStamp(), Duration.ZERO, getQueueLength(), getWorkOrderClass(workOrder.getId()));
    }

    private List<WorkOrder> getQueueForId(long id){
        if(id%3 == 0 && id%5 == 0){
            return managementQueue;
        }
        else if( id%5 == 0){
            return vipQueue;
        }
        else if (id%3 == 0 ){
            return priorityQueue;
        }
        else{
            return normalQueue;
        }
    }

    private WorkOrderClass getWorkOrderClass(long id){
        if(id%3 == 0 && id%5 == 0){
            return WorkOrderClass.MANAGEMENT_OVERRIDE;
        }
        else if( id%5 == 0){
            return WorkOrderClass.VIP;
        }
        else if (id%3 == 0 ){
            return WorkOrderClass.PRIORITY;
        }
        else{
            return WorkOrderClass.NOMRAL;
        }
    }

    private long getQueueLength(){
        return normalQueue.size() + priorityQueue.size() + vipQueue.size() + managementQueue.size();
    }

}
