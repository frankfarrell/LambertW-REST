package com.github.frankfarrell.snowball.service;

import com.github.frankfarrell.snowball.model.QueuedWorkOrder;
import com.github.frankfarrell.snowball.model.WorkOrder;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;

/**
 * Created by Frank on 12/02/2016.
 */
public interface WorkOrderQueue {

    List<QueuedWorkOrder> getAllWorkOrders();

    QueuedWorkOrder getWorkOrder(Long id);

    Future<Boolean> removeWorkOrder(Long id);

    Future<Optional<QueuedWorkOrder>> popWorkOrder();

    Future<QueuedWorkOrder> pushWorkOrder(WorkOrder workOrder);

}
