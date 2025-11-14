package com.drathonix.rockbreakerplugin

class APIModInfo(val modid: String?, val curseSlug: String?, val rinthSlug: String?) {
    constructor () : this(null, null, null)
    constructor (modid: String) : this(modid, modid, modid)
    constructor (modid: String, slug: String) : this(modid, slug, slug)
}