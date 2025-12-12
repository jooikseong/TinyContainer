package com.naver.aop;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;

public class LoggingInvocationHandler implements InvocationHandler {

    private  final Object target; // 실제 빈*UserService) 인스턴스

    public LoggingInvocationHandler(Object target) {
        this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        // 1. 실제 타겟 클래스에서 해당 메서드를 찾음 (어노테이션 확인용)
        // Proxy 객체의 메서드가 아닌, 실제 타겟 클래스의 메서드를 찾아야 어노테이션 확인
        Method targetMethod = target.getClass().getMethod(method.getName(), method.getParameterTypes());

        // 2. 메서드에 @MyLogging 어노테이션이 붙어 있는지 확인
        if(targetMethod.isAnnotationPresent(MyLoging.class)) {
            // --- Before Advice (메서드 실행 전 로깅) ---
            System.out.println("\n[AOP Log] >> 메소드 호출 시작: " + method.getName() + " with args: " + Arrays.toString(args));

            try {
                // 3. 실제 타겟 객체의 메서드 실행
                Object result = method.invoke(target, args);

                System.out.println("[AOP Log << 메소드 호출 완료 : " + method.getName() + " with args: " + Arrays.toString(args) );
                return result;
            } catch (Exception e){
                System.out.println("[AOP Log] !!! 메소드 실행 중 예외 발생 : " + e.getCause().getMessage());
                throw e.getCause();
            }
        }else {
            // @MyLOgging이 없으면 부가 기능 없이 실제 메서드 실행
            return method.invoke(target, args);
        }
    }

}
