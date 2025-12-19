package com.naver.chapter5aop;

@MyComponent
@MyScope("prototype")
@MyEnableAop
public class UserService implements IUserService {

    private final IUserRepository userRepository;
    private final long instanceId = (long) (Math.random() * 10000);

    @MyAutowired
    // 생성자 파마리터도 인터페이스 타입으로 받습니다.
    public UserService(IUserRepository userRepository) {
        this.userRepository = userRepository;
        System.out.println("-> UserService 생성자 실행. Instance ID: " + instanceId);
    }

    @Override
    @MyLoging
    public void displayUserInfo(String id) {
        String result = this.userRepository.findUser(id);
        System.out.println("[ID " + instanceId + "] UserService result " + result);
    }

    @Override
    public long getInstanceId() {
        return instanceId;
    }

    @Override
    public void printId(){
        System.out.println("No AOP here. ID: " + instanceId);
    }

}
