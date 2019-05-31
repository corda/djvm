package net.corda.djvm.code.instructions

import org.objectweb.asm.Label

/**
 * Try-catch block.
 *
 * @param typeName The type of the exception being caught.
 * @param handler The label of the exception handler.
 */
class TryCatchBlock(
        typeName: String,
        handler: Label
) : TryBlock(handler, typeName)
