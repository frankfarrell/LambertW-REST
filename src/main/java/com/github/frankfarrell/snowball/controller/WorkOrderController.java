package com.github.frankfarrell.snowball.controller;

import com.github.frankfarrell.snowball.model.QueuedWorkOrder;
import com.github.frankfarrell.snowball.model.WorkOrder;
import com.github.frankfarrell.snowball.model.statistics.StatisticalSummary;
import com.github.frankfarrell.snowball.model.statistics.StatisticalSummaryRequest;
import com.github.frankfarrell.snowball.service.WorkOrderQueue;
import com.github.frankfarrell.snowball.service.statistics.StatisticsService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.Instant;
import java.util.List;

/**
 * Created by Frank on 12/02/2016.
 */
@RestController
@RequestMapping("/workorder")
public class WorkOrderController {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private WorkOrderQueue workOrderQueue;
    private StatisticsService statisticsService;

    @Autowired
    public WorkOrderController(WorkOrderQueue workOrderQueue, StatisticsService statisticsService){
        this.workOrderQueue = workOrderQueue;
        this.statisticsService = statisticsService;
    }

    @RequestMapping(
            value = "",
            method = RequestMethod.GET,
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @ApiOperation(value = "getWorkOrders", nickname = "Get All Orders")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success", response = QueuedWorkOrder.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Failure")})
    public @ResponseBody
    ResponseEntity<List<QueuedWorkOrder>> getWorkOrders(){
        log.debug("Client Request for all work orders");
        List<QueuedWorkOrder> workOrders = workOrderQueue.getAllWorkOrders();
        return new ResponseEntity<List<QueuedWorkOrder>>(workOrders, HttpStatus.OK);
    }

    @RequestMapping(
            value = "{id}",
            method = RequestMethod.GET,
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @ApiOperation(value = "getWorkOrder", nickname = "Get Order")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success", response = QueuedWorkOrder.class),
            @ApiResponse(code = 404, message = "Not Found"),
            @ApiResponse(code = 500, message = "Failure")})
    public @ResponseBody ResponseEntity<QueuedWorkOrder> getWorkOrder(@PathVariable @Valid long id){//TODO validate all ids

        QueuedWorkOrder workOrder = workOrderQueue.getWorkOrder(id);

        return new ResponseEntity<QueuedWorkOrder>(workOrder, HttpStatus.OK);

    }

    @RequestMapping(
            value = "{id}",
            method = RequestMethod.DELETE,
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    public @ResponseBody ResponseEntity deleteWorkOrder(@PathVariable @Valid Long id){//TODO validate all ids

        return new ResponseEntity(HttpStatus.NO_CONTENT);

    }


    /*
    Pop from queue is not implemented as Get as this is not idempotent.
    This has the effect of the changing the state of the queue
    Possible extensions are to implement ack and Delete on client side?
     */
    @RequestMapping(
            value = "",
            method = RequestMethod.POST,
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    public @ResponseBody ResponseEntity<WorkOrder> popWorkOrder(@RequestBody int i){//TODO What sort of command?
        WorkOrder workOrder = workOrderQueue.popWorkOrder();
        return new ResponseEntity<WorkOrder>(workOrder, HttpStatus.OK);
    }

    @RequestMapping(
            value = "{id}",
            method = RequestMethod.PUT,
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    public @ResponseBody ResponseEntity<QueuedWorkOrder> pushWorkOrder(@PathVariable long id, @RequestBody Instant timeStamp){//TODO Change this to ISOFormat, put in request header? or WorkOrder with serialiser
        QueuedWorkOrder workOrder = workOrderQueue.pushWorkOrder(new WorkOrder(id, timeStamp));
        return new ResponseEntity<QueuedWorkOrder>(workOrder, HttpStatus.OK);
    }

    /*
    Average wait time
    GET /average ?
    Simple, easy to understand. But not idempotent or representation of a state
    or
    POST / {resources: TYPE, statistics: average/variance, max, min, etc}
    http://stackoverflow.com/questions/430809/rest-interface-for-finding-average
    https://www.thoughtworks.com/insights/blog/rest-api-design-resource-modeling
     */
    @RequestMapping(
            value = "/statistics",
            method = RequestMethod.POST,
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    public @ResponseBody ResponseEntity<StatisticalSummary> getWorkOrderStatistics(@RequestBody StatisticalSummaryRequest statisticalSummaryRequest){//TODO Change this to ISOFormat, put in request header? or WorkOrder with serialiser

        StatisticalSummary statisticalSummary = statisticsService.getStatistics(statisticalSummaryRequest);
        return new ResponseEntity<StatisticalSummary>(statisticalSummary, HttpStatus.OK);
    }

}
