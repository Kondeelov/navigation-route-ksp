package com.kondee.navigationrouteprocessor

import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.symbol.Nullability
import com.kondee.navigationrouteprocessor.extension.getAnnotation
import com.kondee.navigationrouteprocessor.extension.hasAnnotation
import com.kondee.navigationrouteprocessor.extension.newLine
import com.kondee.navigationrouteprocessor.extension.plusAssign
import java.io.OutputStream

class NavigationScreenVisitor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>
) : KSVisitorVoid() {

    private lateinit var file: OutputStream

    override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit) {
        super.visitFunctionDeclaration(function, data)

        if (!function.isPublic()) {
            throw IllegalStateException("Function ${function.qualifiedName} is not public")
        }

        val mandatoryFields = mutableMapOf<String, KSName?>()
        val optionalFields = mutableMapOf<String, KSName?>()

        val packageName = function.packageName.asString()
        val functionName = function.simpleName.asString()
        val fileName = "${functionName}Route"

        var routeName = fileName

        function.annotations.getAnnotation(NavigationScreen::class).arguments.forEach {
            if (it.name?.asString() == "name") {
                val name = it.value as String
                if (name.isNotBlank()) {
                    routeName = name
                }
            }
        }

        file = codeGenerator.createNewFile(
            dependencies = Dependencies(false),
            packageName = packageName,
            fileName = fileName
        )

        val annotatedParameter = function.parameters.filter {
            it.annotations.hasAnnotation(NavigationArgument::class)
        }

        file.use { file ->
            file += "package $packageName"
            file.newLine(2)
            file += "import com.kondee.navigationrouteprocessor.Defaults"
            file.newLine(2)
            file += "class $fileName("
            file.newLine()

            annotatedParameter.forEach {
                val typeResolved = it.type.resolve()
                val typeString = typeResolved.declaration.qualifiedName
                val nameString = it.name?.asString() ?: ""
                val isNullable = typeResolved.nullability == Nullability.NULLABLE
                val nullableProperty = if (isNullable) "?" else ""
                if (!isNullable) {
                    mandatoryFields[nameString] = typeString
                    file += "\tprivate val $nameString: ${typeString?.asString()}$nullableProperty,"
                    file.newLine()
                } else {
                    optionalFields[nameString] = typeString
                }
            }

            file += ") {"
            file.newLine(2)

            optionalFields.forEach { field ->
                val name = field.key
                val type = field.value?.getShortName()

                file += "\tprivate var $name: $type? = null\n\n"

                file += "\tfun set${name.replaceFirstChar { it.uppercase() }}(v: $type) {\n"
                file += "\t\t$name = v\n"
                file += "\t}\n\n"
            }

//            Function to get the navigation route
            generateGetNavigationWithArgsFunction(file, routeName, mandatoryFields, optionalFields)

//            Function to get Route for composable
            generateCompanionObjectGetRouteFunction(file, routeName, mandatoryFields, optionalFields)

            file += "}"

            generateNavigationArgumentsWithExtension(file, mandatoryFields, optionalFields, functionName)

            generateEncodeDecodeFunction(file)
        }
    }

    private fun generateGetNavigationWithArgsFunction(
        file: OutputStream,
        routeName: String,
        mandatoryFields: MutableMap<String, KSName?>,
        optionalFields: MutableMap<String, KSName?>
    ) {
        file += "\tfun getNavigationWithArgs(): String {"
        file.newLine()

        val generatedRoute = buildString {
            append("\t\tval route = buildString {")
            appendLine()
            append("\t\t\tappend(\"$routeName")

            mandatoryFields.forEach {
                append("/")
                append(it.key)
                append("/")
                append("\${encode(${it.key})}")
            }

            append("\")")

            appendLine()
            appendLine()

            if (optionalFields.isNotEmpty()) {
                append("\t\t\tvar queries: MutableList<String> = mutableListOf()")

                appendLine()
                appendLine()

                optionalFields.forEach { entry ->
                    append("\t\t\tif (${entry.key} != null) {")
                    appendLine()
                    append("\t\t\t\tqueries.add(\"${entry.key}=\${encode(${entry.key})}\")")
                    appendLine()
                    append("\t\t\t}")

                    appendLine()
                }

                appendLine()

                append("\t\t\tval queryString = queries.joinToString(prefix = \"?\", separator = \"&\")")
                appendLine()
                append("\t\t\tappend(\"\$queryString\")")
            }

            appendLine()
            append("\t\t}")
        }

        file += generatedRoute

        file.newLine()

        file += "\t\treturn route"
        file.newLine()
        file += "\t}"
        file.newLine(2)
    }

    private fun generateCompanionObjectGetRouteFunction(
        file: OutputStream,
        routeName: String,
        mandatoryFields: MutableMap<String, KSName?>,
        optionalFields: MutableMap<String, KSName?>
    ) {
        file += "\tcompanion object {"
        file.newLine()
        file += "\t\tfun getRoute(): String {"
        file.newLine()

        val generatedRoute = buildString {
            append("\t\t\tval route = buildString {")
            appendLine()
            append("\t\t\t\tappend(\"$routeName")

            mandatoryFields.forEach {
                append("/")
                append(it.key)
                append("/")
                append("{${it.key}}")
            }

            append("\")")

            appendLine()
            appendLine()

            if (optionalFields.isNotEmpty()) {
                append("\t\t\t\tvar queries: MutableList<String> = mutableListOf()")

                appendLine()
                appendLine()

                optionalFields.onEachIndexed { index, entry ->
                    append("\t\t\t\t\tqueries.add(\"${entry.key}={${entry.key}}\")")
                    appendLine()
                }

                appendLine()

                append("\t\t\t\tval queryString = queries.joinToString(prefix = \"?\", separator = \"&\")")
                appendLine()
                append("\t\t\t\tappend(\"\$queryString\")")
            }

            appendLine()
            append("\t\t\t}")
        }

        file += generatedRoute

        file.newLine()

        file += "\t\t\treturn route"
        file.newLine()
        file += "\t\t}"
        file.newLine()
        file += "\t}"
        file.newLine()
    }

    private fun generateNavigationArgumentsWithExtension(
        file: OutputStream,
        mandatoryFields: MutableMap<String, KSName?>,
        optionalFields: MutableMap<String, KSName?>,
        functionName: String
    ) {
        file.newLine(2)

        if (mandatoryFields.isNotEmpty() && optionalFields.isNotEmpty()) {

            file += "data class ${functionName.replaceFirstChar { it.uppercase() }}Arguments("
            file.newLine()
            mandatoryFields.forEach {
                file += "\tval ${it.key}: ${it.value?.asString()},"
                file.newLine()
            }
            optionalFields.forEach {
                file += "\tval ${it.key}: ${it.value?.asString()}?, "
                file.newLine()
            }
            file += ")"

            file.newLine(2)

            file += "fun androidx.navigation.NavBackStackEntry.to${functionName.replaceFirstChar { it.uppercase() }}Arguments(): ${functionName.replaceFirstChar { it.uppercase() }}Arguments {"
            file.newLine()

            file += "\tval args = ${functionName.replaceFirstChar { it.uppercase() }}Arguments("
            file.newLine()
            mandatoryFields.forEach {
                file += "\t\t${it.key} = decode(this.arguments?.getString(\"${it.key}\")"
                if (it.value?.getShortName() != "String") {
                    file += ")"
                    file += "?.to${it.value?.getShortName()}OrNull()"
                } else {
                    file += ")"
                }
                file += " ?: Defaults.getDefaultValue(${it.value?.getShortName()}::class),"
                file.newLine()
            }
            optionalFields.forEach {
                file += "\t\t${it.key} = decode(this.arguments?.getString(\"${it.key}\")"
                if (it.value?.getShortName() != "String") {
                    file += ")"
                    file += "?.to${it.value?.getShortName()}OrNull()"
                } else {
                    file += ")"
                }
                file += ","
                file.newLine()
            }
            file += "\t)"
            file.newLine()

            file += "\treturn args"

            file.newLine()
            file += "}"
        }
    }

    private fun generateEncodeDecodeFunction(file: OutputStream) {

        file.newLine()

        file += "private fun <T> encode(s: T): String {\n" +
                "    val encodedString = android.util.Base64.encode(s.toString().toByteArray(), android.util.Base64.NO_WRAP).toString(Charsets.UTF_8)\n" +
                "    return encodedString\n" +
                "}"

        file.newLine(2)

        file += "private fun decode(s: String?): String? {\n" +
                "    if (s.isNullOrBlank()) {\n" +
                "        return null\n" +
                "    }\n" +
                "    val decodedString = android.util.Base64.decode(s.toByteArray(), android.util.Base64.NO_WRAP).toString(Charsets.UTF_8)\n" +
                "    return decodedString\n" +
                "}"
    }
}
