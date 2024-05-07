/*
 * Copyright (c) Matyrobbrt
 * SPDX-License-Identifier: MIT
 */

package org.groovymc.gml.internal

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import net.neoforged.fml.ModContainer
import net.neoforged.fml.ModLoadingException
import net.neoforged.fml.ModLoadingIssue
import net.neoforged.fml.common.Mod
import net.neoforged.fml.javafmlmod.AutomaticEventSubscriber
import net.neoforged.fml.loading.FMLLoader
import net.neoforged.fml.loading.modscan.ModAnnotation
import net.neoforged.neoforgespi.IIssueReporting
import net.neoforged.neoforgespi.language.IModInfo
import net.neoforged.neoforgespi.language.IModLanguageLoader
import net.neoforged.neoforgespi.language.ModFileScanData
import net.neoforged.neoforgespi.locating.IModFile
import org.groovymc.gml.GMod
import org.groovymc.gml.internal.locator.ModLocatorInjector
import org.groovymc.gml.util.Environment

import java.lang.annotation.ElementType
import java.util.stream.Stream

@CompileStatic
final class GMLLangLoader implements IModLanguageLoader {
    GMLLangLoader() {
        ModLocatorInjector.inject()
    }

    @Override
    String name() {
        return 'gml'
    }

    @Override
    ModContainer loadMod(IModInfo info, ModFileScanData modFileScanData, ModuleLayer moduleLayer) throws ModLoadingException {
        // TODO: re-enable script mods
        /*
        // Basically, this check will check if the mod file is a ScriptModFile
        final file = modFileScanData.getIModInfoData()[0].file

        if (ScriptFileCompiler.isScriptMod(file)) {
            // ... and if so, call `compile` on it, to compile the scripts and re-scan the files for metadata
            compile(file, modFileScanData)
        }
        */

        List<String> modClasses = Stream.concat(
                modFileScanData.getAnnotatedBy(GMod.class, ElementType.TYPE),
                modFileScanData.getAnnotatedBy(Mod.class, ElementType.TYPE)
        ).filter { data ->
            return data.annotationData().get('value') == info.modId
        }.filter { ad ->
            return AutomaticEventSubscriber.getSides(ad.annotationData().get('dist')).contains(FMLLoader.getDist()) &&
                    getEnvironments(ad.annotationData().get('environment')).contains(Environment.current())
        }.map { ad ->
            return ad.clazz().getClassName()
        }.toList()
        final threadLoader = Thread.currentThread().contextClassLoader
        ModExtensionLoader.setup(threadLoader)
        final gContainer = Class.forName('org.groovymc.gml.GModContainer', true, threadLoader)
        final ctor = gContainer.getDeclaredConstructor(IModInfo, List, ModFileScanData, ModuleLayer)
        return (ModContainer) ctor.newInstance(info, modClasses, modFileScanData, moduleLayer)
    }

    @Override
    void validate(IModFile file, Collection<ModContainer> loadedContainers, IIssueReporting reporter) {
        super.validate(file, loadedContainers, reporter)
        Set<String> modIds = new HashSet(file.modInfos.findAll { it.loader === this }.collect { it.modId })

        Stream.concat(
                file.scanResult.getAnnotatedBy(GMod.class, ElementType.TYPE),
                file.scanResult.getAnnotatedBy(Mod.class, ElementType.TYPE)
        ).filter { data ->
            return !modIds.contains((String)data.annotationData().get('value'))
        }.forEach { data ->
            ModLoadingIssue issue = ModLoadingIssue.error(
                    "fml.modloading.javafml.dangling_entrypoint",
                    data.annotationData().get('value'),
                    data.clazz().getClassName(),
                    file.filePath
            ).withAffectedModFile(file)
            reporter.addIssue(issue)
        }
    }

    @CompileDynamic
    private static void compile(IModFile file, ModFileScanData scanData) {
        // TODO: re-enable script mods
        //new ScriptFileCompiler((FileSystem)file.fs, (String)file.modId, (String)file.rootPackage, (AtomicBoolean)file.wasCompiled, (ModFile)file).compile(scanData)
    }

    static EnumSet<Environment> getEnvironments(Object data) {
        return data == null ? EnumSet.allOf(Environment.class) : (data as List<ModAnnotation.EnumHolder>).collect {
            Environment.valueOf(it.value())
        }.with {
            EnumSet.copyOf(it)
        }
    }
}
