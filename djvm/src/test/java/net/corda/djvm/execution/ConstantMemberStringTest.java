package net.corda.djvm.execution;

import net.corda.djvm.TestBase;
import net.corda.djvm.TypedTaskFactory;
import net.corda.djvm.WithJava;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static net.corda.djvm.SandboxType.JAVA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class ConstantMemberStringTest extends TestBase {
    private static final String SECRET_MESSAGE = "Very, very secret!";
    private static final String MESSAGE = "Hello Sandbox!";

    ConstantMemberStringTest() {
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
            return new HasOnlyPrivateMember().getHidden();
        }
    }

    @SuppressWarnings({"FieldCanBeLocal", "WeakerAccess"})
    public static class HasOnlyPrivateMember {
        private final String hidden = SECRET_MESSAGE;

        public String getHidden() {
            return hidden;
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
            return new HasOnlyPublicMember().member;
        }
    }

    public static class HasOnlyPublicMember {
        public final String member = MESSAGE;
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
            HasPublicAndPrivateMembers object = new HasPublicAndPrivateMembers(message);
            return new String[] { object.member, object.getHidden() };
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class HasPublicAndPrivateMembers {
        public final String member = MESSAGE;
        private final String hidden;

        public HasPublicAndPrivateMembers(String hidden) {
            this.hidden = hidden;
        }

        public String getHidden() {
            return hidden;
        }
    }
}
