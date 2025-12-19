package com.naver.chapter5aop;

// ===============================================
// 3. 서비스/리포지토리 인터페이스 및 클래스 정의
// (JDK Proxy를 위해 인터페이스가 필수!)
// ===
interface IUserRepository {
    String findUser(String id);
    long getCreationTime();
}
