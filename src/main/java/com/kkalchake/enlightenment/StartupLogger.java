package com.kkalchake.enlightenment;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.event.ApplicationReadyEvent;

@Component
public class StartupLogger {

    @EventListener(ApplicationReadyEvent.class)
    public void onStatusCheck() {
        System.out.println("------------------------------------------------");
        System.out.println(" SERVER IS LIVE on http://localhost:8080!");
        System.out.println(" Welcome Page: http://localhost:8080/welcome");
        System.out.println("------------------------------------------------");
    }
}
