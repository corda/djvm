package net.corda.djvm.references

/**
 * Reference to class member.
 *
 * @property className Class name of the owner.
 * @property memberName Name of the referenced field or method.
 * @property descriptor The descriptor of the field or method.
 */
data class MemberReference(
        override val className: String,
        override val memberName: String,
        override val descriptor: String
) : EntityReference, MemberInformation
