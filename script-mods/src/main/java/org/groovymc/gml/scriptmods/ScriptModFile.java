/*
 * Copyright (c) Matyrobbrt
 * SPDX-License-Identifier: MIT
 */

package org.groovymc.gml.scriptmods;

import net.neoforged.fml.loading.moddiscovery.ModFile;
import net.neoforged.neoforgespi.locating.IModProvider;
import net.neoforged.neoforgespi.locating.ModFileFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.FileSystem;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("unused")
public class ScriptModFile extends ModFile {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptModFile.class);

    public final AtomicBoolean wasCompiled = new AtomicBoolean();
    public final FileSystem fs;
    public final String rootPackage;
    public final String modId;
    public ScriptModFile(ScriptJar scriptJar, IModProvider provider, ModFileFactory.ModFileInfoParser parser, String rootPackage, String modId) {
        super(scriptJar, provider, parser, "MOD");
        fs = scriptJar.fileSystem();
        this.rootPackage = rootPackage;
        this.modId = modId;
    }
}
