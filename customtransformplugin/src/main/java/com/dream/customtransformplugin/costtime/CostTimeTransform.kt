package com.dream.customtransformplugin.costtime

import com.android.build.api.variant.VariantInfo
import com.dream.customtransformplugin.base.BaseCustomTransform
import org.gradle.api.Incubating
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import java.io.InputStream
import java.io.OutputStream

/**
 * function: waiting for add
 *
 * @author zy
 * @since 2022/10/30
 */
class CostTimeTransform: BaseCustomTransform(true) {

    /**
     * 获取 Transform 名称
     */
    override fun getName(): String {
        return "CostTimeTransform"
    }

    /**
     * 过滤只统计以 Activity.class 结尾的文件
     */
    override fun classFilter(className: String) = className.endsWith("Activity.class")


    /**
     * 用于过滤 Variant，返回 false 表示 Variant 不执行该 Transform
     */
    @Incubating
    override fun applyToVariant(variant: VariantInfo?): Boolean {
        return "debug" == variant?.buildTypeName
    }


    override fun provideFunction() = { input: InputStream,output: OutputStream ->
        //使用 input 输入流构建 ClassReader
        val reader = ClassReader(input)
        //使用 ClassReader 和 flags 构建 ClassWriter
        val writer = ClassWriter(reader, ClassWriter.COMPUTE_FRAMES)
        //使用 ClassWriter 构建我们自定义的 ClassVisitor
        val visitor = CostTimeClassVisitor(writer)
        //最后通过 ClassReader 的 accept 将每一条字节码指令传递给 ClassVisitor
        reader.accept(visitor, ClassReader.EXPAND_FRAMES)
        //将修改后的字节码文件转换成字节数组
        val byteArray = writer.toByteArray()
        //最后通过输出流修改文件，这样就实现了字节码的插桩
        output.write(byteArray)
    }
}