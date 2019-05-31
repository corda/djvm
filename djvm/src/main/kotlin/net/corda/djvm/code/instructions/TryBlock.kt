package net.corda.djvm.code.instructions

import org.objectweb.asm.Label

/**
 * Try block.
 *
 * @property handler The label of the exception handler.
 * @property typeName The type of the exception being caught.
 */
open class TryBlock(
        val handler: Label,
        val typeName: String
) : NoOperationInstruction()