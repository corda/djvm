package net.corda.djvm.execution;

import net.corda.djvm.TestBase;
import net.corda.djvm.TypedTaskFactory;
import net.corda.djvm.WithJava;
import org.junit.jupiter.api.Test;
import sun.security.x509.AVA;
import sun.security.x509.X500Name;

import javax.security.auth.x500.X500Principal;
import java.util.function.Function;

import static net.corda.djvm.SandboxType.JAVA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class X500Tests extends TestBase {
    X500Tests() {
        super(JAVA);
    }

    @Test
    void testCreateX500Principal() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                String result = WithJava.run(taskFactory, CreateX500Principal.class, "CN=Example,O=Corda,C=GB");
                assertEquals("cn=example,o=corda,c=gb", result);
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    public static class CreateX500Principal implements Function<String, String> {
        public String apply(String input) {
            X500Principal principal = new X500Principal(input);
            return principal.getName(X500Principal.CANONICAL);
        }
    }

    @Test
    void testX500PrincipalToX500Name() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                String[] result = WithJava.run(taskFactory, X500PrincipalToX500Name.class, "CN=Example,O=Corda,C=GB");
                assertThat(result).isEqualTo(new String[]{
                    "c=gb", "cn=example", "o=corda"
                });
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    public static class X500PrincipalToX500Name implements Function<String, String[]> {
        public String[] apply(String input) {
            X500Name name = X500Name.asX500Name(new X500Principal(input));
            return name.allAvas().stream().map(AVA::toRFC2253CanonicalString).sorted().toArray(String[]::new);
        }
    }

    @Test
    void testX500NameToX500Principal() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                String result = WithJava.run(taskFactory, X500NameToX500Principal.class, "CN=Example,O=Corda,C=GB");
                assertEquals("cn=example,o=corda,c=gb", result);
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    public static class X500NameToX500Principal implements Function<String, String> {
        public String apply(String input) {
            X500Principal principal = X500Name.asX500Name(new X500Principal(input)).asX500Principal();
            return principal.getName(X500Principal.CANONICAL);
        }
    }
}
