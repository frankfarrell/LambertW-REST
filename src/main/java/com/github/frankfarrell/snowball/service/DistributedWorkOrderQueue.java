package com.github.frankfarrell.snowball.service;

import com.github.frankfarrell.snowball.exceptions.AlreadyExistsException;
import com.github.frankfarrell.snowball.exceptions.NotFoundException;
import com.github.frankfarrell.snowball.model.QueuedWorkOrder;
import com.github.frankfarrell.snowball.model.WorkOrder;
import com.github.frankfarrell.snowball.model.WorkOrderClass;
import com.github.frankfarrell.snowball.service.WorkOrderQueue;
import io.swagger.models.auth.In;
import org.redisson.RedissonClient;
import org.redisson.core.RScoredSortedSet;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;

/**
 * Created by Frank on 16/02/2016.
 *
 * Distributed Priority queue backed by Redis.
 * Using Redisson distributed collections.
 *
 * Implementation:
 * Uses 4 redis ScoredSortedSets (one per WorkOrderType)
 * Scores the WorkOrders by Seconds since the EPOCH, GMT
 * Set property means that only one instance of Id can be stored in queue
 *
 * Future Extensions: Allows for Time To love on set entries
 */
public class DistributedWorkOrderQueue implements WorkOrderQueue {

    private RScoredSortedSet<Long> normalQueue;
    private RScoredSortedSet<Long> priorityQueue;
    private RScoredSortedSet<Long> vipQueue;
    private RScoredSortedSet<Long> managementQueue;

    private final OffsetDateTime EPOCH = OffsetDateTime.ofInstant(Instant.EPOCH, ZoneId.of("UTC"));

    private RedissonClient redisson;

    @Autowired
    public DistributedWorkOrderQueue(RedissonClient redisson) {
        this.redisson = redisson;
        normalQueue = redisson.getScoredSortedSet("normal");
        priorityQueue =  redisson.getScoredSortedSet("priority");
        vipQueue = redisson.getScoredSortedSet("vip");
        managementQueue =  redisson.getScoredSortedSet("management");
    }

    @Override
    public List<QueuedWorkOrder> getAllWorkOrders() {
        /*
        Get all queues.
        Map through function -> getComparatorForClass
        Sort All
         */

        


        return null;
    }

    @Override
    public QueuedWorkOrder getWorkOrder(Long id) {

        final RScoredSortedSet<Long> queue = getQueueForId(id);

        if(queue.contains(id)){
            OffsetDateTime timestamp = EPOCH.plus(queue.getScore(id).longValue(), ChronoUnit.SECONDS);

            long durationInQueue =  timestamp.until(OffsetDateTime.now(), ChronoUnit.SECONDS);
            long positionInQueue = getPositionForId(id);
            WorkOrderClass workOrderClass =getWorkOrderClass(id);

            return new QueuedWorkOrder(id, timestamp, durationInQueue, positionInQueue, workOrderClass);
        }
        else{
            throw new NotFoundException("id", id);
        }
    }

    @Override
    public void removeWorkOrder(Long id) {
        final RScoredSortedSet<Long> queue = getQueueForId(id);
        queue.remove(id);
    }

    @Override
    public WorkOrder popWorkOrder() {
        /*
        Get top from each queue
        Pick one that is first according to sorting
         */
        return null;
    }

    @Override
    public QueuedWorkOrder pushWorkOrder(WorkOrder workOrder) {

        final RScoredSortedSet<Long> queue = getQueueForId(workOrder.getId());

        if(queue.contains(workOrder.getId())){
            throw new AlreadyExistsException("id", workOrder.getId());
        }
        else{
            queue.add(EPOCH.until(workOrder.getTimeStamp(), ChronoUnit.SECONDS), workOrder.getId());
            return new QueuedWorkOrder(workOrder.getId(), workOrder.getTimeStamp(), 0, getQueueLength(), getWorkOrderClass(workOrder.getId()));
        }

    }

    //TODO If sets dont exist create them!!!
    //Why ? Last remove deletes the set
    private RScoredSortedSet<Long> getQueueForId(long id){
        if(id%3 == 0 && id%5 == 0){
            return managementQueue;
        }
        else if(id%5 == 0){
            return vipQueue;
        }
        else if(id%3 == 0 ){
            return priorityQueue;
        }
        else{
            return normalQueue;
        }
    }

    //TODO If sets dont exist create them!!!
    //Why ? Last remove deletes the set
    private RScoredSortedSet<Long> getQueueForClass(WorkOrderClass orderClass){
        switch(orderClass) {
            case MANAGEMENT_OVERRIDE:
                return managementQueue;
            case VIP:
                return vipQueue;
            case PRIORITY:
                return priorityQueue;
            case NOMRAL:
                return normalQueue;
            default:
                throw new IllegalStateException();
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

    private Long getPositionForId(Long id){
        //We know the score for current entry. eg 3897
        //We can do the inverse function for other queues. Eg, what value of Max(3, nlogn) would give 3897
        //Do a ZRANGE and a count on those. And we get our number.
        //Would also be useful if you wanted to pick first 10 say

        //TODO We need to normalise the score to now

        //This is seconds since epoch on insert
        Double rangeScore =  getQueueForId(id).getScore(id);

        OffsetDateTime now = OffsetDateTime.now();

        //How many seconds now after EPOCH, minus score
        final Double durationInQueue = EPOCH.until(now, ChronoUnit.SECONDS) - rangeScore;

        Long managementQueueSize = new Long(managementQueue.size());
        Long vipQueueSizeForRange = getQueueSizeInRange(WorkOrderClass.VIP, durationInQueue, now);
        Long priorityQueueSizeForRange = getQueueSizeInRange(WorkOrderClass.PRIORITY, durationInQueue, now);
        Long normalQueueSizeForRange = getQueueSizeInRange(WorkOrderClass.NOMRAL, durationInQueue, now);

        return managementQueueSize + vipQueueSizeForRange + priorityQueueSizeForRange + normalQueueSizeForRange;
    }

    /*
    Calculate queue entries higher than given duration.
    Apply Priority Function
    Get how long ago that is
    Get the time frmo EPOCH until that time
    => Score in ScoredSortedSet
     */
    private Long getQueueSizeInRange(WorkOrderClass orderClass, Double durationInQueue, OffsetDateTime now){

        Double durationAsProductOfPrioirityFunction = getPriorityFunctionForClass(orderClass).apply(durationInQueue);

        OffsetDateTime startTime = now.minus(durationAsProductOfPrioirityFunction.longValue(), ChronoUnit.SECONDS);

        Long startScoreForClass = EPOCH.until(startTime, ChronoUnit.SECONDS);

        return new Long(getQueueForClass(orderClass)
                .valueRange(0, true, startScoreForClass, false)
                .size());
    }

    /*
    This is used to find values that would be ahead of a given value in the queue
    So, if we have a value in normal queue 3 seconds,
    we want to get  all values in Normal between 0 - 3 seconds
    and all values in Prioirity between 0 - 3*log(3) seconds
    and all values in VIP between 0-2*3*log(3) seconds
    */
    private Function<Double, Double> getPriorityFunctionForClass(WorkOrderClass orderClass){
        switch(orderClass) {
            case MANAGEMENT_OVERRIDE:
                throw new IllegalStateException();
            case VIP:
                return value -> Math.max(4, 2*value* Math.log(value));
            case PRIORITY:
                return value ->Math.max(3, value* Math.log(value));
            case NOMRAL:
                return value -> value;
            default:
                throw new IllegalStateException();
        }
    }

}
