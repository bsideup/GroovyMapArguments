package ru.trylogic.groovy.mapargs;

import org.codehaus.groovy.transform.GroovyASTTransformationClass;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@GroovyASTTransformationClass("ru.trylogic.groovy.mapargs.MapArgumentsASTTransformation")
public @interface MapArguments {
    
    Class typeHint() default Object.class;
}
