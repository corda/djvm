package net.corda.djvm.execution;

import com.example.testing.HappyObject;
import net.corda.djvm.TestBase;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;

import static net.corda.djvm.SandboxType.JAVA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class AnnotatedJavaPackageTest extends TestBase {
    AnnotatedJavaPackageTest() {
        super(JAVA);
    }

    @Test
    void testSandboxAnnotation() {
        sandbox(ctx -> {
            try {
                Class<?> sandboxClass = ctx.getClassLoader().toSandboxClass(HappyObject.class.getName());
                Package sandboxPackage = sandboxClass.getPackage();
                Annotation[] annotations = sandboxPackage.getAnnotations();
                assertThat(annotations).hasSize(1);
                assertThat(annotations[0].toString())
                    .matches("^\\Q@sandbox.net.corda.djvm.JavaAnnotation$1DJVM(value=\\E\"?Package Level\"?\\)$");
            } catch (Exception e) {
                fail(e);
            }
        });
    }
}
