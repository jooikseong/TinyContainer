package com.naver.chapter4scope;

@MyComponent
@MyScope("prototype")
public class UserService {
    private final UserRepository userRepository;
    private final long instanceId = (long) (Math.random() * 10000);

    @MyAutowired
    public UserService(UserRepository userRepository){
        this.userRepository = userRepository;
        System.out.println("-> UserService 생성자 실행. InstanceId: " + instanceId);
    }

    public void displayUserInfo(String id){
        String result = this.userRepository.findUser(id);
        System.out.println("[ID" + instanceId + "] UserService result " + result  );
    }

    public long getInstanceId(){
        return this.instanceId;
    }

}
