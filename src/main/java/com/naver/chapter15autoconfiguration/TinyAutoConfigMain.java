package com.naver.chapter15autoconfiguration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

// 1. 자동 설정을 위한 어노테이션
@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE) @interface MyConfiguration {}
@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD) @interface MyBean {}

// 특정 클래스가 클래스패스에 존재할 때만 동작하도록 하는 조건
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@interface MyConditionalOnClass {
    String value();
}

// 2. 자동 설정 대상 (가상의 라이브러리 상황)

// 시나리오 A: 프로젝트에 존재하는 라이브러리 클래스 // 존재하므로 등록됨
class RealLibrary{}

// 시나리오 B: 프로젝트에 없는 라이브러리 클래스 (가정)
// class MissingLibrary {}

@MyConfiguration
//@MyConditionalOnClass("RealLibrary")
@MyConditionalOnClass("com.naver.chapter15autoconfiguration.RealLibrary")
class LibraryAutoConfig {
    @MyBean
    public String realService() { return "RealLibrary 서비스가 자동 등록되었습니다."; }
}

@MyConfiguration
@MyConditionalOnClass("com.notfound.FakeLibrary") // 절대 존재할 수 없는 경로 // 존재하지 않으므로 무시됨
class MissingAutoConfig {
    @MyBean
    public String missingService() { return "이 메시지는 보이면 안 됩니다."; }
}

// 3. Auto Configuration 엔진

class TinyContainer {
    private final Map<String, Object> beanMap = new HashMap<>();

    public void loadAutoConfigurations(Class<?>... configClasses){
        System.out.println("--- Tiny Auto-Configuration 스캔 시작 ---");

        for (Class<?> configClass : configClasses) {
            if (configClass.isAnnotationPresent(MyConfiguration.class)) {
                // 어노테이션 존재 여부를 먼저 확인
                MyConditionalOnClass condAnno = configClass.getAnnotation(MyConditionalOnClass.class);

                if (condAnno == null) {
                    // 조건 어노테이션이 없으면 무조건 등록
                    registerBeans(configClass);
                    continue;
                }

                String requiredClassName = condAnno.value();
                if(isClassPresent(requiredClassName)) {
                    System.out.println("[Condition Match] '" + requiredClassName + "' 발견! 설정을 활성화합니다.");
                    registerBeans(configClass);
                } else {
                    System.out.println("[Condition Skip] '" + requiredClassName + "' 없음. 설정을 건너뜁니다.");
                }
            }

//            // 1. @MyConfiguration이 붙어있는지 확인
//            if(!configClass.isAnnotationPresent(MyConfiguration.class)) continue;
//
//            // 2. @MyConditionalOnClass 어노테이션 가져오기
//            MyConditionalOnClass condition = configClass.getAnnotation(MyConditionalOnClass.class);
//
//            boolean shouldRegister = true;
//            if(condition != null) {
//                String requiredClassName = condition.value();
//                shouldRegister = isClassPresent(requiredClassName);
//
//                if(shouldRegister) {
//                    System.out.println("[Condition Match] '" + requiredClassName + "' 발견!");
//                } else {
//                    System.out.println("[Condition Skip] '" + requiredClassName + "' 없음. 건너뜁니다.");
//                }
//            }
//
//            if(shouldRegister) {
//                registerBeans(configClass);
//            }
        }
    }

    private void registerBeans(Class<?> configClass) {
        try {
            Object configInstance = configClass.getDeclaredConstructor().newInstance();
            for (Method method : configClass.getDeclaredMethods()) {
                if(method.isAnnotationPresent(MyBean.class)) {
                    Object bean = method.invoke(configInstance);
                    beanMap.put(method.getName(), bean);
                    System.out.println(" -> 빈 등록 완료: " + method.getName());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 리플렉션을 이용해 클래스 존재 여부 파악 ( 스프링 부트의 핵심 기법 )
    private boolean isClassPresent(String className) {
        try {
            // 현재 클래스 로더에서 해당 이름의 클래스를 찾을 수 있는지 확인
            Class.forName(className, false, this.getClass().getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public void printAllBeans() {
        System.out.println("\n--- 현재 등록된 전체 빈 목록 ---");
        beanMap.forEach((name, bean) -> System.out.println(name + " : " + bean));
    }

}

public class TinyAutoConfigMain {
    public static void main(String[] args) {
        TinyContainer tinyContainer = new TinyContainer();

        // 잠재적인 자동 설정 클래스들을 전달
        tinyContainer.loadAutoConfigurations(
                LibraryAutoConfig.class,
                MissingAutoConfig.class
        );

        tinyContainer.printAllBeans();
    }
}
