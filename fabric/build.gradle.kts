import org.gradle.internal.extensions.stdlib.toDefaultLowerCase
import org.gradle.internal.impldep.org.apache.commons.lang3.function.TriConsumer
import java.util.Optional
import java.util.function.BiConsumer
import java.util.function.Consumer
import java.util.function.Supplier

// Baseline code. Minimal edits necessary.
plugins {
    `maven-publish`
    kotlin("jvm") version "1.9.22"
    //id("fabric-loom") // Leaving this here if you want to swap loom.
    id("dev.architectury.loom")
    //id("dev.kikugie.j52j") // Recommended by kiku if using swaps in json5.
    id("me.modmuss50.mod-publish-plugin")
}

// Leave this alone unless adding more dependencies.
repositories {
    mavenCentral()
    exclusiveContent {
        forRepository { maven("https://www.cursemaven.com") { name = "CurseForge" } }
        filter { includeGroup("curse.maven") }
    }
    exclusiveContent {
        forRepository { maven("https://api.modrinth.com/maven") { name = "Modrinth" } }
        filter { includeGroup("maven.modrinth") }
    }
    maven("https://maven.neoforged.net/releases/")
    maven("https://maven.architectury.dev/")
    maven("https://modmaven.dev/")
    maven("https://panel.ryuutech.com/nexus/repository/maven-releases/")
}

fun bool(str: String) : Boolean {
    return str.toDefaultLowerCase().startsWith("t")
}

fun boolProperty(key: String) : Boolean {
    if(!hasProperty(key)){
        return false
    }
    return bool(property(key).toString())
}

fun listProperty(key: String) : List<String> {
    if(!hasProperty(key)){
        return emptyList()
    }
    val str = property(key).toString();
    if(str == "UNSET"){
        return emptyList()
    }
    return str.split(" ");
}

fun optionalStrProperty(key: String) : Optional<String> {
    if(!hasProperty(key)){
        return Optional.empty()
    }
    val str = property(key).toString()
    if(str =="UNSET"){
        return Optional.empty()
    }
    return Optional.of(str)
}

class VersionRange(public val min: String, public val max: String){
    fun asForgelike() : String{
        return "${if(min.isEmpty()) "(" else "["}${min},${max}${if(max.isEmpty()) ")" else "]"}"
    }
    fun asFabric() : String{
        //TODO
        return "notDone"
    }
}

/**
 * Creates a VersionRange from a listProperty
 */
fun versionProperty(key: String) : VersionRange {
    if(!hasProperty(key)){
        return VersionRange("","")
    }
    val list = listProperty(key)
    if(list.isEmpty()){
        return VersionRange("","")
    }
    else if(list.size == 1) {
        return VersionRange(list.get(0),"")
    }
    else{
        return VersionRange(list.get(0),list.get(1))
    }
}

/**
 * Creates a VersionRange unless the value is UNSET
 */
fun optionalVersionProperty(key: String) : Optional<VersionRange>{
    val str = optionalStrProperty(key)
    if(!hasProperty(key)){
        return Optional.empty()
    }
    if(!str.isPresent){
        return Optional.empty()
    }
    return Optional.of(versionProperty(key))
}

enum class EnvType {
    FABRIC,
    FORGE,
    NEOFORGE
}

/**
 * Stores core dependency and environment information.
 */
class Env {
    val archivesBaseName = property("archives_base_name")

    val mcVersion = versionProperty("deps.core.mc.version_range");

    val loader = property("loom.platform").toString()
    val isFabric = loader == "fabric"
    val isForge = loader == "forge"
    val isNeo = loader == "neoforge"
    val type = if(isFabric) EnvType.FABRIC else if(isForge) EnvType.FORGE else EnvType.NEOFORGE

    val javaVer = if(atMost("1.16.5")) 8 else if(atMost("1.20.4")) 17 else 21

    val fabricLoaderVersion = versionProperty("deps.core.fabric.loader.version_range")
    val forgeVersion = versionProperty("deps.core.forge.version_range")
    val neoforgeVersion = versionProperty("deps.core.neoforge.version_range")
    // The modloader system is separate from the API in Neo
    val neoforgeLoaderVersion = versionProperty("deps.core.neoforge.loader.version_range")

    fun atLeast(version: String) = stonecutter.compare(mcVersion.min, version) >= 0
    fun atMost(version: String) = stonecutter.compare(mcVersion.min, version) <= 0
    fun isNot(version: String) = stonecutter.compare(mcVersion.min, version) != 0
    fun isExact(version: String) = stonecutter.compare(mcVersion.min, version) == 0
}
val env = Env()

/**
 * APIs with hardcoded support for convenience. These are optional.
 */
class APIs {
    val fabricApi = optionalVersionProperty("deps.api.fabric")
    val architecturyApi = optionalVersionProperty("deps.api.architectury")
}
val apis = APIs()

// Stores information about the mod itself.
class ModProperties {
    val id = property("mod.id").toString()
    val displayName = property("mod.display_name").toString()
    val version = property("version").toString()
    val description = property("mod.description").toString()
    val authors = property("mod.authors").toString()
    val icon = property("mod.icon").toString()
    val issueTracker = property("mod.issue_tracker").toString()
    val license = property("mod.license").toString()
    val sourceUrl = property("mod.github_url").toString()
    val generalWebsite = optionalStrProperty("mod.general_website").orElse(sourceUrl)
}

/**
 * Stores information specifically for fabric.
 * Fabric requires that the mod's client and common main() entry points be included in the fabric.mod.json file.
 */
class ModFabric {
    val commonEntry = "${group}.${env.archivesBaseName}.fabric.${property("mod.fabric.entry.common").toString()}"
    val clientEntry = "${group}.${env.archivesBaseName}.fabric.${property("mod.fabric.entry.client").toString()}"
}

/**
 * Provides access to the mixins for specific environments.
 * All environments are provided the vanilla mixin if it is enabled.
 */
class ModMixins {
    val enableVanillaMixin = boolProperty("mixins.vanilla.enable");
    val enableFabricMixin = boolProperty("mixins.fabric.enable");
    val enableForgeMixin = boolProperty("mixins.forge.enable");
    val enableNeoforgeMixin = boolProperty("mixins.neoforge.enable");

    val vanillaMixin = "mixins.${mod.id}.json"
    val fabricMixin = "mixins.fabric.${mod.id}.json"
    val forgeMixin = "mixins.forge.${mod.id}.json"
    val neoForgeMixin = "mixins.neoforge.${mod.id}.json"
    val extraMixins = listProperty("mixins.extras")

    /**
     * Modify this method if you need better control over the mixin list.
     */
    fun getMixins(env: EnvType) : List<String> {
        val out = arrayListOf<String>();
        if(enableVanillaMixin) out.add(vanillaMixin);
        when (env) {
            EnvType.FABRIC -> if(enableFabricMixin) out.add(fabricMixin);
            EnvType.FORGE -> if(enableForgeMixin) out.add(forgeMixin);
            EnvType.NEOFORGE -> if(enableNeoforgeMixin) out.add(neoForgeMixin);
        }
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
class ModPublish {
    val mcTargets = arrayListOf<String>();
    val modrinthProjectToken = property("publish.token.modrinth").toString();
    val curseforgeProjectToken = property("publish.token.curseforge").toString();
    val mavenURL = property("publish.maven.url").toString();
    val dryRunMode = boolProperty("publish.dry_run")

    init {
        val tempmcTargets = listProperty("publish_acceptable_mc_versions")
        if(tempmcTargets.isEmpty()){
            mcTargets.add(env.mcVersion.min);
        }
        else{
            mcTargets.addAll(tempmcTargets);
        }
    }
}
val modPublish = ModPublish()

/**
 * These dependencies will be added to the fabric.mods.json, META-INF/neoforge.mods.toml, and META-INF/mods.toml file.
 */
class ModDependencies {
    val loadBefore = listProperty("deps.before")
    val loadAfterOptional = listProperty("deps.load_after_optional")
    val loadAfterRequired = listProperty("deps.load_after_required")

    private fun fre(list: List<String>, versionAcceptor: BiConsumer<String,VersionRange> ) {
        for (i in 0 until list.size-3 step 3) {
            val modid = list.get(i)
            val min = list.get(i+1)
            val max = list.get(i+2)
            versionAcceptor.accept(modid,VersionRange(min,max))
        }
    }
    fun forEachAfter(cons: BiConsumer<String,VersionRange>){
        forEachRequired(cons)
        forEachOptional(cons)
    }

    fun forEachBefore(cons: Consumer<String>){
        loadBefore.forEach(cons)
    }

    fun forEachOptional(cons: BiConsumer<String,VersionRange>){
        fre(loadAfterOptional,cons)
    }

    fun forEachRequired(cons: BiConsumer<String,VersionRange>){
        fre(loadAfterRequired,cons)
        cons.accept("minecraft",env.mcVersion)
        if(env.isForge) {
            cons.accept("forge", env.forgeVersion)
        }
        if (env.isNeo){
            cons.accept("neoforge", env.neoforgeVersion)
        }
        if(env.isFabric) {
            cons.accept("fabric", env.fabricLoaderVersion)
        }
        apis.fabricApi.ifPresent{ver->
            cons.accept(dynamics.fabricAPIID, ver)
        }
        apis.architecturyApi.ifPresent{ver->
            cons.accept("architectury", ver)
        }
        // +++ Insert code here for other modloaders and additional apis
    }
}
val dependencies = ModDependencies()

/**
 * These values will change between versions and mod loaders. Handles generation of specific entries in mods.toml and neoforge.mods.toml
 */
class SpecialMultiversionedConstants {
    val fabricAPIID = if(env.atMost("1.16.5")) "fabric" else "fabric-api"
    val mixinField = if(env.atMost("1.20.4") && env.isNeo) neoForgeMixinField() else if(env.isFabric) fabricMixinField() else ""

    val architecturyGroup = if(env.atLeast("1.18.0")) "dev.architectury" else "me.shedaniel"
    val forgelikeLoaderVer =  if(env.isForge) env.forgeVersion.asForgelike() else env.neoforgeLoaderVersion.asForgelike()
    val forgelikeAPIVer = if(env.isForge) env.forgeVersion.asForgelike() else env.neoforgeVersion.asForgelike()
    val dependenciesField = if(env.isFabric) fabricDependencyList() else forgelikeDependencyField()
    val excludes = excludes0()
    private val mandatoryIndicator = if(env.isNeo) "required" else "mandatory"
    private fun excludes0() : List<String> {
        var out = arrayListOf<String>()
        if(!env.isForge) {
            // NeoForge before 1.21 still uses the forge mods.toml :/ One of those goofy changes between versions.
            if(!env.isNeo || !env.atLeast("1.20.6")) {
                out.add("META-INF/mods.toml")
            }
        }
        if(!env.isFabric){
            out.add("fabric.mod.json")
        }
        if(!env.isNeo){
            out.add("META-INF/neoforge.mods.toml")
        }
        return out
    }
    private fun neoForgeMixinField () : String {
        var out = ""
        for (mixin in modMixins.getMixins(EnvType.NEOFORGE)) {
            out += "[[mixins]]\nconfig=\"${mixin}\"\n"
        }
        return out
    }
    private fun fabricMixinField () : String {
        val list = modMixins.getMixins(EnvType.FABRIC)
        if(list.isEmpty()){
            return ""
        }
        else{
            var out = "mixins:[\n"
            for ((index, mixin) in list.withIndex()) {
                out += "\"${mixin}\""
                if(index < list.size-1){
                    out+=","
                }
                out+="\n"
            }
            return "$out],\n"
        }
    }
    private fun fabricDependencyList() : String{
        var out = "depends:{"
        var useComma = false
        dependencies.forEachRequired{modid,ver->
            if(useComma){
                out+=","
            }
            out+="\n"
            out+="\"{$modid}\": \"${ver.asFabric()}\""
            useComma = true
        }
        return "$out},\n"

    }
    private fun forgelikeDependencyField() : String {
        var out = ""
        dependencies.forEachBefore{modid ->
            out += forgedep(modid,VersionRange("",""),"BEFORE",false)
        }
        dependencies.forEachOptional{modid,ver->
            out += forgedep(modid,ver,"AFTER",false)
        }
        dependencies.forEachRequired{modid,ver->
            out += forgedep(modid,ver,"AFTER",true)
        }
        return out
    }
    private fun forgedep(modid: String, versionRange: VersionRange, order: String, mandatory: Boolean) : String {
        return "[[dependencies.${mod.id}]]\n" +
                "modId=\"${modid}\"\n" +
                "${mandatoryIndicator}=${mandatory}\n" +
                "versionRange=\"${versionRange.asForgelike()}\"\n" +
                "ordering=\"${order}\"\n" +
                //TODO: if I need this to be modified I will add configurability for it, but I don't
                "side=\"BOTH\"\n"
    }
}


val mod = ModProperties()
val modFabric = ModFabric()
val modMixins = ModMixins()
val dynamics = SpecialMultiversionedConstants()

version = "${mod.version}+${env.mcVersion}+${env.loader}"
group = property("group").toString();

// Adds both optional and required dependencies to stonecutter version checking.
dependencies.forEachAfter{mid, ver ->
    stonecutter.dependency(mid,ver.min)
}

loom {
    if (env.isForge) forge {
        for (mixin in modMixins.getMixins(EnvType.FORGE)) {
            mixinConfigs(
                mixin
            )
        }
    }

    decompilers {
        get("vineflower").apply { // Adds names to lambdas - useful for mixins
            options.put("mark-corresponding-synthetics", "1")
        }
    }

    runConfigs.all {
        ideConfigGenerated(stonecutter.current.isActive)
        vmArgs("-Dmixin.debug.export=true")
        runDir = "../../run"
    }
}
//base { archivesName.set(env.archivesBaseName) }

dependencies {
    minecraft("com.mojang:minecraft:${env.mcVersion.min}")
    mappings(loom.officialMojangMappings())

    if(env.isFabric) {
        modImplementation("net.fabricmc:fabric-loader:${env.fabricLoaderVersion.min}")
        apis.architecturyApi.ifPresent {ver->
            modApi("${dynamics.architecturyGroup}:architectury-fabric:${ver.min}")
        }
        apis.fabricApi.ifPresent {ver->
            modApi("net.fabricmc.fabric-api:fabric-api:${ver.min}")
        }
    }
    if(env.isForge){
        "forge"("net.minecraftforge:forge:${env.forgeVersion.min}")
        apis.architecturyApi.ifPresent {ver->
            modApi("${dynamics.architecturyGroup}:architectury-forge:${ver.min}")
        }
    }
    if(env.isNeo){
        "neoForge"("net.neoforged:neoforge:${env.neoforgeVersion.min}")
        apis.architecturyApi.ifPresent { ver->
            modApi("${dynamics.architecturyGroup}:architectury-neoforge:${ver.min}")
        }
    }
    vineflowerDecompilerClasspath("org.vineflower:vineflower:1.10.1")
}

/*java {
    withSourcesJar()
    val java = if(env.javaVer == 8) JavaVersion.VERSION_1_8 else if(env.javaVer == 17) JavaVersion.VERSION_17 else JavaVersion.VERSION_21
}*/

tasks.processResources {
    val map = mapOf(
        "id" to mod.id,
        "name" to mod.displayName,
        "display_name" to mod.displayName,
        "version" to mod.version,
        "description" to mod.description,
        "authors" to mod.authors,
        "github_url" to mod.sourceUrl,
        "source_url" to mod.sourceUrl,
        "website" to mod.generalWebsite,
        "icon" to mod.icon,
        "fabric_common_entry" to modFabric.commonEntry,
        "fabric_client_entry" to modFabric.clientEntry,
        "mc_min" to env.mcVersion.min,
        "mc_max" to env.mcVersion.max,
        "issue_tracker" to mod.issueTracker,
        "java_ver" to env.javaVer,
        "forgelike_loader_ver" to dynamics.forgelikeLoaderVer,
        "forgelike_api_ver" to dynamics.forgelikeAPIVer,
        "loader_id" to env.loader,
        "license" to mod.license,
        "mixin_field" to dynamics.mixinField,
        "dependencies_field" to dynamics.dependenciesField
    )
    map.forEach{ key, value ->
        inputs.property(key,value)
    }
    dynamics.excludes.forEach{file->
        exclude(file)
    }
    filesMatching("fabric.mod.json") { expand(map) }
    filesMatching("META-INF/mods.toml") { expand(map) }
    filesMatching("META-INF/neoforge.mods.toml") { expand(map) }
    modMixins.getMixins(env.type).forEach { str->
        filesMatching(str) { expand(map) }
    }
}