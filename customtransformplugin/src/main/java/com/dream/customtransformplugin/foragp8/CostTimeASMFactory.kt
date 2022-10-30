package com.dream.customtransformplugin.foragp8

import com.android.build.api.instrumentation.*
import com.dream.customtransformplugin.costtime.CostTimeClassVisitor
import org.objectweb.asm.ClassVisitor


abstract class CostTimeASMFactory: AsmClassVisitorFactory<InstrumentationParameters.None> {
    override fun createClassVisitor(
        classContext: ClassContext,
        nextClassVisitor: ClassVisitor
    ): ClassVisitor {
        return CostTimeClassVisitor(nextClassVisitor)
    }

    override fun isInstrumentable(classData: ClassData): Boolean {
        return true
    }
}