package com.naver.chapter5aop;

@MyComponent @MyScope("singletone")
public class UserRepository implements IUserRepository {

    // 싱글톤임을 확인하기 위한 필드
    private final long createionTime = System.currentTimeMillis();


    @Override
    public long getCreationTime() {
        return createionTime;
    }

    @Override
    public String findUser(String id){
        return "User with id " + id + " found. Time: " + createionTime;
    }

}
