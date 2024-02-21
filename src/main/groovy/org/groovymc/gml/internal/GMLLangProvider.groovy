/*
 * Copyright (c) Matyrobbrt
 * SPDX-License-Identifier: MIT
 */

package org.groovymc.gml.internal

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import net.neoforged.fml.loading.moddiscovery.ModFile
import net.neoforged.neoforgespi.language.IModLanguageProvider
import net.neoforged.neoforgespi.language.ModFileScanData
import net.neoforged.neoforgespi.locating.IModFile
import org.groovymc.gml.GMod
import org.groovymc.gml.internal.locator.ModLocatorInjector
import org.groovymc.gml.internal.scripts.ScriptFileCompiler
import org.objectweb.asm.Type

import java.nio.file.FileSystem
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer

@Slf4j
@CompileStatic
final class GMLLangProvider implements IModLanguageProvider {
    private static final Type GMOD_TYPE = Type.getType(GMod)

    GMLLangProvider() {
        ModLocatorInjector.inject()
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
            if (ScriptFileCompiler.isScriptMod(file)) {
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
        new ScriptFileCompiler((FileSystem)file.fs, (String)file.modId, (String)file.rootPackage, (AtomicBoolean)file.wasCompiled, (ModFile)file).compile(scanData)
    }
}
