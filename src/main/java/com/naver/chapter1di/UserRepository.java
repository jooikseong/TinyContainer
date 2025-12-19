package com.naver.chapter1di;

import com.naver.chapter2ioc.MyComponent;

@MyComponent
public class UserRepository {

    public String findUser(String id) {
        return "User with ID " + id + " found from DB (simulated)";
    }
}
