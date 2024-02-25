package com.abs.cmn.oia;

import java.io.IOException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;

import com.abs.cmn.oia.config.OiaPropertyObject;
import org.springframework.stereotype.Component;


@EnableEurekaClient
@SpringBootApplication
public class OiaApplication {

    Environment env;
    public static void main(String[] args) throws IOException {
        SpringApplication.run(OiaApplication.class, args);

    }

    @Bean
    public OiaPropertyObject getOiaPropertyObject() {
        return OiaPropertyObject.createInstance(env);
    }

}
