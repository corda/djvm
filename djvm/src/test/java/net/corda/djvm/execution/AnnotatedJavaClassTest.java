package net.corda.djvm.execution;

import net.corda.djvm.TestBase;
import net.corda.djvm.JavaAnnotation;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;

import static net.corda.djvm.SandboxType.JAVA;
import static net.corda.djvm.messages.Severity.WARNING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class AnnotatedJavaClassTest extends TestBase {
    AnnotatedJavaClassTest() {
        super(JAVA);
    }

    @Test
    void testSandboxAnnotations() {
        assertThat(UserData.class.getAnnotation(JavaAnnotation.class)).isNotNull();

        parentedSandbox(WARNING, true, ctx -> {
            try {
                Class<?> sandboxClass = loadClass(ctx, UserData.class.getName()).getType();
                @SuppressWarnings("unchecked")
                Class<? extends Annotation> sandboxAnnotation
                    = (Class<? extends Annotation>) loadClass(ctx, JavaAnnotation.class.getName()).getType();
                assertThat(sandboxClass.getAnnotation(sandboxAnnotation)).isNotNull();
            } catch (Exception e) {
                fail(e);
            }
            return null;
        });
    }

    @SuppressWarnings("WeakerAccess")
    @JavaAnnotation
    static class UserData {}
}
