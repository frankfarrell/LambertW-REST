package com.github.frankfarrell.snowball.service;

import com.github.frankfarrell.snowball.model.QueuedWorkOrder;
import com.github.frankfarrell.snowball.model.WorkOrder;
import com.github.frankfarrell.snowball.model.WorkOrderClass;
import com.sun.corba.se.impl.ior.WireObjectKeyTemplate;
import org.redisson.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by Frank on 12/02/2016.
 */
@Service
public class NonPeristentWorkOrderQueue implements WorkOrderQueue{

    /*
    How to implement priority queue with all the features below?
    4 Lists
    4 Priority Queues
    4 Sorted Set of EntrySets, eg an Ordered Hash Map that is unique on key
     */

    /*
    TODO
    Map<time ,id> sorted on time, from epoch.
     */

    private List<WorkOrder> normalQueue;
    private List<WorkOrder> priorityQueue;
    private List<WorkOrder> vipQueue;
    private List<WorkOrder> managementQueue;

    @Autowired
    public NonPeristentWorkOrderQueue(RedissonClient redisson) {
        normalQueue = new LinkedList<WorkOrder>();
        priorityQueue = new LinkedList<WorkOrder>();
        vipQueue = new LinkedList<WorkOrder>();
        managementQueue = new LinkedList<WorkOrder>();
    }


    /**
     * @return All WorkOrders Sorted By Current Priority
     * Returns id, time of entry, time in queue, position in queue, Type Of Order
     */
    @Override
    public List<QueuedWorkOrder> getAllWorkOrders(){

        /*
        Start with management q
         */
        final OffsetDateTime currentTime = OffsetDateTime.now();

        List<QueuedWorkOrder> managementOrders = IntStream.range(0, managementQueue.size())
                .mapToObj(i ->{
                    WorkOrder order = managementQueue.get(i);
                    return new QueuedWorkOrder(order.getId(),
                            order.getTimeStamp(),
                            order.getTimeStamp().until(currentTime,ChronoUnit.SECONDS),
                            i,
                            WorkOrderClass.MANAGEMENT_OVERRIDE );
                }).collect(Collectors.toList());

        /*
        Iter over the other three
         */
        return managementOrders;
    }

    /*
    Use to get the top order. Eg pick 1 from each list and sort
    Use to sort all
     */
    //private List<QueuedWorkOrder> sortWorkOrders(List<WorkOrder> orders, OffsetDateTime currentTime){
    //}

    @Override
    public void removeWorkOrder(Long id){
        List<WorkOrder> queue = getQueueForId(id);

        queue = queue.stream().filter(workOrder -> workOrder.getId() != id).collect(Collectors.toList());
    }

    @Override
    public QueuedWorkOrder getWorkOrder(Long id){

        List<WorkOrder> queue = getQueueForId(id);
        //TODO find item in the queue
        return new QueuedWorkOrder(id, OffsetDateTime.now(), 0, 5, WorkOrderClass.NOMRAL);
    }

    //TODO Split this out into methods to be reused
    //Eg get lazy stream order
    @Override
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

            OffsetDateTime currentTime = OffsetDateTime.now();

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

    @Override
    public QueuedWorkOrder pushWorkOrder(WorkOrder workOrder){

        List<WorkOrder> queue = getQueueForId(workOrder.getId());

        queue.add(workOrder);

        return new QueuedWorkOrder(workOrder.getId(), workOrder.getTimeStamp(), 0, getQueueLength(), getWorkOrderClass(workOrder.getId()));
    }

    /*
    For a given id it returns ranking function - Rank higher number is better, but mngmt override always wins
    getPriorityFunctionForId(4) returns function(id, duration) return long
     */
    /*
    private BiFunction<Long, Double, Long> getPriorityFunctionForId(long id){
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
    */

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
