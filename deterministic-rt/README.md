# The Deterministic Java Runtime.

```xml
<dependency>
    <groupId>net.corda</groupId>
    <artifactId>deterministic-rt</artifactId>
    <version>${version}</version>
</dependency>
```

The `deterministic-rt.jar` artifact implements a subset of the Java 8 APIs, as built from OpenJDK8 sources.
Some obviously non-deterministic APIs, such as `System.currentTimeMillis()`, `System.nanoTime()`, as well
as file and network handling have been deleted. The DJVM does not execute any of the remaining classes
directly, and _cannot_ anyway because Java forbids user classloaders from defining new classes inside
the `java.*`package space. Instead, the DJVM considers `determinisitc-rt.jar` to be a resource, loading
class byte-code on demand and then rewriting it to define a new class in the `sandbox.*` package space.

For example:

    java.lang.Exception -> sandbox.java.lang.Exception

We use ProGuard to ensure that the subset of classes inside `deterministic-rt.jar` is both closed
and self-consistent.

### The deterministic compile-time artifact.

```xml
<dependency>
    <groupId>net.corda</groupId>
    <artifactId>deterministic-rt</artifactId>
    <version>${version}</version>
    <classifier>api</classifier>
</dependency>
```

The `deterministic-rt.jar` artifact still contains some classes that the DJVM will refuse to execute within
our deterministic sandbox, e.g. `java.lang.invoke.*`. To help Java developers write DJVM-compatible code, we
also generate an `api` artifact where these classes have been deleted. You can pass this artifact to the Java
compiler via its `-Xbootclasspath` option.


# How To Build.

Our deterministic subset of the OpenJDK8 build is contained in the `determinsitic-jvm8` branch of
[this Git repository](https://github.com/corda/openjdk).
    
Each release is tagged, e.g. `deterministic/1.0`. The tag to build is specified in this project's
[`gradle.properties`](./gradle.properties):

    deterministic_rt_tag=deterministic/1.0

The artifacts can then be built using the Gradle wrapper in our parent directory:

    $ ../gradlew build

And subsequently published to Artifactory:

    $ ../gradlew artifactoryPublish

Releases and release candidates should be published to the Artifactory's `corda-dependencies` repository.
