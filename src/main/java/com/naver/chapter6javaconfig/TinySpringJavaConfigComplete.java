package com.naver.chapter6javaconfig;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// ===============================================
// 1. 어노테이션 정의
// ===============================================

/** 빈으로 등록될 클래스에 붙이는 어노테이션 */
@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE)
@interface MyComponent {}

/** 의존성 주입이 필요한 생성자에 붙이는 어노테이션 */
@Retention(RetentionPolicy.RUNTIME) @Target({ElementType.CONSTRUCTOR, ElementType.FIELD})
@interface MyAutowired {}

/** 빈의 스코프를 지정하는 어노테이션 */
@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE)
@interface MyScope { String value() default "singleton"; }

// AOP 관련 어노테이션
/** 이 빈에 대해 프록시(AOP)를 적용합니다. */
@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE)
@interface MyEnableAop {}

/** 이 메서드에 부가 기능(로깅)을 적용합니다. */
@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD)
@interface MyLogging {}

// Java Configuration 관련 어노테이션
/** 설정 정보를 담고 있는 클래스에 붙입니다. */
@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE)
@interface MyConfiguration {}

/** 설정 클래스에서 빈을 정의하는 메서드에 붙입니다. */
@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD)
@interface MyBean {}


// ===============================================
// 2. AOP 핸들러 구현 (부가 기능 로직)
// ===============================================

class LoggingInvocationHandler implements InvocationHandler {

    private final Object target;

    public LoggingInvocationHandler(Object target) {
        this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        // 프록시 객체의 메서드가 아닌, 실제 타겟 클래스의 메서드를 찾아 어노테이션 확인
        Method targetMethod = target.getClass().getMethod(method.getName(), method.getParameterTypes());

        if (targetMethod.isAnnotationPresent(MyLogging.class)) {
            // Before Advice
            System.out.println("\n[AOP Log] >>> 메소드 호출 시작: " + method.getName() + " with args: " + Arrays.toString(args));

            try {
                // 실제 메서드 실행
                Object result = method.invoke(target, args);

                // After Returning Advice
                System.out.println("[AOP Log] <<< 메소드 호출 완료: " + method.getName());
                return result;
            } catch (Exception e) {
                // After Throwing Advice
                System.err.println("[AOP Log] !!! 메소드 실행 중 예외 발생: " + e.getCause().getMessage());
                throw e.getCause();
            }
        } else {
            // AOP 비적용 메서드
            return method.invoke(target, args);
        }
    }
}


// ===============================================
// 3. 서비스/리포지토리 인터페이스 및 클래스 정의
// ===============================================

// UserRepository
interface IUserRepository {
    String findUser(String id);
    long getCreationTime();
}

@MyComponent @MyScope("singleton")
class UserRepository implements IUserRepository {
    private final long creationTime = System.currentTimeMillis();

    @Override
    public long getCreationTime() { return creationTime; }

    @Override
    public String findUser(String id) {
        return "User with ID: " + id + " found. Repository Time: " + creationTime;
    }
}

// UserService
interface IUserService {
    void displayUserInfo(String id);
    long getInstanceId();
    IUserRepository getUserRepository();
}

@MyComponent
@MyScope("prototype")
@MyEnableAop // AOP 적용 대상
class UserService implements IUserService {

    private final IUserRepository userRepository;
    private final long instanceId = (long) (Math.random() * 10000);

    public IUserRepository getUserRepository() {
        return this.userRepository;
    }

    @MyAutowired
    public UserService(IUserRepository userRepository) {
        this.userRepository = userRepository;
        System.out.println("-> UserService 생성자 실행. Instance ID: " + instanceId);
    }

    @Override
    @MyLogging // AOP 적용 대상 메서드
    public void displayUserInfo(String id) {
        String result = this.userRepository.findUser(id);
        System.out.println("[ID " + instanceId + "] UserService result: " + result);
    }

    @Override
    public long getInstanceId() {
        return instanceId;
    }

    // AOP가 적용되지 않는 일반 메서드
    public void printId() {
        System.out.println("No AOP here. ID: " + instanceId);
    }
}


// Java Config를 통해 등록될 새로운 서비스
interface IGreetingService {
    String greet();
    IUserRepository getRepository();
}

class GreetingService implements IGreetingService {
    private final IUserRepository repository;

    public GreetingService(IUserRepository repository) {
        this.repository = repository;
    }

    @Override
    public String greet() {
        return "Hello from Java Configured Bean! Repository Time: " + repository.getCreationTime();
    }

    @Override
    public IUserRepository getRepository() {
        return repository;
    }
}


// ===============================================
// 4. Java Configuration 클래스 정의
// ===============================================

@MyConfiguration
class AppConfig {

    /** * @MyBean 메서드의 파라미터(IUserRepository)를 컨테이너에서 찾아 DI를 수행합니다.
     * 메서드 이름(greetingService)이 빈 이름이 됩니다.
     */
    @MyBean
    public IGreetingService greetingService(IUserRepository userRepository) {
        System.out.println("-> @MyBean 메서드 실행: GreetingService 생성 (UserRepository 의존성 주입 확인)");
        return new GreetingService(userRepository);
    }
}


// ===============================================
// 5. 빈 정의 정보 클래스
// ===============================================

class BeanDefinition {
    Class<?> beanClass;
    String scope;

    public BeanDefinition(Class<?> beanClass) {
        this.beanClass = beanClass;
        // @MyBean으로 등록된 빈은 어노테이션이 없으므로 기본값(singleton)이 적용됨
        if (beanClass.isAnnotationPresent(MyScope.class)) {
            this.scope = beanClass.getAnnotation(MyScope.class).value();
        } else {
            this.scope = "singleton";
        }
    }
    public Class<?> getBeanClass() { return beanClass; }
    public String getScope() { return scope; }
}


// ===============================================
// 6. 핵심 IoC/DI/AOP/Java Config 컨테이너 구현 (TinyContainer)
// ===============================================

class TinyContainer {

    private final Map<String, Object> singletonBeanMap = new HashMap<>();
    private final Map<String, BeanDefinition> beanDefinitionMap = new HashMap<>();

    public TinyContainer() {
        System.out.println("--- TinyContainer 초기화 시작 ---");

        // 1. 모든 Component 및 Configuration 클래스 정보 수집
        Set<Class<?>> componentClasses = scanComponents();
        componentClasses.forEach(clazz -> {
            String name = getBeanName(clazz);
            beanDefinitionMap.put(name, new BeanDefinition(clazz));
        });

        // 2. @MyComponent 싱글톤 빈 생성 및 주입 (@MyBean 의존성으로 사용됨)
        createSingletonBeans();

        // 3. Java Configuration (@MyBean) 빈 생성 및 등록 (새로운 기능)
        processJavaConfig();

        System.out.println("--- TinyContainer 초기화 완료 (" + singletonBeanMap.size() + "/" + beanDefinitionMap.size() + "개 빈 등록) ---");
    }

    private void createSingletonBeans() {
        Set<Class<?>> componentSingletonClasses = beanDefinitionMap.values().stream()
                .filter(def -> def.getScope().equals("singleton"))
                .map(BeanDefinition::getBeanClass)
                // AppConfig도 BeanDefinitionMap에 있지만, Component가 아니므로 제외하고 별도로 처리
                .filter(cls -> cls.isAnnotationPresent(MyComponent.class))
                .collect(Collectors.toSet());

        createBeansWithConstructorDI(componentSingletonClasses, singletonBeanMap);
    }

    /**
     * Java Configuration 클래스를 처리하여 @MyBean 메서드를 통해 빈을 생성하고 등록합니다.
     */
    private void processJavaConfig() {
        List<Class<?>> configClasses = beanDefinitionMap.values().stream()
                .map(BeanDefinition::getBeanClass)
                .filter(cls -> cls.isAnnotationPresent(MyConfiguration.class))
                .collect(Collectors.toList());

        for (Class<?> configClass : configClasses) {
            try {
                // 설정 클래스 인스턴스 생성 (별도의 싱글톤 관리는 하지 않고 바로 생성)
                Object configInstance = configClass.getDeclaredConstructor().newInstance();

                for (Method method : configClass.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(MyBean.class)) {
                        // @MyBean 메서드의 의존성(파라미터)을 해결
                        Class<?>[] parameterTypes = method.getParameterTypes();
                        List<Object> dependencyArgs = resolveDependencies(parameterTypes);

                        if (dependencyArgs != null) {
                            // @MyBean 메서드 호출하여 빈 인스턴스 얻음 (DI 실행)
                            Object beanInstance = method.invoke(configInstance, dependencyArgs.toArray());

                            // 빈 등록 (메서드 이름을 빈 이름으로 사용, 싱글톤)
                            String beanName = method.getName();
                            beanDefinitionMap.put(beanName, new BeanDefinition(beanInstance.getClass()));
                            singletonBeanMap.put(beanName, beanInstance);
                            System.out.println(" -> @MyBean 빈 등록 완료: " + beanName);
                        } else {
                            // 이 Tiny Spring은 DI 실패 시 오류를 발생시키지 않고 건너뜁니다.
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Java Config 처리 중 오류 발생: " + configClass.getName(), e);
            }
        }
    }

    /** * IoC/DI/AOP 핵심: 의존성 해결 -> 인스턴스 생성 -> AOP 프록시 생성 -> 등록
     */
    private void createBeansWithConstructorDI(Set<Class<?>> componentClasses, Map<String, Object> targetMap) {
        Set<Class<?>> remainingClasses = new HashSet<>(componentClasses);
        int prevSize = remainingClasses.size();

        while (!remainingClasses.isEmpty()) {
            boolean beanCreatedInThisIteration = false;

            for (Class<?> clazz : new ArrayList<>(remainingClasses)) {
                try {
                    Constructor<?> constructorToUse = getConstructorToUse(clazz);
                    List<Object> dependencyArgs = resolveDependencies(constructorToUse.getParameterTypes());

                    if (dependencyArgs != null) {
                        // 1. 실제 타겟 객체 생성 및 DI 실행
                        Object instance = constructorToUse.newInstance(dependencyArgs.toArray());

                        // 2. AOP 적용 여부 확인 및 프록시 생성
                        Object finalInstance = instance;
                        if (instance.getClass().isAnnotationPresent(MyEnableAop.class)) {
                            finalInstance = Proxy.newProxyInstance(
                                    instance.getClass().getClassLoader(),
                                    instance.getClass().getInterfaces(),
                                    new LoggingInvocationHandler(instance)
                            );
                            System.out.println(" -> AOP 프록시 생성 완료: " + getBeanName(clazz));
                        }

                        // 3. 최종 인스턴스(프록시 또는 실제 객체)를 맵에 등록
                        String beanName = getBeanName(clazz);
                        targetMap.put(beanName, finalInstance);

                        remainingClasses.remove(clazz);
                        beanCreatedInThisIteration = true;
                        System.out.println(" -> @MyComponent 빈 등록 완료: " + beanName);
                    }
                } catch (Exception e) {
                    System.err.println("빈 생성, DI 및 AOP 중 오류 발생: " + clazz.getName() + " - " + e);
                }
            }

            if (!beanCreatedInThisIteration && remainingClasses.size() == prevSize) {
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

    // ------------------------------------------
    // 보조 메서드 (Helper Methods)
    // ------------------------------------------

    private Constructor<?> getConstructorToUse(Class<?> clazz) throws NoSuchMethodException {
        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            if (constructor.isAnnotationPresent(MyAutowired.class)) {
                return constructor;
            }
        }
        return clazz.getDeclaredConstructor();
    }

    private List<Object> resolveDependencies(Class<?>[] parameterTypes) {
        List<Object> resolvedDependencies = new ArrayList<>();

        for (Class<?> paramType : parameterTypes) {
            Object dependency = findBeanByType(paramType);

            if (dependency == null) {
                return null; // 의존성 해결 실패
            }
            resolvedDependencies.add(dependency);
        }
        return resolvedDependencies;
    }

    private Object findBeanByType(Class<?> type) {
        // 현재는 싱글톤 맵에서만 의존성을 찾습니다.
        for (Object bean : singletonBeanMap.values()) {
            if (type.isInstance(bean)) {
                return bean;
            }
        }
        return null;
    }

    // 예시를 위한 수동 클래스 스캔 (Component + Configuration 포함)
    private Set<Class<?>> scanComponents() {
        return Set.of(UserService.class, UserRepository.class, AppConfig.class).stream()
                .filter(cls -> cls.isAnnotationPresent(MyComponent.class) || cls.isAnnotationPresent(MyConfiguration.class))
                .collect(Collectors.toSet());
    }

    private String getBeanName(Class<?> clazz) {
        return clazz.getSimpleName().substring(0, 1).toLowerCase() + clazz.getSimpleName().substring(1);
    }
}


// ===============================================
// 7. 메인 애플리케이션
// ===============================================

public class TinySpringJavaConfigComplete {
    public static void main(String[] args) {

        // 1. 컨테이너 초기화: Component 싱글톤 생성 -> Java Config 빈 생성
        TinyContainer container = new TinyContainer();

        System.out.println("\n--- Java Configuration 빈 테스트 ---");

        // 2. @MyBean으로 등록된 IGreetingService 빈 요청
        IGreetingService greetingService = container.getBean("greetingService", IGreetingService.class);
        System.out.println("GreetingService 호출 결과: " + greetingService.greet());

        // 3. GreetingService가 UserRepository (싱글톤)을 주입받았는지 확인
        IUserRepository userRepositoryFromConfig = greetingService.getRepository();

        System.out.println("\n--- @MyComponent (AOP) 빈 테스트 ---");

        // 4. AOP 적용된 UserService (Prototype) 요청
        IUserService userService = container.getBean("userService", IUserService.class);

        // 5. AOP 메서드 호출 (로깅 출력 예상)
        System.out.println("\n[Test] userService.displayUserInfo() 호출");
        userService.displayUserInfo("C");

        // 6. 모든 빈이 동일한 UserRepository 싱글톤을 공유하는지 확인
        System.out.println("\n--- 최종 싱글톤 공유 테스트 ---");
        IUserRepository userRepositoryFromComponent = userService.getUserRepository(); // DI된 UserRepository 접근

        System.out.println("Config 빈의 Repository 생성 시간: " + userRepositoryFromConfig.getCreationTime());
        System.out.println("Component 빈의 Repository 생성 시간: " + userRepositoryFromComponent.getCreationTime());
        System.out.println("두 Repository 인스턴스는 같습니다 (Singleton 공유): " +
                (userRepositoryFromConfig.getCreationTime() == userRepositoryFromComponent.getCreationTime()));
    }
}