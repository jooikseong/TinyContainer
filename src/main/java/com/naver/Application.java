package com.naver;

// IOC 컨테이너의 핵심 기능을 정의하는 인터페이스 예시
public interface Application {
    // 빈 ID로 객체를 얻어오는 메서드
    Object getBean(String name);
    // 빈 타입으로 객체를 얻어오는 메서드 (타입 안정성 확보)
    <T> T getBean(Class<T> type);
    // 컨테이너를 초기화하고 모든 빈을 등록/생성하는 메서드
    void initialize();
}
