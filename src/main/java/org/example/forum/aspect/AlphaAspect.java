package org.example.forum.aspect;

//@Component
//@Aspect
//public class AlphaAspect {
//
//    @Pointcut("execution(* org.example.forum.service.*.*(..))") //第一个*代表所有的返回值，后面意思：所有的service的所有组件的所有方法的所有的参数
//    public void pointCut(){
//
//    }
//
//    @Before("pointCut()") //在连接点的开始打印
//    public void before(){
//        System.out.println("before");
//    }
//
//    @After("pointCut()") //在连接点以后
//    public void after(){
//        System.out.println("after");
//    }
//
//    @AfterReturning("pointCut()") //在返回值之后
//    public void afterReturning(){
//        System.out.println("afterReturning");
//    }
//
//    @AfterThrowing("pointCut()") //在跑出异常之后
//    public void afterThrowing(){
//        System.out.println("afterThrowing");
//    }
//
//    @Around("pointCut()") //在连接点前后都打
//    public Object around(ProceedingJoinPoint joinPoint) throws Throwable{
//        System.out.println("around before");
//        Object obj = joinPoint.proceed(); //调用原始对象的业务逻辑处理方法
//        System.out.println("around after");
//        return obj;
//    }
//
//
//}
