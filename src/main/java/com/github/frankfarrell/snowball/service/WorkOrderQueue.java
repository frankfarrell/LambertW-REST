package com.github.frankfarrell.snowball.service;

import com.github.frankfarrell.snowball.model.QueuedWorkOrder;
import com.github.frankfarrell.snowball.model.WorkOrder;

import java.util.List;

/**
 * Created by Frank on 12/02/2016.
 */
public interface WorkOrderQueue {

    List<QueuedWorkOrder> getAllWorkOrders();

    QueuedWorkOrder getWorkOrder(long id);

    void removeWorkOrder(long id);

    WorkOrder popWorkOrder();

    QueuedWorkOrder pushWorkOrder(WorkOrder workOrder);

}
