import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import java.util.Arrays;

/**
 * Created by Frank on 11/02/2016.
 */
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
}
