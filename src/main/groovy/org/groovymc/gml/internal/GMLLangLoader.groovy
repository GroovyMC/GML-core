/*
 * Copyright (c) Matyrobbrt
 * SPDX-License-Identifier: MIT
 */

package org.groovymc.gml.internal

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import net.neoforged.neoforgespi.language.IModInfo
import net.neoforged.neoforgespi.language.IModLanguageProvider
import net.neoforged.neoforgespi.language.ModFileScanData

@Canonical
@PackageScope
@CompileStatic
final class GMLLangLoader implements IModLanguageProvider.IModLanguageLoader {
    final String className, modId
    @Override
    <T> T loadMod(IModInfo info, ModFileScanData modFileScanResults, ModuleLayer layer) {
        final threadLoader = Thread.currentThread().contextClassLoader
        ModExtensionLoader.setup(threadLoader)
        final gContainer = Class.forName('org.groovymc.gml.GModContainer', true, threadLoader)
        final ctor = gContainer.getDeclaredConstructor(IModInfo, String, ModFileScanData, ModuleLayer)
        return ctor.newInstance(info, className, modFileScanResults, layer) as T
    }
}
