package com.drathonix.rockbreakerplugin

import dev.kikugie.stonecutter.build.param.StonecutterBuildConfig

import dev.kikugie.stonecutter.build.StonecutterBuildExtension

import org.gradle.api.Plugin
import org.gradle.api.Project

import org.gradle.kotlin.dsl.*

class RockbreakerPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = project.run {
        val stonecutter = project.extensions.getByType(StonecutterBuildExtension::class.java)
        val rockbreaker = project.extensions.create("rockbreaker", RockbreakerExtension::class.java, project)

        // Adds both optional and required dependencies to stonecutter version checking.
        rockbreaker.modDependencies.forEachAfter{mid, ver ->
            (stonecutter as StonecutterBuildConfig).dependencies[mid] = ver.min
        }
        rockbreaker.apis.forEach{ src ->
            src.modInfo.modid?.let {
                (stonecutter as StonecutterBuildConfig).constants[it] = src.enabled
                src.versionRange.ifPresent{ ver ->
                    (stonecutter as StonecutterBuildConfig).dependencies[it] = ver.min
                }
            }
        }
        (stonecutter as StonecutterBuildConfig).constants["fabric"]=rockbreaker.env.isFabric
        (stonecutter as StonecutterBuildConfig).constants["forge"]=rockbreaker.env.isForge
        (stonecutter as StonecutterBuildConfig).constants["neoforge"]=rockbreaker.env.isNeo

        version = "${rockbreaker.mod.version}+${rockbreaker.env.mcVersion.min}+${rockbreaker.env.loader}"
        group = property("group").toString()

        if(rockbreaker.atMost("1.20.6")){
            tasks.replace("processResources",RepluralizationTask::class)
            project.tasks.getByName("processResources",RepluralizationTask::class) {
                autoPluralize=rockbreaker.listProperty("resources.pluralization.targets")
            }
        }
    }

}