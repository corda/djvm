@file:JvmName("SewingKit")
package net.corda.djvm.rewiring

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Supplier

/**
 * Visits a simple annotation while accumulating the names, types
 * and values of its fields. Does NOT support fields which are
 * themselves annotations because none of the annotations that it
 * needs to stitch have any such fields.
 *
 * When the source annotation has been completely visited, this
 * visitor invokes its [complete] callback.
 */
class AnnotationStitcher(
    api: Int,
    av: AnnotationVisitor,
    private val descriptor: String,
    private val complete: Consumer<AnnotationStitcher>
) : AnnotationAccumulator(api, av) {

    override fun visitEnd() {
        super.visitEnd()
        complete.accept(this)
    }

    fun accept(cv: ClassVisitor, transform: Function<String, String>) {
        accept(transform, Supplier { cv.visitAnnotation(transform.apply(descriptor), true) })
    }

    fun accept(mv: MethodVisitor, transform: Function<String, String>) {
        accept(transform, Supplier { mv.visitAnnotation(transform.apply(descriptor), true) })
    }
}

abstract class AnnotationAccumulator(api: Int, av: AnnotationVisitor) : AnnotationVisitor(api, av) {
    private val entries = mutableListOf<AnnotationElement>()

    final override fun visit(name: String?, value: Any?) {
        super.visit(name, value)
        entries.add(AnnotationValue(name, value))
    }

    final override fun visitEnum(name: String?, descriptor: String, value: String) {
        super.visitEnum(name, descriptor, value)
        entries.add(AnnotationEnum(name, descriptor, value))
    }

    final override fun visitArray(name: String): AnnotationVisitor? {
        val array = super.visitArray(name) ?: return null
        return AnnotationArray(api, array, name).apply {
            entries.add(this)
        }
    }

    fun accept(transform: Function<String, String>, visitor: Supplier<AnnotationVisitor?>) {
        visitor.get()?.run {
            for (entry in entries) {
                entry.accept(this, transform)
            }
            visitEnd()
        }
    }
}

private interface AnnotationElement {
    val name: String?

    fun accept(av: AnnotationVisitor, transform: Function<String, String>)
}

private class AnnotationArray(
    api: Int,
    av: AnnotationVisitor,
    override val name: String
) : AnnotationAccumulator(api, av), AnnotationElement {

    override fun accept(av: AnnotationVisitor, transform: Function<String, String>) {
        accept(transform, Supplier { av.visitArray(name) })
    }
}

private class AnnotationValue(
    override val name: String?,
    val value: Any?
) : AnnotationElement {

    override fun accept(av: AnnotationVisitor, transform: Function<String, String>) {
        av.visit(name, value)
    }
}

private class AnnotationEnum(
    override val name: String?,
    val descriptor: String,
    val value: String
) : AnnotationElement {

    override fun accept(av: AnnotationVisitor, transform: Function<String, String>) {
        av.visitEnum(name, transform.apply(descriptor), value)
    }
}
