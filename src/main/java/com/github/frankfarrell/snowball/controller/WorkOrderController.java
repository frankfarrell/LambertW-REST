package com.github.frankfarrell.snowball.controller;

import com.github.frankfarrell.snowball.model.WorkOrder;
import com.github.frankfarrell.snowball.service.WorkOrderQueue;
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

import java.util.List;

/**
 * Created by Frank on 12/02/2016.
 */
@RestController
@RequestMapping("/workorder")
public class WorkOrderController {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private WorkOrderQueue workOrderQueue;

    @Autowired
    public WorkOrderController(WorkOrderQueue workOrderQueue){
        this.workOrderQueue = workOrderQueue;
    }

    @RequestMapping(
            value = "",
            method = RequestMethod.GET,
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @ApiOperation(value = "getWorkOrders", nickname = "getGreeting")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success", response = List.class),
            @ApiResponse(code = 500, message = "Failure")})
    public @ResponseBody
    ResponseEntity<List<WorkOrder>> getWorkOrders(){
        log.debug("Client Request for all work orders");
        List<WorkOrder> workOrders = workOrderQueue.getAllWorkOrders();
        return new ResponseEntity<List<WorkOrder>>(workOrders, HttpStatus.OK);
    }

}
