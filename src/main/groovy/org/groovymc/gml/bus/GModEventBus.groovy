/*
 * Copyright (c) Matyrobbrt
 * SPDX-License-Identifier: MIT
 */

package org.groovymc.gml.bus

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType
import net.neoforged.bus.api.Event
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent
import net.neoforged.fml.event.lifecycle.FMLDedicatedServerSetupEvent
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent
import net.neoforged.fml.loading.FMLEnvironment

@Canonical
@CompileStatic
final class GModEventBus implements IEventBus {
    @Delegate
    final IEventBus delegate

    void onCommonSetup(@ClosureParams(value = SimpleType, options = 'net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent')
                       final Closure<?> closure) {
        addListener(FMLCommonSetupEvent, { event -> closure.call(event) })
    }

    void onClientSetup(@ClosureParams(value = SimpleType, options = 'net.neoforged.fml.event.lifecycle.FMLClientSetupEvent')
                       final Closure<?> closure) {
        if (FMLEnvironment.dist.isClient())
            addListener(FMLClientSetupEvent, { event -> closure.call(event) })
    }

    void onDedicatedServerSetup(@ClosureParams(value = SimpleType, options = 'net.neoforged.fml.event.lifecycle.FMLDedicatedServerSetupEvent')
                                final Closure<?> closure) {
        if (FMLEnvironment.dist.isDedicatedServer())
            addListener(FMLDedicatedServerSetupEvent, { FMLDedicatedServerSetupEvent event -> closure.call(event) })
    }

    void onLoadComplete(@ClosureParams(value = SimpleType, options = 'net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent')
                        final Closure<?> closure) {
        addListener(FMLLoadCompleteEvent, { event -> closure.call(event) })
    }

    def methodMissing(String name, def args) {
        args = args as Object[]
        if (name.startsWith('on') && name.length() > 2) {
            final String event = name.substring(2)
            final GString eventClass = "net.neoforged.fml.event.lifecycle.FML${event}Event"
            try {
                final Class<?> clazz = Class.forName(eventClass)
                final Closure<?> closure = args[0] as Closure<?>
                addListener(clazz as Class<? extends Event>, closure)
            } catch (ClassNotFoundException e) {
                throw new Exception("Cannot find event class \"$eventClass\"", e)
            }
        }
        throw new MissingMethodException(name, this.class, args)
    }
}
