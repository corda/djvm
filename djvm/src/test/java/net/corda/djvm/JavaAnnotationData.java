package net.corda.djvm;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@SuppressWarnings("unused")
@Target({TYPE, METHOD, FIELD})
@Retention(RUNTIME)
@Documented
@Inherited
public @interface JavaAnnotationData {
    String stringData() default "<none>";
    String[] stringsData() default { "<none>" };
    double doubleData() default 0.0d;
    double[] doublesData() default { 0.0d };
    float floatData() default 0.0f;
    float[] floatsData() default { 0.0f };
    long longData() default 0;
    long[] longsData() default { 0 };
    int intData() default 0;
    int[] intsData() default { 0 };
    short shortData() default 0;
    short[] shortsData() default { 0 };
    boolean flag() default false;
    boolean[] flags() default { false };
    char charData() default '?';
    char[] charsData() default { '?' };
    byte byteData() default 0;
    byte[] bytesData() default { 0 };
    Label label() default Label.UGLY;
    Label[] labels() default { Label.UGLY };
}
