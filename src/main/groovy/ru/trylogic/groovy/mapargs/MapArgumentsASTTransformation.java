package ru.trylogic.groovy.mapargs;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.transform.AbstractASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.List;

@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
public class MapArgumentsASTTransformation extends AbstractASTTransformation {
    @Override
    public void visit(ASTNode[] nodes, SourceUnit source) {
        init(nodes, source);

        MethodNode methodNode = (MethodNode) nodes[1];

        Parameter[] parameters = methodNode.getParameters();
        Parameter closureParameter = null;

        boolean hasClosureParameter = false;
        switch (parameters.length) {
            case 0:
                addError("MapArguments parameters should not be empty", methodNode);
                return;
            case 1:
                break;
            case 2:
                closureParameter = parameters[1];
                if (!(closureParameter.getType().isDerivedFrom(ClassHelper.CLOSURE_TYPE)) && !ClassHelper.isSAMType(closureParameter.getType())) {
                    addError("MapArguments second parameter can be a closure only", closureParameter);
                    return;
                }
                hasClosureParameter = true;
                break;
            default:
                addError("MapArguments parameters length should not be greater than 2", methodNode);
                return;
        }

        ClassNode mapNode = nonGeneric(ClassHelper.MAP_TYPE);
        Parameter mapParameter = new Parameter(mapNode, "__namedArgs");

        Parameter[] mapBasedMethodParameters = hasClosureParameter ? new Parameter[]{mapParameter, closureParameter} : new Parameter[]{mapParameter};


        ClassNode declaringClass = methodNode.getDeclaringClass();

        if (declaringClass.hasMethod(methodNode.getName(), mapBasedMethodParameters)) {
            addError("This class already have Map-based method", methodNode);
            return;
        }

        Expression convertedValueExpression;

        ClassNode parameterType = parameters[0].getType();
        if (checkForMapConstructor(parameterType)) {
            ArgumentListExpression convertedArguments = new ArgumentListExpression(new VariableExpression(mapParameter));

            if ((parameterType.getModifiers() & Opcodes.ACC_STATIC) == 0) {
                convertedArguments.getExpressions().add(0, VariableExpression.THIS_EXPRESSION);
            }

            convertedValueExpression = new ConstructorCallExpression(parameterType, convertedArguments);
        } else {
            convertedValueExpression = new StaticMethodCallExpression(
                    ClassHelper.makeWithoutCaching(DefaultGroovyMethods.class, false),
                    "asType",
                    new ArgumentListExpression(new VariableExpression(mapParameter), new ClassExpression(parameterType))
            );
        }
        ArgumentListExpression oldMethodArguments = new ArgumentListExpression(convertedValueExpression);

        if (hasClosureParameter) {
            oldMethodArguments.addExpression(new VariableExpression(closureParameter));
        }

        List<MethodNode> generatedMethods = new ArrayList<MethodNode>();
        generatedMethods.add(declaringClass.addMethod(
                methodNode.getName(),
                methodNode.getModifiers(),
                methodNode.getReturnType(),
                mapBasedMethodParameters,
                methodNode.getExceptions(),
                new ExpressionStatement(
                        new MethodCallExpression(
                                VariableExpression.THIS_EXPRESSION,
                                methodNode.getName(),
                                oldMethodArguments
                        )
                )
        ));

        if (hasClosureParameter) {
            generatedMethods.add(declaringClass.addMethod(
                    methodNode.getName(),
                    methodNode.getModifiers(),
                    methodNode.getReturnType(),
                    new Parameter[]{mapBasedMethodParameters[0]},
                    methodNode.getExceptions(),
                    new ExpressionStatement(
                            new MethodCallExpression(
                                    VariableExpression.THIS_EXPRESSION,
                                    methodNode.getName(),
                                    new ArgumentListExpression(
                                            oldMethodArguments.getExpression(0),
                                            new CastExpression(
                                                    closureParameter.getType(),
                                                    new ConstantExpression(null)
                                            )
                                    )
                            )
                    )
            ));
        }

        for (MethodNode generatedMethod : generatedMethods) {
            AnnotationNode mapArgumentsAnnotationNode = new AnnotationNode(ClassHelper.makeWithoutCaching(MapArguments.class));
            mapArgumentsAnnotationNode.setMember("typeHint", new ClassExpression(parameterType));
            generatedMethod.addAnnotation(mapArgumentsAnnotationNode);
        }
    }

    protected boolean checkForMapConstructor(ClassNode parameterType) {
        List<ConstructorNode> declaredConstructors = parameterType.getDeclaredConstructors();

        for (ConstructorNode declaredConstructor : declaredConstructors) {
            Parameter[] declaredConstructorParameters = declaredConstructor.getParameters();
            if (declaredConstructorParameters.length != 1) {
                continue;
            }

            ClassNode declaredConstructorParameterType = declaredConstructorParameters[0].getType();

            if (declaredConstructorParameterType == null) {
                continue;
            }

            if (declaredConstructorParameterType.equals(ClassHelper.MAP_TYPE)) {
                return true;
            }
        }

        return false;
    }
}
