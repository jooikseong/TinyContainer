package com.naver.chapter4scope;

public class MainApplication {
    public static void main(String[] args) {
        TinyContainer container = new TinyContainer("com.tiny.spring");

        // 1. 첫번째 UserService 요청
        UserService userService1 = container.getBean("userService", UserService.class);
        System.out.println("\n--- 1차 요청 ---");
        userService1.displayUserInfo("A");

        // 잠시 대기
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {}

        // 2. 두번째 UserServiece 요청
        UserService userService2 = container.getBean("userService", UserService.class);
        System.out.println("\n--- 2차 요청 ---");
        userService2.displayUserInfo("B");

        // 결과 비교
        System.out.println("\n--- 결과 비교 ---");
        System.out.println("Service 1 ID: " + userService1.getInstanceId());
        System.out.println("Service 2 ID: " + userService2.getInstanceId());

        boolean areDiofferent = (userService1 != userService2);
        System.out.println("UserService 인스턴스는 다릅니다 (prototype): " + areDiofferent);
    }
}
