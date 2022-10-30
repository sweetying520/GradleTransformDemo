@file:Suppress("UnstableApiUsage")

package com.dream.customtransformplugin.fixthirdlib

import com.android.build.api.variant.VariantInfo
import com.dream.customtransformplugin.base.BaseCustomTransform
import javassist.ClassPool
import org.gradle.api.Incubating
import java.io.InputStream
import java.io.OutputStream

/**
 * function: 修复第三方库 Transform
 *
 * @author zy
 * @since 2022/10/30
 */
class FixThirdLibTransform : BaseCustomTransform(true) {

    /**
     * 获取 Transform 名称
     */
    override fun getName(): String {
        return "FixThirdLibTransform"
    }

    /**
     * 只处理 StringUtils.class 文件，其他的都给过滤掉
     */
    override fun classFilter(className: String) = className.endsWith("StringUtils.class")

    /**
     * 用于过滤 Variant，返回 false 表示 Variant 不执行该 Transform
     */
    @Incubating
    override fun applyToVariant(variant: VariantInfo?): Boolean {
        return "debug" == variant?.buildTypeName
    }


    /**
     * 通过此方法进行字节码插桩
     */
    override fun provideFunction() = { input: InputStream, output: OutputStream ->
        try {
            val classPool = ClassPool.getDefault()
            val makeClass = classPool.makeClass(input)
            //对 StringUtils 的 getLength 进行插桩
            val getLengthMethod = makeClass.getDeclaredMethod("getLength")
            getLengthMethod.insertBefore("{System.out.println(\"Hello getLength bug修复了..\");if($1==null)return 0;}")

            //对 StringUtils 的 getCharArray 进行插桩
            val getCharArrayMethod = makeClass.getDeclaredMethod("getCharArray")
            getCharArrayMethod.insertBefore("{System.out.println(\"Hello getCharArray bug修复了..\");if($1==null)return new char[0];}")

            //打印 log，此 log 是 BaseCustomTransform 里面的
            log("插桩的类名：${makeClass.name}")
            makeClass.declaredMethods.forEach {
                log("插桩的方法名：$it")
            }
            output.write(makeClass.toBytecode())
            makeClass.detach()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}