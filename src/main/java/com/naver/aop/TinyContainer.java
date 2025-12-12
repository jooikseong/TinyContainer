package com.naver.aop;

import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.stream.Collectors;

public class TinyContainer {
    private final Map<String, Object> singletonBeanMap = new HashMap<>();
    private final Map<String, BeanDefinition> beanDefinitionMap = new HashMap<>();

    public TinyContainer() {
        System.out.println("TinyContainer constructor called");

        Set<Class<?>> componentClasses = scanComponents();
        componentClasses.forEach(clazz -> {
            String name = getBeanName(clazz);
            beanDefinitionMap.put(name, new BeanDefinition(clazz));
        });

        createSingletonBeans();

        System.out.println("TinyContainer constructor created ( " + singletonBeanMap.size() + " / " + beanDefinitionMap.size() + " beans");
    }

    private Set<Class<?>> scanComponents() {
        return Set.of(UserService.class, UserRepository.class).stream()
                .filter(cls -> cls.isAnnotationPresent(MyComponent.class))
                .collect(Collectors.toSet());
    }

    private void createSingletonBeans() {
        Set<Class<?>> singletonClasses = beanDefinitionMap.values().stream()
                .filter(def -> def.getScope().equals("singleton"))
                .map(BeanDefinition::getBeanClass)
                .collect(Collectors.toSet());
        createBeansWithConstructorDI(singletonClasses, singletonBeanMap);
    }

    // IOC/DI/AOP 핵심: 의존성 해결 -> 인스턴스 생성 -> AOP 프록시 생성 -> 등록
    private void createBeansWithConstructorDI(Set<Class<?>> componentClasses, Map<String, Object> targetMap) {
        Set<Class<?>> remainingClasses =new HashSet<>(componentClasses);
        int prevSize = remainingClasses.size();

        while (!remainingClasses.isEmpty()) {
            boolean beanCreatedInThisInteration = false;

            for (Class<?> clazz : new ArrayList<>(remainingClasses)) {
                try {
                    Constructor<?> constructorToUse = getConstructorToUse(clazz);
                    // 1. 의존성 파마리터 해결
                    List<Object> dependencyArgs = resolveDependencies(constructorToUse.getParameterTypes());

                    if(dependencyArgs != null){
                        // 2. 실제 타겟 객체 생성 및 DI 실행
                        Object instance = constructorToUse.newInstance(dependencyArgs.toArray());

                        // 3. AOP 적용 여부 확인 및 포록시 생성 (AOP 핵심)
                        Object finalInstance = instance;
                        if(instance.getClass().isAnnotationPresent(MyEnableAop.class)){
                            // JDK Dynamic Proxy 생성
                            finalInstance = Proxy.newProxyInstance(
                                    instance.getClass().getClassLoader(),
                                    instance.getClass().getInterfaces(),
                                    new LoggingInvocationHandler(instance
                            ));
                            System.out.println(" -> AOP 프록시 생성 완료 : " + getBeanName(clazz));
                        }

                        // 4. 최종 인스턴스(프록시 또는 실제 객체)를 맵에 등록
                        String beanName = getBeanName(clazz);
                        targetMap.put(beanName, finalInstance);

                        remainingClasses.remove(clazz);
                        beanCreatedInThisInteration = true;
                        System.out.println(" -> 빈 등록 완료 : " + beanName);
                    }
                } catch (Exception e){
                    System.out.println("빈 생성,DIO 및 AOP 중 오류 발생: " + clazz.getName() + e);
                }
            }

            if(!beanCreatedInThisInteration && remainingClasses.size() == prevSize){
                System.err.println("오류: 일부 빈이 생성되지 못했습니다. 순환 참조가 발생했거나 의존성 빈을 찾을 수 없습니다: " + remainingClasses);
                break;
            }
            prevSize = remainingClasses.size();
        }
    }

    /** * 스코프 처리 핵심 메서드
     */
    public <T> T getBean(String name, Class<T> requiredType) {
        BeanDefinition definition = beanDefinitionMap.get(name);
        if (definition == null) return null;

        if (definition.getScope().equals("singleton")) {
            Object bean = singletonBeanMap.get(name);
            if (bean != null && requiredType.isInstance(bean)) {
                return requiredType.cast(bean);
            }
        } else if (definition.getScope().equals("prototype")) {
            try {
                Map<String, Object> tempMap = new HashMap<>();
                // Prototype 인스턴스 1개만 생성 및 DI, AOP 수행
                createBeansWithConstructorDI(Set.of(definition.getBeanClass()), tempMap);
                Object newBean = tempMap.get(name);

                if (newBean != null && requiredType.isInstance(newBean)) {
                    System.out.println(" -> Prototype 빈 새로 생성: " + name);
                    return requiredType.cast(newBean);
                }
            } catch (Exception e) {
                System.err.println("Prototype 빈 생성 중 오류 발생: " + name);
            }
        }

        return null;
    }

    private List<Object> resolveDependencies(Class<?>[] parameterTypes) {
        List<Object> resolvedDependencies = new ArrayList<>();

        for (Class<?> paramType : parameterTypes) {
            Object dependency = findBeanByType(paramType);

            if (dependency == null) {
                return null;
            }
            resolvedDependencies.add(dependency);
        }
        return resolvedDependencies;
    }

    private Object findBeanByType(Class<?> type) {
        for (Object bean : singletonBeanMap.values()) {
            if (type.isInstance(bean)) {
                return bean;
            }
        }
        return null;
    }

    private Constructor<?> getConstructorToUse(Class<?> clazz) throws NoSuchMethodException {
        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            if (constructor.isAnnotationPresent(MyAutowired.class)) {
                return constructor;
            }
        }
        return clazz.getDeclaredConstructor();
    }

    private String getBeanName(Class<?> clazz) {
        return clazz.getSimpleName().substring(0, 1).toLowerCase() + clazz.getSimpleName().substring(1);
    }
}
