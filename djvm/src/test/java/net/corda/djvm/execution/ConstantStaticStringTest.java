package net.corda.djvm.execution;

import net.corda.djvm.TestBase;
import net.corda.djvm.TypedTaskFactory;
import net.corda.djvm.WithJava;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static net.corda.djvm.SandboxType.JAVA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class ConstantStaticStringTest extends TestBase {
    private static final String SECRET_MESSAGE = "Very, very secret!";
    private static final String MESSAGE = "Hello Sandbox!";

    ConstantStaticStringTest() {
        super(JAVA);
    }

    @Test
    void testReadOnlyPrivateMember() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                String result = WithJava.run(taskFactory, ReadOnlyPrivateMember.class, null);
                assertThat(result).isEqualTo(SECRET_MESSAGE);
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    public static class ReadOnlyPrivateMember implements Function<String, String> {
        @Override
        public String apply(String unused) {
            return HasOnlyPrivateMember.getHidden();
        }
    }

    @SuppressWarnings({"FieldCanBeLocal", "WeakerAccess"})
    public static class HasOnlyPrivateMember {
        private static final String HIDDEN = SECRET_MESSAGE;

        public static String getHidden() {
            return HIDDEN;
        }
    }

    @Test
    void testReadPublicMember() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                String result = WithJava.run(taskFactory, ReadOnlyPublicMember.class, null);
                assertThat(result).isEqualTo(MESSAGE);
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    public static class ReadOnlyPublicMember implements Function<String, String> {
        @Override
        public String apply(String unused) {
            return HasOnlyPublicMember.MEMBER;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class HasOnlyPublicMember {
        public static final String MEMBER = MESSAGE;
    }

    @Test
    void testReadPublicAndPrivateMembers() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                String[] result = WithJava.run(taskFactory, ReadPublicAndPrivateMembers.class, SECRET_MESSAGE);
                assertThat(result).containsExactly(
                    MESSAGE, SECRET_MESSAGE
                );
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    public static class ReadPublicAndPrivateMembers implements Function<String, String[]> {
        @Override
        public String[] apply(String message) {
            return new String[] {
                HasPublicAndPrivateMembers.MEMBER,
                HasPublicAndPrivateMembers.getHidden()
            };
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class HasPublicAndPrivateMembers {
        public static final String MEMBER = MESSAGE;
        private static final String HIDDEN = SECRET_MESSAGE;

        public static String getHidden() {
            return HIDDEN;
        }
    }
}
