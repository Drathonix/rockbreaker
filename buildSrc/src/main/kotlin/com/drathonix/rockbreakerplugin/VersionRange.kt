package com.drathonix.rockbreakerplugin

class VersionRange(val min: String, val max: String) {
    fun asForgelike(): String {
        return "${if (min.isEmpty()) "(" else "["}${min},${max}${if (max.isEmpty()) ")" else "]"}"
    }

    fun asFabric(): String {
        var out = ""
        if (min.isNotEmpty()) {
            out += ">=$min"
        }
        if (max.isNotEmpty()) {
            if (out.isNotEmpty()) {
                out += " "
            }
            out += "<=$max"
        }
        return out
    }
}