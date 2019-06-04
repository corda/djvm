package net.corda.djvm.code.instructions

import org.objectweb.asm.Label

/**
 * Try-finally block.
 *
 * @param handler The handler for the finally-block.
 */
class TryFinallyBlock(
        handler: Label
) : TryBlock(handler, "")
