/*
 * Copyright (c) Matyrobbrt
 * SPDX-License-Identifier: MIT
 */

package org.groovymc.gml.transform.gmods

import net.neoforged.neoforge.common.NeoForge
import org.groovymc.gml.GMLModLoadingContext
import org.groovymc.gml.bus.GModEventBus
import groovy.transform.CompileStatic
import net.neoforged.bus.api.IEventBus
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.tools.GeneralUtils
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.trait.Traits
import org.groovymc.gml.transform.TransformationUtils
import org.groovymc.gml.transform.api.GModTransformer
import org.objectweb.asm.Opcodes

@CompileStatic
final class BusTransformer implements GModTransformer {
    @Override
    void transform(ClassNode classNode, AnnotationNode annotationNode, SourceUnit source) {
        final modBus = classNode.addField(
                'modBus', Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, ClassHelper.make(GModEventBus),
                GeneralUtils.callX(GeneralUtils.callX(ClassHelper.make(GMLModLoadingContext), 'get'), 'getModEventBus')
        )
        getOrCreateMethod(classNode, 'getModBus', modBus.type).setCode(
                GeneralUtils.returnS(GeneralUtils.fieldX(modBus))
        )

        final forgeBus = classNode.addField(
                'forgeBus', Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, ClassHelper.make(IEventBus),
                GeneralUtils.propX(GeneralUtils.classX(NeoForge), 'EVENT_BUS')
        )
        getOrCreateMethod(classNode, 'getForgeBus', forgeBus.type).setCode(
                GeneralUtils.returnS(GeneralUtils.fieldX(forgeBus))
        )
    }

    private static MethodNode getOrCreateMethod(ClassNode clazz, String name, ClassNode type) {
        return (clazz.getMethod(name, Parameter.EMPTY_ARRAY)?.tap {
            it.annotations.removeIf { it.classNode == Traits.TRAITBRIDGE_CLASSNODE }
        } ?: clazz.addMethod(
                name, Opcodes.ACC_PUBLIC, type, Parameter.EMPTY_ARRAY, ClassNode.EMPTY_ARRAY, null
        )).tap {
            it.addAnnotation(TransformationUtils.GENERATED_ANNOTATION)
        }
    }
}
