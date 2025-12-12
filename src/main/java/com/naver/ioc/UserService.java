package com.naver.ioc;

import com.naver.di.MyAutowired;
import com.naver.di.UserRepository;

@MyComponent("userService")
public class UserService {
    public void greet(){
        System.out.println("Hello");
    }
}
