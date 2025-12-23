package com.naver.chapter1ioc;

public class MainApp {
    public static void main(String[] args) {
        TinyContainer tinyContainer = new TinyContainer();

        try{
            // 컨테이너 초기화 (UserService가 등록됨)
            tinyContainer.initialize("com.example.service"); // 가상의 패키지 경로

            // 컨테이너로부터 객체 요청 (DI의 핵심)
            UserService userService = (UserService) tinyContainer.getBean("userService");

            // 객체 사용
            userService.greet();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
