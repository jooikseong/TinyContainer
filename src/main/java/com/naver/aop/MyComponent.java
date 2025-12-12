package com.naver.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME) // 런타임 시점에도 어노테이션 정보 유지
@Target(ElementType.TYPE) // 클래스, 인터페이스 등에 붙일 수 있도록 지정
public @interface MyComponent {
    String value() default "";
}
