package net.corda.djvm.rewiring

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.commons.Remapper

class KotlinMetadataVisitor(
    api: Int,
    av: AnnotationVisitor,
    private val remapper: Remapper
) : AnnotationVisitor(api, av) {
    companion object {
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
