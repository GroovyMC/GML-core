/*
 * Copyright (c) Matyrobbrt
 * SPDX-License-Identifier: MIT
 */

package org.groovymc.gml.transform.api

import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.control.SourceUnit

/**
 * A transformer for {@linkplain org.groovymc.gml.GMod GMods}. <br>
 * Global transformers can be registered with a service file or with {@linkplain org.groovymc.gml.transform.gmods.GModASTTransformer#registerGlobalTransformer(org.groovymc.gml.transform.api.GModTransformer)},
 * and mod-specific transformers with {@linkplain org.groovymc.gml.transform.gmods.GModASTTransformer#registerTransformer(java.lang.String, org.groovymc.gml.transform.api.GModTransformer)}.
 */
@CompileStatic
interface GModTransformer {
    /**
     * Transforms the GMod class.
     * @param classNode the class to transform
     * @param annotationNode the GMod annotation
     * @param source the source unit
     */
    void transform(ClassNode classNode, AnnotationNode annotationNode, SourceUnit source)
}