package com.drathonix.rockbreakerplugin

import java.util.*
import java.util.function.Predicate

/**
 * APIs must have a maven source.
 * If the version range is not present then the API will not be used.
 * If modid is null then the API will not be declared as a dependency in uploads.
 * The enable condition determines whether the API will be used for this version.
 */
class APISource(
    val type: DepType,
    val modInfo: APIModInfo,
    val mavenLocation: String,
    val versionRange: Optional<VersionRange>,
    private val enableCondition: Predicate<APISource>
) {
    val enabled = this.enableCondition.test(this)
}