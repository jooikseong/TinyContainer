package com.naver.chapter7mvc;

import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE) @interface MyController {}
@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD) @interface MyRequestMapping { String value();}
@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE) @interface MyComponent {}

@MyComponent
class HelloService {
    public String getGreeting(String name) {
        return "Hello " + name + "! Welcome to Tiny Spring MVC";
    }
}

@MyController
class UserController {
    private final HelloService helloService;

    public UserController(HelloService helloService) {
        this.helloService = helloService;
    }

    @MyRequestMapping("/user")
    public String getUserInfo(String id) {
        return "Response User ID: " + id + ", Data: {name: Tiny User, age: 25}";
    }

    @MyRequestMapping("/greet")
    public String greet(String name) {
        return "[Response] Service Message: " + helloService.getGreeting(name);
    }
}

class TinyDispatcherServlet {
    // URL 경로와 실행할 메서드 정보를 매핑하여 저장
    private final Map<String, HandlerMethod> handlerMapping = new HashMap<>();
    private final TinyContainer container;

    public TinyDispatcherServlet(TinyContainer container) {
        this.container = container;
        initHandlerMapping();
    }

    // 컨트롤러를 스캔하여 URL 매핑 테이블 작성
    private void initHandlerMapping() {
        for (Object bean : container.getAllBeans()){
            Class<?> clazz = bean.getClass();
            if(clazz.isAnnotationPresent(MyController.class)){
                for (Method method : clazz.getDeclaredMethods()){
                    if(method.isAnnotationPresent(MyRequestMapping.class)){
                        String url = method.getAnnotation(MyRequestMapping.class).value();
                        handlerMapping.put(url, new HandlerMethod(bean, method));
                        System.out.println(" -> [MVC] 매핑 등록 : " + url + " = > " + method.getName());
                    }
                }
            }
        }
    }

    // 실제 HTTP 요청을 처리하는 핵심 메서드
    public void service(String url, Map<String, String> params) {
        System.out.println("\n[Request] Incoming URL:" + url + ", Param: " + params);

        HandlerMethod handlerMethod = handlerMapping.get(url);
        if(handlerMethod == null){
            System.out.println("[Response] 404 Not Found");
            return;
        }

        try {
            // 메서드 파라미터 이름에 맞춰 전달받은 params 매칭 (간단 구현)
            Parameter[] parameters = handlerMethod.method.getParameters();
            Object[] args = new Object[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                args[i] = parameters[i].getName();
            }

            // 컨트롤러 메서드 실행
            Object result = handlerMethod.method.invoke(handlerMethod.controller, args);
            System.out.println(result);
        } catch (Exception e) {
            System.out.println("[Response] 500 Internal Server Error");
        }
    }

    // 컨트롤러 인스턴스와 메서드 정보를 묶어주는 래퍼 클래스
    private static class HandlerMethod {
        Object controller;
        Method method;
        HandlerMethod(Object controller, Method method) {this.controller = controller;this.method = method;}
    }

}

class TinyContainer {
    private final Map<String, Object> beanMap = new HashMap<>();

    public TinyContainer() {
        // 수동 빈 등록 ( 실제로는 스캔 로직 )
        HelloService helloService = new HelloService();
        beanMap.put("helloService", helloService);
        beanMap.put("userService", new UserController(helloService));
        System.out.println("--- TinyContainer 빈 등록 완료 ---");
    }

    public Collection<Object> getAllBeans() { return beanMap.values(); }
}

public class TinySpringMvcComplete {
    public static void main(String[] args) {
        // 컨테이너 초기화
        TinyContainer container = new TinyContainer();

        // DispatcherServlet 초기화 (핸들러 매핑 생성)
        TinyDispatcherServlet tinyDispatcherServlet = new TinyDispatcherServlet(container);

        // 가상 HTTP 요청 테스트
        Map<String, String> params1 = new HashMap<>();
        params1.put("name", "Tiny User");
        tinyDispatcherServlet.service("/user", params1);

        Map<String, String> params2 = new HashMap<>();
        params1.put("name", "Gildong");
        tinyDispatcherServlet.service("/greet", params1);

        tinyDispatcherServlet.service("/invalid", new HashMap<>());
    }
}