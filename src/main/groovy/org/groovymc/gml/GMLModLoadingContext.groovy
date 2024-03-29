/*
 * Copyright (c) Matyrobbrt
 * SPDX-License-Identifier: MIT
 */

//file:noinspection unused
package org.groovymc.gml

import org.groovymc.gml.bus.GModEventBus
import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import groovy.transform.PackageScopeTarget
import net.neoforged.fml.ModLoadingContext

@CompileStatic
@PackageScope([PackageScopeTarget.CONSTRUCTORS])
class GMLModLoadingContext {

    private final GModContainer container

    GMLModLoadingContext(GModContainer container) {
        this.container = container
    }

    /**
     * @return the mod's event bus, to allow subscription to mod-specific events
     */
    GModEventBus getModEventBus() {
        return container.modBus
    }

    /**
     * @return the mod's container
     */
    GModContainer getContainer() {
        return container
    }

    /**
     * Helper to get the right instance from the {@link net.neoforged.fml.ModLoadingContext} correctly.
     * @return The GMLMod language specific extension from the ModLoadingContext
     */
    static GMLModLoadingContext get() {
        return ModLoadingContext.get().extension()
    }

}
