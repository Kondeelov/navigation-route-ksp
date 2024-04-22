package com.kondee.navigationrouteprocessor.extension

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.Nullability
import com.google.devtools.ksp.symbol.Variance
import java.io.OutputStream
import java.io.OutputStreamWriter
import kotlin.reflect.KClass

// return true if a particular modifier is included in a list
fun Collection<Modifier>.containsIgnoreCase(name: String): Boolean {
    return stream().anyMatch { it.name.equals(name, true) }
}

fun Sequence<KSAnnotation>.hasAnnotation(target: KClass<*>): Boolean {
    for (element in this) if (element.shortName.asString() == target.java.simpleName) return true
    return false
}

fun Sequence<KSAnnotation>.getAnnotation(target: KClass<*>): KSAnnotation {
    return getAnnotationIfExist(target)
        ?: throw NoSuchElementException("Sequence contains no element matching the predicate.")
}

private fun Sequence<KSAnnotation>.getAnnotationIfExist(target: KClass<*>): KSAnnotation? {
    for (element in this) if (element.shortName.asString() == target.java.simpleName) return element
    return null
}

fun visitTypeArguments(typeArguments: List<KSTypeArgument>, error: (String, KSNode) -> Unit): String {
    var result = ""
    if (typeArguments.isNotEmpty()) {
        result += "<"
        typeArguments.forEach { arg ->
            result += "${visitTypeArgument(arg, error)}, "
        }
        result += ">"
    }
    return result
}

private fun visitTypeArgument(typeArgument: KSTypeArgument, error: (String, KSNode) -> Unit): String {
    var result = ""
    when (val variance: Variance = typeArgument.variance) {
        Variance.STAR -> result += "*" // <*>
        Variance.COVARIANT, Variance.CONTRAVARIANT -> result += "${variance.label} " // <out ...>, <in ...>
        Variance.INVARIANT -> {} /*Do nothing.*/
    }
    if (result.endsWith("*").not()) {
        val resolvedType = typeArgument.type?.resolve()
        result += resolvedType?.declaration?.qualifiedName?.asString() ?: run {
            error("Invalid type argument", typeArgument)
        }

        // Generating nested generic parameters if any.
        val genericArguments = typeArgument.type?.element?.typeArguments ?: emptyList()
        result += visitTypeArguments(genericArguments, error)

        // Handling nullability.
        result += if (resolvedType?.nullability == Nullability.NULLABLE) "?" else ""
    }
    return result
}

fun OutputStream.newLine(
    count: Int = 1
) {
    for (i in 0 until count) {
        this.write("\n".toByteArray())
    }
}

operator fun OutputStream.plusAssign(text: String) {
    return this.write(text.toByteArray())
}

operator fun OutputStreamWriter.plusAssign(text: String) {
    return this.write(text)
}