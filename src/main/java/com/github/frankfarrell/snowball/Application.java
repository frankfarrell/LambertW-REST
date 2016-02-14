package com.github.frankfarrell.snowball;

import com.github.frankfarrell.snowball.controller.WorkOrderController;
import com.github.frankfarrell.snowball.service.MockWorkOrderImpl;
import com.github.frankfarrell.snowball.service.WorkOrderQueue;
import com.github.frankfarrell.snowball.service.statistics.StatisticsService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import static springfox.documentation.builders.PathSelectors.regex;

import java.util.Arrays;

/**
 * Created by Frank on 11/02/2016.
 */
@EnableSwagger2
@SpringBootApplication
public class Application {

    private static ApplicationContext ctx;

    public static void main(String[] args) {
        ctx = SpringApplication.run(Application.class, args);

        printBeans();
    }

    private static void printBeans(){
        System.out.println("Beans List:");

        String[] beanNames = ctx.getBeanDefinitionNames();
        Arrays.sort(beanNames);
        for (String beanName : beanNames) {
            System.out.println(beanName);
        }
    }

    @Bean
    public WorkOrderController workOrderController(WorkOrderQueue workOrderQueue, StatisticsService statisticsService){
        return new WorkOrderController(workOrderQueue, statisticsService);
    }

    @Bean
    public StatisticsService statisticsService(WorkOrderQueue workOrderQueue){
        return new StatisticsService(workOrderQueue);
    }

    @Bean
    public WorkOrderQueue workOrderQueue(){
        return new MockWorkOrderImpl();
    }

    @Bean
    public Docket newsApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("WorkOrders")
                .apiInfo(apiInfo())
                .select()
                .paths(regex("/workorder.*"))
                .build()
                .pathMapping("/");
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("Frank's Work Orders")
                .description("Priority Queue Work Orders")
                .contact("Frank Farrell")
                .license("Apache License Version 2.0")
                .licenseUrl("https://github.com/frankfarrell/SNOWBALL-MAGIC-19851014/blob/master/LICENSE")
                .version("2.0")
                .build();
    }

}
