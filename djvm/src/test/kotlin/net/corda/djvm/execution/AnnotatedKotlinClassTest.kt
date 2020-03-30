package net.corda.djvm.execution

import net.corda.djvm.KotlinAnnotation
import net.corda.djvm.KotlinLabel
import net.corda.djvm.SandboxType.KOTLIN
import net.corda.djvm.TestBase
import net.corda.djvm.TypedTaskFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.fail
import java.util.function.Function
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor

class AnnotatedKotlinClassTest : TestBase(KOTLIN) {
    // The @kotlin.Metadata annotation is unavailable for Kotlin < 1.3.
    @Suppress("unchecked_cast")
    private val kotlinMetadata: Class<out Annotation> = Class.forName("kotlin.Metadata") as Class<out Annotation>

    @Test
    fun testSandboxAnnotation() = sandbox(
        visibleAnnotations = emptySet(),
        sandboxOnlyAnnotations = setOf("net.corda.djvm.*")
    ) {
        assertThat(UserKotlinData::class.findAnnotation<KotlinAnnotation>()).isNotNull

        @Suppress("unchecked_cast")
        val sandboxAnnotation = loadClass<KotlinAnnotation>().type as Class<out Annotation>
        val sandboxClass = loadClass<UserKotlinData>().type

        val annotationValue = sandboxClass.getAnnotation(sandboxAnnotation)
        assertThat(annotationValue.toString())
            .matches("^\\Q@sandbox.net.corda.djvm.KotlinAnnotation(value=\\E\"?Hello Kotlin!\"?\\)\$")
    }

    @Disabled("This test needs java.lang.Class.getEnclosingMethod() inside the sandbox.")
    @Test
    fun testAnnotationInsideSandbox() = sandbox {
        val taskFactory: TypedTaskFactory = classLoader.createTypedTaskFactory()
        val result = taskFactory.create(ReadAnnotation::class.java).apply(null)
        assertThat(result)
            .matches("^\\Q@sandbox.net.corda.djvm.KotlinAnnotation(value=\\E\"?Hello Kotlin!\"?\\)\$")
    }

    class ReadAnnotation : Function<Any?, String> {
        override fun apply(t: Any?): String {
            return UserKotlinData::class.findAnnotation<KotlinAnnotation>().toString()
        }
    }

    @Test
    fun testPreservingKotlinMetadataAnnotation() = sandbox {
        val sandboxClass = loadClass<UserKotlinData>().type
        @Suppress("unchecked_cast")
        val sandboxMetadataClass = loadClass(kotlinMetadata.name).type as Class<out Annotation>

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
    fun `test reflection can fetch all annotations`() = sandbox(
        visibleAnnotations = emptySet(),
        sandboxOnlyAnnotations = setOf("net.corda.djvm.**")
    ) {
        val sandboxClass = loadClass<UserKotlinData>().type
        val kotlinAnnotations = sandboxClass.kotlin.annotations.map { ann ->
            ann.annotationClass.qualifiedName
        }
        assertThat(kotlinAnnotations).containsExactlyInAnyOrder(
            "sandbox.net.corda.djvm.KotlinAnnotation",
            "sandbox.kotlin.Metadata"
        )

        val javaAnnotations = sandboxClass.annotations.map { ann ->
            ann.annotationClass.qualifiedName
        }
        assertThat(javaAnnotations).containsExactlyInAnyOrder(
            "sandbox.net.corda.djvm.KotlinAnnotation",
            "sandbox.kotlin.Metadata",
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
            "sandbox.net.corda.djvm.KotlinAnnotation",
            "net.corda.djvm.KotlinAnnotation",
            "sandbox.kotlin.Metadata"
        )

        val javaAnnotations = sandboxClass.annotations.map { ann ->
            ann.annotationClass.qualifiedName
        }
        assertThat(javaAnnotations).containsExactlyInAnyOrder(
            "sandbox.net.corda.djvm.KotlinAnnotation",
            "net.corda.djvm.KotlinAnnotation",
            "sandbox.kotlin.Metadata",
            "kotlin.Metadata"
        )
    }

    @Test
    fun `test reflection can fetch all meta-annotations`() = sandbox {
        @Suppress("unchecked_cast")
        val sandboxAnnotation = loadClass<KotlinAnnotation>().type as Class<out Annotation>

        val kotlinAnnotations = sandboxAnnotation.kotlin.annotations.map { ann ->
            ann.annotationClass.qualifiedName
        }
        assertThat(kotlinAnnotations).containsExactlyInAnyOrder(
            "java.lang.annotation.Inherited",
            "kotlin.annotation.MustBeDocumented",
            "kotlin.annotation.Retention",
            "kotlin.annotation.Target",
            "sandbox.kotlin.Metadata",
            "sandbox.kotlin.annotation.MustBeDocumented"
        )

        val javaAnnotations = sandboxAnnotation.annotations.map { ann ->
            ann.annotationClass.qualifiedName
        }
        assertThat(javaAnnotations).containsExactlyInAnyOrder(
            "kotlin.annotation.Retention",
            "kotlin.annotation.Target",
            "kotlin.annotation.MustBeDocumented",
            "kotlin.Metadata",
            "sandbox.kotlin.annotation.MustBeDocumented",
            "sandbox.kotlin.Metadata",
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
        val sandboxFunction = sandboxClass.kotlin.declaredFunctions.single { it.name == "holdAnnotation" }
        val kotlinAnnotations = sandboxFunction.annotations.map { ann ->
            ann.annotationClass.qualifiedName
        }
        assertThat(kotlinAnnotations).containsExactlyInAnyOrder(
            "sandbox.net.corda.djvm.KotlinAnnotation", "net.corda.djvm.KotlinAnnotation"
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
            .find { it.matches("^\\Q@sandbox.net.corda.djvm.KotlinLabel(name=\\E\"?ZERO\"?\\)\$".toRegex()) }
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
            "kotlin.annotation.Repeatable",
            "kotlin.annotation.MustBeDocumented",
            "kotlin.annotation.Retention",
            "kotlin.annotation.Target",
            "sandbox.kotlin.Metadata",
            "sandbox.kotlin.annotation.Repeatable",
            "sandbox.kotlin.annotation.MustBeDocumented"
        )

        val javaAnnotations = sandboxAnnotation.annotations.map { ann ->
            ann.annotationClass.qualifiedName
        }
        assertThat(javaAnnotations).containsExactlyInAnyOrder(
            "kotlin.annotation.Retention",
            "kotlin.annotation.Target",
            "kotlin.annotation.MustBeDocumented",
            "kotlin.annotation.Repeatable",
            "kotlin.Metadata",
            "sandbox.kotlin.annotation.MustBeDocumented",
            "sandbox.kotlin.annotation.Repeatable",
            "sandbox.kotlin.Metadata",
            "java.lang.annotation.Documented",
            "java.lang.annotation.Retention",
            "java.lang.annotation.Target"
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
