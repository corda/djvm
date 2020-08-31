package net.corda.djvm.code

/**
 * A definition provider is a hook for [net.corda.djvm.code.impl.ClassMutator],
 * from where one can modify the name and meta-data of processed classes and
 * class members.
 */
interface DefinitionProvider