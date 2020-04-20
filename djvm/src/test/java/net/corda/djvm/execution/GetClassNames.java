package net.corda.djvm.execution;

import java.util.function.Function;

public class GetClassNames implements Function<String, String[]> {
    @Override
    public String[] apply(String unused) {
        return new String[]{
            GetClassNames.class.getName(),
            GetClassNames.class.getSimpleName(),
            GetClassNames.class.getCanonicalName(),
            GetClassNames.class.getTypeName(),
            GetClassNames.class.toGenericString(),
            GetClassNames.class.toString()
        };
    }
}
