package com.github.frankfarrell.snowball.service;

import com.github.frankfarrell.snowball.model.WorkOrder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Frank on 12/02/2016.
 */
@Service
public class MockWorkOrderImpl implements WorkOrderQueue{

    public List<WorkOrder> getAllWorkOrders(){

        ArrayList<WorkOrder> allWorkOrders = new ArrayList<WorkOrder>(2);

        WorkOrder workOrder1 = new WorkOrder(1L, Instant.now());
        WorkOrder workOrder2 = new WorkOrder(2L, Instant.now());

        allWorkOrders.add(workOrder1);
        allWorkOrders.add(workOrder2);

        return allWorkOrders;

    }

}
