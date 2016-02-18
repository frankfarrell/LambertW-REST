package com.github.frankfarrell.snowball.service;

import com.github.frankfarrell.snowball.model.QueuedWorkOrder;
import com.github.frankfarrell.snowball.model.WorkOrderClass;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.redisson.RedissonClient;
import org.redisson.client.protocol.ScoredEntry;
import org.redisson.core.RScoredSortedSet;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

/**
 * Created by Frank on 17/02/2016.
 *
 * Unit Tests for Distributed Work Queue Get All
 * Mock Redisson Client
 * Mock Clock
 *
 */
public class DistributedWorkQueuePopTest {

    //This is the number of seconds since epoch of our initial clock time "2016-02-16T05:30:00" UTC
    final static Double BASE_SCORE = 1455600600.0;
    //Initialise Clock to "2016-02-16T05:30:00"
    final static OffsetDateTime BASE_TIME = OffsetDateTime.parse("2016-02-16T05:30:00Z");

    DistributedWorkOrderQueue distributedWorkOrderQueue;

    @Mock
    RedissonClient mockRedissonClient;

    @Mock
    RScoredSortedSet<Long> mockNormalQueue;

    @Mock
    RScoredSortedSet<Long> mockPriorityQueue;

    @Mock
    RScoredSortedSet<Long> mockVipQueue;

    @Mock
    RScoredSortedSet<Long> mockManagementQueue;

    ArrayList<ScoredEntry<Long>> normalEntries = new ArrayList<>();
    ArrayList<ScoredEntry<Long>> priorityEntries = new ArrayList<>();
    ArrayList<ScoredEntry<Long>> vipEntries = new ArrayList<>();
    ArrayList<ScoredEntry<Long>> managementEntries = new ArrayList<>();

    Clock mockClock;

    @Before
    public void setUp() throws Exception {

        MockitoAnnotations.initMocks(this);

        //Cannot use when(x).thenReturn(y) because of parametrized types
        doReturn(mockNormalQueue).when(mockRedissonClient).getScoredSortedSet("normal");
        doReturn(mockPriorityQueue).when(mockRedissonClient).getScoredSortedSet("priority");
        doReturn(mockVipQueue).when(mockRedissonClient).getScoredSortedSet("vip");
        doReturn(mockManagementQueue).when(mockRedissonClient).getScoredSortedSet("management");

        setMocksForQueue(mockManagementQueue, managementEntries);
        setMocksForQueue(mockVipQueue, vipEntries);
        setMocksForQueue(mockPriorityQueue, priorityEntries);
        setMocksForQueue(mockNormalQueue, normalEntries);

        mockClock = Clock.fixed(BASE_TIME.toInstant(), ZoneId.of("UTC"));

        distributedWorkOrderQueue = new DistributedWorkOrderQueue(mockRedissonClient, mockClock);
    }

    private void setMocksForQueue(RScoredSortedSet<Long> queue, ArrayList<ScoredEntry<Long>> entries){
        setEntryRangeForQueue(queue, entries);
        setRemoveRangeByRankQueue(queue, entries);
        setSizeForQueue(queue, entries);
    }

    private void setSizeForQueue(RScoredSortedSet<Long> queue, ArrayList<ScoredEntry<Long>> entries){
        when(queue.size()).thenAnswer(x -> entries.size());
    }

    private void setEntryRangeForQueue(RScoredSortedSet<Long> queue, ArrayList<ScoredEntry<Long>> entries){
        when(queue.entryRange(anyInt(), anyInt())).thenAnswer(
                //ZRANGE is inclusive, but sublit is not, hence +1
                invocation -> {
                    if(entries.size() > (int)invocation.getArguments()[1] ){
                        entries.sort(new Comparator<ScoredEntry<Long>>() {
                            @Override
                            public int compare(ScoredEntry<Long> o1, ScoredEntry<Long> o2) {
                                if(o1.getScore() > o2.getScore()){
                                    return 1;
                                }
                                if(o2.getScore() > o1.getScore()){
                                    return -1;
                                }
                                else return 0;
                            }
                        });
                        return entries.subList((int)invocation.getArguments()[0], (int)invocation.getArguments()[1]+1);
                    }
                    else{
                        return new ArrayList<>();
                    }
                }
        );
    }

    private void setRemoveRangeByRankQueue(RScoredSortedSet<Long> queue, ArrayList<ScoredEntry<Long>> entries){
        when(queue.removeRangeByRank(anyInt(), anyInt())).thenAnswer(
                //ZRANGE is inclusive, but IntStreamRange is not, hence +1
                invocation -> {
                    IntStream.range((int)invocation.getArguments()[0], (int)invocation.getArguments()[1]+1)
                            .forEach(index->{
                                entries.remove(index);
                            });
                    return 1;
                }
        );
    }

    @After
    public void tearDown(){
        normalEntries.clear();
        priorityEntries.clear();
        vipEntries.clear();
        managementEntries.clear();
    }

    @Test
    public void testPopWorkOrdersSingleManagement() throws Exception{

        //Set up state of the queues with each test
        managementEntries.add(0, new ScoredEntry<>(BASE_SCORE-1, 15L));

        QueuedWorkOrder workOrder = distributedWorkOrderQueue.popWorkOrder().get().get();

        assert(workOrder.getPositionInQueue() == 0);
        assert(workOrder.getDurationInQueue() == 1);
        assert(workOrder.getWorkOrderClass().equals(WorkOrderClass.MANAGEMENT_OVERRIDE));
        assert(workOrder.getTimeStamp().equals(BASE_TIME.minus(1, ChronoUnit.SECONDS)));

    }

    @Test
    public void testPopWorkOrdersSingleVip() throws Exception{

        vipEntries.add(0, new ScoredEntry<>(BASE_SCORE-5, 10L));

        QueuedWorkOrder workOrder = distributedWorkOrderQueue.popWorkOrder().get().get();

        assert(workOrder.getPositionInQueue() == 0);
        assert(workOrder.getDurationInQueue() == 5);
        assert(workOrder.getWorkOrderClass().equals(WorkOrderClass.VIP));
        assert(workOrder.getTimeStamp().equals(BASE_TIME.minus(5, ChronoUnit.SECONDS)));
    }

    @Test
    public void testPopWorkOrdersSinglePriority() throws Exception{

        priorityEntries.add(0, new ScoredEntry<>(BASE_SCORE-1000, 9L));

        QueuedWorkOrder workOrder = distributedWorkOrderQueue.popWorkOrder().get().get();

        assert(workOrder.getPositionInQueue() == 0);
        assert(workOrder.getDurationInQueue() == 1000);
        assert(workOrder.getWorkOrderClass().equals(WorkOrderClass.PRIORITY));
        assert(workOrder.getTimeStamp().equals(BASE_TIME.minus(1000, ChronoUnit.SECONDS)));

    }

    @Test
    public void testPopWorkOrdersSingleNormal() throws Exception{

        normalEntries.add(0, new ScoredEntry<>(BASE_SCORE-50000, 1L));

        QueuedWorkOrder workOrder = distributedWorkOrderQueue.popWorkOrder().get().get();

        assert(workOrder.getPositionInQueue() == 0);
        assert(workOrder.getDurationInQueue() == 50000);
        assert(workOrder.getWorkOrderClass().equals(WorkOrderClass.NOMRAL));
        assert(workOrder.getTimeStamp().equals(BASE_TIME.minus(50000, ChronoUnit.SECONDS)));

    }

    /*
    Get all ordering according to priority rule
     */

    @Test
    public void testPopWorkOrderGivesValuesSortedByPrioritySameTimeStamp() throws Exception {

        /*
        4 work orders with same timestmp, different priorities
         */
        managementEntries.add(0, new ScoredEntry<>(BASE_SCORE - 100, 15L));
        priorityEntries.add(0, new ScoredEntry<>(BASE_SCORE - 100, 9L));
        vipEntries.add(0, new ScoredEntry<>(BASE_SCORE - 100, 10L));
        normalEntries.add(0, new ScoredEntry<>(BASE_SCORE - 100, 1L));

        //All values should be at index 0 when popped

        assert (distributedWorkOrderQueue.popWorkOrder().get().get().equals(new QueuedWorkOrder(15L, BASE_TIME.minus(100, ChronoUnit.SECONDS), 100L, 0, WorkOrderClass.MANAGEMENT_OVERRIDE)));

        assert (distributedWorkOrderQueue.popWorkOrder().get().get().equals(new QueuedWorkOrder(10L, BASE_TIME.minus(100, ChronoUnit.SECONDS), 100L, 0, WorkOrderClass.VIP)));

        assert (distributedWorkOrderQueue.popWorkOrder().get().get().equals(new QueuedWorkOrder(9L, BASE_TIME.minus(100, ChronoUnit.SECONDS), 100L, 0, WorkOrderClass.PRIORITY)));

        assert (distributedWorkOrderQueue.popWorkOrder().get().get().equals(new QueuedWorkOrder(1L, BASE_TIME.minus(100, ChronoUnit.SECONDS), 100L, 0, WorkOrderClass.NOMRAL)));

        assert (!distributedWorkOrderQueue.popWorkOrder().get().isPresent());

    }

    @Test
    public void testGetAllReturnsSortedByPriorityWithDifferentTimeStamps() throws Exception{

        /*
        8 work orders with different timestamps, different priorities
         */
        managementEntries.add(0, new ScoredEntry<>(BASE_SCORE-100, 15L));//0
        managementEntries.add(1, new ScoredEntry<>(BASE_SCORE-50, 30L));//1

        vipEntries.add(0, new ScoredEntry<>(BASE_SCORE-200, 10L));//2
        vipEntries.add(1, new ScoredEntry<>(BASE_SCORE-5, 20L));//4

        priorityEntries.add(0, new ScoredEntry<>(BASE_SCORE-100, 9L));//3
        priorityEntries.add(1, new ScoredEntry<>(BASE_SCORE-6, 12L));//5

        normalEntries.add(0, new ScoredEntry<>(BASE_SCORE-7, 1L));//6
        normalEntries.add(1, new ScoredEntry<>(BASE_SCORE-6, 2L));//7

        assertEquals(distributedWorkOrderQueue.popWorkOrder().get().get(), new QueuedWorkOrder(15L, BASE_TIME.minus(100, ChronoUnit.SECONDS),100L,  0, WorkOrderClass.MANAGEMENT_OVERRIDE));
        assertEquals(distributedWorkOrderQueue.popWorkOrder().get().get(), new QueuedWorkOrder(30L, BASE_TIME.minus(50, ChronoUnit.SECONDS),50L, 0, WorkOrderClass.MANAGEMENT_OVERRIDE));

        assertEquals(distributedWorkOrderQueue.popWorkOrder().get().get(), new QueuedWorkOrder(10L, BASE_TIME.minus(200, ChronoUnit.SECONDS),200L,  0, WorkOrderClass.VIP));

        assertEquals(distributedWorkOrderQueue.popWorkOrder().get().get(), new QueuedWorkOrder(9L, BASE_TIME.minus(100, ChronoUnit.SECONDS),100L,  0, WorkOrderClass.PRIORITY));

        assertEquals(distributedWorkOrderQueue.popWorkOrder().get().get(), new QueuedWorkOrder(20L, BASE_TIME.minus(5, ChronoUnit.SECONDS),5L,  0, WorkOrderClass.VIP));

        assertEquals(distributedWorkOrderQueue.popWorkOrder().get().get(), new QueuedWorkOrder(12L, BASE_TIME.minus(6, ChronoUnit.SECONDS),6L, 0, WorkOrderClass.PRIORITY));

        assertEquals(distributedWorkOrderQueue.popWorkOrder().get().get(), new QueuedWorkOrder(1L, BASE_TIME.minus(7, ChronoUnit.SECONDS),7L,  0, WorkOrderClass.NOMRAL));
        assertEquals(distributedWorkOrderQueue.popWorkOrder().get().get(), new QueuedWorkOrder(2L, BASE_TIME.minus(6, ChronoUnit.SECONDS),6L,  0, WorkOrderClass.NOMRAL));

        assert (!distributedWorkOrderQueue.popWorkOrder().get().isPresent());
    }


    /*
    Get all ordering changes correctly with time
     */

    @Test
    public void testGetAllReturnsSortedByPriorityWithDifferentTimeStampsWithChangeInTime() throws Exception{

        /*
        8 work orders with different timestamps, different priorities       Orig    Score @0       Score @ +10 Sec Score
         */
        managementEntries.add(0, new ScoredEntry<>(BASE_SCORE-100, 15L));   //0                                         0
        managementEntries.add(1, new ScoredEntry<>(BASE_SCORE-50, 30L));    //1                                         1

        vipEntries.add(0, new ScoredEntry<>(BASE_SCORE-10, 10L));           //4     2nlogn  ~ 46       120              2
        vipEntries.add(1, new ScoredEntry<>(BASE_SCORE-5, 20L));            //7     2nlogn ~ 16        81               5

        priorityEntries.add(0, new ScoredEntry<>(BASE_SCORE-18, 9L));      //3      nlogn ~   52        92              4
        priorityEntries.add(1, new ScoredEntry<>(BASE_SCORE-13, 12L));     //5      nlogn ~   33        71              6

        normalEntries.add(0, new ScoredEntry<>(BASE_SCORE-30, 1L));          //6     30                 40              7
        normalEntries.add(1, new ScoredEntry<>(BASE_SCORE-100, 2L));          //2    100                110             3

        assertEquals(distributedWorkOrderQueue.popWorkOrder().get().get(), new QueuedWorkOrder(15L, BASE_TIME.minus(100, ChronoUnit.SECONDS),100L,  0, WorkOrderClass.MANAGEMENT_OVERRIDE));

        assertEquals(distributedWorkOrderQueue.popWorkOrder().get().get(), new QueuedWorkOrder(30L, BASE_TIME.minus(50, ChronoUnit.SECONDS),50L, 0, WorkOrderClass.MANAGEMENT_OVERRIDE));

        assertEquals(distributedWorkOrderQueue.popWorkOrder().get().get(), new QueuedWorkOrder(2L, BASE_TIME.minus(100, ChronoUnit.SECONDS),100L,  0, WorkOrderClass.NOMRAL));

        assertEquals(distributedWorkOrderQueue.popWorkOrder().get().get(),new QueuedWorkOrder(9L, BASE_TIME.minus(18, ChronoUnit.SECONDS),18L,  0, WorkOrderClass.PRIORITY));

        tickClock(10);

        assertEquals(distributedWorkOrderQueue.popWorkOrder().get().get(),new QueuedWorkOrder(10L, BASE_TIME.minus(10, ChronoUnit.SECONDS),20L,  0, WorkOrderClass.VIP));

        assertEquals(distributedWorkOrderQueue.popWorkOrder().get().get(), new QueuedWorkOrder(20L, BASE_TIME.minus(5, ChronoUnit.SECONDS),15L,  0, WorkOrderClass.VIP));

        assertEquals(distributedWorkOrderQueue.popWorkOrder().get().get(),new QueuedWorkOrder(12L, BASE_TIME.minus(13, ChronoUnit.SECONDS),23L, 0, WorkOrderClass.PRIORITY));

        assertEquals(distributedWorkOrderQueue.popWorkOrder().get().get(),new QueuedWorkOrder(1L, BASE_TIME.minus(30, ChronoUnit.SECONDS),40L, 0, WorkOrderClass.NOMRAL));

        assert (!distributedWorkOrderQueue.popWorkOrder().get().isPresent());
    }

    private void tickClock(Integer seconds){
        distributedWorkOrderQueue.setClock(Clock.offset(mockClock, Duration.of(seconds, ChronoUnit.SECONDS)));
    }

}
