package com.github.frankfarrell.snowball.service;

import com.github.frankfarrell.snowball.exceptions.AlreadyExistsException;
import com.github.frankfarrell.snowball.exceptions.NotFoundException;
import com.github.frankfarrell.snowball.model.QueuedWorkOrder;
import com.github.frankfarrell.snowball.model.WorkOrder;
import com.github.frankfarrell.snowball.model.WorkOrderClass;
import com.github.frankfarrell.snowball.service.WorkOrderQueue;
import io.swagger.models.auth.In;
import org.redisson.RedissonClient;
import org.redisson.client.protocol.ScoredEntry;
import org.redisson.core.RScoredSortedSet;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

    private final RedissonClient redisson;

    protected Clock clock;

    @Autowired
    public DistributedWorkOrderQueue(RedissonClient redisson,Clock clock) {
        this.redisson = redisson;
        this.clock = clock;

        normalQueue = redisson.getScoredSortedSet("normal");
        priorityQueue =  redisson.getScoredSortedSet("priority");
        vipQueue = redisson.getScoredSortedSet("vip");
        managementQueue =  redisson.getScoredSortedSet("management");
    }

    private Collection<ScoredEntry<Long>> getAllInQueue(RScoredSortedSet<Long> queue){
        return queue.entryRange(Double.MIN_VALUE, true, Double.MAX_VALUE, true);
    }

    @Override
    public List<QueuedWorkOrder> getAllWorkOrders() {
        /*
        Get all queues.
        Map through function -> getComparatorForClass
        Sort All
         */

        final OffsetDateTime now = getCurrentTime();

        List<ScoredEntry<Long>> managementWorkOrders = new ArrayList<>();

        managementWorkOrders.addAll(getAllInQueue(managementQueue));

        //TODO Check anything here

        //First get Management Queue,
        List<QueuedWorkOrder> managementOrders = IntStream.range(0, managementWorkOrders.size())
                .mapToObj(index ->{
                    ScoredEntry<Long> order = managementWorkOrders.get(index);
                    return new QueuedWorkOrder(order.getValue(),
                            EPOCH.plus(order.getScore().longValue(), ChronoUnit.SECONDS),
                            EPOCH.until(now, ChronoUnit.SECONDS) - order.getScore().longValue(),
                            index,
                            WorkOrderClass.MANAGEMENT_OVERRIDE);
                }).collect(Collectors.toList());

        //Group all the rest together.
        //IntStream.range(managementWorkOrders.size() , restOfThem - 1)
        //And sort using function and now

        //This is not he optimal solution,

        List<ScoredEntry<Long>> nonManagementWorkOrders = new ArrayList<>();
        nonManagementWorkOrders.addAll(getAllInQueue(vipQueue));
        nonManagementWorkOrders.addAll(getAllInQueue(priorityQueue));
        nonManagementWorkOrders.addAll(getAllInQueue(normalQueue));


        //Sort them according to priority function score
        List<ScoredEntry<DurationValueStruct>> otherWorkOrders = nonManagementWorkOrders.stream()
                .map(entry -> {
                    Double durationInQueue = EPOCH.until(now, ChronoUnit.SECONDS) - entry.getScore();
                    Double priorityScore = getPriorityFunctionForClass(getWorkOrderClass(entry.getValue())).apply(durationInQueue);
                    return new ScoredEntry<DurationValueStruct>(priorityScore, new DurationValueStruct(entry.getValue(), durationInQueue, entry.getScore()));
                })
                .sorted((x,y)-> {
                    if(x.getScore() > y.getScore()){
                        return -1;
                    }
                    if(y.getScore() >x.getScore()){
                        return 1;
                    }
                    else return 0;
                })
                .collect(Collectors.toList());

        //Map in the index
        //Map to QueuedWorkOrder -> Duration
        List<QueuedWorkOrder> nonManagementOrders = IntStream.range(managementWorkOrders.size(), nonManagementWorkOrders.size() + managementWorkOrders.size())
                .mapToObj(index ->{
                    DurationValueStruct order = otherWorkOrders.get(index - managementWorkOrders.size()).getValue();
                    return new QueuedWorkOrder(
                            order.getValue(),
                            EPOCH.plus(order.getOriginalScore().longValue(), ChronoUnit.SECONDS),
                            order.getDurationInQueue().longValue(),
                            index,
                            getWorkOrderClass(order.getValue()));
                }).collect(Collectors.toList());


        managementOrders.addAll(nonManagementOrders);
        return managementOrders;
    }


    @Override
    public QueuedWorkOrder getWorkOrder(Long id) {

        final RScoredSortedSet<Long> queue = getQueueForId(id);

        if(queue.contains(id)){
            OffsetDateTime timestamp = EPOCH.plus(queue.getScore(id).longValue(), ChronoUnit.SECONDS);

            long durationInQueue =  timestamp.until(getCurrentTime(), ChronoUnit.SECONDS);
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

        if(managementQueue.size()>0){
            //TODO Not threadsafe, needs to be done in Transaction, eg Lua Script?
            Collection<ScoredEntry<Long>> x = managementQueue.entryRange(0, 0);
            managementQueue.removeRangeByRank(0,0);
            ArrayList<ScoredEntry<Long>> y = new ArrayList<>();
            y.addAll(x);
            ScoredEntry<Long> z = y.get(0);
            return new WorkOrder(z.getValue(), EPOCH.plus(z.getScore().longValue(), ChronoUnit.SECONDS));
        }
        else{
            //TODO This works but has null pointers etc
            Collection<ScoredEntry<Long>> vip = vipQueue.entryRange(0, 0);
            ArrayList<ScoredEntry<Long>> vipList = new ArrayList<>();
            vipList.addAll(vip);
            ScoredEntry<Long> vipValue = vipList.get(0);
            Double vipRank = getPriorityFunctionForClass(WorkOrderClass.VIP).apply(vipValue.getScore());

            Collection<ScoredEntry<Long>> priority = priorityQueue.entryRange(0, 0);
            ArrayList<ScoredEntry<Long>> priorityList = new ArrayList<>();
            vipList.addAll(priority);
            ScoredEntry<Long> priorityValue = vipList.get(0);
            Double priorityRank = getPriorityFunctionForClass(WorkOrderClass.PRIORITY).apply(priorityValue.getScore());

            Collection<ScoredEntry<Long>> normal = normalQueue.entryRange(0, 0);
            ArrayList<ScoredEntry<Long>> normalList = new ArrayList<>();
            vipList.addAll(normal);
            ScoredEntry<Long> normalValue = vipList.get(0);
            Double normalRank = getPriorityFunctionForClass(WorkOrderClass.NOMRAL).apply(priorityValue.getScore());

            WorkOrder nextWorkOrder;
            if(vipRank >= priorityRank && vipRank >= normalRank){
                nextWorkOrder = new WorkOrder(vipValue.getValue(), EPOCH.plus(vipValue.getScore().longValue(), ChronoUnit.SECONDS));
                vipQueue.removeRangeByRank(0,0);
            }
            else if(priorityRank >= vipRank  && priorityRank >= normalRank){
                nextWorkOrder = new WorkOrder(priorityValue.getValue(), EPOCH.plus(priorityValue.getScore().longValue(), ChronoUnit.SECONDS));
                normalQueue.removeRangeByRank(0,0);
            }
            else{
                nextWorkOrder = new WorkOrder(normalValue.getValue(), EPOCH.plus(normalValue.getScore().longValue(), ChronoUnit.SECONDS));
                normalQueue.removeRangeByRank(0,0);
            }
            return nextWorkOrder;
        }
    }

    @Override
    public QueuedWorkOrder pushWorkOrder(WorkOrder workOrder) {

        final RScoredSortedSet<Long> queue = getQueueForId(workOrder.getId());

        if(queue.contains(workOrder.getId())){
            throw new AlreadyExistsException("id", workOrder.getId());
        }
        else{
            queue.add(EPOCH.until(workOrder.getTimeStamp(), ChronoUnit.SECONDS), workOrder.getId());
            return new QueuedWorkOrder(workOrder.getId(), workOrder.getTimeStamp(), 0, getPositionForId(workOrder.getId()), getWorkOrderClass(workOrder.getId()));
        }
    }

    private OffsetDateTime getCurrentTime(){
        return OffsetDateTime.now(clock);
    }


    //TODO If sets dont exist anymore do we need to create them?
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

    /*
    For a given id it gives the position of the WorkOrder .
    This is not effiecient for multiple calls, but is effcient for one call
    */
    private Long getPositionForId(Long id){

        //This is seconds since epoch on insert
        final Double rangeScore =  getQueueForId(id).getScore(id);

        final OffsetDateTime now = getCurrentTime();

        //How many seconds now after EPOCH, minus score
        final Double durationInQueue = EPOCH.until(now, ChronoUnit.SECONDS) - rangeScore;


        final Long managementQueueSize = new Long(managementQueue.size());
        final Long vipQueueSizeForRange = getQueueSizeInRange(WorkOrderClass.VIP, durationInQueue, now);
        final Long priorityQueueSizeForRange = getQueueSizeInRange(WorkOrderClass.PRIORITY, durationInQueue, now);
        final Long normalQueueSizeForRange = getQueueSizeInRange(WorkOrderClass.NOMRAL, durationInQueue, now);

        //How many items are ahead of this in the Queue, minus 1 offset
        return managementQueueSize + vipQueueSizeForRange + priorityQueueSizeForRange + normalQueueSizeForRange -1;
    }

    /*
    Calculate queue entries higher than given duration.
    Apply Priority Function
    Get how long ago that is
    Get the time frmo EPOCH until that time
    => Score in ScoredSortedSet
     */
    //TODO Fix bugs here
    private Long getQueueSizeInRange(WorkOrderClass orderClass, Double durationInQueue, OffsetDateTime now){

        Double durationAsProductOfPrioirityFunction = getPriorityFunctionForClass(orderClass).apply(durationInQueue);

        OffsetDateTime startTime = now.minus(durationAsProductOfPrioirityFunction.longValue(), ChronoUnit.SECONDS);

        Long startScoreForClass = EPOCH.until(startTime, ChronoUnit.SECONDS);

        return new Long(getQueueForClass(orderClass)
                .valueRange(0, true, startScoreForClass, true)
                .size());
    }

    /*
    This is used to find values that would be ahead of a given value in the queue
    So, if we have a value in normal queue 3 seconds,
    we want to get  all values in Normal between 0 - 3 seconds
    and all values in Prioirity between 0 - 3*log(3) seconds
    and all values in VIP between 0-2*3*log(3) seconds
    */
    private Function<Double, Double> getInversePriorityFunctionForClass(WorkOrderClass orderClass){
        switch(orderClass) {
            case MANAGEMENT_OVERRIDE:
                throw new IllegalStateException();
            case VIP:
                return value -> lambertWFunction(value)/2;
            case PRIORITY:
                return value -> lambertWFunction(value);
            case NOMRAL:
                return value -> value;
            default:
                throw new IllegalStateException();
        }
    }

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

    /*
    Struct Class for Queue Value and Duration in Queue
    Eg so we don't do work we have to throw away
     */
    private class DurationValueStruct{

        private final Long value;
        private final Double durationInQueue;
        private final Double originalScore;

        public DurationValueStruct(Long value, Double durationInQueue, Double originalScore) {
            this.value = value;
            this.durationInQueue = durationInQueue;
            this.originalScore = originalScore;
        }

        public Long getValue() {
            return value;
        }

        public Double getDurationInQueue() {
            return durationInQueue;
        }

        public Double getOriginalScore() {
            return originalScore;
        }
    }

    /**
     * Lambert W(z) funtion - implemented using Lagrange Inversion Theorem
     * Gives inverse of nlogn
     * for |z| < 1/e
     * @param z
     * @return
     */
    public double lambertWFunction(double z)
    {
        double S = 0.0;
        for (int k=1; k <= 1000; k++)
        {
            S += (StrictMath.pow(-k, k-1) * StrictMath.pow(z,k)) / factorial(k);
        }
        return S;
    }

    public double factorial(int N)
    {
        int n = 1;
        double f = 1.0;
        do
        {
            f *= n;
            n++;
        } while (n <= N);

        return f;
    }

    /*
    Hook to Clock for unit testing
     */
    protected void setClock(Clock clock) {
        this.clock = clock;
    }

}
