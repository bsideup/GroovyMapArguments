package ru.trylogic.groovy.mapargs

import groovy.transform.CompileStatic
import ru.trylogic.groovy.mapargs.MapArguments

@CompileStatic
class MapArgumentsTest extends GroovyShellTestCase {
    
    static abstract class TestTransformer {
        abstract void transform(Test test);
        
        void blah() {
            
        }
    }

    static class Test {
        String a;

        Integer b;
    }
    
    class TestWithMapConstructor extends Test {
        TestWithMapConstructor(Map map) {
            this.a = map.a as String;
            this.b = map.b as Integer;
        }
    }

    @MapArguments
    static Test staticQwe(Test args, Closure cl = null) {

        if(args.a == null) {
            throw new Exception("a should be set");
        }

        cl?.call(args)
        return args
    }

    @MapArguments
    Test qwe(Test args, TestTransformer cl = null) {
        return staticQwe(args, cl ? cl.&transform : null)
    }

    @MapArguments
    TestWithMapConstructor qweWithMapConstructor(TestWithMapConstructor args, Closure cl = null) {
        staticQwe(args, cl)

        return args;
    }

    void testNamedArguments() {
        Test result = qwe(a : "3", b : 100500)

        assertAll(result);
    }

    void testNamedArgumentsWithClosure() {
        Test result = qwe(a : "non3", b : 100500) { Test test ->
            test.a = "3"
        };

        assertAll(result);
    }

    void testStaticNamedArguments() {
        Test result = staticQwe(a : "3", b : 100500)

        assertAll(result);
    }

    void testStaticNamedArgumentsWithClosure() {
        Test result = staticQwe(a : "non3", b : 100500) { Test test ->
            test.a = "3"
        };

        assertAll(result);
    }

    void testNormalMapArguments() {
        Test result = qwe([a : "3", b : 100500])

        assertAll(result);
    }

    void testNormalMapArgumentsWithClosure() {
        Test result = qwe([a : "non3", b : 100500]) { Test test ->
            test.a = "3"
        };

        assertAll(result);
    }

    void testObjectWithMapConstructor() {
        Test result = qweWithMapConstructor(a : "non3", b : 100500) { TestWithMapConstructor test ->
            test.a = "3"
        };

        assertAll(result);
    }
    
    void assertAll(Test result) {
        assert result != null
        assert result.a == "3"
        assert result.b == 100500
    }
}