package com.naver.aop;

class BeanDefinition {
    Class<?> beanClass;
    String scope;

    public BeanDefinition(Class<?> beanClass) {
        this.beanClass = beanClass;
        if(beanClass.isAnnotationPresent(com.naver.scope.MyScope.class)){
            this.scope = beanClass.getAnnotation(MyScope.class).value();
        } else {
            this.scope = "singleton";
        }
    }
    public Class<?> getBeanClass() { return beanClass; }
    public String getScope() { return scope; }
}
