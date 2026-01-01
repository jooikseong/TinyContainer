package com.naver.chapter14async;


import java.lang.annotation.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// 비동기 관련 어노테이션 정의
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface MyAsync {}

// 비동기 실행을 담당하는 AOP 핸들러
class AsyncInvocationHandler implements InvocationHandler {

    private final Object target;
    // 모든 비동기 작업을 처리할 공용 스레드 풀
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    public AsyncInvocationHandler(Object target) {
        this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 실제 클래스의 메서드에서 @MyAsync 확인
        Method targetMethod = target.getClass().getMethod(method.getName(), method.getParameterTypes());

        if(targetMethod.isAnnotationPresent(MyAsync.class)) {
            // --- 비동기 로직 ---
            System.out.println("[Async] >>> '" + method.getName() + "' 작업을 별도 스레드에 위임합니다.");

            executorService.submit(() -> {
                try {
                    method.invoke(target, args);
                } catch (Exception e) {
                    System.out.println("[Async Error] 비동기 작업 중 오류 " + e.getMessage());
                }
            });

            return null; // 비동기이므로 즉신 리턴 (void 메서드 기준)
        }

        // 일반 메서드는 그대로 실행
        return method.invoke(target, args);
    }
}

// 비즈니스 서비스 (인터페이스 필수)
interface IMailService {
    void sendMail(String address, String content);
}

class MailService implements IMailService {
    @Override
    @MyAsync // 이 메서드는 비동기로 동작함
    public void sendMail(String address, String content) {
        try {
            System.out.println("[Mail] 메일 발송 중: " + address + "...");
            Thread.sleep(3000);
            System.out.println("[Mail] 발송 완료: " + content);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

public class TinyAsyncComplete {
    public static void main(String[] args) throws InterruptedException {
        // 실제 객체와 프록시 생성
        IMailService realService = new MailService();
        IMailService proxyService = (IMailService) Proxy.newProxyInstance(
                IMailService.class.getClassLoader(),
                new Class[]{IMailService.class},
                new AsyncInvocationHandler(realService)
        );

        System.out.println("--- 메인 쓰레드: 작업 시작 ---");

        // 비동기 메서드 호출 (3초 걸리는 작업이지만 즉시 당므 줄로 넘어감)
        proxyService.sendMail("user@naver.com", "반가워요! Tiny Spring입니다.");

        System.out.println("--- 메인 스레드: 메일 발송을 위임하고 즉시 다른 일을 합니다. ---");
        System.out.println("--- 메인 스레드: 화면에 결과를 먼저 보여줍니다. ---");

        // 프로그램이 바로 종료되지 않도록 잠시 대기
        Thread.sleep(4000);
        System.out.println("\n--- 전체 프로그램 종료 ---");
    }
}
