package com.github.frankfarrell.snowball.service;

import com.github.frankfarrell.snowball.model.WorkOrderClass;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.redisson.RedissonClient;
import org.redisson.client.protocol.ScoredEntry;
import org.redisson.core.RScoredSortedSet;

import java.time.Clock;
import java.util.ArrayList;
import java.util.function.Function;

/**
 * Created by Frank on 17/02/2016.
 */
public class DistributedWorkOrderQueuePriorityFunctionTest {

    private static final double EPSILON = 0.01;

    DistributedWorkOrderQueue distributedWorkOrderQueue;

    @Mock
    RedissonClient mockRedissonClient;

    @Mock
    Clock mockClock;

    @Before
    public void setUp() throws Exception {

        MockitoAnnotations.initMocks(this);

        distributedWorkOrderQueue = new DistributedWorkOrderQueue(mockRedissonClient, mockClock);

    }

    /*
    2nlogn
     */
    @Test
    public void testVipPriorityFunction(){
        Function<Double, Double> vipPriorityFunction = distributedWorkOrderQueue.getPriorityFunctionForClass(WorkOrderClass.VIP);

        assert(equalsWithinEpsilon(vipPriorityFunction.apply(3.0), 6.592));
        assert(equalsWithinEpsilon(vipPriorityFunction.apply(12.0), 59.64));
        assert(equalsWithinEpsilon(vipPriorityFunction.apply( 80.0), 701.12));
        assert(equalsWithinEpsilon(vipPriorityFunction.apply(1000.0), 13899.0));
        assert(equalsWithinEpsilon(vipPriorityFunction.apply(50000.0), 1081980.0));

        /*
        All Vip Orders have a minimum priority of 3
         */
        assert(equalsWithinEpsilon(vipPriorityFunction.apply(0.5), 4.0));
        assert(equalsWithinEpsilon(vipPriorityFunction.apply(1.0), 4.0));
    }

    /*
    inverse 2nlogn
     */
    @Test
    public void testInverseVipPriorityFunction(){
        Function<Double, Double> inverseVipPriorityFunction = distributedWorkOrderQueue.getInversePriorityFunctionForClass(WorkOrderClass.VIP);
        Function<Double, Double> vipPriorityFunction = distributedWorkOrderQueue.getPriorityFunctionForClass(WorkOrderClass.VIP);


        assert(equalsWithinEpsilon(inverseVipPriorityFunction.apply(59.64), 12.0));
        assert(equalsWithinEpsilon(inverseVipPriorityFunction.apply(701.12), 80.0));
        assert(equalsWithinEpsilon(inverseVipPriorityFunction.apply(13899.0), 1000.0));
        assert(equalsWithinEpsilon(inverseVipPriorityFunction.apply(1081980.0), 50000.0));

        /*
        All Vip Orders have a minimum priority of 4
         */
        assert(equalsWithinEpsilon(inverseVipPriorityFunction.apply(6.592), 4.0));
        assert(equalsWithinEpsilon(inverseVipPriorityFunction.apply(1.0), 4.0));
    }

    /*
   nlogn
    */
    @Test
    public void testPriorityPriorityFunction(){
        Function<Double, Double> priorityFunction = distributedWorkOrderQueue.getPriorityFunctionForClass(WorkOrderClass.PRIORITY);
        assert(equalsWithinEpsilon(priorityFunction.apply(12.0), 29.82));
        assert(equalsWithinEpsilon(priorityFunction.apply(80.0), 350.4));
        assert(equalsWithinEpsilon(priorityFunction.apply(1000.0), 6900.0));
        assert(equalsWithinEpsilon(priorityFunction.apply(50000.0), 541000.0 ));

        /*
        All Normal Orders have a minimum priority of 3
         */
        assert(equalsWithinEpsilon(priorityFunction.apply(0.5), 3.0));
        assert(equalsWithinEpsilon(priorityFunction.apply(1.0), 3.0));
    }

    /*
   inverse nlogn
    */
    @Test
    public void testInversePriorityPriorityFunction(){
        Function<Double, Double> inversePriorityFunction = distributedWorkOrderQueue.getInversePriorityFunctionForClass(WorkOrderClass.PRIORITY);
        assert(equalsWithinEpsilon(inversePriorityFunction.apply(29.82), 12.0));
        assert(equalsWithinEpsilon(inversePriorityFunction.apply(350.4), 80.0));
        assert(equalsWithinEpsilon(inversePriorityFunction.apply(6900.0), 1000.0));
        assert(equalsWithinEpsilon(inversePriorityFunction.apply(541000.0), 50000.0));

        /*
        All Vip Orders have a minimum priority of 4
         */
        assert(equalsWithinEpsilon(inversePriorityFunction.apply(2.0), 3.0));
        assert(equalsWithinEpsilon(inversePriorityFunction.apply(1.0), 3.0));
    }

    private boolean equalsWithinEpsilon(Double actual, Double value){
        return value - value*EPSILON < actual && actual < value + value*EPSILON;
    }
}
