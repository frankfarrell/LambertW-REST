package com.github.frankfarrell.snowball.service;

import com.github.frankfarrell.snowball.model.QueuedWorkOrder;
import com.github.frankfarrell.snowball.model.WorkOrder;

import java.util.List;
import java.util.Optional;

/**
 * Created by Frank on 12/02/2016.
 */
public interface WorkOrderQueue {

    List<QueuedWorkOrder> getAllWorkOrders();

    QueuedWorkOrder getWorkOrder(Long id);

    void removeWorkOrder(Long id);

    Optional<QueuedWorkOrder> popWorkOrder();

    QueuedWorkOrder pushWorkOrder(WorkOrder workOrder);

}
