package com.abs.cmn.oia.activator;

import com.abs.cmn.oia.config.GracefulShutdownTomcatConnector;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.math.NumberUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@Getter
public class OiaApStoppedActivator implements ApplicationListener<ContextClosedEvent> {

    @Value("${ap.timeout-ms.shutdown}")
    private String awaitTerminateTime;

    private final GracefulShutdownTomcatConnector gracefulShutdownTomcatConnector;

    public OiaApStoppedActivator(GracefulShutdownTomcatConnector gracefulShutdownTomcatConnector) {
        this.gracefulShutdownTomcatConnector = gracefulShutdownTomcatConnector;
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        log.info("Start onApplicationEvent");
        log.info("Start GracefulShutdownEventListener");


        gracefulShutdownTomcatConnector.getConnector().pause();

        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) gracefulShutdownTomcatConnector.getConnector()
                .getProtocolHandler()
                .getExecutor();

        threadPoolExecutor.shutdown();
        log.info("Thread Pool is Shutdown.");

        try {
            long defaultAwait = 180000;
            Long awaitTime = null;
            String strAwaitTime = this.getAwaitTerminateTime();
            if(NumberUtils.isNumber(strAwaitTime)){
                awaitTime = Long.valueOf(strAwaitTime);
            } else
            {
                // default 값
                awaitTime = defaultAwait;
            }
            log.info("await Terminate Time: " + String.valueOf(awaitTime) + " MilliSeconds.");
            threadPoolExecutor.awaitTermination(awaitTime, TimeUnit.MILLISECONDS);

            log.info("Web Application Gracefully Stopped.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();

            log.error("Web Application Graceful Shutdown Failed.");
        }


        log.info("Complete GracefulShutdownEventListener");
        threadPoolExecutor.shutdownNow();
    }
}
