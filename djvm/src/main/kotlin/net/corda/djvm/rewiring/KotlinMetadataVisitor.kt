package net.corda.djvm.rewiring

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.commons.Remapper

/**
 * Maps the descriptor strings inside [Metadata.d2] into the sandbox.*
 * package space. This means that Kotlin reflection will now generate
 * the new sandboxed method and field signatures when it "rehydrates"
 * the ProtoBuf data inside the [Metadata.d1] field.
 *
 * From an end-user perspective, this realigns Kotlin reflection with
 * Java reflection and allows Kotlin reflection to (e.g.) identify and
 * invoke the new class's primary constructor.
 */
class KotlinMetadataVisitor(
    api: Int,
    av: AnnotationVisitor,
    private val remapper: Remapper
) : AnnotationVisitor(api, av) {
    companion object {
        @JvmField
        val INTERNAL_NAME = "L([^.;\\[]++)+;".toRegex()
    }

    override fun visitArray(name: String): AnnotationVisitor? {
        return super.visitArray(name)?.let {
            if (name == "d2") KotlinMetadataTransformer(it) else it
        }
    }

    private inner class KotlinMetadataTransformer(av: AnnotationVisitor) : AnnotationVisitor(api, av) {
        override fun visit(name: String?, value: Any?) {
            val newValue = if (value !is String || !INTERNAL_NAME.containsMatchIn(value)) {
                value
            } else if (value.startsWith('(')) {
                remapper.mapMethodDesc(value)
            } else {
                remapper.mapDesc(value)
            }
            super.visit(name, newValue)
        }
    }
}
