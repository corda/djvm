package net.corda.djvm.assertions

import net.corda.djvm.TestBase
import net.corda.djvm.code.Instruction
import net.corda.djvm.costing.RuntimeCostSummary
import net.corda.djvm.messages.MessageCollection
import net.corda.djvm.messages.Message
import net.corda.djvm.references.ClassRepresentation
import net.corda.djvm.references.ClassHierarchy
import net.corda.djvm.references.Member
import net.corda.djvm.references.ReferenceMap
import net.corda.djvm.rewiring.LoadedClass
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.IterableAssert
import org.assertj.core.api.ListAssert
import org.assertj.core.api.ThrowableAssertAlternative
import org.assertj.core.api.ThrowingConsumer

/**
 * Extensions used for testing.
 */
object AssertionExtensions {

    @JvmStatic
    fun assertThat(loadedClass: LoadedClass) =
            AssertiveClassWithByteCode(loadedClass)

    @JvmStatic
    fun assertThat(costs: RuntimeCostSummary) =
            AssertiveRuntimeCostSummary(costs)

    @JvmStatic
    fun assertThat(messages: MessageCollection) =
            AssertiveMessages(messages)

    @JvmStatic
    fun assertThat(hierarchy: ClassHierarchy) =
            AssertiveClassHierarchy(hierarchy)

    @JvmStatic
    fun assertThat(references: ReferenceMap) =
            AssertiveReferenceMap(references)

    fun assertThatDJVM(obj: Any) = AssertiveDJVMObject(obj)

    inline fun <reified T> IterableAssert<ClassRepresentation>.hasClass(): IterableAssert<ClassRepresentation> = this
            .`as`("HasClass(${T::class.java.name})")
            .anySatisfy(ThrowingConsumer {
                assertThat(it.name).isEqualTo(TestBase.nameOf<T>())
            })

    fun IterableAssert<Member>.hasMember(name: String, descriptor: String): IterableAssert<Member> = this
            .`as`("HasMember($name:$descriptor)")
            .anySatisfy(ThrowingConsumer {
                assertThat(it.memberName).isEqualTo(name)
                assertThat(it.descriptor).isEqualTo(descriptor)
            })

    inline fun <reified TInstruction : Instruction> IterableAssert<Pair<Member, Instruction>>.hasInstruction(
            methodName: String, description: String, noinline predicate: ((TInstruction) -> Unit)? = null
    ): IterableAssert<Pair<Member, Instruction>> = this
            .`as`("Has(${TInstruction::class.java.name} in $methodName(), $description)")
            .anySatisfy(ThrowingConsumer {
                assertThat(it.first.memberName).isEqualTo(methodName)
                assertThat(it.second).isInstanceOf(TInstruction::class.java)
                predicate?.invoke(it.second as TInstruction)
            })

    fun <T : Throwable> ThrowableAssertAlternative<T>.withProblem(message: String): ThrowableAssertAlternative<T> = this
            .withStackTraceContaining(message)

    fun ListAssert<Message>.withMessage(message: String): ListAssert<Message> = this
            .`as`("HasMessage($message)")
            .anySatisfy(ThrowingConsumer {
                assertThat(it.message).contains(message)
            })

}
