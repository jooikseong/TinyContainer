package com.naver.chapter2di;

import com.naver.chapter1ioc.MyComponent;

@MyComponent
public class UserRepository {

    public String findUser(String id) {
        return "User with ID " + id + " found from DB (simulated)";
    }
}
