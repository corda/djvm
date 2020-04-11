package net.corda.djvm.rewiring

import net.corda.djvm.analysis.AnalysisConfiguration
import org.objectweb.asm.Type
import org.objectweb.asm.commons.Remapper

class SyntheticRemapper(private val configuration: AnalysisConfiguration) : Remapper() {
    private val classResolver = configuration.classResolver
    private val syntheticResolver = configuration.syntheticResolver

    fun mapAnnotationName(internalName: String): String {
        val resolvedName = classResolver.resolve(internalName)
        return syntheticResolver.getRealAnnotationName(resolvedName)
    }

    fun mapAnnotation(type: Type): Type {
        val mappedName = mapAnnotationName(type.internalName)
        return Type.getObjectType(mappedName)
    }

    fun mapAnnotationDesc(descriptor: String): String {
        return mapAnnotation(Type.getType(descriptor)).descriptor
    }

    override fun map(internalName: String): String {
        return when(internalName) {
            "java/lang/String", "java/lang/Class" -> internalName
            else -> {
                val header = configuration.getSourceHeader(internalName)
                when {
                    header.isAnnotation -> mapAnnotationName(internalName)
                    header.isEnum -> "java/lang/String"
                    else -> internalName
                }
            }
        }
    }
}
