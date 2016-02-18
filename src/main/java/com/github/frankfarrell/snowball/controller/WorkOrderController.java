package com.github.frankfarrell.snowball.controller;

import com.github.frankfarrell.snowball.model.QueuedWorkOrder;
import com.github.frankfarrell.snowball.model.WorkOrder;
import com.github.frankfarrell.snowball.model.statistics.QueueStatistics;
import com.github.frankfarrell.snowball.model.statistics.StatisticalSummaryRequest;
import com.github.frankfarrell.snowball.service.WorkOrderQueue;
import com.github.frankfarrell.snowball.service.statistics.StatisticsService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

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
    public @ResponseBody ResponseEntity<QueuedWorkOrder> getWorkOrder(@PathVariable Long id){

        QueuedWorkOrder workOrder = workOrderQueue.getWorkOrder(id);

        return new ResponseEntity<QueuedWorkOrder>(workOrder, HttpStatus.OK);

    }

    @RequestMapping(
            value = "{id}",
            method = RequestMethod.DELETE,
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    public @ResponseBody ResponseEntity deleteWorkOrder(@PathVariable Long id) throws InterruptedException, ExecutionException{//TODO validate all ids

        //Blocks until Request Complete
        //Should this be fire and forget?
        workOrderQueue.removeWorkOrder(id).get();
        return new ResponseEntity(HttpStatus.NO_CONTENT);

    }


    /*
    Pop from queue is not implemented as Get as this is not idempotent.
    This has the effect of the changing the state of the queue
    Possible extensions are to implement ack and Delete on client side?

    ActiveMQ Solution: http://activemq.apache.org/restful-queue.html
    POST /subscriptions
    Returns uri to a resource, hidden from other clients
    Client does a GET and a DELETE
    Server can implement time out, if client fails tricky
    */
    @RequestMapping(
            value = "",
            method = RequestMethod.POST,
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    public @ResponseBody ResponseEntity<QueuedWorkOrder> popWorkOrder() throws InterruptedException, ExecutionException{

        Optional<QueuedWorkOrder> nextOrder = workOrderQueue.popWorkOrder().get();
        if(nextOrder.isPresent()){
            return new ResponseEntity<QueuedWorkOrder>(nextOrder.get(), HttpStatus.OK);
        }
        else{
            //Request was understood, but no content to return
            return new ResponseEntity<QueuedWorkOrder>(HttpStatus.NO_CONTENT);
        }
    }

    /*
    Clients pass Timestamp with request.
    Timestamp must be of Zoned ISO Format: YYYY-MM-DDTHH:MM:SS+ZoneId
    For instance 2016-02-12T03:21:55+00:00
     */
    @RequestMapping(
            value = "",
            method = RequestMethod.PUT,
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    public @ResponseBody ResponseEntity<QueuedWorkOrder> pushWorkOrder(@RequestBody @Valid WorkOrder order) throws InterruptedException, ExecutionException{
        QueuedWorkOrder workOrder = workOrderQueue.pushWorkOrder(order).get();
        return new ResponseEntity<QueuedWorkOrder>(workOrder, HttpStatus.CREATED);
    }

    /*
    Uses POST as operation not a representation of state
    See:
    http://stackoverflow.com/questions/430809/rest-interface-for-finding-average
    https://www.thoughtworks.com/insights/blog/rest-api-design-resource-modeling
     */
    @RequestMapping(
            value = "/statistics",
            method = RequestMethod.POST,
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    public @ResponseBody ResponseEntity<QueueStatistics> getWorkOrderStatistics(@RequestBody StatisticalSummaryRequest statisticalSummaryRequest){

        QueueStatistics statisticalSummary = statisticsService.getStatistics(statisticalSummaryRequest);
        return new ResponseEntity<QueueStatistics>(statisticalSummary, HttpStatus.OK);
    }

}
