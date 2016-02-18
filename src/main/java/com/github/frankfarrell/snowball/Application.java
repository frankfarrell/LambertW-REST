package com.github.frankfarrell.snowball;

import com.github.frankfarrell.snowball.controller.WorkOrderController;
import com.github.frankfarrell.snowball.service.DistributedWorkOrderQueue;
import com.github.frankfarrell.snowball.service.WorkOrderQueue;
import com.github.frankfarrell.snowball.service.statistics.StatisticsService;
import org.redisson.Config;
import org.redisson.Redisson;
import org.redisson.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
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

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
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

    /*
    Inject Clock Dependency.
    Enables easier unit testing, eg with Clock.fixed()
    Decouples PriorityQueue from Clock implementation
     */
    @Bean
    public Clock clock(){
        return Clock.systemUTC();
    }

    @Bean
    public StatisticsService statisticsService(WorkOrderQueue workOrderQueue){
        return new StatisticsService(workOrderQueue);
    }

    @Bean
    public WorkOrderQueue workOrderQueue(RedissonClient redisson, Clock clock){
        return new DistributedWorkOrderQueue(redisson, clock);
    }

    @Value("${spring.redis.port}")
    private int redisPort;

    @Value("${spring.redis.address}")
    private String redisAddress;

    @Value("${spring.redis.heroku}")
    private Boolean redisHeroku;

    @Bean
    public RedissonClient redisson() throws URISyntaxException{
        /*
        Three Possibilities here
        Redis Running on Local,etc
        Redis Running Embedded
        Redis Running Heroku
         */
        Config config = new Config();

        //Unfortunately this is the only way to get it to work
        if(redisHeroku){

            //redis://h:pq37lpe3ove4v5m82vnngas54c@ec2-176-34-249-171.eu-west-1.compute.amazonaws.com:19589
            URI redisURI = new URI(System.getenv("REDIS_URL"));

            config.useSingleServer()
                    .setAddress(redisURI.getHost() + ":" + redisURI.getPort())
                    .setPassword(redisURI.getAuthority());
        }
        else{
            config.useSingleServer().setAddress(redisAddress +":"+ redisPort);
        }

        return Redisson.create(config);
    }

    @Bean
    public Docket newsApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("WorkOrders")
                .apiInfo(apiInfo())
                .select()
                .paths(regex("/workorder.*"))
                .build()
                .pathMapping("/")
                .directModelSubstitute(OffsetDateTime.class, String.class)
                .directModelSubstitute(Duration.class, Double.class);
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
