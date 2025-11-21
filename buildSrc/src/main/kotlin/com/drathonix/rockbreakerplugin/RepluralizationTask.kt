package com.drathonix.rockbreakerplugin;

import org.gradle.api.tasks.Input
import org.gradle.language.jvm.tasks.ProcessResources
import java.io.File

/**
 * Replaces the normal copy task and post-processes the files.
 * Effectively renames datapack directories due to depluralization past 1.20.5.
 */
open class RepluralizationTask : ProcessResources() {
    @Input
    var autoPluralize: ArrayList<String> = arrayListOf<String>()
    override fun copy() {
        super.copy()
        val root = destinationDir.absolutePath
        autoPluralize.forEach { path ->
            val file = File(root.plus(path))
            if(file.exists()){
                file.copyRecursively(File(file.absolutePath.plus("s")),true)
                file.deleteRecursively()
            }
        }
    }
}
