package com.dream.gradletransformdemo;


import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.Modifier;

/**
 * function: Javassist 测试类
 *
 * @author zy
 * @since 2022/10/30
 */

public class TestCreateClass {

    /**
     * 创建一个 Person.class 文件
     */
    public static CtClass createPersonClass() throws Exception {
        ClassPool pool = ClassPool.getDefault();

        // 1. 创建一个空类：Person ，包名：com.dream.gradletransformdemo
        CtClass cc = pool.makeClass("com.dream.gradletransformdemo.Person");

        // 2. 新增一个字段 private String name;
        // 字段名为 name
        CtField param = new CtField(pool.get("java.lang.String"), "name", cc);
        // 访问级别是 private
        param.setModifiers(Modifier.PRIVATE);
        // 初始值是 "erdai"
        cc.addField(param, CtField.Initializer.constant("erdai"));

        // 3. 生成 setter、getter 方法
        cc.addMethod(CtNewMethod.setter("setName", param));
        cc.addMethod(CtNewMethod.getter("getName", param));

        // 4. 添加无参的构造函数
        CtConstructor cons = new CtConstructor(new CtClass[]{}, cc);
        //方法体：this.name = "xiaoming";
        cons.setBody("{name = \"xiaoming\";}");
        cc.addConstructor(cons);

        // 5. 添加有参的构造函数
        cons = new CtConstructor(new CtClass[]{pool.get("java.lang.String")}, cc);
        // 方法提：this.name = var1;
        //$0=this $1,$2,$3... 代表方法参数
        cons.setBody("{$0.name = $1;}");
        cc.addConstructor(cons);

        // 6. 创建一个名为 printName 的方法，无参数，无返回值，输出 name 值
        CtMethod ctMethod = new CtMethod(CtClass.voidType, "printName", new CtClass[]{}, cc);
        ctMethod.setModifiers(Modifier.PUBLIC);
        ctMethod.setBody("{if($0.name == null){System.out.println(\"报错了\");}System.out.println(this.name);}");
        cc.addMethod(ctMethod);

        // 将 Person.class 文件输出到如下文件夹
        cc.writeFile("/Users/zhouying/AndroidStudioProjects/MixDemo/GradleTransformDemo/app/src/main/java/");
        return cc;
    }


    public static void main(String[] args) {
        try {
            createPersonClass();
//            ClassPool classPool = ClassPool.getDefault();
//            //通过生成类的绝对位置构建 CtClass 实例对象
//            classPool.appendClassPath("/Users/zhouying/AndroidStudioProjects/MixDemo/GradleTransformDemo/app/src/main/java/");
//            //获取接口
//            CtClass iPersonCtClass = classPool.get("com.dream.gradletransformdemo.IPerson");
//            //获取生成的类 Person
//            CtClass personCtClass = classPool.get("com.dream.gradletransformdemo.Person");
//            //让 Person 实现 IPerson 接口
//            personCtClass.setInterfaces(new CtClass[]{iPersonCtClass});
//
//            //接下俩就可以通过接口进行调用了
//            IPerson person = (IPerson) personCtClass.toClass().newInstance();
//            person.setName("erdai666");
//            person.printName();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

interface IPerson{
    void setName(String name);

    String getName();

    void printName();
}