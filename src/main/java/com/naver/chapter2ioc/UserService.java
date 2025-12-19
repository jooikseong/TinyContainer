package com.naver.chapter2ioc;

@MyComponent("userService")
public class UserService {
    public void greet(){
        System.out.println("Hello");
    }
}
