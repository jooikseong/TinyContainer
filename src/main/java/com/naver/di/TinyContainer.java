package com.naver.di;

import com.naver.ioc.MyComponent;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TinyContainer {

    private final Map<String, Object> beanMap = new HashMap<>();

    public TinyContainer(String basePackage) {
        System.out.println("--- TinyContainer 초기화 시작 ---");

        // 1. 클래스 스캔 및 빈 생성
        Set<Class<?>> componentClasses = scanComponents(basePackage);
        createBeans(componentClasses);

        // 2. 의존성 주입 (DI) 실행
        injectDependencies();

        System.out.println("--- TinyContainer 초기화 완료 (" +beanMap.size() + "개 빈 등록)---");
    }

    private void injectDependencies() {
        System.out.println("--- 의존성 주입 시작 ---");

        for (Object beanInstance : beanMap.values()) {
            // 모든 필드 순회
            for (Field field : beanInstance.getClass().getDeclaredFields()) {

                // @MyAutowried 필드 탐색
                if(field.isAnnotationPresent(MyAutowired.class)){

                    Class<?> dependencyType = field.getType();
                    Object dependencyInstance = findBeanType(dependencyType);

                    if(dependencyInstance != null){
                        try{
                            field.setAccessible(true);
                            field.set(beanInstance, dependencyInstance);
                            System.out.println("-> DI 성공: " + beanInstance.getClass().getSimpleName() +
                                    "." + field.getName() + "<--" + dependencyType.getSimpleName()
                                    );

                        }catch (IllegalAccessException e) {
                            throw new RuntimeException("의존성 주입 중 오류 발생 : " + e);
                        }
                    } else {
                        System.out.println("경고: " + dependencyType.getSimpleName() + " 타입의 의존성 빈을 찾을 수 없습니다.");
                    }
                }
            }
        }
    }

    private Object findBeanType(Class<?> dependencyType) {
        for (Object bean : beanMap.values()) {
            if(dependencyType.isInstance(bean)){
                return bean;
            }
        }
        return null;
    }

    public <T> T getBean(String name, Class<T> requiredType) {
        Object bean = beanMap.get(name);
        if(bean != null && requiredType.isInstance(bean)){
            return requiredType.cast(bean);
        }
        return null;
    }

    private void createBeans(Set<Class<?>> componentClasses) {
        for (Class<?> clazz : componentClasses) {
            try {
                Object instance = clazz.getDeclaredConstructor().newInstance();
                String beanName = clazz.getSimpleName().substring(0, 1).toLowerCase() + clazz.getSimpleName().substring(1);
                beanMap.put(beanName, instance);
                System.out.println(" -> 빈 생성 및 등록: " + beanName);
            } catch (Exception e) {
                throw new RuntimeException("빈 생성 중 오류 발생" + clazz.getName(), e);
            }
        }
    }

    private Set<Class<?>> scanComponents(String basePackage) {
        // 실제 스캐닝 로직 대신, 예시를 위해 대상 클래스를 명시적으로 로드
        try {
            Set<Class<?>> componentClasses = Set.of(
                    UserService.class,
                    UserRepository.class
            ).stream()
            .filter(cls -> cls.isAnnotationPresent(MyComponent.class))
            .collect(Collectors.toSet());

            System.out.println("스캔된 @MyComponent 클래스 : " + componentClasses.size());
            return componentClasses;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
