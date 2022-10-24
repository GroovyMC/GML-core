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

package com.matyrobbrt.gml.internal

import com.matyrobbrt.gml.GMod
import com.matyrobbrt.gml.mappings.MappingsProvider
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import net.minecraftforge.fml.loading.FMLEnvironment
import net.minecraftforge.forgespi.language.ILifecycleEvent
import net.minecraftforge.forgespi.language.IModLanguageProvider
import net.minecraftforge.forgespi.language.ModFileScanData
import net.minecraftforge.forgespi.locating.IModFile
import org.objectweb.asm.Type

import java.util.function.Consumer
import java.util.function.Supplier

@Slf4j
@CompileStatic
final class GMLLangProvider implements IModLanguageProvider {
    private static final Type GMOD_TYPE = Type.getType(GMod)

    GMLLangProvider() {
        if (FMLEnvironment.production) {
            // Only load mappings in prod
            MappingsProvider.INSTANCE.startMappingsSetup()
        }
    }

    @Override
    String name() {
        return 'gml'
    }

    @Override
    Consumer<ModFileScanData> getFileVisitor() {
        return { ModFileScanData scanData ->
            // Basically, this check will check if the mod file is a ScriptModFile
            final file = scanData.getIModInfoData()[0].file
            if (file.metaClass.respondsTo(null, 'compile')) {
                // ... and if so, call `compile` on it, to compile the scripts and re-scan the files for metadata
                compile(file, scanData)
            }

            final Map<String, IModLanguageLoader> mods = scanData.annotations
                .findAll { it.annotationType() == GMOD_TYPE }
                .collect { new GMLLangLoader(it.clazz().className, it.annotationData()['value'] as String) }
                .each { log.debug('Found GMod entry-point class {} for mod {}', it.className, it.modId) }
                .collectEntries { [it.modId, it] }
            scanData.addLanguageLoader mods
        }
    }

    @CompileDynamic
    private static void compile(IModFile file, ModFileScanData scanData) {
        file.compile(scanData)
    }

    @Override
    <R extends ILifecycleEvent<R>> void consumeLifecycleEvent(final Supplier<R> consumeEvent) { }
}
