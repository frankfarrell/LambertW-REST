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
import org.redisson.core.RLock;
import org.redisson.core.RReadWriteLock;
import org.redisson.core.RScoredSortedSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
 * Possible Future Extensions: Allows for Time To live on set entries
 */
public class DistributedWorkOrderQueue implements WorkOrderQueue {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private RScoredSortedSet<Long> normalQueue;
    private RScoredSortedSet<Long> priorityQueue;
    private RScoredSortedSet<Long> vipQueue;
    private RScoredSortedSet<Long> managementQueue;

    private final OffsetDateTime EPOCH = OffsetDateTime.ofInstant(Instant.EPOCH, ZoneId.of("UTC"));

    private final RedissonClient redisson;

    protected Clock clock;

    private static final String MANAGEMENT_QUEUE_NAME = "management";
    private static final String VIP_QUEUE_NAME = "vip";
    private static final String PRIORITY_QUEUE_NAME = "priority";
    private static final String NORMAL_QUEUE_NAME = "normal";

    @Autowired
    public DistributedWorkOrderQueue(RedissonClient redisson,Clock clock) {
        this.redisson = redisson;
        this.clock = clock;

        normalQueue = redisson.getScoredSortedSet(NORMAL_QUEUE_NAME);
        priorityQueue =  redisson.getScoredSortedSet(PRIORITY_QUEUE_NAME);
        vipQueue = redisson.getScoredSortedSet(VIP_QUEUE_NAME);
        managementQueue =  redisson.getScoredSortedSet(MANAGEMENT_QUEUE_NAME);
    }

    @Override
    public List<QueuedWorkOrder> getAllWorkOrders() {

        /*
        Get all queues.
        Map through priority function
        Sort All
         */
        log.info("Enter getAllWorkOrders");

        final OffsetDateTime now = getCurrentTime();

        List<ScoredEntry<Long>> managementWorkOrders = new ArrayList<>();

        managementWorkOrders.addAll(getAllInQueue(managementQueue));

        //First get Management Queue,
        List<QueuedWorkOrder> allOrders = IntStream.range(0, managementWorkOrders.size())
                .mapToObj(index ->{
                    ScoredEntry<Long> order = managementWorkOrders.get(index);
                    return new QueuedWorkOrder(order.getValue(),
                            EPOCH.plus(order.getScore().longValue(), ChronoUnit.SECONDS),
                            EPOCH.until(now, ChronoUnit.SECONDS) - order.getScore().longValue(),
                            index,
                            WorkOrderClass.MANAGEMENT_OVERRIDE);
                }).collect(Collectors.toList());


        //Group the rest together.
        //This might not be the optimal solution
        List<ScoredEntry<Long>> nonManagementWorkOrders = new ArrayList<>();
        nonManagementWorkOrders.addAll(getAllInQueue(vipQueue));
        nonManagementWorkOrders.addAll(getAllInQueue(priorityQueue));
        nonManagementWorkOrders.addAll(getAllInQueue(normalQueue));

        //Sort them according to priority function score
        List<ScoredEntry<DurationValueStruct>> otherWorkOrders = nonManagementWorkOrders.stream()
                .map(entry -> {
                    Double durationInQueue = getDurationInQueue(entry.getScore()).doubleValue();
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


        allOrders.addAll(nonManagementOrders);

        log.info("Exit getAllWorkOrders with {} work order", allOrders.size());

        return allOrders;
    }


    @Override
    public QueuedWorkOrder getWorkOrder(Long id) {

        log.info("Enter getWorkOrder for id : {}", id);

        final RScoredSortedSet<Long> queue = getQueueForId(id);

        if(queue.contains(id)){
            OffsetDateTime timestamp = getOriginalTimestamp(queue.getScore(id));

            long durationInQueue =  timestamp.until(getCurrentTime(), ChronoUnit.SECONDS);

            //Calculates position in queue for now
            long positionInQueue = getPositionForId(id);
            WorkOrderClass workOrderClass = getWorkOrderClass(id);

            QueuedWorkOrder workOrderResult = new QueuedWorkOrder(id, timestamp, durationInQueue, positionInQueue, workOrderClass);
            log.info("Exit getWorkOrder for id : {}, result : {}", id, workOrderResult);

            return workOrderResult;
        }
        else{
            log.info("Exit getWorkOrder for id : {}, no result found", id);
            throw new NotFoundException("id", id);
        }
    }

    /*
    Async call to remove work order from queue
    */
    @Override
    @Async(value = "queueWriteTaskExecutor")
    public Future<Boolean> removeWorkOrder(Long id) {

        log.info("Enter removeWorkOrder for id : {}", id);

        final RScoredSortedSet<Long> queue = getQueueForId(id);
        Boolean result = queue.remove(id);

        log.info("Exit removeWorkOrder for id : {}, ", id);
        return new AsyncResult<>(result);
    }

    /*
    Async call to pop top priority order from queue
    */
    @Override
    @Async(value = "queueWriteTaskExecutor")
    public Future<Optional<QueuedWorkOrder>> popWorkOrder() {

        /*
        Get top from each queue
        Pick one that is first according to sorting
         */
        log.info("Enter popWorkOrder");

        if(managementQueue.size()>0){

            Collection<ScoredEntry<Long>> asCollection = managementQueue.entryRange(0, 0);
            ArrayList<ScoredEntry<Long>> asList = new ArrayList<>();
            asList.addAll(asCollection);
            ScoredEntry<Long> managementOrder = asList.get(0);
            QueuedWorkOrder orderToPop = new QueuedWorkOrder(
                    managementOrder.getValue(),
                    getOriginalTimestamp(managementOrder.getScore()),
                    getDurationInQueue(managementOrder.getScore()),
                    0,
                    WorkOrderClass.MANAGEMENT_OVERRIDE
                    );

            managementQueue.removeRangeByRank(0,0);
            log.info("Exit popWorkOrder with {}", orderToPop);
            return new AsyncResult<Optional<QueuedWorkOrder>>(Optional.of(orderToPop));
        }
        else{
            //Iterate over the three other WorkOrder Classes.
            //Rank by Type and Sort by Rank
            //Pick First
            Optional<QueuedWorkOrder> topRankedValue = Arrays.asList(
                            WorkOrderClass.VIP,
                            WorkOrderClass.PRIORITY,
                            WorkOrderClass.NOMRAL
                    )
                    .stream()
                    .map(orderClass ->{
                        RScoredSortedSet<Long> queue = getQueueForClass(orderClass);
                        ArrayList<ScoredEntry<Long>> asList = new ArrayList<>();
                        asList.addAll(queue.entryRange(0, 0));
                        if(asList.size()>0){

                            ScoredEntry<Long> value = asList.get(0);
                            Long durationInQueue = getDurationInQueue(value.getScore());
                            Double rank = getPriorityFunctionForClass(orderClass).apply(durationInQueue.doubleValue());
                            QueuedWorkOrder workOrder = new QueuedWorkOrder(
                                    value.getValue(),
                                    getOriginalTimestamp(value.getScore()),
                                    durationInQueue,
                                    0,
                                    orderClass);

                            workOrder.setRank(rank);
                            //Pass down rank, id, timestamp, duration in queue
                            return workOrder;
                        }
                        else{
                            return null;
                        }
                    })
                    .filter(value -> value != null)
                    //Sort ascending to return top ranking
                    .sorted(((x,y)-> {
                        if(x.getRank() > y.getRank()){
                            return -1;
                        }
                        if(y.getRank() >x.getRank()){
                            return 1;
                        }
                        else return 0;
                    }))
                    .findFirst();

            if(topRankedValue.isPresent()){
                log.info("Exit popWorkOrder with {}", topRankedValue.get());
                getQueueForClass(topRankedValue.get().getWorkOrderClass()).removeRangeByRank(0,0);
            }
            else{
                log.info("Exit popWorkOrder with no work order");
            }
            return new AsyncResult<Optional<QueuedWorkOrder>>(topRankedValue);

        }
    }

    /*
    Async call to push work order into queue
    For queue to operate without non-stop queue size growth, pop frequency must be greater or equal than average than push frequency.
    */
    @Override
    @Async(value = "queuePushTaskExecutor")
    public Future<QueuedWorkOrder> pushWorkOrder(WorkOrder workOrder) {

        log.info("Enter pushWorkOrder for id : {}", workOrder.getId());
        final RScoredSortedSet<Long> queue = getQueueForClass(getWorkOrderClass(workOrder.getId()));

        if(queue.contains(workOrder.getId())){
            log.info("Exit pushWorkOrder for id : {}, order for id already exists", workOrder.getId());
            throw new AlreadyExistsException("id", workOrder.getId());
        }
        else{
            queue.add(EPOCH.until(workOrder.getTimeStamp(), ChronoUnit.SECONDS), workOrder.getId());

            QueuedWorkOrder enqueuedWorkOrder = new QueuedWorkOrder(workOrder.getId(), workOrder.getTimeStamp(), 0, getPositionForId(workOrder.getId()), getWorkOrderClass(workOrder.getId()));

            log.info("Exit pushWorkOrder for id : {}, queued value : {}", workOrder.getId(), enqueuedWorkOrder);

            return new AsyncResult<>(enqueuedWorkOrder);
        }
    }

    private Collection<ScoredEntry<Long>> getAllInQueue(RScoredSortedSet<Long> queue){
        return queue.entryRange(Double.MIN_VALUE, true, Double.MAX_VALUE, true);
    }

    private OffsetDateTime getCurrentTime(){
        return OffsetDateTime.now(clock);
    }

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

    private Long getDurationInQueue(Double score){
        return EPOCH.until(getCurrentTime(), ChronoUnit.SECONDS) - score.longValue();
    }

    private OffsetDateTime getOriginalTimestamp(Double score){
        return EPOCH.plus(score.longValue(), ChronoUnit.SECONDS);
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
        final Double durationInQueue = getDurationInQueue(rangeScore).doubleValue();

        if (getWorkOrderClass(id).equals(WorkOrderClass.MANAGEMENT_OVERRIDE)) {
            return getQueueSizeInRange(WorkOrderClass.MANAGEMENT_OVERRIDE ,WorkOrderClass.MANAGEMENT_OVERRIDE, durationInQueue, now) -1;
        }
        else{
            final Long managementQueueSize = new Long(managementQueue.size());
            final Long vipQueueSizeForRange = getQueueSizeInRange(getWorkOrderClass(id), WorkOrderClass.VIP, durationInQueue, now);
            final Long priorityQueueSizeForRange = getQueueSizeInRange(getWorkOrderClass(id), WorkOrderClass.PRIORITY, durationInQueue, now);
            final Long normalQueueSizeForRange = getQueueSizeInRange(getWorkOrderClass(id), WorkOrderClass.NOMRAL, durationInQueue, now);

            //How many items are ahead of this in the Queue, minus 1 offset
            return managementQueueSize + vipQueueSizeForRange + priorityQueueSizeForRange + normalQueueSizeForRange -1;
        }
    }

    /*
    NB This is an approximate method that gives a very accurate indication of the position of a given item in the queue

    Calculate queue entries higher than given duration.
    Get the time from EPOCH until that time
    => Score in ScoredSortedSet
     */
    protected Long getQueueSizeInRange(WorkOrderClass orderClassOfValue, WorkOrderClass orderClassCompared, Double durationInQueue, OffsetDateTime now){

        //Caculate priority function for current value
        //Calculate inverse priority value other queues would need to achieve to out rank/equal that
        Double durationAsProductOfPrioirityFunction = getInversePriorityFunctionForClass(orderClassCompared)
                .apply(getPriorityFunctionForClass(orderClassOfValue).apply(durationInQueue));

        OffsetDateTime startTime = now.minus(durationAsProductOfPrioirityFunction.longValue(), ChronoUnit.SECONDS);

        Long startScoreForClass = EPOCH.until(startTime, ChronoUnit.SECONDS);

        return new Long(getQueueForClass(orderClassCompared)
                .valueRange(0, true, startScoreForClass, true)
                .size());
    }

    /*
    This is used to find values that would be ahead of a given value in the queue
    So, if we have a value in normal queue 10 seconds,
    we want to get  all values in Normal between 0 - 10 seconds
    and all values in Prioirity between 0 - inverse(10*log(10))seconds
    and all values in VIP between 0- inverse(2*10*log(10)) seconds
    Use Lambert W function for inverse of nlogn
    */
    protected Function<Double, Double> getInversePriorityFunctionForClass(WorkOrderClass orderClass){
        switch(orderClass) {
            case MANAGEMENT_OVERRIDE:
                return value -> value; //This is only for case of comparing management with management
            case VIP:
                // https://en.wikipedia.org/wiki/Lambert_W_function#Example_4
                return value -> Math.max(4, Math.exp(lambertWFunction(value/2)));
            case PRIORITY:
                return value -> Math.max(3, Math.exp(lambertWFunction(value)));
            case NOMRAL:
                return value -> value;
            default:
                throw new IllegalStateException();
        }
    }

    protected Function<Double, Double> getPriorityFunctionForClass(WorkOrderClass orderClass){
        switch(orderClass) {
            case MANAGEMENT_OVERRIDE:
                return value -> value;
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

    /*
    From https://gist.github.com/cab1729/1318030
     */
    public static double lambertWFunction(double z)
    {
        double S = 0.0;
        for (int n=1; n <= 100; n++)
        {
            double Se = S * StrictMath.pow(StrictMath.E, S);
            double S1e = (S+1) * StrictMath.pow(StrictMath.E, S);

            if (1E-12 > StrictMath.abs((z-Se)/S1e))
            {
                return S;
            }
            S -= (Se-z) / (S1e - (S+2) * (Se-z) / (2*S+2));
        }
        return S;
    }

    /*
    Hook to Clock for unit testing
    Needed because Clock is immutable
     */
    protected void setClock(Clock clock) {
        this.clock = clock;
    }

}
