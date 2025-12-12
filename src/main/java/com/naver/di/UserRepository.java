package com.naver.di;

import com.naver.ioc.MyComponent;

@MyComponent
public class UserRepository {

    public String findUser(String id) {
        return "User with ID " + id + " found from DB (simulated)";
    }
}
