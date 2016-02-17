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

import static org.mockito.Mockito.*;

import java.time.*;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by Frank on 17/02/2016.
 *
 * Unit Tests for Distributed Work Queue Get All
 * Mock Redisson Client
 * Mock Clock
 *
 */
public class DistributedWorkQueueGetAllTest {

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

        doReturn(normalEntries).when(mockNormalQueue).entryRange(Double.MIN_VALUE, true, Double.MAX_VALUE, true);
        doReturn(priorityEntries).when(mockPriorityQueue).entryRange(Double.MIN_VALUE, true, Double.MAX_VALUE, true);
        doReturn(vipEntries).when(mockVipQueue).entryRange(Double.MIN_VALUE, true, Double.MAX_VALUE, true);
        doReturn(managementEntries).when(mockManagementQueue).entryRange(Double.MIN_VALUE, true, Double.MAX_VALUE, true);


        mockClock = Clock.fixed(BASE_TIME.toInstant(), ZoneId.of("UTC"));

        distributedWorkOrderQueue = new DistributedWorkOrderQueue(mockRedissonClient, mockClock);
    }

    @After
    public void tearDown(){
        normalEntries.clear();
        priorityEntries.clear();
        vipEntries.clear();
        managementEntries.clear();
    }

    /*
    Get all With Single Item In Queue
     */

    @Test
    public void testGetAllWorkOrdersSingleManagement(){

        //Set up state of the queues with each test
        managementEntries.add(0, new ScoredEntry<>(BASE_SCORE-1, 15L));

        QueuedWorkOrder workOrder = distributedWorkOrderQueue.getAllWorkOrders().get(0);

        assert(workOrder.getPositionInQueue() == 0);
        assert(workOrder.getDurationInQueue() == 1);
        assert(workOrder.getWorkOrderClass().equals(WorkOrderClass.MANAGEMENT_OVERRIDE));
        assert(workOrder.getTimeStamp().equals(BASE_TIME.minus(1, ChronoUnit.SECONDS)));

        tickClock(10);

        QueuedWorkOrder workOrderTenSecondsLater = distributedWorkOrderQueue.getAllWorkOrders().get(0);

        assert(workOrderTenSecondsLater.getPositionInQueue() == 0);
        assert(workOrderTenSecondsLater.getDurationInQueue() == 11);
        assert(workOrderTenSecondsLater.getWorkOrderClass().equals(WorkOrderClass.MANAGEMENT_OVERRIDE));
        assert(workOrderTenSecondsLater.getTimeStamp().equals(BASE_TIME.minus(1, ChronoUnit.SECONDS)));
    }

    @Test
    public void testGetAllWorkOrdersSingleVip(){

        vipEntries.add(0, new ScoredEntry<>(BASE_SCORE-5, 10L));

        QueuedWorkOrder workOrder = distributedWorkOrderQueue.getAllWorkOrders().get(0);

        assert(workOrder.getPositionInQueue() == 0);
        assert(workOrder.getDurationInQueue() == 5);
        assert(workOrder.getWorkOrderClass().equals(WorkOrderClass.VIP));
        assert(workOrder.getTimeStamp().equals(BASE_TIME.minus(5, ChronoUnit.SECONDS)));

        tickClock(20);

        QueuedWorkOrder workOrderTwentySecondsLater = distributedWorkOrderQueue.getAllWorkOrders().get(0);

        assert(workOrderTwentySecondsLater.getPositionInQueue() == 0);
        assert(workOrderTwentySecondsLater.getDurationInQueue() == 25);
        assert(workOrderTwentySecondsLater.getWorkOrderClass().equals(WorkOrderClass.VIP));
        assert(workOrderTwentySecondsLater.getTimeStamp().equals(BASE_TIME.minus(5, ChronoUnit.SECONDS)));


    }

    @Test
    public void testGetAllWorkOrdersSinglePriority(){

        priorityEntries.add(0, new ScoredEntry<>(BASE_SCORE-1000, 9L));

        QueuedWorkOrder workOrder = distributedWorkOrderQueue.getAllWorkOrders().get(0);

        assert(workOrder.getPositionInQueue() == 0);
        assert(workOrder.getDurationInQueue() == 1000);
        assert(workOrder.getWorkOrderClass().equals(WorkOrderClass.PRIORITY));
        assert(workOrder.getTimeStamp().equals(BASE_TIME.minus(1000, ChronoUnit.SECONDS)));

        tickClock(100);

        QueuedWorkOrder workOrderHundredSecondsLater = distributedWorkOrderQueue.getAllWorkOrders().get(0);

        assert(workOrderHundredSecondsLater.getPositionInQueue() == 0);
        assert(workOrderHundredSecondsLater.getDurationInQueue() == 1100);
        assert(workOrderHundredSecondsLater.getWorkOrderClass().equals(WorkOrderClass.PRIORITY));
        assert(workOrderHundredSecondsLater.getTimeStamp().equals(BASE_TIME.minus(1000, ChronoUnit.SECONDS)));


    }

    @Test
    public void testGetAllWorkOrdersSingleNormal(){

        normalEntries.add(0, new ScoredEntry<>(BASE_SCORE-50000, 1L));

        QueuedWorkOrder workOrder = distributedWorkOrderQueue.getAllWorkOrders().get(0);

        assert(workOrder.getPositionInQueue() == 0);
        assert(workOrder.getDurationInQueue() == 50000);
        assert(workOrder.getWorkOrderClass().equals(WorkOrderClass.NOMRAL));
        assert(workOrder.getTimeStamp().equals(BASE_TIME.minus(50000, ChronoUnit.SECONDS)));

        tickClock(20000);

        QueuedWorkOrder workOrderHundredSecondsLater = distributedWorkOrderQueue.getAllWorkOrders().get(0);

        assert(workOrderHundredSecondsLater.getPositionInQueue() == 0);
        assert(workOrderHundredSecondsLater.getDurationInQueue() == 70000);
        assert(workOrderHundredSecondsLater.getWorkOrderClass().equals(WorkOrderClass.NOMRAL));
        assert(workOrderHundredSecondsLater.getTimeStamp().equals(BASE_TIME.minus(50000, ChronoUnit.SECONDS)));

    }

    /*
    Get all ordering according to priority rule
     */

    @Test
    public void testGetAllReturnsSortedByPrioritySameTimeStamp(){

        /*
        4 work orders with same timestmp, different priorities
         */
        managementEntries.add(0, new ScoredEntry<>(BASE_SCORE-100, 15L));
        priorityEntries.add(0, new ScoredEntry<>(BASE_SCORE-100, 9L));
        vipEntries.add(0, new ScoredEntry<>(BASE_SCORE-100, 10L));
        normalEntries.add(0, new ScoredEntry<>(BASE_SCORE-100, 1L));

        List<QueuedWorkOrder> workOrders = distributedWorkOrderQueue.getAllWorkOrders();

        assert(workOrders.get(0).equals(new QueuedWorkOrder(15L, BASE_TIME.minus(100, ChronoUnit.SECONDS),100L,  0, WorkOrderClass.MANAGEMENT_OVERRIDE)));

        assert(workOrders.get(1).equals(new QueuedWorkOrder(10L, BASE_TIME.minus(100, ChronoUnit.SECONDS),100L,  1, WorkOrderClass.VIP)));

        assert(workOrders.get(2).equals(new QueuedWorkOrder(9L, BASE_TIME.minus(100, ChronoUnit.SECONDS),100L,  2, WorkOrderClass.PRIORITY)));

        assert(workOrders.get(3).equals(new QueuedWorkOrder(1L, BASE_TIME.minus(100, ChronoUnit.SECONDS),100L,  3, WorkOrderClass.NOMRAL)));
    }

    @Test
    public void testGetAllReturnsSortedByPriorityWithDifferentTimeStamps(){

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

        List<QueuedWorkOrder> workOrders = distributedWorkOrderQueue.getAllWorkOrders();

        assert(workOrders.get(0).equals(new QueuedWorkOrder(15L, BASE_TIME.minus(100, ChronoUnit.SECONDS),100L,  0, WorkOrderClass.MANAGEMENT_OVERRIDE)));
        assert(workOrders.get(1).equals(new QueuedWorkOrder(30L, BASE_TIME.minus(50, ChronoUnit.SECONDS),50L, 1, WorkOrderClass.MANAGEMENT_OVERRIDE)));

        assert(workOrders.get(2).equals(new QueuedWorkOrder(10L, BASE_TIME.minus(200, ChronoUnit.SECONDS),200L,  2, WorkOrderClass.VIP)));
        assert(workOrders.get(4).equals(new QueuedWorkOrder(20L, BASE_TIME.minus(5, ChronoUnit.SECONDS),5L,  4, WorkOrderClass.VIP)));

        assert(workOrders.get(3).equals(new QueuedWorkOrder(9L, BASE_TIME.minus(100, ChronoUnit.SECONDS),100L,  3, WorkOrderClass.PRIORITY)));
        assert(workOrders.get(5).equals(new QueuedWorkOrder(12L, BASE_TIME.minus(6, ChronoUnit.SECONDS),6L, 5, WorkOrderClass.PRIORITY)));

        assert(workOrders.get(6).equals(new QueuedWorkOrder(1L, BASE_TIME.minus(7, ChronoUnit.SECONDS),7L,  6, WorkOrderClass.NOMRAL)));
        assert(workOrders.get(7).equals(new QueuedWorkOrder(2L, BASE_TIME.minus(6, ChronoUnit.SECONDS),6L,  7, WorkOrderClass.NOMRAL)));
    }


    /*
    Get all ordering changes correctly with time
     */

    @Test
    public void testGetAllReturnsSortedByPriorityWithDifferentTimeStampsWithChangeInTime(){

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

        List<QueuedWorkOrder> workOrders = distributedWorkOrderQueue.getAllWorkOrders();

        assert(workOrders.get(0).equals(new QueuedWorkOrder(15L, BASE_TIME.minus(100, ChronoUnit.SECONDS),100L,  0, WorkOrderClass.MANAGEMENT_OVERRIDE)));
        assert(workOrders.get(1).equals(new QueuedWorkOrder(30L, BASE_TIME.minus(50, ChronoUnit.SECONDS),50L, 1, WorkOrderClass.MANAGEMENT_OVERRIDE)));

        assert(workOrders.get(4).equals(new QueuedWorkOrder(10L, BASE_TIME.minus(10, ChronoUnit.SECONDS),10L,  4, WorkOrderClass.VIP)));
        assert(workOrders.get(7).equals(new QueuedWorkOrder(20L, BASE_TIME.minus(5, ChronoUnit.SECONDS),5L,  7, WorkOrderClass.VIP)));

        assert(workOrders.get(3).equals(new QueuedWorkOrder(9L, BASE_TIME.minus(18, ChronoUnit.SECONDS),18L,  3, WorkOrderClass.PRIORITY)));
        assert(workOrders.get(5).equals(new QueuedWorkOrder(12L, BASE_TIME.minus(13, ChronoUnit.SECONDS),13L, 5, WorkOrderClass.PRIORITY)));

        assert(workOrders.get(6).equals(new QueuedWorkOrder(1L, BASE_TIME.minus(30, ChronoUnit.SECONDS),30L, 6, WorkOrderClass.NOMRAL)));
        assert(workOrders.get(2).equals(new QueuedWorkOrder(2L, BASE_TIME.minus(100, ChronoUnit.SECONDS),100L,  2, WorkOrderClass.NOMRAL)));

        tickClock(10);

        List<QueuedWorkOrder> workOrdersAfterTick = distributedWorkOrderQueue.getAllWorkOrders();

        assert(workOrdersAfterTick.get(0).equals(new QueuedWorkOrder(15L, BASE_TIME.minus(100, ChronoUnit.SECONDS),110L,  0, WorkOrderClass.MANAGEMENT_OVERRIDE)));
        assert(workOrdersAfterTick.get(1).equals(new QueuedWorkOrder(30L, BASE_TIME.minus(50, ChronoUnit.SECONDS),60L, 1, WorkOrderClass.MANAGEMENT_OVERRIDE)));

        assert(workOrdersAfterTick.get(2).equals(new QueuedWorkOrder(10L, BASE_TIME.minus(10, ChronoUnit.SECONDS),20L,  2, WorkOrderClass.VIP)));
        assert(workOrdersAfterTick.get(5).equals(new QueuedWorkOrder(20L, BASE_TIME.minus(5, ChronoUnit.SECONDS),15L,  5, WorkOrderClass.VIP)));

        assert(workOrdersAfterTick.get(4).equals(new QueuedWorkOrder(9L, BASE_TIME.minus(18, ChronoUnit.SECONDS),28L,  4, WorkOrderClass.PRIORITY)));
        assert(workOrdersAfterTick.get(6).equals(new QueuedWorkOrder(12L, BASE_TIME.minus(13, ChronoUnit.SECONDS),23L, 6, WorkOrderClass.PRIORITY)));

        assert(workOrdersAfterTick.get(7).equals(new QueuedWorkOrder(1L, BASE_TIME.minus(30, ChronoUnit.SECONDS),40L, 7, WorkOrderClass.NOMRAL)));
        assert(workOrdersAfterTick.get(3).equals(new QueuedWorkOrder(2L, BASE_TIME.minus(100, ChronoUnit.SECONDS),110L,  3, WorkOrderClass.NOMRAL)));
    }

    /*
    test for the max 3,4 rules
     */

    private void tickClock(Integer seconds){
        distributedWorkOrderQueue.setClock(Clock.offset(mockClock, Duration.of(seconds, ChronoUnit.SECONDS)));
    }

}
