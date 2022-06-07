package net.corda.djvm.assertions

import net.corda.djvm.references.ClassHierarchy
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.ThrowingConsumer

open class AssertiveClassHierarchy(protected val hierarchy: ClassHierarchy) {

    fun hasCount(count: Int): AssertiveClassHierarchy {
        assertThat(hierarchy.names.size)
                .`as`("Number of classes")
                .isEqualTo(count)
        return this
    }

    fun hasClass(name: String): AssertiveClassHierarchyWithClass {
        assertThat(hierarchy.names)
            .`as`("Class($name)")
            .anySatisfy(ThrowingConsumer {
                assertThat(it).isEqualTo(name)
            })
        return AssertiveClassHierarchyWithClass(hierarchy, name)
    }

}
