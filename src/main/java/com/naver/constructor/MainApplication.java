package com.naver.constructor;

public class MainApplication {
    public static void main(String[] args) {
        // 컨테이너 초기화 -> (UserRepository 생성) -> (UserService 생성, DI 포함)
        TinyContainer container = new TinyContainer("com.tiny.spring");

        UserService userService = container.getBean("userService", UserService.class);

        if(userService != null){
            System.out.println("--- 컨테이너 사용: 생성자 DI 테스트 ---");
            userService.displayUserInfo("54321");
        } else {
            System.out.println("UserService 빈을 찾을수 없습니다.");
        }
    }
}
