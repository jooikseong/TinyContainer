package com.naver.chapter5aop;

public class MainApplication {
    public static void main(String[] args) {
        // 1. 컨테이너 초기화 (IoC/DI/AOP 실행)
        TinyContainer container = new TinyContainer();

        System.out.println("TinyContainer constructor called");

        // 2. UserSErvice (prototype, AOP 적용) 요청
        IUserService service1 = container.getBean("userService", IUserService.class);

        // 3. AOP 메서드 호출 (로깅 출력 예상)
        System.out.println("\n[Test] 1차 service1.displayUserInfo() 호출");
        service1.displayUserInfo("A");

        // 4. AOP 비적용 메서드 호출 ( 로깅 출력 없음 예상 )
        System.out.println("\n[Test] 1차 service1.printId() 호출 (AOP 미적용)");
        service1.printId();// 프록시는 IUserServgice 타입이지만, printId는 UserService에만 있으므로 캐스팅 필요

        // 5. 두번째 UserService 요청 (새 인스턴스 생성)
        IUserService service2 = container.getBean("userService", IUserService.class);
        System.out.println("\n[Test] 2차 service2.displayuserInfo() 호출");
        service2.displayUserInfo("B");

        System.out.println("\n--- 최종 결과 요약 ---");
        System.out.println("Service 1 ID: "+ service1.getInstanceId() );
        System.out.println("Service 2 ID: "+ service2.getInstanceId() );
        System.out.println("UserSErvice 인스턴스는 다릅니다 (Prototype) : " + (service1 != service2));
    }
}
