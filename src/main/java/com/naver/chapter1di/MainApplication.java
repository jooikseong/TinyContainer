package com.naver.chapter1di;

public class MainApplication {
    public static void main(String[] args) {

        // 컨테이너 초기화 -> 빈 생성 -> DI 실행
        TinyContainer tinyContainer = new TinyContainer("com.tiny.spring");

        // UserSErvice 빈ㄷ을 가져옵니다. (이미 UserRepository가 주입된 상태)
        UserService userService = tinyContainer.getBean("userService", UserService.class);

        if(userService != null) {
            System.out.println("--- 컨테이너 사용 : DI 테스트 ---");
            userService.displayUserInfo("12345");
        } else {
            System.out.println("UserService 빈을 찾을 수 없습니다.");
        }
    }
}
