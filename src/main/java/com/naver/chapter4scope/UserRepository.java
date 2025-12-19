package com.naver.chapter4scope;

@MyComponent
public class UserRepository {
    // 싱글톤임을 확인하기 위한 필드
    private final long createionTime = System.currentTimeMillis();

    public long getCreateionTime() {return this.createionTime;}

    public String findUser(String id){
        return "User with id " + id + " found. Time: " + createionTime;
    }

}
