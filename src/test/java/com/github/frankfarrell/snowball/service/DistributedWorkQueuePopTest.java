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
import java.util.List;

import static org.mockito.Mockito.doReturn;

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


    private void tickClock(Integer seconds){
        distributedWorkOrderQueue.setClock(Clock.offset(mockClock, Duration.of(seconds, ChronoUnit.SECONDS)));
    }

}
