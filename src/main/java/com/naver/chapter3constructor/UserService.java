package com.naver.chapter3constructor;

@MyComponent
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

//    @MyAutowired
//    public UserService(UserRepository userRepository) {
//        // 컨테이너가 주입해준 인자를 필드에 할당
//        this.userRepository = userRepository;
//        System.out.println("UserService 생성자 실행 : UserRepository 주입됨");
//    }

    public void displayUserInfo(String id) {
        String result = this.userRepository.findUser(id);
        System.out.println("UserService result (Constructor DI)" + result);
    }
}
