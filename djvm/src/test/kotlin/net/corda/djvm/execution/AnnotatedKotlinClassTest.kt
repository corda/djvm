package net.corda.djvm.execution

import net.corda.djvm.KotlinAnnotation
import net.corda.djvm.KotlinLabel
import net.corda.djvm.SandboxType.KOTLIN
import net.corda.djvm.TestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.fail
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.functions
import kotlin.reflect.full.primaryConstructor

class AnnotatedKotlinClassTest : TestBase(KOTLIN) {
    // The @kotlin.Metadata annotation is unavailable for Kotlin < 1.3.
    @Suppress("unchecked_cast")
    private val kotlinMetadata: Class<out Annotation> = Class.forName("kotlin.Metadata") as Class<out Annotation>

    @Test
    fun testSandboxAnnotation() = sandbox {
        assertThat(UserKotlinData::class.findAnnotation<KotlinAnnotation>()).isNotNull

        @Suppress("unchecked_cast")
        val sandboxAnnotation = loadClass("sandbox.net.corda.djvm.KotlinAnnotation\$1DJVM").type as Class<out Annotation>
        val sandboxClass = loadClass<UserKotlinData>().type

        val annotationValue = sandboxClass.getAnnotation(sandboxAnnotation)
        assertThat(annotationValue.toString())
            .matches("^\\Q@sandbox.net.corda.djvm.KotlinAnnotation$1DJVM(value=\\E\"?Hello Kotlin!\"?\\)\$")
    }

    @Test
    @Suppress("unchecked_cast")
    fun testSandboxAnnotationWithEnumValue() = sandbox {
        val sandboxClass = loadClass("sandbox.net.corda.djvm.KotlinAnnotation\$1DJVM").type as Class<out Annotation>
        val sandboxAnnotation = loadClass("sandbox.kotlin.annotation.Retention\$1DJVM").type as Class<out Annotation>

        val retentionValue = sandboxClass.kotlin.annotations.filterIsInstance(sandboxAnnotation)
            .singleOrNull() ?: fail("No @Retention\$1DJVM annotation found")
        val policy = retentionValue::class.functions.firstOrNull { it.name == "value" }
            ?.call(retentionValue) as? String
            ?: fail("@Retention\$1DJVM has no value!")
        assertThat(policy).isEqualTo("RUNTIME")
    }

    @Test
    @Suppress("unchecked_cast")
    fun testSandboxAnnotationWithEnumArrayValue() = sandbox {
        val sandboxClass = loadClass("sandbox.net.corda.djvm.KotlinAnnotation\$1DJVM").type as Class<out Annotation>
        val sandboxAnnotation = loadClass("sandbox.kotlin.annotation.Target\$1DJVM").type as Class<out Annotation>

        val targetValue = sandboxClass.kotlin.annotations.filterIsInstance(sandboxAnnotation)
            .singleOrNull() ?: fail("No @Target\$1DJVM annotation found")
        val targets = targetValue::class.functions.firstOrNull { it.name == "allowedTargets" }
            ?.call(targetValue) as? Array<String>
            ?: fail("@Target\$1DJVM has no allowed targets!")
        assertThat(targets).containsExactlyInAnyOrder("CLASS", "FUNCTION", "PROPERTY", "FIELD")
    }

    @Test
    fun testPreservingKotlinMetadataAnnotation() = sandbox {
        val sandboxClass = loadClass<UserKotlinData>().type
        @Suppress("unchecked_cast")
        val sandboxMetadataClass = loadClass("sandbox.kotlin.Metadata\$1DJVM").type as Class<out Annotation>

        val metadata = sandboxClass.getAnnotation(kotlinMetadata)
        val sandboxMetadata = sandboxClass.getAnnotation(sandboxMetadataClass)

        with(AnnotationAssert(kotlinMetadata, sandboxMetadataClass)) {
            assertAll(
                { assertPropertyEquals("k", metadata, sandboxMetadata) },
                { assertPropertyEquals("bv", metadata, sandboxMetadata) },
                { assertPropertyEquals("mv", metadata, sandboxMetadata) },
                { assertPropertyEquals("d1", metadata, sandboxMetadata) },
                { assertPropertyEquals("d2", metadata, sandboxMetadata) },
                { assertPropertyEquals("pn", metadata, sandboxMetadata) },
                { assertPropertyEquals("xi", metadata, sandboxMetadata) },
                { assertPropertyEquals("xs", metadata, sandboxMetadata) }
            )
        }
    }

    class AnnotationAssert(private val type1: Class<out Annotation>, private val type2: Class<out Annotation>) {
        fun assertPropertyEquals(methodName: String, annotation1: Annotation, annotation2: Annotation) {
            val value1 = type1.getDeclaredMethod(methodName).invoke(annotation1)
            val value2 = type2.getDeclaredMethod(methodName).invoke(annotation2)
            when (value1) {
                is ByteArray -> assertArrayEquals(value1, value2 as ByteArray)
                is IntArray -> assertArrayEquals(value1, value2 as IntArray)
                is Array<*> -> assertArrayEquals(value1, value2 as Array<*>)
                else -> assertEquals(value1, value2)
            }
        }
    }

    @Test
    fun `test sandboxed class still knows its own primary constructor`() = sandbox {
        val sandboxClass = loadClass<UserKotlinData>().type
        val primaryConstructor = sandboxClass.kotlin.primaryConstructor ?: fail("Primary constructor missing!")

        val sandboxData = with(DJVM(classLoader)) {
            primaryConstructor.call(sandbox("Sandbox Magic!"), sandbox(123), 999L)
        }
        assertNotNull(sandboxData)
        assertEquals("sandbox.${UserKotlinData::class.java.name}", sandboxData::class.java.name)
        assertEquals("UserData: message='Sandbox Magic!', number=123, bigNumber=999", sandboxData.toString())
    }

    @Test
    fun `test reflection can fetch all annotations`() = sandbox {
        val sandboxClass = loadClass<UserKotlinData>().type
        val kotlinAnnotations = sandboxClass.kotlin.annotations.map { ann ->
            ann.annotationClass.qualifiedName
        }
        assertThat(kotlinAnnotations).containsExactlyInAnyOrder(
            "sandbox.net.corda.djvm.KotlinAnnotation\$1DJVM",
            "sandbox.kotlin.Metadata\$1DJVM"
        )

        val javaAnnotations = sandboxClass.annotations.map { ann ->
            ann.annotationClass.qualifiedName
        }
        assertThat(javaAnnotations).containsExactlyInAnyOrder(
            "sandbox.net.corda.djvm.KotlinAnnotation\$1DJVM",
            "sandbox.kotlin.Metadata\$1DJVM",
            "kotlin.Metadata"
        )
    }

    @Test
    fun `test reflection can fetch all stitched annotations`() = sandbox(
            visibleAnnotations = setOf(KotlinAnnotation::class.java)
    ) {
        val sandboxClass = loadClass<UserKotlinData>().type
        val kotlinAnnotations = sandboxClass.kotlin.annotations.map { ann ->
            ann.annotationClass.qualifiedName
        }
        assertThat(kotlinAnnotations).containsExactlyInAnyOrder(
            "sandbox.net.corda.djvm.KotlinAnnotation\$1DJVM",
            "net.corda.djvm.KotlinAnnotation",
            "sandbox.kotlin.Metadata\$1DJVM"
        )

        val javaAnnotations = sandboxClass.annotations.map { ann ->
            ann.annotationClass.qualifiedName
        }
        assertThat(javaAnnotations).containsExactlyInAnyOrder(
            "sandbox.net.corda.djvm.KotlinAnnotation\$1DJVM",
            "net.corda.djvm.KotlinAnnotation",
            "sandbox.kotlin.Metadata\$1DJVM",
            "kotlin.Metadata"
        )
    }

    @Test
    fun `test reflection can fetch all meta-annotations`() = sandbox {
        @Suppress("unchecked_cast")
        val sandboxAnnotation = loadClass("sandbox.net.corda.djvm.KotlinAnnotation\$1DJVM").type as Class<out Annotation>

        val kotlinAnnotations = sandboxAnnotation.kotlin.annotations.map { ann ->
            ann.annotationClass.qualifiedName
        }
        assertThat(kotlinAnnotations).containsExactlyInAnyOrder(
            "java.lang.annotation.Inherited",
            "java.lang.annotation.Retention",
            "java.lang.annotation.Documented",
            "java.lang.annotation.Target",
            "kotlin.annotation.MustBeDocumented",
            "sandbox.kotlin.Metadata\$1DJVM",
            "sandbox.kotlin.annotation.Retention\$1DJVM",
            "sandbox.kotlin.annotation.Target\$1DJVM"
        )

        val javaAnnotations = sandboxAnnotation.annotations.map { ann ->
            ann.annotationClass.qualifiedName
        }
        assertThat(javaAnnotations).containsExactlyInAnyOrder(
            "kotlin.annotation.MustBeDocumented",
            "sandbox.kotlin.Metadata\$1DJVM",
            "sandbox.kotlin.annotation.Retention\$1DJVM",
            "sandbox.kotlin.annotation.Target\$1DJVM",
            "java.lang.annotation.Documented",
            "java.lang.annotation.Inherited",
            "java.lang.annotation.Retention",
            "java.lang.annotation.Target"
        )
    }

    @Test
    fun `test reflection can fetch all stitched method annotations`() = sandbox(
        visibleAnnotations = setOf(KotlinAnnotation::class.java)
    ) {
        val sandboxClass = loadClass<UserKotlinData>().type
        val sandboxFunction = sandboxClass.kotlin.functions.single { it.name == "holdAnnotation" }
        val kotlinAnnotations = sandboxFunction.annotations.map { ann ->
            ann.annotationClass.qualifiedName
        }
        assertThat(kotlinAnnotations).containsExactlyInAnyOrder(
            "sandbox.net.corda.djvm.KotlinAnnotation\$1DJVM", "net.corda.djvm.KotlinAnnotation"
        )
    }

    @Test
    fun `test single repeatable annotation from outside sandbox`() = sandbox(
        visibleAnnotations = setOf(KotlinLabel::class.java)
    ) {
        assertThat(UserKotlinLabel::class.findAnnotation<KotlinLabel>()).isNotNull

        val sandboxClass = loadClass<UserKotlinLabel>().type
        val annotations = sandboxClass.kotlin.annotations.groupByTo(LinkedHashMap()) { ann ->
            ann.annotationClass.qualifiedName?.startsWith("sandbox.")
        }

        val sandboxAnnotations = annotations[true] ?: fail("No sandbox annotations found")
        assertEquals(2, sandboxAnnotations.size)
        val kotlinLabel = sandboxAnnotations.map(Annotation::toString)
            .find { it.matches("^\\Q@sandbox.net.corda.djvm.KotlinLabel$1DJVM(name=\\E\"?ZERO\"?\\)\$".toRegex()) }
        assertNotNull(kotlinLabel, "@KotlinLabel annotation missing")

        val kotlinAnnotations = annotations[false] ?: fail("No Kotlin annotations found")
        assertEquals(1, kotlinAnnotations.size)
        assertThat(kotlinAnnotations[0].toString())
            .matches("^\\Q@net.corda.djvm.KotlinLabel(name=\\E\"?ZERO\"?\\)$")
    }

    @Test
    fun `test reflection can fetch repeatable`() = sandbox {
        @Suppress("unchecked_cast")
        val sandboxAnnotation = loadClass<KotlinLabel>().type as Class<out Annotation>

        val kotlinAnnotations = sandboxAnnotation.kotlin.annotations.map { ann ->
            ann.annotationClass.qualifiedName
        }
        assertThat(kotlinAnnotations).containsExactlyInAnyOrder(
            "kotlin.annotation.MustBeDocumented",
            "sandbox.kotlin.Metadata\$1DJVM",
            "sandbox.kotlin.annotation.Retention\$1DJVM",
            "sandbox.kotlin.annotation.Target\$1DJVM",
            "sandbox.java.lang.annotation.Retention\$1DJVM",
            "sandbox.java.lang.annotation.Target\$1DJVM",
            "sandbox.kotlin.annotation.Repeatable\$1DJVM"
        )

        val javaAnnotations = sandboxAnnotation.annotations.map { ann ->
            ann.annotationClass.qualifiedName
        }
        assertThat(javaAnnotations).containsExactlyInAnyOrder(
            "kotlin.annotation.MustBeDocumented",
            "kotlin.Metadata",
            "sandbox.kotlin.annotation.Retention\$1DJVM",
            "sandbox.kotlin.annotation.Target\$1DJVM",
            "sandbox.java.lang.annotation.Retention\$1DJVM",
            "sandbox.java.lang.annotation.Target\$1DJVM",
            "sandbox.kotlin.annotation.Repeatable\$1DJVM",
            "sandbox.kotlin.Metadata\$1DJVM",
            "java.lang.annotation.Documented"
        )
    }
}

@KotlinAnnotation("Hello Kotlin!")
@Suppress("unused")
class UserKotlinData(val message: String, val number: Int?, val bigNumber: Long) {
    constructor(message: String, number: Int) : this(message, number, 0)
    constructor(message: String, bigNumber: Long) : this(message, 0, bigNumber)
    constructor(message: String) : this(message, null, 0)

    @KotlinAnnotation
    fun holdAnnotation() {}

    override fun toString(): String = "UserData: message='$message', number=$number, bigNumber=$bigNumber"
}

@KotlinLabel("ZERO")
class UserKotlinLabel
