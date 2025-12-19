package com.naver.chapter6javaconfig2;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

// ===============================================
// 1. 어노테이션 정의
// ===============================================
@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE) @interface MyComponent {}
@Retention(RetentionPolicy.RUNTIME) @Target({ElementType.CONSTRUCTOR, ElementType.METHOD}) @interface MyAutowired {}
@Retention(RetentionPolicy.RUNTIME) @Target({ElementType.TYPE, ElementType.METHOD}) @interface MyScope { String value() default "singleton"; }
@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE) @interface MyConfiguration {} // 추가
@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD) @interface MyBean {} // 추가

// ===============================================
// 2. 설정 클래스 및 서비스 정의 (Java Config 예시)
// ===============================================

// 외부 라이브러리 클래스라고 가정 (수정 불가)
class ExternalService {
    public void hello() { System.out.println("Hello from External Service!"); }
}

@MyConfiguration
class AppConfig {
    @MyBean
    @MyScope("singleton")
    public ExternalService externalService() {
        return new ExternalService(); // 사용자가 직접 객체 생성 로직 제어
    }
}

@MyComponent
class MyService {
    private final ExternalService externalService;

    @MyAutowired // Java Config로 등록된 빈을 주입받음
    public MyService(ExternalService externalService) {
        this.externalService = externalService;
    }

    public void run() {
        externalService.hello();
        System.out.println("MyService is running with ExternalService.");
    }
}

// ===============================================
// 3. 빈 정의 정보 및 컨테이너 구현
// ===============================================

class BeanDefinition {
    Class<?> beanClass;
    String scope;
    Method factoryMethod; // @MyBean 메서드 정보 저장
    Object configInstance; // @Configuration 클래스의 인스턴스

    public BeanDefinition(Class<?> beanClass, String scope) {
        this.beanClass = beanClass;
        this.scope = scope;
    }
}

class TinyContainer {
    private final Map<String, Object> singletonMap = new HashMap<>();
    private final Map<String, BeanDefinition> definitionMap = new HashMap<>();

    public TinyContainer(Class<?>... configClasses) {
        System.out.println("--- TinyContainer Java Config 시작 ---");

        // 1. Configuration 클래스 스캔 및 @MyBean 등록
        for (Class<?> configClass : configClasses) {
            if (configClass.isAnnotationPresent(MyConfiguration.class)) {
                processConfig(configClass);
            }
        }

        // 2. 컴포넌트 스캔 (여기서는 예시로 MyService만 등록)
        registerComponent(MyService.class);

        // 3. 싱글톤 빈 생성 및 의존성 주입
        refresh();

        System.out.println("--- TinyContainer 초기화 완료 ---");
    }

    private void processConfig(Class<?> configClass) {
        try {
            Object configInstance = configClass.getDeclaredConstructor().newInstance();
            for (Method method : configClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(MyBean.class)) {
                    String beanName = method.getName();
                    String scope = method.isAnnotationPresent(MyScope.class) ?
                            method.getAnnotation(MyScope.class).value() : "singleton";

                    BeanDefinition def = new BeanDefinition(method.getReturnType(), scope);
                    def.factoryMethod = method;
                    def.configInstance = configInstance;
                    definitionMap.put(beanName, def);
                    System.out.println(" -> @MyBean 등록: " + beanName);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void registerComponent(Class<?> clazz) {
        String name = clazz.getSimpleName().substring(0, 1).toLowerCase() + clazz.getSimpleName().substring(1);
        String scope = clazz.isAnnotationPresent(MyScope.class) ?
                clazz.getAnnotation(MyScope.class).value() : "singleton";
        definitionMap.put(name, new BeanDefinition(clazz, scope));
    }

    private void refresh() {
        for (String beanName : definitionMap.keySet()) {
            getBean(beanName, Object.class);
        }
    }

    public <T> T getBean(String name, Class<T> type) {
        if (singletonMap.containsKey(name)) return type.cast(singletonMap.get(name));

        BeanDefinition def = definitionMap.get(name);
        if (def == null) return null;

        Object instance = createBean(name, def);
        if ("singleton".equals(def.scope)) {
            singletonMap.put(name, instance);
        }
        return type.cast(instance);
    }

    private Object createBean(String name, BeanDefinition def) {
        try {
            if (def.factoryMethod != null) {
                // Case 1: @MyBean 메서드를 통해 생성
                return def.factoryMethod.invoke(def.configInstance);
            } else {
                // Case 2: 일반 컴포넌트 생성자 주입
                Constructor<?> constructor = def.beanClass.getDeclaredConstructors()[0];
                Class<?>[] paramTypes = constructor.getParameterTypes();
                Object[] args = new Object[paramTypes.length];

                for (int i = 0; i < paramTypes.length; i++) {
                    args[i] = findBeanByType(paramTypes[i]);
                }
                return constructor.newInstance(args);
            }
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private Object findBeanByType(Class<?> type) {
        for (String name : definitionMap.keySet()) {
            if (type.isAssignableFrom(definitionMap.get(name).beanClass)) {
                return getBean(name, Object.class);
            }
        }
        return null;
    }
}

// ===============================================
// 4. 실행 클래스
// ===============================================
public class TinySpringFullConfig {
    public static void main(String[] args) {
        // AppConfig를 설정 정보로 사용하여 컨테이너 생성
        TinyContainer container = new TinyContainer(AppConfig.class);

        MyService myService = container.getBean("myService", MyService.class);
        myService.run();
    }
}