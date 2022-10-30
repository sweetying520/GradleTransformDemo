package com.dream.gradletransformdemo;

import java.lang.reflect.Method;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.Modifier;

/**
 * function: Javassist 修改测试类
 *
 * @author zy
 * @since 2022/10/30
 */
public class TestUpdatePersonService {

    public static void main(String[] args) {
        try {
            ClassPool pool = ClassPool.getDefault();
            CtClass ctClass = pool.get("com.dream.gradletransformdemo.PersonService");

            CtMethod personFly = ctClass.getDeclaredMethod("personFly");
            //在 personFly 方法的前后插入代码
            personFly.insertBefore("System.out.println(\"起飞之前准备降落伞\");");
            personFly.insertAfter("System.out.println(\"成功落地...\");");

            //新增一个方法
            CtMethod ctMethod = new CtMethod(CtClass.voidType,"joinFriend",new CtClass[]{},ctClass);
            ctMethod.setModifiers(Modifier.PUBLIC);
            ctMethod.setBody("System.out.println(\"I want to be your friend\");");
            ctClass.addMethod(ctMethod);

            //获取类对象，接下来就可以愉快的使用反射了
            Class<?> clazz = ctClass.toClass();
            Object o = clazz.newInstance();
            //调用 personFly 方法
            Method personFlyMethod = clazz.getDeclaredMethod("personFly");
            personFlyMethod.invoke(o);
            //调用 joinFriend 方法
            Method joinFriendMethod = clazz.getDeclaredMethod("joinFriend");
            joinFriendMethod.invoke(o);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
