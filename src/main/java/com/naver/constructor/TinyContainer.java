package com.naver.constructor;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.stream.Collectors;

public class TinyContainer {

    private final Map<String, Object> beanMap = new HashMap<>();

    public TinyContainer(String basePackage) {
        System.out.println("--- TinyContainer 초기화 시작 ---");

        // 1. 클래스 스캔 (Ioc)
        Set<Class<?>> componentClasses = scanComponents(basePackage);

        // 2. 생성자 기반으로 빈 생성 (DI 로직 포함)
        createBeansWithConstructorDI(componentClasses);

        System.out.println("--- TinyContainer 초기화 완료 (" + beanMap.size() + ")---");
    }

    private void createBeansWithConstructorDI(Set<Class<?>> componentClasses) {
        // 아직 생성되지 않은 빈 목록을 관리
        Set<Class<?>> remainingClasses = new java.util.HashSet<>(componentClasses);
        int prevSize = remainingClasses.size();

        while (!remainingClasses.isEmpty()) {
            boolean beanCreatedInThisIteration = false;

            // 아직 생성되지 않은 클래스들을 순회
            for(Class<?> clazz : new ArrayList<>(remainingClasses)) {

                // 1. 사용할 생성자를 찾습니다. (@MyAutowired가 붙은 생성자 또는 기본 생성자)
                Constructor<?> constructorToUse = getConstructorToUse(clazz);

                if(constructorToUse == null) {
                    System.out.println("오류: " + clazz.getSimpleName() + "에서 사용할 생성자를 찾을 수 없습니다.");
                    continue;
                }

                // 2. 생성자에 필요한 의존성 파라미터를 해결합니다.
                List<Object> dependancyArgs = resolveDendencies(constructorToUse.getParameterTypes());

                // 3. 모든 의존성이 해결되었는지 확인
                if(dependancyArgs != null) {
                    try {
                        // 4. 생성자를 호출하여 인스턴스 생성 (DI 실행)
                        Object instance = constructorToUse.newInstance(dependancyArgs.toArray());

                        // 5. 컨테이너에 등록
                        String beanName = clazz.getSimpleName().substring(0, 1).toLowerCase() + clazz.getSimpleName().substring(1);
                        beanMap.put(beanName, instance);
                        System.out.println("-> 빈 생성 및 등록 완료 (DI): " + beanName);

                        remainingClasses.remove(clazz);
                        beanCreatedInThisIteration = true;
                    } catch (Exception e) {
                        throw new RuntimeException("생성자 주입 중 오류 발생 " + e);
                    }
                }
            }
            // 무한 루프 방지 ( 의존성 순환 참조 발생 시 )
            if(!beanCreatedInThisIteration && remainingClasses.size() == prevSize) {
                System.out.println("오류: 일부 빈이 생성되지 못했습니다. 순환 참조가 발생했거나 의존성빈을 찾을 수 없습니다: " + remainingClasses);
                break;
            }
            prevSize = remainingClasses.size();
        }
    }

    private Constructor<?> getConstructorToUse(Class<?> clazz) {
        // @MyAutowired가 붙은 생성자를 찾습니다.
        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            if (constructor.isAnnotationPresent(MyAutowired.class)) {
                return constructor;
            }
        }
        // 없으면 기본 생성자를 찾습니다.
        try {
            return clazz.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    // 주어진 타입 배열에 해당하는 빈들을 컨테이너에서 찾아 리스트로 반환합니다.
    private List<Object> resolveDendencies(Class<?>[] parameterTypes) {
        List<Object> resolvedDependencies = new ArrayList<>();

        for(Class<?> paramType : parameterTypes) {
            Object dependency = findBeanByType(paramType);

            // 의존성 빈이 아직 컨테이너에 없으면 null 반환 (다음 순서에 다시 시도해야 함)
            if (dependency == null) {
                return null;
            }
            resolvedDependencies.add(dependency);
        }
        return resolvedDependencies;
    }


    private Object findBeanByType(Class<?> paramType) {
        for(Object bean : beanMap.values()) {
            if(paramType.isInstance((bean))){
                return bean;
            }
        }
        return null;
    }

    private Set<Class<?>> scanComponents(String basePackage) {
        // 예시를 위해 대상 클래스 직접 지정
        return new java.util.HashSet<>(List.of(UserService.class, UserRepository.class))
                .stream()
                .filter(cls -> cls.isAnnotationPresent(MyComponent.class))
                .collect(Collectors.toSet());
    }

    public UserService getBean(String name, Class<UserService> requireType) {
        Object bean = beanMap.get(name);
        if(bean != null && requireType.isInstance(bean)) {
            return requireType.cast(bean);
        }
        return null;
    }
}

