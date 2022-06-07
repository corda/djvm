package net.corda.djvm.assertions

import net.corda.djvm.references.ClassRepresentation
import net.corda.djvm.references.ClassHierarchy
import net.corda.djvm.references.Member
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.ThrowingConsumer

open class AssertiveClassHierarchyWithClass(
        hierarchy: ClassHierarchy,
        private val className: String
) : AssertiveClassHierarchy(hierarchy) {

    private val clazz: ClassRepresentation
        get() = hierarchy[className]!!

    fun withInterfaceCount(count: Int): AssertiveClassHierarchyWithClass {
        assertThat(clazz.interfaces.size)
                .`as`("$clazz.InterfaceCount($count)")
                .isEqualTo(count)
        return this
    }

    fun withInterface(name: String): AssertiveClassHierarchyWithClass {
        assertThat(clazz.interfaces).contains(name)
        return this
    }

    fun withMemberCount(count: Int): AssertiveClassHierarchyWithClass {
        assertThat(clazz.members.size)
                .`as`("MemberCount($className)")
                .isEqualTo(count)
        return this
    }

    fun withMember(name: String, descriptor: String): AssertiveClassHierarchyWithClassAndMember {
        assertThat(clazz.members.values as Iterable<Member>)
                .`as`("Member($className.$name:$descriptor")
                .anySatisfy(ThrowingConsumer {
                    assertThat(it.memberName).isEqualTo(name)
                    assertThat(it.descriptor).isEqualTo(descriptor)
                })
        val member = clazz.members.values.first {
            it.memberName == name && it.descriptor == descriptor
        }
        return AssertiveClassHierarchyWithClassAndMember(hierarchy, className, member)
    }

}
