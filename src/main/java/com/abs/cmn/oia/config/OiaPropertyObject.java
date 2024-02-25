package com.abs.cmn.oia.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import lombok.Getter;

@Getter
@Component
public class OiaPropertyObject {


    Environment env;
    @Value("${ap.info.group}")
    private String groupName;
    @Value("${ap.info.site}")
    private String siteName;
    @Value("${ap.info.env}")
    private String envType;

    @Value("${ap.timeout-ms.request}")
    private int requestTimeout;

    // SEQ Library
    @Value("${ap.sequence.file.path}")
    private String sequenceFilePath;
    @Value("${ap.sequence.file.name}")
    private String sequenceFileName;

    @Value("${ap.interface.destination.receive.queue}")
    private String replyQueueName;

    @Value("${ap.interface.destination.test.send.topic}")
    private String testSendTopicName;



    private static OiaPropertyObject instance;

    @Bean
    // Public method to get the Singleton instance
    public static OiaPropertyObject createInstance(Environment env) {
        if (instance == null) {
            synchronized (OiaPropertyObject.class) {
                // Double-check to ensure only one instance is created
                if (instance == null) {
                    instance = new OiaPropertyObject(env);
                }
            }
        }

        return instance;
    }

    public static OiaPropertyObject getInstance(){
        return instance;
    }
    public OiaPropertyObject(Environment env) {
        this.env = env;
        instance = this;
    }

}
