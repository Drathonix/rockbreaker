package com.drathonix.rockbreakerplugin

import org.gradle.api.Project

import java.util.Optional
import java.util.function.BiConsumer
import java.util.function.Consumer

import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import dev.kikugie.stonecutter.build.param.StonecutterBuildConfig

open class RockbreakerExtension(val project: Project) {
    val stonecutter: StonecutterBuildExtension = project.extensions.getByType(StonecutterBuildExtension::class.java)

    fun stoneCutterHasProperty(str: String): Boolean {
        return project.hasProperty(str)
    }

    fun stoneCutterProperty(str: String): Any? {
        return project.property(str)
    }

    fun bool(str: String): Boolean {
        return str.lowercase().startsWith("t")
    }

    fun boolProperty(key: String): Boolean {
        if (!stoneCutterHasProperty(key)) {
            return false
        }
        return bool(stoneCutterProperty(key).toString())
    }

    fun listProperty(key: String): ArrayList<String> {
        if (!stoneCutterHasProperty(key)) {
            return arrayListOf()
        }
        val str = stoneCutterProperty(key).toString()
        if (str == "UNSET") {
            return arrayListOf()
        }
        return ArrayList(str.split(" "))
    }

    fun optionalStrProperty(key: String): Optional<String> {
        if (!stoneCutterHasProperty(key)) {
            return Optional.empty()
        }
        val str = stoneCutterProperty(key).toString()
        if (str == "UNSET") {
            return Optional.empty()
        }
        return Optional.of(str)
    }

    /**
     * Creates a VersionRange from a listProperty
     */
    fun versionProperty(key: String): VersionRange {
        if (!stoneCutterHasProperty(key)) {
            return VersionRange("", "")
        }
        val list = listProperty(key)
        for (i in 0 until list.size) {
            if (list[i] == "UNSET") {
                list[i] = ""
            }
        }
        return if (list.isEmpty()) {
            VersionRange("", "")
        } else if (list.size == 1) {
            VersionRange(list[0], "")
        } else {
            VersionRange(list[0], list[1])
        }
    }

    /**
     * Creates a VersionRange unless the value is UNSET
     */
    fun optionalVersionProperty(key: String): Optional<VersionRange> {
        val str = optionalStrProperty(key)
        if (!stoneCutterHasProperty(key)) {
            return Optional.empty()
        }
        if (!str.isPresent) {
            return Optional.empty()
        }
        return Optional.of(versionProperty(key))
    }

    fun atLeast(version: String) = env.atLeast(version)
    fun atMost(version: String) = env.atMost(version)
    fun isNot(version: String) = env.isNot(version)
    fun isExact(version: String) = env.isExact(version)


    /**
     * Stores core dependency and environment information.
     */
    public inner class Env {
        val archivesBaseName = stoneCutterProperty("archives_base_name").toString()

        val mcVersion = versionProperty("deps.core.mc.version_range")

        val loader = stoneCutterProperty("loom.platform").toString()
        val isFabric = loader == "fabric"
        val isForge = loader == "forge"
        val isNeo = loader == "neoforge"
        val isCommon = project.parent!!.name == "common"
        val isApi = project.parent!!.name == "api"
        val type = if (isFabric) EnvType.FABRIC else if (isForge) EnvType.FORGE else EnvType.NEOFORGE

        val javaVer = if (atMost("1.16.5")) 8 else if (isExact("1.17.1")) 16 else if (atMost("1.20.4")) 17 else 21

        val fabricLoaderVersion = versionProperty("deps.core.fabric.loader.version_range")
        val forgeMavenVersion = versionProperty("deps.core.forge.version_range")
        val forgeVersion =
            VersionRange(extractForgeVer(forgeMavenVersion.min), extractForgeVer(forgeMavenVersion.max))

        // FML language version is usually the first two numbers only.
        private val fgl: String =
            if (isForge) forgeMavenVersion.min.substring(forgeMavenVersion.min.lastIndexOf("-")+1) else ""
        val forgeLanguageVersion = VersionRange(if (isForge) fgl.substring(0, fgl.indexOf(".")) else "", "")
        val neoforgeVersion = versionProperty("deps.core.neoforge.version_range")

        // The modloader system is separate from the API in Neo
        val neoforgeLoaderVersion = versionProperty("deps.core.neoforge.loader.version_range")

        fun atLeast(version: String) = stonecutter.compare(mcVersion.min, version) >= 0
        fun atMost(version: String) = stonecutter.compare(mcVersion.min, version) <= 0
        fun isNot(version: String) = stonecutter.compare(mcVersion.min, version) != 0
        fun isExact(version: String) = stonecutter.compare(mcVersion.min, version) == 0

        private fun extractForgeVer(str: String): String {
            val split = str.split("-")
            if (split.size == 1) {
                return split[0]
            }
            if (split.size > 1) {
                return split[1]
            }
            return ""
        }
    }

    public val env = Env()
    private val apis = ArrayList<APISource>()

    fun getAPIs(): Collection<APISource>{
        return ArrayList(apis)
    }

    fun addAPI(src: APISource){
        apis.add(src)
        src.modInfo.modid?.let {
            (stonecutter as StonecutterBuildConfig).constants[it] = src.enabled
            src.versionRange.ifPresent{ ver ->
                (stonecutter as StonecutterBuildConfig).dependencies[it] = ver.min
            }
        }
    }

    // Stores information about the mod itself.
    public inner class ModProperties {
        val id = stoneCutterProperty("mod.id").toString()
        val displayName = stoneCutterProperty("mod.display_name").toString()
        val version = stoneCutterProperty("version").toString()
        val description = optionalStrProperty("mod.description").orElse("")
        val authors = stoneCutterProperty("mod.authors").toString()
        val icon = stoneCutterProperty("mod.icon").toString()
        val issueTracker = optionalStrProperty("mod.issue_tracker").orElse("")
        val license = optionalStrProperty("mod.license").orElse("")
        val sourceUrl = optionalStrProperty("mod.source_url").orElse("")
        val generalWebsite = optionalStrProperty("mod.general_website").orElse(sourceUrl)
    }
    public val mod = ModProperties()

    /**
     * Stores information specifically for fabric.
     * Fabric requires that the mod's client and common main() entry points be included in the fabric.mod.json file.
     */
    inner class ModFabric {
        val commonEntry =
            "${project.group}.${env.archivesBaseName}.fabric.${stoneCutterProperty("mod.fabric.entry.common").toString()}"
        val clientEntry =
            "${project.group}.${env.archivesBaseName}.fabric.${stoneCutterProperty("mod.fabric.entry.client").toString()}"
    }

    /**
     * Provides access to the mixins for specific environments.
     * All environments are provided the vanilla mixin if it is enabled.
     */
    public inner class ModMixins {
        val enableVanillaMixin = boolProperty("mixins.vanilla.enable")
        val enableFabricMixin = boolProperty("mixins.fabric.enable")
        val enableForgeMixin = boolProperty("mixins.forge.enable")
        val enableNeoforgeMixin = boolProperty("mixins.neoforge.enable")

        val vanillaMixin = "mixins.${mod.id}.json"
        val fabricMixin = "mixins.fabric.${mod.id}.json"
        val forgeMixin = "mixins.forge.${mod.id}.json"
        val neoForgeMixin = "mixins.neoforge.${mod.id}.json"
        val extraMixins = listProperty("mixins.extras")

        /**
         * Modify this method if you need better control over the mixin list.
         */
        fun getMixins(env: EnvType): List<String> {
            val out = arrayListOf<String>()
            if (enableVanillaMixin) out.add(vanillaMixin)
            when (env) {
                EnvType.FABRIC -> if (enableFabricMixin) out.add(fabricMixin)
                EnvType.FORGE -> if (enableForgeMixin) out.add(forgeMixin)
                EnvType.NEOFORGE -> if (enableNeoforgeMixin) out.add(neoForgeMixin)
            }
            out.addAll(extraMixins)
            return out
        }
    }

    /**
     * Controls publishing. For publishing to work dryRunMode must be false.
     * Modrinth and Curseforge project tokens are publicly accessible, so it is safe to include them in files.
     * Do not include your API keys in your project!
     *
     * The Modrinth API token should be stored in the MODRINTH_TOKEN environment variable.
     * The curseforge API token should be stored in the CURSEFORGE_TOKEN environment variable.
     */
    public inner class ModPublish {
        val mcTargets = arrayListOf<String>()
        val modrinthProjectToken = stoneCutterProperty("publish.token.modrinth").toString()
        val curseforgeProjectToken = stoneCutterProperty("publish.token.curseforge").toString()
        val mavenURL = optionalStrProperty("publish.maven.url")
        val dryRunMode = boolProperty("publish.dry_run")

        init {
            val tempmcTargets = listProperty("publish_acceptable_mc_versions")
            if (tempmcTargets.isEmpty()) {
                mcTargets.add(env.mcVersion.min)
            } else {
                mcTargets.addAll(tempmcTargets)
            }
        }
    }

    public val modPublish = ModPublish()

    /**
     * These dependencies will be added to the fabric.mods.json, META-INF/neoforge.mods.toml, and META-INF/mods.toml file.
     */
    public inner class ModDependencies {
        val loadBefore = listProperty("deps.before")
        fun forEachAfter(cons: BiConsumer<String, VersionRange>) {
            forEachRequired(cons)
            forEachOptional(cons)
        }

        fun forEachBefore(cons: Consumer<String>) {
            loadBefore.forEach(cons)
        }

        fun forEachOptional(cons: BiConsumer<String, VersionRange>) {
            apis.forEach { src ->
                if (src.enabled && src.type.isOptional() && src.type.includeInDepsList()) src.versionRange.ifPresent { ver ->
                    src.modInfo.modid?.let {
                        cons.accept(it, ver)
                    }
                }
            }
        }

        fun forEachRequired(cons: BiConsumer<String, VersionRange>) {
            cons.accept("minecraft", env.mcVersion)
            if (env.isForge) {
                cons.accept("forge", env.forgeVersion)
            }
            if (env.isNeo) {
                cons.accept("neoforge", env.neoforgeVersion)
            }
            if (env.isFabric) {
                cons.accept("fabric", env.fabricLoaderVersion)
            }
            apis.forEach { src ->
                if (src.enabled && !src.type.isOptional() && src.type.includeInDepsList()) src.versionRange.ifPresent { ver ->
                    src.modInfo.modid?.let {
                        cons.accept(it, ver)
                    }
                }
            }
        }
    }

    public val modDependencies = ModDependencies()
    public val modMixins = ModMixins()

    /**
     * These values will change between versions and mod loaders and may be changed by the project script. Handles generation of specific entries in mods.toml and neoforge.mods.toml
     */
    public inner class Dynamics {
        fun mixinField() = if (env.isNeo) neoForgeMixinField() else if (env.isFabric) fabricMixinField() else ""

        fun forgelikeLoaderVer() =
            if (env.isForge) env.forgeLanguageVersion.asForgelike() else env.neoforgeLoaderVersion.asForgelike()
        fun forgelikeAPIVer() = if (env.isForge) env.forgeVersion.asForgelike() else env.neoforgeVersion.asForgelike()
        fun dependenciesField() = if (env.isFabric) fabricDependencyList() else forgelikeDependencyField()
        fun excludes(): List<String> {
            val out = arrayListOf<String>()
            if (!env.isForge) {
                // NeoForge before 1.20.5 still uses the forge mods.toml :/ One of those goofy changes between versions.
                if (!env.isNeo || !env.atMost("1.20.4")) {
                    out.add("META-INF/mods.toml")
                }
            }
            if (!env.isFabric) {
                out.add("fabric.mod.json")
            }
            if (!env.isNeo || env.atMost("1.20.4")) {
                out.add("META-INF/neoforge.mods.toml")
            }
            return out
        }

        private fun neoForgeMixinField(): String {
            var out = ""
            for (mixin in modMixins.getMixins(EnvType.NEOFORGE)) {
                out += "[[mixins]]\nconfig=\"${mixin}\"\n"
            }
            return out
        }

        private fun fabricMixinField(): String {
            val list = modMixins.getMixins(EnvType.FABRIC)
            if (list.isEmpty()) {
                return ""
            } else {
                var out = "  \"mixins\" : [\n"
                for ((index, mixin) in list.withIndex()) {
                    out += "    \"${mixin}\""
                    if (index < list.size - 1) {
                        out += ","
                    }
                    out += "\n"
                }
                return "$out  ],"
            }
        }

        private fun fabricDependencyList(): String {
            var out = "  \"depends\":{"
            var useComma = false
            modDependencies.forEachRequired { modid, ver ->
                if (useComma) {
                    out += ","
                }
                out += "\n"
                out += "    \"${modid}\": \"${ver.asFabric()}\""
                useComma = true
            }
            return "$out\n  }"

        }

        private fun forgelikeDependencyField(): String {
            var out = ""
            modDependencies.forEachBefore { modid ->
                out += forgedep(modid, VersionRange("", ""), "BEFORE", false)
            }
            modDependencies.forEachOptional { modid, ver ->
                out += forgedep(modid, ver, "AFTER", false)
            }
            modDependencies.forEachRequired { modid, ver ->
                out += forgedep(modid, ver, "AFTER", true)
            }
            return out
        }

        private fun forgedep(modid: String, versionRange: VersionRange, order: String, mandatory: Boolean): String {
            return "[[dependencies.${mod.id}]]\n" +
                    "modId=\"${modid}\"\n" +
                    (if (env.isForge) "mandatory=${mandatory}\n" else "type=\"${if (mandatory) "required" else "optional"}\"\n") +
                    "versionRange=\"${versionRange.asForgelike()}\"\n" +
                    "ordering=\"${order}\"\n" +
                    "side=\"BOTH\"\n"
        }
    }

    public val modFabric = ModFabric()
    public val dynamics = Dynamics()
}