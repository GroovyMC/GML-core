/*
 * MIT License
 *
 * Copyright (c) 2022 matyrobbrt
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.matyrobbrt.gml.transform.gmods

import com.matyrobbrt.gml.bus.GModEventBus
import com.matyrobbrt.gml.transform.api.GModTransformer
import groovy.transform.CompileStatic
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.eventbus.api.IEventBus
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.tools.GeneralUtils
import org.codehaus.groovy.control.SourceUnit
import org.objectweb.asm.Opcodes

@CompileStatic
 final class BusTransformer implements GModTransformer {
    @Override
    void transform(ClassNode classNode, AnnotationNode annotationNode, SourceUnit source) {
        classNode.addProperty(
                'modBus', Opcodes.ACC_PUBLIC, ClassHelper.make(GModEventBus),
                null, null, null
        )
        classNode.addProperty(
                'forgeBus', Opcodes.ACC_PUBLIC, ClassHelper.make(IEventBus),
                GeneralUtils.propX(GeneralUtils.classX(MinecraftForge), 'EVENT_BUS'), null, null
        )
    }
}