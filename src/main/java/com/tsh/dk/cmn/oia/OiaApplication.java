package com.abs.cmn.oia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class OiaApplication {

    Environment env;
    public static void main(String[] args) {
        SpringApplication.run(OiaApplication.class, args);
    }

    @Bean
    public SolaceConfiguration sessionConfiguration(){
        return new SolaceConfiguration(env);
    }


}
