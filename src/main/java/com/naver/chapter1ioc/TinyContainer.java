package com.naver.chapter1ioc;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TinyContainer {
    // 맵: 빈 이름(String)을 키로, 생성된 객체(Object)를 값으로 저장
    private final Map<String, Object> beanMap = new HashMap<>();

    // 컨테이너 초기화 및 빈 등록
    public void initialize(String basePackage) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        System.out.println("--- TInyContainer 초기화 시작 ---");

        // 1. 지정된 패키지 경로에서 모든 클래스를 찾아옴
        Set<Class<?>> classes = findClasses(basePackage);

        for (Class<?> clazz : classes) {
            // 2. 클래스에 MyComponent 어노테이션이 붙어있는지 확인
            if(clazz.isAnnotationPresent(MyComponent.class)) {

                // 3. 어노테이션에서 빈 이름 가져오기
                MyComponent component = clazz.getAnnotation(MyComponent.class);
                String beanName = component.value().isEmpty()
                        ? clazz.getSimpleName() : component.value();

                // 4. 리플렉션(Reflection)을 사용하여 객체 인스턴스 생성
                Object instance = clazz.getDeclaredConstructor().newInstance();

                // 5. 컨테이너 맵에 저장 ( 빈 등록 완료 )
                beanMap.put(beanName, instance);
                System.out.println("등록 성공: " + beanName + "(" + clazz.getName() + ")");
            }
        }
        System.out.println("--- TinyContainer 초기화 완료 ---");
    }

    public Object getBean(String beanName) {
        return beanMap.get(beanName);
    }

    // (**복잡하므로 findClasses 메서드의 실제 구현 코드는 생략하고 개념만 설명합니다.***)
    // 실제 findClasses는 ClassLoader를 사용하여 .class 파일들을 찾아야 합니다.
    private Set<Class<?>> findClasses(String basePackage) {
        // 이 부분은 클래스 로더와 File I/O를 사용하여 구현해야 하며,
        // 지정된 패키지 경로 아래의 모든 .class 파일들을 재귀적으로 찾아 로드합니다.
        // 현재는 예시를 위해 임시로 등록할 클래스만 반환한다고 가정합니다
        try {
            return Set.of(Class.forName("com.naver.chapter1ioc.UserService"));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("클래스를 찾을 수 없습니다." + e);
        }
    }

}
