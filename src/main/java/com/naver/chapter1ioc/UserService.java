package com.naver.chapter1ioc;

@MyComponent("userService")
public class UserService {
    public void greet(){
        System.out.println("Hello");
    }
}
