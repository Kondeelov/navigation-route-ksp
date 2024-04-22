package com.kondee.navigationrouteprocessor

@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD)
annotation class NavigationArgument(
    val mandatory: Array<String> = [],
    val optional: Array<String> = [],
) {

    companion object {
        const val MANDATORY_KEY = "mandatory"
        const val OPTIONAL_KEY = "optional"
    }
}