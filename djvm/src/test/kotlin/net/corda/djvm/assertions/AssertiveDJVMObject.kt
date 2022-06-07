package net.corda.djvm.assertions

import org.assertj.core.api.Assertions.assertThat

class AssertiveDJVMObject(private val djvmObj: Any) {

    fun hasClassName(className: String): AssertiveDJVMObject {
        assertThat(djvmObj::class.java.name).isEqualTo(className)
        return this
    }

    fun isAssignableFrom(clazz: Class<*>): AssertiveDJVMObject {
        assertThat(djvmObj::class.java.isAssignableFrom(clazz))
        return this
    }

    fun hasGetterValue(methodName: String, value: Any): AssertiveDJVMObject {
        assertThat(djvmObj::class.java.getMethod(methodName).invoke(djvmObj)).isEqualTo(value)
        return this
    }

    fun hasGetterNullValue(methodName: String): AssertiveDJVMObject {
        assertThat(djvmObj::class.java.getMethod(methodName).invoke(djvmObj)).isNull()
        return this
    }
}
