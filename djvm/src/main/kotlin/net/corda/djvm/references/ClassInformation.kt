package net.corda.djvm.references

interface ClassInformation {
    val isInterface: Boolean
    val hasObjectAsSuperclass: Boolean
}
