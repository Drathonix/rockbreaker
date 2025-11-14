package com.drathonix.rockbreakerplugin

enum class DepType {
        API,

        // Optional API
        API_OPTIONAL {
            override fun isOptional(): Boolean {
                return true
            }
        },

        // Implementation
        IMPL,

        // Forge Runtime Library
        FRL {
            override fun includeInDepsList(): Boolean {
                return false
            }
        },

        // Implementation and Included in output jar.
        INCLUDE {
            override fun includeInDepsList(): Boolean {
                return false
            }
        };

        open fun isOptional(): Boolean {
            return false
        }

        open fun includeInDepsList(): Boolean {
            return true
        }
    }