package com.naver.chapter16schedule;

import com.naver.chapter1ioc.MyComponent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// 1. 스케줄러 어노테이션 정의
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface MyScheduled {
    long fixedDelay() default -1;
}

// 2. 스케줄링을 처리하는 서비스 (빈)
@MyComponent
class TinyTaskService {
    private int count = 1;

    @MyScheduled(fixedDelay = 2000)
    public void reportCurrentTime() {
        System.out.println("[Scheduled Task] 현재 시간: " + new Date() + " (실행 횟수: " + count++ + ")");
    }

    @MyScheduled(fixedDelay = 5000)
    public void heavyCleanup() {
        System.out.println("[Scheduled Task] >>> 대량 데이터 정리 작업 중... (5초 주기)");
    }
}

// 3. 스케줄러 엔진 (TinyContainer 확장)
class TinyContainer {
    private final Map<String, Object> beans = new HashMap<>();
    // 자바 표준 스케줄링 전용 스레드 풀
    private final ScheduledExecutorService schedule = Executors.newScheduledThreadPool(2);

    public void init() {
        // 1. 빈 등록
        beans.put("tinyTaskService", new TinyTaskService());

        processScheduling();
    }

    private void processScheduling() {
        for(Object bean : beans.values()) {
            Method[] methods = bean.getClass().getDeclaredMethods();
            for (Method method : methods) {
                if (method.isAnnotationPresent(MyScheduled.class)) {
                    MyScheduled myScheduled = method.getAnnotation(MyScheduled.class);
                    long fixedDelay = myScheduled.fixedDelay();

                    if (fixedDelay > 0) {
                        schedule.scheduleWithFixedDelay(()-> {
                            try {
                                method.invoke(bean);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }, 0, fixedDelay, TimeUnit.MICROSECONDS);
                        System.out.println("[System] 스케줄 등록 완료: " + method.getName());
                    }
                }
            }
        }
    }

    public void stop() {
        schedule.shutdown();
    }

}

public class TinyScheduledMain {
    public static void main(String[] args) throws InterruptedException {
        TinyContainer tinyContainer = new TinyContainer();
        tinyContainer.init();

        System.out.println("--- Tiny Spring Scheduler 가동 중 (15초간 관찰) ---\n");

        Thread.sleep(15000);

        tinyContainer.stop();
        System.out.println("\n--- 스케줄러 종료 ---");
    }
}
