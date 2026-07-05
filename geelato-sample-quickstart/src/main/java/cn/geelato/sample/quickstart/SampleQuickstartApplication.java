package cn.geelato.sample.quickstart;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
public class SampleQuickstartApplication {

    public static void main(String[] args) {
        log.info("Starting SampleQuickstartApplication");
        SpringApplication.run(SampleQuickstartApplication.class, args);
    }
}
