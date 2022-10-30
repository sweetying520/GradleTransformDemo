@file:Suppress("UnstableApiUsage")

package com.dream.customtransformplugin.base


import com.android.SdkConstants
import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.builder.utils.isValidZipEntryName
import com.android.utils.FileUtils
import com.google.common.io.Files
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * function: 自定义 Transform 模版
 *
 * @author zy
 * @since 2022/10/20
 */
abstract class BaseCustomTransform(private val enableLog: Boolean) : Transform() {

    //线程池，可提升 80% 的执行速度
    private var waitableExecutor: WaitableExecutor = WaitableExecutor.useGlobalSharedThreadPool()

    /**
     * 此方法提供给上层进行字节码插桩
     */
    abstract fun provideFunction(): ((InputStream, OutputStream) -> Unit)?

    /**
     * 上层可重写该方法进行文件过滤
     */
    open fun classFilter(className: String) = className.endsWith(SdkConstants.DOT_CLASS)


    /**
     * 默认：获取输入的字节码文件
     */
    override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> {
        return TransformManager.CONTENT_CLASS
    }

    /**
     * 默认：检索整个项目的内容
     */
    override fun getScopes(): MutableSet<in QualifiedContent.Scope> {
        return TransformManager.SCOPE_FULL_PROJECT
    }


    /**
     * 默认开启增量编译
     */
    override fun isIncremental(): Boolean {
        return true
    }

    /**
     * 对输入的数据做检索操作：
     * 1、处理增量编译
     * 2、处理并发逻辑
     */
    override fun transform(transformInvocation: TransformInvocation) {
        super.transform(transformInvocation)

        log("Transform start...")

        //输入内容
        val inputProvider = transformInvocation.inputs
        //输出内容
        val outputProvider = transformInvocation.outputProvider

        // 1. 子类实现字节码插桩操作
        val function = provideFunction()

        // 2. 不是增量编译，删除所有旧的输出内容
        if (!transformInvocation.isIncremental) {
            outputProvider.deleteAll()
        }

        for (input in inputProvider) {
            // 3. Jar 包处理
            log("Transform jarInputs start.")
            for (jarInput in input.jarInputs) {
                val inputJar = jarInput.file
                val outputJar = outputProvider.getContentLocation(jarInput.name, jarInput.contentTypes, jarInput.scopes, Format.JAR)
                if (transformInvocation.isIncremental) {
                    // 3.1. 增量编译中处理 Jar 包逻辑
                    when (jarInput.status ?: Status.NOTCHANGED) {
                        Status.NOTCHANGED -> {
                            // Do nothing.
                        }
                        Status.ADDED, Status.CHANGED -> {
                            // Do transform.
                            waitableExecutor.execute {
                                doTransformJar(inputJar, outputJar, function)
                            }
                        }
                        Status.REMOVED -> {
                            // Delete.
                            FileUtils.delete(outputJar)
                        }
                    }
                } else {
                    // 3.2 非增量编译中处理 Jar 包逻辑
                    waitableExecutor.execute {
                        doTransformJar(inputJar, outputJar, function)
                    }
                }
            }
            // 4. 文件夹处理
            log("Transform dirInput start.")
            for (dirInput in input.directoryInputs) {
                val inputDir = dirInput.file
                val outputDir = outputProvider.getContentLocation(dirInput.name, dirInput.contentTypes, dirInput.scopes, Format.DIRECTORY)
                if (transformInvocation.isIncremental) {
                    // 4.1. 增量编译中处理文件夹逻辑
                    for ((inputFile, status) in dirInput.changedFiles) {
                        val outputFile = concatOutputFilePath(outputDir, inputFile)
                        when (status ?: Status.NOTCHANGED) {
                            Status.NOTCHANGED -> {
                                // Do nothing.
                            }
                            Status.ADDED, Status.CHANGED -> {
                                // Do transform.
                                waitableExecutor.execute {
                                    doTransformFile(inputFile, outputFile, function)
                                }
                            }
                            Status.REMOVED -> {
                                // Delete
                                FileUtils.delete(outputFile)
                            }
                        }
                    }
                } else {
                    // 4.2. 非增量编译中处理文件夹逻辑
                    // Traversal fileTree (depthFirstPreOrder).
                    for (inputFile in FileUtils.getAllFiles(inputDir)) {
                        waitableExecutor.execute {
                            val outputFile = concatOutputFilePath(outputDir, inputFile)
                            if (classFilter(inputFile.name)) {
                                doTransformFile(inputFile, outputFile, function)
                            } else {
                                // Copy.
                                Files.createParentDirs(outputFile)
                                FileUtils.copyFile(inputFile, outputFile)
                            }
                        }
                    }
                }
            }
        }
        waitableExecutor.waitForTasksWithQuickFail<Any>(true)
        log("Transform end...")
    }

    /**
     * Do transform Jar.
     */
    private fun doTransformJar(inputJar: File, outputJar: File, function: ((InputStream, OutputStream) -> Unit)?) {
        // Create parent directories to hold outputJar file.
        Files.createParentDirs(outputJar)
        // Unzip.
        FileInputStream(inputJar).use { fis ->
            ZipInputStream(fis).use { zis ->
                // Zip.
                FileOutputStream(outputJar).use { fos ->
                    ZipOutputStream(fos).use { zos ->
                        var entry = zis.nextEntry
                        while (entry != null && isValidZipEntryName(entry)) {
                            if (!entry.isDirectory) {
                                zos.putNextEntry(ZipEntry(entry.name))
                                if (classFilter(entry.name)) {
                                    // Apply transform function.
                                    applyFunction(zis, zos, function)
                                } else {
                                    // Copy.
                                    zis.copyTo(zos)
                                }
                            }
                            entry = zis.nextEntry
                        }
                    }
                }
            }
        }
    }

    /**
     * Do transform file.
     */
    private fun doTransformFile(inputFile: File, outputFile: File, function: ((InputStream, OutputStream) -> Unit)?) {
        // Create parent directories to hold outputFile file.
        Files.createParentDirs(outputFile)
        FileInputStream(inputFile).use { fis ->
            FileOutputStream(outputFile).use { fos ->
                // Apply transform function.
                applyFunction(fis, fos, function)
            }
        }
    }

    private fun applyFunction(input: InputStream, output: OutputStream, function: ((InputStream, OutputStream) -> Unit)?) {
        try {
            if (null != function) {
                function.invoke(input, output)
            } else {
                // Copy
                input.copyTo(output)
            }
        } catch (e: UncheckedIOException) {
            throw e.cause!!
        }
    }

    /**
     * 创建输出的文件
     */
    private fun concatOutputFilePath(outputDir: File, inputFile: File) = File(outputDir, inputFile.name)

    /**
     * log 打印
     */
    fun log(logStr: String) {
        if (enableLog) {
            println("$name - $logStr")
        }
    }
}