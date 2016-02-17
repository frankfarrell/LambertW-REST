package com.github.frankfarrell.snowball.controller;

import com.github.frankfarrell.snowball.exceptions.AlreadyExistsException;
import com.github.frankfarrell.snowball.exceptions.NotFoundException;
import com.github.frankfarrell.snowball.model.WorkOrder;
import com.github.frankfarrell.snowball.service.WorkOrderQueue;
import com.github.frankfarrell.snowball.service.statistics.StatisticsService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/**
 * Created by Frank on 15/02/2016.
 */
@WebAppConfiguration
public class WorkOrderControllerTest {

    private MockMvc mvc;
    private final String ROOT_URL = "/workorder";

    WorkOrderQueue mockWorkOrderQueue;

    StatisticsService mockStatisticsService;

    @Before
    public void setUp() throws Exception {

        mockWorkOrderQueue = mock(WorkOrderQueue.class);
        mockStatisticsService = mock(StatisticsService.class);

        final StaticApplicationContext applicationContext = new StaticApplicationContext();
        applicationContext.registerSingleton("restExceptionHandler", RestExceptionHandler.class);

        final WebMvcConfigurationSupport webMvcConfigurationSupport = new WebMvcConfigurationSupport();
        webMvcConfigurationSupport.setApplicationContext(applicationContext);

        mvc = MockMvcBuilders
                .standaloneSetup(new WorkOrderController(mockWorkOrderQueue, mockStatisticsService ))
                .setHandlerExceptionResolvers(webMvcConfigurationSupport.handlerExceptionResolver())
                .build();
    }

    //TODO Get happy path

    @Test
    public void getWorkOrderThatDoesNotExist() throws Exception {

        when(mockWorkOrderQueue.getWorkOrder(any(Long.class))).thenThrow(new NotFoundException("id", 123));

        mvc.perform(MockMvcRequestBuilders.get(ROOT_URL + "/123")
                .accept(MediaType.APPLICATION_JSON)
                .contentType("application/json"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void putWorkOrderThatAlreadyExists() throws Exception {

        when(mockWorkOrderQueue.pushWorkOrder(any(WorkOrder.class))).thenThrow(new AlreadyExistsException("id", 123));

        mvc.perform(MockMvcRequestBuilders.put(ROOT_URL)
                .accept(MediaType.APPLICATION_JSON)
                .contentType("application/json")
                .content("{\"id\": 123,\"timeStamp\":\"2016-02-12T03:21:55+00:00\"}"))
                .andExpect(status().isConflict());
    }
}