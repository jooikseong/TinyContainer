package com.naver.chatper11circylardependency;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class ServiceA {
    private final ServiceB serviceB;
    public ServiceA(ServiceB serviceB) {
        this.serviceB = serviceB;
    }
}

class ServiceB {
    private final ServiceA serviceA;
    public ServiceB(ServiceA serviceA) {
        this.serviceA = serviceA;
    }
}

class TinyContainer {
    private final Map<String, Object> beanMap = new HashMap<>();
    private final Set<Class<?>> inCreation = new HashSet<>();

    public <T> T getBean(Class<T> clazz) {
        String beanName = clazz.getSimpleName();

        // 이미 생성된 빈이 있다면 반환 (싱글톤)
        if (beanMap.containsKey(beanName)) {
            return clazz.cast(beanMap.get(beanName));
        }

        // 2. 순환 참조 탐지 핵심
        // 현재 생성 중인 목록에 자기 자신이 이미 있다면? -> 순환 고리 발견
        if (inCreation.contains(clazz)) {
            throw new RuntimeException("\n[Error] 순한 참조(Circular Dependency) 감지됨\n" + "상세 경로 " + inCreation + " ->" + clazz.getSimpleName());
        }

        // 생성 시작 표시
        inCreation.add(clazz);
        System.out.println("[Log] 빈 생성 시도 중 : " + beanName);

        try {
            // 해당 클래스의 유일한 생성자를 가져옴
            Constructor<?> constructor = clazz.getDeclaredConstructors()[0];
            Class<?>[] paramterTypes = constructor.getParameterTypes();
            Object[] args = new Object[paramterTypes.length];

            // 생성자에 필요한 인자들을 재귀적으로 getBean 호출 (의존성 해결)
            for (int i = 0; i < paramterTypes.length; i++) {
                args[i] = getBean(paramterTypes[i]);
            }

            // 모든 의존성이 해결되면 실제 인스턴스 생성
            Object instance = constructor.newInstance(args);
            beanMap.put(beanName, instance);
            return clazz.cast(instance);
        } catch (Exception e) {
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new RuntimeException(e);
        } finally {
            // 생성 완료 후 목록에서 제거
            inCreation.remove(clazz);
        }
    }
}

public class TinySpringCircularDetect {
    public static void main(String[] args) {
        TinyContainer tinyContainer = new TinyContainer();
        System.out.println("--- 순환 참조 탐지 테스트 시작 ---");
        try {
            // ServiceA를 요청하면 A 생성 시도 -> B 필요 -> B생성 시도 -> A 필요 (여기서 에러)
            tinyContainer.getBean(ServiceA.class);
        } catch (RuntimeException e) {
            System.out.println(e.getMessage());
        }
    }
}
