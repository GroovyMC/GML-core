/*
 * Copyright (c) Matyrobbrt
 * SPDX-License-Identifier: MIT
 */

//file:noinspection GrMethodMayBeStatic
package org.groovymc.gml.transform.gmods

import org.groovymc.gml.GMod
import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotatedNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.AbstractASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.codehaus.groovy.transform.TransformWithPriority
import org.groovymc.gml.transform.api.GModTransformer

@CompileStatic
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class GModASTTransformer extends AbstractASTTransformation implements TransformWithPriority {
    private static final ClassNode GMOD = ClassHelper.make(GMod)

    private static final List<GModTransformer> GLOBAL_TRANSFORMERS = []
    private static final Map<String, List<GModTransformer>> BY_MOD_TRANSFORMERS = [:]

    static {
        GLOBAL_TRANSFORMERS.addAll(ServiceLoader.load(GModTransformer, GModASTTransformer.class.classLoader))
    }

    /**
     * @deprecated use {@link #registerGlobalTransformer(org.groovymc.gml.transform.api.GModTransformer) instead}
     */
    @Deprecated(forRemoval = true, since = '1.1.0')
    static void registerTransformer(GModTransformer transformer) {
        registerGlobalTransformer(transformer)
    }

    /**
     * Registers a global transformer.
     * @param transformer the transformer to register
     */
    static void registerGlobalTransformer(GModTransformer transformer) {
        GLOBAL_TRANSFORMERS.add(transformer)
    }

    /**
     * Registers a transformer for a specific mod id.
     * @param modId the ID of the mod to register the transformer for
     * @param transformer the transformer to register
     */
    static void registerTransformer(String modId, GModTransformer transformer) {
        BY_MOD_TRANSFORMERS.computeIfAbsent(modId, { new ArrayList<GModTransformer>() }).add(transformer)
    }

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        init(nodes, source)
        final node = nodes[1] as AnnotatedNode
        if (!(node instanceof ClassNode)) throw new IllegalArgumentException('@GMod annotation can only be applied to classes!')
        final cNode = node as ClassNode
        final actualAnnotation = cNode.annotations.find { it.classNode == GMOD }
        GLOBAL_TRANSFORMERS.each { it.transform(cNode, actualAnnotation, source) }

        BY_MOD_TRANSFORMERS.computeIfAbsent(getMemberStringValue(actualAnnotation, 'value'), { new ArrayList<GModTransformer>() }).each { GModTransformer it ->
            it.transform(cNode, actualAnnotation, source)
        }
    }

    @Override
    int priority() {
        return -10
    }
}
