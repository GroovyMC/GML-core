package org.groovymc.gml.transform.gmods

import groovy.transform.CompileStatic
import net.neoforged.bus.api.SubscribeEvent
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.expr.ClassExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.MethodReferenceExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.ast.tools.GeneralUtils
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.AbstractASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.groovymc.gml.transform.TransformationUtils

import java.lang.reflect.Modifier

@CompileStatic
@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
class GEventBusSubscriberTransformer extends AbstractASTTransformation {
    private static final ClassNode SUBSCRIBE_EVENT = ClassHelper.make(SubscribeEvent)
    private static final ClassNode MOD_BUS_EVENT = ClassHelper.make('net.neoforged.fml.event.IModBusEvent')
    private static final ClassNode NEOFORGE = ClassHelper.make('net.neoforged.neoforge.common.NeoForge')
    private static final ClassNode MOD_LOADING_CONTEXT = ClassHelper.make('net.neoforged.fml.ModLoadingContext')
    private static final String GML_REGISTRATION_METHOD_NAME = 'gml$registerListeners'

    private static BlockStatement buildRegistration(ClassNode node) {
        ClassExpression classExpression = new ClassExpression(node)
        BlockStatement all = new BlockStatement()
        for (var method : node.methods) {
            if (!method.getAnnotations(SUBSCRIBE_EVENT).empty) {
                if (method.parameters.size() != 1) {
                    throw new IllegalArgumentException("Method ${method.name} annotated with @SubscribeEvent in class ${node.name} annotated with @GEventBusSubscriber must have exactly one parameter!")
                }
                boolean isStatic = Modifier.isStatic(method.getModifiers())
                ClassNode paramType = method.parameters[0].type
                ClassExpression paramTypeExpression = new ClassExpression(paramType)
                MethodReferenceExpression methodReference = new MethodReferenceExpression(isStatic ? classExpression : new VariableExpression('this', node), new ConstantExpression(method.name))
                all.addStatement(buildCheckedListenerAddition(paramTypeExpression, methodReference))
            }
        }
        return all
    }


    private static Statement buildCheckedListenerAddition(ClassExpression paramTypeExpression, MethodReferenceExpression methodReference) {
        return GeneralUtils.ifElseS(
                GeneralUtils.callX(GeneralUtils.classX(MOD_BUS_EVENT), 'isAssignableFrom', GeneralUtils.args(paramTypeExpression)),
                GeneralUtils.stmt(GeneralUtils.callX(
                        GeneralUtils.callX(GeneralUtils.callX(GeneralUtils.callX(new ClassExpression(MOD_LOADING_CONTEXT), 'get'), 'getActiveContainer'), 'getEventBus'),
                        'addListener',
                        GeneralUtils.args(paramTypeExpression, methodReference)
                )),
                GeneralUtils.stmt(GeneralUtils.callX(
                        GeneralUtils.attrX(GeneralUtils.classX(NEOFORGE), GeneralUtils.constX('EVENT_BUS')),
                        'addListener',
                        GeneralUtils.args(paramTypeExpression, methodReference)
                ))
        )
    }

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        init(nodes, source)

        if (!(nodes[1] instanceof ClassNode)) throw new IllegalArgumentException('@GEventBusSubscriber annotation can only be applied to classes!')
        final node = nodes[1] as ClassNode

        println "Transforming ${node.name} to add event bus subscriber registration method"

        BlockStatement target = buildRegistration(node)
        node.addMethod(
                GML_REGISTRATION_METHOD_NAME,
                Modifier.PUBLIC | Modifier.FINAL,
                ClassHelper.VOID_TYPE,
                new Parameter[0],
                new ClassNode[0],
                null
        ).tap {
            it.addAnnotation(TransformationUtils.GENERATED_ANNOTATION)
            it.setCode(target)
        }
    }
}
