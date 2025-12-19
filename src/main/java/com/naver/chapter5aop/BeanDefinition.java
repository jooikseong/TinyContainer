package com.naver.chapter5aop;

class BeanDefinition {
    Class<?> beanClass;
    String scope;

    public BeanDefinition(Class<?> beanClass) {
        this.beanClass = beanClass;
        if(beanClass.isAnnotationPresent(com.naver.chapter4scope.MyScope.class)){
            this.scope = beanClass.getAnnotation(MyScope.class).value();
        } else {
            this.scope = "singleton";
        }
    }
    public Class<?> getBeanClass() { return beanClass; }
    public String getScope() { return scope; }
}
