package com.naver.chapter9security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

@Retention(RetentionPolicy.RUNTIME) @Target({ElementType.METHOD, ElementType.TYPE})
@interface MySecured { String role() default "USER"; }

// 가상의 세션 저장소 ( 로그인 정보 보관 )
class MockSession {
    public static String loggedInUser = null;
    public static String userRole = null;

    public static void login(String user, String role) {
        loggedInUser = user;
        userRole = role;
        System.out.println("Security 로그인 성공: " + user + "(" + role + ")");
    }
}


class TinySecurityInterceptor {
    public boolean preHandle(Object handle, String methodName) throws Exception {
        // 리플랙션으로 실행될 메서드의 @MySecured 확인
        Method method = handle.getClass().getDeclaredMethod(methodName, String.class);

        if(method.isAnnotationPresent(MySecured.class)){
            MySecured secured = method.getAnnotation(MySecured.class);
            String requireRole = secured.role();

            System.out.println("[Security ] 보호된 자원 접근 시도: " + methodName + "(" + requireRole + ")");

            // 1. 인증 체크 (로그인 여부)
            if(MockSession.loggedInUser == null){
                System.out.println("[Security] 거부 : 로그인이 필요합니다.");
                return false;
            }

            // 2. 인가 체크 (권한 일치 여부)
            if(!requireRole.equals(MockSession.userRole) && !"ADMIN".equals(MockSession.userRole)){
                System.out.println("[Security] 거부 : 권한이 부족합니다. ( 보유 권한: " + MockSession.userRole + " )");
                return false;
            }
        }
        return true;
    }
}

class AdminController {
    @MySecured(role = "ADMIN")
    public void deleteUser(String id) {
        System.out.println("[System] 사용자 " + id + " 삭제 완료.");
    }

    public void viewNotice(String id) {
        System.out.println("[System] 공지사항을 조회합니다.");
    }
}

public class TinySecurityExample {
    public static void main(String[] args) throws  Exception {
        AdminController controller = new AdminController();
        TinySecurityInterceptor interceptor = new TinySecurityInterceptor();

        System.out.println("--- 시나리오 1: 비로그인 상태로 관리자 기능 접근 ---");
        if(interceptor.preHandle(controller, "deleteUser")) {
            controller.deleteUser("user123");
        }

        System.out.println("\n--- 시나리오 2: 일반 유저로 로그인 후 관리자 기능 접근 ---");
        MockSession.login("Gildong", "USER");
        if (interceptor.preHandle(controller, "deleteUser")){
            controller.deleteUser("user123");
        }

        System.out.println("\n--- 시나리오 3: 관리자로 로그인 후 관리자 기능 접근 ---");
        MockSession.login("ADMIN_King", "ADMIN");
        if(interceptor.preHandle(controller, "deleteUser")){
            controller.deleteUser("user123");
        }
    }
}
