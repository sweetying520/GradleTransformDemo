package com.dream.gradletransformdemo;

import java.lang.reflect.Method;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;

/**
 * function: waiting for add
 *
 * @author zy
 * @since 2022/10/30
 */
public class TestThirdLibFix {

    public static void main(String[] args) {
        try {
            ClassPool classPool = ClassPool.getDefault();
            CtClass ctClass = classPool.get("com.dream.gradletransformdemo.MyUtils");
            CtMethod getLengthMethod = ctClass.getDeclaredMethod("getLength");
            getLengthMethod.insertBefore("System.out.println(\"Hello\");");
            getLengthMethod.insertBefore("{if($1==null)return 0;}");
            //getLengthMethod.insertBefore("{ System.out.println(\\$0);}");

            //获取类对象，接下来就可以愉快的使用反射了
            Class<?> clazz = ctClass.toClass();
            Object o = clazz.newInstance();
            //调用 personFly 方法
            Method getLengthMethod1 = clazz.getDeclaredMethod("getLength",String.class);
            System.out.println(getLengthMethod1.invoke(o,"123"));

        } catch (Exception e) {
            e.printStackTrace();
        }


        //System.out.println(MyStringUtils.getLength(null));
    }
}
