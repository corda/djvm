# The Deterministic JVM Library.

## Introduction.

```xml
<dependency>
    <groupId>net.corda.djvm</groupId>
    <artifactId>djvm</artifactId>
    <version>${version}</version>
</dependency>
```

The deterministic JVM (DJVM) implements a Java 8 ClassLoader containing classes whose inputs are provided
solely by the user. Operations built from these classes will therefore be "pure", i.e. their outputs will
be determined by their inputs alone and cannot be influenced by factors such as hardware random number
generators, system clocks, network packets or the contents of the local filesystem.

## Creating a Sandbox.

A sandbox is ultimately equivalent to an instance of `SandboxClassLoader`. We create one of these in stages.

### UserSource.

The DJVM reads "source" byte-code from an implementation of the DJVM's `UserSource` interface.
```java
package net.corda.djvm.source;

public interface Source extends AutoCloseable {
    URL findResource(String name);
    Enumeration<URL> findResources(String name);
    URL[] getURLs();
}

public interface UserSource extends Source {}
```

This will most likely be a descendant of `URLClassLoader` such as the DJVM's `UserPathSource` class:
```kotlin
package net.corda.djvm.source

class UserPathSource(urls: Array<URL>) : URLClassLoader(urls, null), UserSource {
    constructor(paths: List<Path>) : this(resolvePaths(paths))
}

```

### ApiSource.

Java APIs for the DJVM to use inside the sandbox are read from an implementation of `ApiSource`:

```java
package net.corda.djvm.source;

public interface ApiSource extends Source {}
```

Alternative Java APIs are _mandatory_ if you are running the DJVM on a Java9+ JVM, because the DJVM has no access
to an implementation of the Java 8 APIs in this case, c.f. an instance of `BootstrapClassLoader` containing
`deterministic-rt.jar`.

```kotlin
package net.corda.djvm.source

class BootstrapClassLoader(bootstrapJar: Path) : URLClassLoader(resolvePaths(listOf(bootstrapJar)), null), ApiSource {
    override fun getResource(name: String): URL? = findResource(name)
    override fun getResources(name: String): Enumeration<URL> = findResources(name)
}
```

However, using `deterministic-rt.jar` is still _strongly recommended_ when running on Java 8.

## AnalysisConfiguration.

This is the lowest level configuration object for a `SandboxClassLoader` instance, and is created using
```kotlin
package net.corda.djvm.analysis

fun AnalysisConfiguration.createRoot(
    userSource: UserSource,
    whitelist: Whitelist,
    visibleAnnotations: Set<Class<out Annotation>> = emptySet(),
    minimumSeverityLevel: Severity = Severity.WARNING,
    bootstrapSource: ApiSource? = null,
    analyzeAnnotations: Boolean = false,
    prefixFilters: List<String> = emptyList(),
    classModule: ClassModule = ClassModule(),
    memberModule: MemberModule = MemberModule()
): AnalysisConfiguration
```

where:
- `userSource` is an instance of `UserSource` that contains the user's classes to be sandboxed.
- `whitelist` contains the class names which the DJVM should _not_ map into the sandbox. Regular
expressions are supported, although you probably still want to use `Whitelist.MINIMAL` anyway.
- `visibleAnnotations` Not only will occurrences of these annotations be mapped into the `sandbox.*`,
package space, but the original annotations will be preserved too.
- `minimumSeverityLevel` is the minimum message severity level to be recorded in `MessageCollection` by
`sandbox.*` classes.
- `bootstrapSource` is an instance of `ApiSource` containing an implementation of Java 8 APIs. A `null`
value forcs the DJVM to use the underlying JVM's API classes instead.
- `analyzeAnnotations` determines whether the DJVM should include class references from annotations during
the analysts phase.
- `prefixFilter` is another logging option. If set, only messages from classes matching one of these
prefixes will be recorded in `MessageCollection`.
- `classModule`, `memberModule` Just accept the default values...

## SandboxConfiguration.

This is the highest level configuration object. Not only does it determine which transformation rules
are applied to your byte-code, but it also caches the transformed byte-code so that subsequent
`SandboxClassLoader` instances created from this configuration can pass it immediately to
`ClassLoader.defineClass()`.

```kotlin
package net.corda.djvm

fun SandboxConfiguration.createFor(
    analysisConfiguration: AnalysisConfiguration,
    profile: ExecutionProfile?,
    externalCache: ExternalCache?
): SandboxConfiguration
```
where:
- `analysisConfguration` is an instance of `AnalysisConfiguration`
- `profile` is an `ExecutionProfile` containing the maximum number of throws, jumps and method invocations
etc that a sandbox created using this configuration can perform before throwing `ThresholdViolationError`.
A `null` profile disables this completely by not instrumenting the byte-code.
- `externalCache` is an optional implementation of `ConcurrentMap<ByteCodekey,ByteCode>` provided by
the caller. This is useful for sharing sandbox byte-code across multiple `SandboxConfiguration` instances,
although it is then the caller's responsibility to ensure that all of these configurations are consistent.
In other words, **don't** do this unless you are **sure** that these configurations would all generate
the exact same byte-code in all cases.

Note: Use of an external cache can also be toggled per `SandboxClassLoader` via the `SandboxClassLoader`'s
`externalCaching` property.

Once you have created a `SandboxConfiguration`, the final step is to create a `SandboxRuntimeContext` which
manages the lifecycle of its `SandboxClassLoader` field.
```
new SandboxRuntimeContext(configuration).use(ctx -> {
    SandboxClassLoader classLoader = ctx.getClassLoader();

    // Create and instantiate classes using this context's sandbox classloader.
});
```

All `sandbox.*` classes generated by executing the lambda function will be preserved inside the context's
`SandboxClassLoader`, along with any values stored in their `static` fields. To reset these `static` fields
to their initial values, you will need to create a brand new context object. However, the `SandboxConfiguration`
object also caches the byte-code for these `sandbox.*` classes so that subsequent contexts do not need to
regenerate it from the original sources. The context adds any new byte-code to this cache after each lambda
function completes.

You may prefer to use the higher-level `IsolatedTask` to run a single lambda inside its own thread:
```
IsolatedTask.Result<T> value = new IsolatedTask(threadName, configuration).run(classLoader -> {
    // Use this task's SandboxClassLoader. 
    return <instance of T>;
});
```

### Child Configurations.

Consider the case where you want to build many different sandboxes that share a set of common libraries.
Each sandbox will needs its own copies of all of the library classes, but these classes will all be
created from identical byte-code. It therefore makes sense to create a base `SandboxConfiguration` object
that contains _only_ the common libraries (and the Java APIs themselves), and then to extend this by
creating different "child" configurations:

```kotlin
package net.corda.djvm

class SandboxConfiguration {
    fun createChild(userSource: UserSource, configure: Consumer<in ChildOptions>): SandboxConfiguration
}
```

The `UserSource` here would contain only the libraries that are unique to each child sandbox. You can also
customise how the DJVM generates byte-code for these classes to a certain degree via the`ChildOptions`:

```java
package net.corda.djvm;

interface ChildOptions {
    void setMinimumSeverityLevel(Severity level);
    void setVisibleAnnotations(Iterable<Class<? extends Annotation>> annotations);
    void setExternalCache(ConcurrentMap<ByteCodeKey, ByteCode> externalCache);
}
```

Building a `SandboxRuntimeContext` from a child configuration will create a separate `SandboxClassLoader`
for each `SandboxConfiguration` in the chain, all arranged in corresponding parent/child relationships.


## Preloading Sandbox Byte-Code.

By default, a `SandboxClassLoader` will generate the `sandbox.*` classes that it needs "on demand".
This strategy means that the DJVM won't generate classes that it will never execute, and is intended
as an optimisation. However, if you know that a particular JAR (most likely one that you yourself
have created) will end up being mostly executed regardless then you can tag this JAR by including an
empty file called:

    META-INF/DJVM-preload

and then invoke its `SandboxConfiguration` instance's `preload()` function. This function scans the
`UserSource` for all JARs containing `META-INF/DJVM-preload` files and caches `sandbox.*` byte-code
for all classes inside those JARs. It will also cache byte-code for all classes that they reference,
either directly or indirectly, and so will probably include a large number of Java API classes too.


## Running inside the DJVM Sandbox.

In theory, once you have created a `SandboxClassLoader` object, you can create sandboxed versions
of your classes using `ClassLoader.loadClass(String)` or `Class.forName(String, boolean, ClassLoader)`
and then execute them using Java reflection. However, this isn't very convenient. To help you, the
DJVM has built in support for executing implementations of `java.util.function.Function<T, R>.`

### Java Example

Consider this simple Java task: 
```java
import java.util.function.Function;

public class SimpleTask implements Function<long[], Long> {
    @Override
    public Long apply(long[] input) {
        if (input == null) {
            return null;
        }
        long total = 0;
        for (long number : input) {
            total += number;
        }
        return total;
    }
}
```
We can execute this task inside a sandbox using the following Java lambda:
```java
import net.corda.djvm.SandboxRuntimeContext;
import net.corda.djvm.TypedTaskFactory;
import net.corda.djvm.execution.SandboxRuntimeException;
import net.corda.djvm.rewiring.SandboxClassLoader;
import java.util.function.Function;

class ExampleCode {
    void example(SandboxRuntimeContext context) {
        context.use(ctx -> {
            try {
                SandboxClassLoader cl = ctx.getClassLoader();

                // Create a reusable task factory.
                TypedTaskFactory taskFactory = cl.createTypedTaskFactory();

                // Wrap SimpleTask inside an instance of sandbox.Task.
                Function<long[], Long> simpleTask = taskFactory.create(SimpleTask.class);

                // Execute SimpleTask inside the sandbox.
                Long result = simpleTask.apply(new long[]{ 1000, 200, 30, 4 });
            } catch (Exception e) {
                throw new SandboxRutimeException(e.getMessage(), e);
            }
        });
    }
}
```

The important point to understand here is that the sandboxed version of our `SimpleTask` class will
implement `sandbox.java.util.function.Function<T,R>` instead of `java.util.functon.Function<T,R>`,
which makes it unassignable to anything outside the sandbox except for `java.lang.Object`.
However, both of these interfaces still define an `apply` method with an identical signature, which
allows us to create a special `Task` wrapper class:
```kotlin
package sandbox

class Task(
    private val function: sandbox.java.util.function.Function<in Any?, out Any?>?
) : sandbox.java.util.function.Function<Any?, Any?>, java.util.function.Function<Any?, Any?> {
    override fun apply(input: Any?): Any? {
        val value = try {
            function?.apply(input?.sandbox())
        } catch (t: Throwable) {
            throw t.escapeSandbox()
        }
        return value?.unsandbox()
    }
}
```

Wrapping sandboxed `Function` classes in this way allows us to invoke their `apply` methods
without needing to use Java reflection. The DJVM's `sandbox()` and `unsandbox()` functions
map the parameter and return value objects into and out of the sandbox respectively,
although they are limited to handling only the following types:
- Boxed primitive types:
    - `java.lang.Integer`
    - `java.lang.Long`
    - `java.lang.Short`
    - `java.lang.Byte`
    - `java.lang.Char`
    - `java.lang.String`
    - `java.lang.Boolean`
    - `java.lang.Float`
    - `java.lang.Double`
- Java `enum` types, i.e. descendants of `java.lang.Enum<?>`
- `java.util.UUID`
- Java's built-in "time" types:
    - `java.util.Date`
    - `java.time.Instant`
    - `java.time.Duration`
    - `java.time.Period`
    - `java.time.LocalDate`
    - `java.time.LocalTime`
    - `java.time.LocalDateTime`
    - `java.time.OffsetTime`
    - `java.time.OffsetDateTime`
    - `java.time.MonthDay`
    - `java.time.YearMonth`
    - `java.time.Year`
    - `java.time.ZonedDateTime`
    - `java.time.ZoneId`
- Implementors of `java.io.InputStream`, which are mapped to an internal implementation
of `sandbox.java.io.InputStream` only.

Note that we do not need to map primitive types such as `int`, `long` and `boolean`.
Arrays in Java are also a special kind of primitive type. When transforming arrays
of objects, it is the array's _component type_ which is significant.

You would need to devise an appropriate external serialization mechanism to transform
any function arguments and return values which are more complex than these, and then
execute your tasks using the DJVM's `RawTask` wrapper:

```kotlin
package sandbox

class RawTask(
    private val function: sandbox.java.util.function.Function<Any?, Any?>?
) : sandbox.java.util.function.Function<Any?, Any?>, java.util.function.Function<Any?, Any?> {
    /**
     * This function runs inside the sandbox, and performs NO marshalling
     * of the input and output objects. This must be done by the caller.
     */
    override fun apply(input: Any?): Any? {
        return try {
            function?.apply(input)
        } catch (t: Throwable) {
            throw t.escapeSandbox()
        }
    }
}
```
We would execute `SimpleTask` via `RawTask` like this:
```java
import net.corda.djvm.SandboxRuntimeContext;
import net.corda.djvm.execution.SandboxRuntimeException;
import net.corda.djvm.rewiring.SandboxClassLoader;
import java.util.function.Function;

class ExampleCode {
    void example(SandboxRuntimeContext context) {
        context.use(ctx -> {
            try {
                SandboxClassLoader cl = ctx.getClassLoader();

                // Create reusable factories.
                Function<? super Object, ? extends Function<? super Object, ?>> rawTaskFactory = cl.createRawTaskFactory();
                Function<Class<? extends Function<?, ?>>, ?> sandboxFunction = cl.createSandboxFunction();
                Function<Class<? extends Function<?, ?>>, ? extends Function<? super Object, ?>> taskFactory = rawTaskFactory.compose(sandboxFunction);

                // Wrap SimpleTask inside an instance of sandbox.RawTask.
                Function<? super Object, ?> simpleTask = taskFactory.apply(SimpleTask.class);

                // Execute SimpleTask inside the sandbox.
                Object result = simpleTask.apply(new long[]{ 1000, 200, 30, 4 });
            } catch (Exception e) {
                throw new SandboxRutimeException(e.getMessage(), e);
            }
        });
    }
}
```
The task's return value will now be an instance of `sandbox.java.lang.Long`.

It is also worth noting that while the `sandboxFunction` factory requires that
`SimpleTask` has a no-argument constructor, `rawTaskFactory` only requires
that its input implements `sandbox.java.util.function.Function`. The
`sandboxFunction` factory is just a convenience to handle what is considered
to be the most common use-case.

### Linking an external function into the sandbox.

In some rare cases, you may need to call a non-sandbox function from within
sandbox code. For example, to deserialize lazily instances of complex sandbox
objects, or to avoid transforming a `java.io.ByteArrayInputStream` object
into a `sandbox.java.io.ByteArrayInputStream` before it can be consumed. To
do this, you would wrap your code as a `Function` and pass it to this method:
```java
package net.corda.djvm.rewiring;

import java.util.function.Function;

public class SandboxClassLoader {
    public <T> Function<? super T, ?> createForImport(Function<? super T, ?> function);
}
```
This method wraps an instance of `java.util.function.Function` inside `sandbox.ImportTask`,
where `ImportTask` is defined as:
```kotlin
package sandbox

class ImportTask(
    private val function: java.util.function.Function<Any?, Any?>
) : sandbox.java.util.function.Function<Any?, Any?>, java.util.function.Function<Any?, Any?> {
    /**
     * This allows [function] to be executed inside the sandbox.
     * !!! USE WITH EXTREME CARE !!!
     */
    override fun apply(input: Any?): Any? {
        return try {
            function.apply(input)
        } catch (e: Exception) {
            throw e.toRuntimeException()
        } catch (t: Throwable) {
            checkCatch(t)
            throw t.toRuleViolationError()
        }
    }
}
```
In practice, you would probably use it something like this:
```java
import net.corda.djvm.SandboxRuntimeContext;
import net.corda.djvm.execution.SandboxRuntimeException;
import net.corda.djvm.rewiring.SandboxClassLoader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.function.Function;

import static java.nio.charset.StandardCharsets.UTF_8;

class ExampleCode {
    void example(SandboxRuntimeContext context) {
        context.use(ctx -> {
            try {
                SandboxClassLoader classLoader = ctx.getClassLoader();

                // E.g. a function to create an instance of InputStream.
                Function<String, InputStream> exampleFunction = s -> new ByteArrayInputStream(s.getBytes(UTF_8));

                // Create a reusable factory function that can transform java.io.InputStream
                // into sandbox.java.io.InputStream.
                Function<? super Object, ?> sandboxInput = classLoader.createBasicInput();

                // Combine these two functions into a new java.util.function.Function,
                // and then wrap that inside an instance of sandbox.ImportTask.
                // This task is assignable to both java.util.function.Function and
                // sandbox.java.util.function.Function, and when invoked will return
                // an instance of sandbox.java.io.InputStream.
                Function<? super String, ?> importTask = classLoader.createForImport(
                    exampleFunction.andThen(sandboxInput)
                );

                // Use importTask...
                // ...
            } catch (Exception e) {
                throw new SandboxRutimeException(e.getMessage(), e);
            }
        });
    }
}
```
The imported `Function` can reasonably be expected to throw exceptions, but we
cannot assume that the DJVM will be able to transform the `Throwable` classes
into their `sandbox.*` equivalents. The `ImportTask` will therefore replace
anything assignable to `java.lang.Exception` with an instance of
`java.lang.RuntimeException`, and any other `Throwable` with an uncatchable
`RuleViolationError`.

### Running a Predicate<T> inside the sandbox.

The DJVM also supports executing `Predicate<T>` inside the sandbox via the `PredicateTask`:
```kotlin
package sandbox

class PredicateTask(
    private val predicate: sandbox.java.util.function.Predicate<Any?>
) : sandbox.java.util.function.Predicate<Any?>, java.util.function.Predicate<Any?> {
    /**
     * This predicate runs inside the sandbox, and performs NO marshalling
     * of the input object. This must be done by the caller.
     */
    override fun test(input: Any?): Boolean {
        return try {
            predicate.test(input)
        } catch (t: Throwable) {
            throw t.escapeSandbox()
        }
    }
}
```

This allows us to return primitive `boolean` values from the sandbox without needing
to box and then unbox them.

As with `Function` tasks, we would create a `rawPredicateFactory` function
from the `SandboxClassLoader` to wrap our instance of
`sandbox.java.util.function.Predicate<T>` inside a `PredicateTask`. We would
then be able to invoke this task from outside the sandbox via its
`java.util.function.Predicate<T>` interface.

We can also create a reusable `sandboxPredicate` "convenience" factory
function to return instances of our `java.util.function.Predicate<T>`
classes mapped into the sandbox, on the assumption that most such classes
will have a no-argument constructor.

For example:
```kotlin
class ExampleCode {
    fun example(ctx: SandboxRuntimeContext) = ctx.use(Consumer {
        val sandboxPredicate = it.classLoader.createSandboxPredicate()
        val rawPredicateFactory = it.classLoader.createRawPredicateFactory()
        val predicateFactory = rawPredicateFactory.compose(sandboxPredicate)

        val predicateTask = predicateFactory.apply(MyPredicate::class.java)
        // Use the predicate...
    })
}
```

There's no technical reason why the DJVM could not support other functional
interfaces in the exact same way. However, no "Real World" use case for any
of them has yet presented itself.
