package com.naver.chapter1di;

import com.naver.chapter2ioc.MyComponent;

@MyComponent
public class UserService {

    @MyAutowired
    private UserRepository userRepository;

    public void displayUserInfo(String id){
        String result = userRepository.findUser(id);
        System.out.println("UserService result " + result);
    }

}
