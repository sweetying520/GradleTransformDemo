package com.dream.customtransformplugin

import com.android.build.api.instrumentation.FramesComputationMode
import com.android.build.api.instrumentation.InstrumentationScope
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.Variant
import com.android.build.gradle.AppExtension
import com.dream.customtransformplugin.costtime.CostTimeTransform
import com.dream.customtransformplugin.fixthirdlib.FixThirdLibTransform
import com.dream.customtransformplugin.foragp8.CostTimeASMFactory
import org.gradle.api.Plugin
import org.gradle.api.Project
import kotlin.jvm.functions.Function1


/**
 * 自定义：CustomTransformPlugin
 */
class CustomTransformPlugin: Plugin<Project> {

    override fun apply(project: Project) {
        println("Hello CustomTransformPlugin")

        //新增的代码
        // 1、获取 Android 扩展
        val androidExtension = project.extensions.getByType(AppExtension::class.java)
        // 2、注册 Transform
        androidExtension.registerTransform(MyCustomTransform())
        androidExtension.registerTransform(FixThirdLibTransform())
        //androidExtension.registerTransform(CostTimeTransform())

        //AppExtension VS AndroidComponentsExtension
        //使用新方式注册
        val extension = project.extensions.getByType(AndroidComponentsExtension::class.java) as AndroidComponentsExtension
        extension.onVariants(extension.selector().all()){ variant ->
            variant.instrumentation.transformClassesWith(CostTimeASMFactory::class.java,InstrumentationScope.PROJECT){}
            variant.instrumentation.setAsmFramesComputationMode(FramesComputationMode.COMPUTE_FRAMES_FOR_INSTRUMENTED_METHODS)
        }
    }
}