package com.naver.chapter3constructor;

@MyComponent
public class UserRepository {

    public String findUser(String id) {
        return "User with ID " + id + " found from DB (simulated)";
    }
}
