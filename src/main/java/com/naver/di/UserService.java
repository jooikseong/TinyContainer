package com.naver.di;

import com.naver.ioc.MyComponent;

@MyComponent
public class UserService {

    @MyAutowired
    private UserRepository userRepository;

    public void displayUserInfo(String id){
        String result = userRepository.findUser(id);
        System.out.println("UserService result " + result);
    }

}
