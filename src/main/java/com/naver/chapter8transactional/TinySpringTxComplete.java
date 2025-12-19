package com.naver.chapter8transactional;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface MyTransactional {}

// 가상의 데이터베이스 연결 및 상태 관리
class MockDatabase {
    private static boolean isTransactionActive = false;
    private static final List<String> dataLog = new ArrayList<>();

    public static void beginTransaction(){
        isTransactionActive = true;
        System.out.println("beginTransaction");
    }

    public static void commitTransaction(){
        if(isTransactionActive){
            System.out.println("commitTransaction");
            isTransactionActive = false;
        }
    }

    public static void rollbackTransaction(){
        if(isTransactionActive){
            System.out.println("rollbackTransaction");
            dataLog.clear();
            isTransactionActive = false;
        }
    }

    public static void saveData(String data){
        if(isTransactionActive){
            dataLog.add(data);
            System.out.println("[DB] 데이터 임시 저장: " + data);
        } else {
            System.out.println("[DB] 에러 : 트랜잭션 없이 저장 불가 !");
        }
    }
}

// AOP 트랜잭션 핸들러 핵심 로직
class TransactionInvocationHandler implements InvocationHandler {
    private final Object target;

    public TransactionInvocationHandler(Object target) {
        this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 실제 클래스의 메서드에서 @MyTransactional 확인
        Method targetMethod = target.getClass().getMethod(method.getName(), method.getParameterTypes());

        if(targetMethod.isAnnotationPresent(MyTransactional.class)){
            try {
                // 1. Before: 트랜잭션 시작
                MockDatabase.beginTransaction();

                // 2. Target: 실제 비즈니스 로직 실행
                Object result = method.invoke(target, args);

                // 3. After Returning: 성공 및 커밋
                MockDatabase.commitTransaction();
                return result;
            } catch (InvocationTargetException e) {
                MockDatabase.rollbackTransaction();
                throw e.getTargetException();
            }

        }

        // 어노테이션 없으면 그냥 실행
        return method.invoke(target, args);
    }
}

// 비즈니스 서비스 (인터페이스 필수 - JDK Proxy)
interface IOrderService {
    void placeOrder(String item, boolean makeError);
}

class OrderService implements IOrderService {
    @Override
    @MyTransactional
    public void placeOrder(String item, boolean makeError) {
        System.out.println("[Service] 주문 로직 실행 중...");

        MockDatabase.saveData("주문시 생성: " + item);
        MockDatabase.saveData("재고 차감 " + item);

        if(makeError){
            System.out.println("[Service] 아차! 예상치 못한 에러 발생!");
            throw new RuntimeException("결제 서버 장애");
        }

        MockDatabase.saveData("배송 요청: " + item);
    }
}


public class TinySpringTxComplete {
    public static void main(String[] args) {
        // 1. 실제 객체 생성
        IOrderService orderService = new OrderService();

        // 2. 트랜잭션 프록시 생성 (컨테이너가 해주는 작업)
        IOrderService proxyService = (IOrderService) Proxy.newProxyInstance(
                IOrderService.class.getClassLoader(),
                new Class[]{IOrderService.class},
                new TransactionInvocationHandler(orderService)
        );
        System.out.println("--- 시나리오 1 : 정상 주문 (Commit 예상) ---");
        try{
            proxyService.placeOrder("노트북", false);
        }catch (Exception e){
            System.out.println("메인에서 잡은 예외: " + e.getMessage());
        }

        System.out.println("\n--- 시나리오 2: 주문 중 에러 발생 (Rollback 예상) ---");
        try {
            proxyService.placeOrder("스마트폰", true);
        }catch (Exception e){
            System.out.println("Main 최종 예외 처리: " + e.getMessage());
        }

    }
}
