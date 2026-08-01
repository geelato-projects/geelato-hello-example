package cn.geelato.sample.ormevent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ORM 事件机制示例工程启动类。
 *
 * <p>基于最轻量的 geelato-orm（H2 内存库），演示 ORM 事件机制的各项能力。
 */
@SpringBootApplication
public class SampleOrmEventApplication {

    public static void main(String[] args) {
        SpringApplication.run(SampleOrmEventApplication.class, args);
    }
}
