package com.dream.customtransformplugin

import com.dream.customtransformplugin.base.BaseCustomTransform
import java.io.InputStream
import java.io.OutputStream

/**
 * function: 自定义 Transform
 */
class MyCustomTransform: BaseCustomTransform(true) {

    override fun getName(): String {
        return "ErdaiTransform"
    }

    /**
     * 此方法可以使用 ASM 或 Javassist 进行字节码插桩
     * 目前只是一个默认实现
     */
    override fun provideFunction() = { ios: InputStream, zos: OutputStream ->
        zos.write(ios.readAllBytes())
    }
}