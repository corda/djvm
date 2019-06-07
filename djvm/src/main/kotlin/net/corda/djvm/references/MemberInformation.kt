package net.corda.djvm.references

/**
 * Representation of a class member.
 *
 * @property className The name of the owning class.
 * @property memberName The name of the member.
 * @property descriptor The descriptor of the member.
 * @property reference The absolute name of the referenced member.
 */
interface MemberInformation {
    val className: String
    val memberName: String
    val descriptor: String
    @JvmDefault
    val reference: String get() = "$className.$memberName:$descriptor"

    @JvmDefault
    val isMethod: Boolean get() = descriptor.startsWith('(')
}
