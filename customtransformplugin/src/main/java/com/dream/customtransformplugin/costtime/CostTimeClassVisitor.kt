package com.dream.customtransformplugin.costtime

import org.objectweb.asm.*
import org.objectweb.asm.commons.AdviceAdapter


class CostTimeClassVisitor(nextVisitor: ClassVisitor) : ClassVisitor(Opcodes.ASM6,nextVisitor) {

    var isHook = false

    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        val visitor = super.visitMethod(access, name, descriptor, signature, exceptions)
        // AdviceAdapter 是 MethodVisitor 的子类，使用 AdviceAdapter 可以更方便的修改方法的字节码。
        // AdviceAdapter其中几个重要方法如下：
        // void visitCode()：表示 ASM 开始扫描这个方法
        // void onMethodEnter()：进入这个方法
        // void onMethodExit()：即将从这个方法出去
        // void onVisitEnd()：表示方法扫描完毕
        return object : AdviceAdapter(Opcodes.ASM6, visitor, access, name, descriptor) {

            /**
             * 访问自定义注解
             */
            override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
                //如果添加了自定义注解，则进行 hook
                if ("Lcom/dream/gradletransformdemo/annotation/CostTime;" == descriptor) {
                    isHook = true
                }
                return super.visitAnnotation(descriptor, visible)
            }

            //在原方法代码的前面插入代码
            override fun onMethodEnter() {
                if(!isHook)return
                visitMethodInsn(INVOKESTATIC, "android/os/SystemClock", "elapsedRealtime", "()J", false)
                visitVarInsn(LSTORE, 2)
                visitLdcInsn("erdai")
                visitTypeInsn(NEW, "java/lang/StringBuilder")
                visitInsn(DUP)
                visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false)
                visitLdcInsn("onCreate startTime: ")
                visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false)
                visitVarInsn(LLOAD, 2)
                visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(J)Ljava/lang/StringBuilder;", false)
                visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false)
                visitMethodInsn(INVOKESTATIC, "android/util/Log", "d", "(Ljava/lang/String;Ljava/lang/String;)I", false)
                visitInsn(POP)
            }

            //在原方法代码的后面插入代码
            override fun onMethodExit(opcode: Int) {
                if(!isHook)return
                visitMethodInsn(INVOKESTATIC, "android/os/SystemClock", "elapsedRealtime", "()J", false)
                visitVarInsn(LSTORE, 4)
                visitLdcInsn("erdai")
                visitTypeInsn(NEW, "java/lang/StringBuilder")
                visitInsn(DUP)
                visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false)
                visitLdcInsn("onCreate endTime: ")
                visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false)
                visitVarInsn(LLOAD, 4)
                visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(J)Ljava/lang/StringBuilder;", false)
                visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false)
                visitMethodInsn(INVOKESTATIC, "android/util/Log", "d", "(Ljava/lang/String;Ljava/lang/String;)I", false)
                visitInsn(POP)
                visitVarInsn(LLOAD, 4)
                visitVarInsn(LLOAD, 2)
                visitInsn(LSUB)
                visitVarInsn(LSTORE, 6)
                visitLdcInsn("erdai")
                visitTypeInsn(NEW, "java/lang/StringBuilder")
                visitInsn(DUP)
                visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false)
                visitLdcInsn("onCreate \u8017\u65f6: ")
                visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false)
                visitVarInsn(LLOAD, 6)
                visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(J)Ljava/lang/StringBuilder;", false)
                visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false)
                visitMethodInsn(INVOKESTATIC, "android/util/Log", "d", "(Ljava/lang/String;Ljava/lang/String;)I", false)
                visitInsn(POP)
            }
        }
    }
}
