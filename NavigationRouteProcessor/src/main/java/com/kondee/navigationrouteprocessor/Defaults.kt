package com.kondee.navigationrouteprocessor

import org.jetbrains.annotations.NotNull
import kotlin.reflect.KClass

object Defaults {

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getDefaultValue(clazz: KClass<T>): T {
        val defaultValue = when (clazz) {
            String::class -> {
                "" as T
            }
            Int::class -> {
                0 as T
            }
            Long::class -> {
                0L as T
            }
            Float::class -> {
                0f as T
            }
            Double::class -> {
                0.0 as T
            }
            Boolean::class -> {
                false as T
            }
            else -> {
                Unit as T
            }
        }
        return defaultValue
    }
}