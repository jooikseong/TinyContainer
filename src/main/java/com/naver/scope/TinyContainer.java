package com.naver.scope;


import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

class BeanDefinition {
    Class<?> beanClass;
    String scope;

    public BeanDefinition(Class<?> beanClass) {
        this.beanClass = beanClass;
        if(beanClass.isAnnotationPresent(MyScope.class)){
            this.scope = beanClass.getAnnotation(MyScope.class).value();
        } else {
            this.scope = "singleton";
        }
    }
    public Class<?> getBeanClass() { return beanClass; }
    public String getScope() { return scope; }
}
class TinyContainer {

    // 1. Singleton 인스턴스를 저장한느 맵 (Singleton Cache)
    private final Map<String, Object> singletonBeanMap = new HashMap<>();
    // 2. 모든 빈의정의(클래스, 스코프 등)를 저장하는 맵
    private final  Map<String, BeanDefinition> beanDefinitionMap = new HashMap<>();

    public TinyContainer(String baseaPackge) {
        System.out.println("--- TinyContainer 초기화 시작 ---");

        Set<Class<?>> componentClasses = scanComponents(baseaPackge);

        // 빈 정의를 먼저 생성하고 맵에 저장
        componentClasses.forEach(clazz -> {
            String name = getBeanName(clazz);
            beanDefinitionMap.put(name, new BeanDefinition(clazz));
        });

        // 싱글톤 빈만 먼저 생성 (프로토타입은 getBean 호출 시 생성)
        createSingletonBeans();

        System.out.println("--- TinyContainer 초기화 완료 (" + singletonBeanMap.size() + ")---");
    }

    private void createSingletonBeans() {
        // 싱글톤 스코프를 가진 빈만 초기화 과정에서 생성 및 주입합니다.
        Set<Class<?>> singletonClasses = beanDefinitionMap.values().stream()
                .filter(def -> def.getScope().equals("singleton"))
                .map(BeanDefinition::getBeanClass)
                .collect(Collectors.toSet());

        // 기존의 생성자 DI 로직을 이용하여 싱글톤 빈 생성 및 주입
        createBeansWithConstructorDI(singletonClasses, singletonBeanMap);
    }

    private void createBeansWithConstructorDI(Set<Class<?>> componentClasses, Map<String, Object> targetMap) {
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
                List<Object> dependancyArgs = resolveDependencies(constructorToUse.getParameterTypes());

                // 3. 모든 의존성이 해결되었는지 확인
                if(dependancyArgs != null) {
                    try {
                        // 4. 생성자를 호출하여 인스턴스 생성 (DI 실행)
                        Object instance = constructorToUse.newInstance(dependancyArgs.toArray());

                        // 5. 컨테이너에 등록
                        String beanName = clazz.getSimpleName().substring(0, 1).toLowerCase() + clazz.getSimpleName().substring(1);
                        targetMap.put(beanName, instance);
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

    public  <T> T getBean(String name, Class<T> requiredType) {
        BeanDefinition beanDefinition = beanDefinitionMap.get(name);
        if(beanDefinition == null){
            return null;
        }

        if(beanDefinition.getScope().equals("singleton")){
            // Singleton: 캐시 맵에서 가져와 반환 (이미 생성되어 있음)
            Object bean = singletonBeanMap.get(name);
            if(bean != null && requiredType.isInstance(bean)){
                return requiredType.cast(bean);
            }
        } else if(beanDefinition.getScope().equals("prototype")){
            // Prototype: 요청 시마다 새로 생성
            try {
                // 새로 생성된 객체를 임시 맵에 저장하여 DI 로직에 전달
                Map<String, Object> tempMap = new HashMap<>();
                createBeanInstance(beanDefinition.getBeanClass(), tempMap);
                Object newBean = tempMap.get(name);

                if(newBean != null && requiredType.isInstance(newBean)){
                    System.out.println(" -> Prototype 빈 새로 생성: " + name);
                    return requiredType.cast(newBean);
                }
            } catch (Exception e){
                System.out.println("Prototype 빈 생성 중 오류 발생: " + name);
            }
        }
        return null;
    }

    private void createBeanInstance(Class<?> clazz, Map<String, Object> tempMap) throws InvocationTargetException, InstantiationException, IllegalAccessException {
        Constructor<?> constuctorToUse = getConstructorToUse(clazz);
        List<Object> dependencyArgs = resolveDependencies(constuctorToUse.getParameterTypes());

        if(dependencyArgs != null){} {
            Object instance = constuctorToUse.newInstance(dependencyArgs.toArray());
            String beanName = getBeanName(clazz);
            tempMap.put(beanName, instance);
        }
    }

    private Object findBeanByType(Class<?> type) {
        // 1. 싱글톤을 먼저 찾습니다. (DI를 위한 주요 소스)
        for (Object bean : singletonBeanMap.values()) {
            if(type.isInstance(bean)){
                return bean;
            }
        }
        // 2. 만약 프로토타입 빈을 DI 해야 한다면, 이 단계에서 복잡한 로직(프록시)이 필요하지만,
        // Tiny Spring에서는 프로토타입은 싱글톤에 주입될 수 없다고 가정합니다.
        return null;
    }

    private String getBeanName(Class<?> clazz) {
        return clazz.getSimpleName().substring(0, 1).toLowerCase() + clazz.getSimpleName().substring(1);
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
    private List<Object> resolveDependencies(Class<?>[] parameterTypes) {
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

    private Set<Class<?>> scanComponents(String basePackage) {
        // 예시를 위해 대상 클래스 직접 지정
        return new java.util.HashSet<>(List.of(UserService.class, UserRepository.class))
                .stream()
                .filter(cls -> cls.isAnnotationPresent(MyComponent.class))
                .collect(Collectors.toSet());
    }

}
