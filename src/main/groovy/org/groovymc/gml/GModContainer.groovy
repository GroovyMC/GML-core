/*
 * Copyright (c) Matyrobbrt
 * SPDX-License-Identifier: MIT
 */

package org.groovymc.gml

import com.google.common.base.Suppliers
import groovy.transform.CompileStatic
import groovy.transform.TupleConstructor
import groovy.util.logging.Slf4j
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.EventBusErrorMessage
import net.neoforged.bus.api.BusBuilder
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.ModLoadingException
import net.neoforged.fml.ModLoadingIssue
import net.neoforged.fml.event.IModBusEvent
import net.neoforged.fml.javafmlmod.AutomaticEventSubscriber
import net.neoforged.fml.loading.FMLLoader
import net.neoforged.neoforgespi.language.IModInfo
import net.neoforged.neoforgespi.language.ModFileScanData
import org.groovymc.gml.bus.GEventBusSubscriber
import org.groovymc.gml.bus.GModEventBus
import org.groovymc.gml.internal.GMLLangLoader
import org.groovymc.gml.util.Environment
import org.groovymc.gml.util.Reflections
import org.objectweb.asm.Type

import java.lang.invoke.MethodType
import java.lang.reflect.Constructor
import java.util.function.Consumer
import java.util.function.Supplier

@Slf4j
@CompileStatic
final class GModContainer extends ModContainer {
    private static final Type GEBS = Type.getType(GEventBusSubscriber)
    private static Consumer<IModInfo> packMetaInjector

    private final List<Class<?>> modClasses
    private final Map<Class<?>, Supplier<Object>> modInstances = [:]
    private final Module module

    private final GModEventBus modBus
    private final ModFileScanData scanData

    GModContainer(final IModInfo info, List<String> entrypoints, final ModFileScanData scanData, final ModuleLayer layer) {
        super(info)
        this.scanData = scanData

        modBus = new GModEventBus(BusBuilder.builder()
                    .setExceptionHandler { bus, event, listeners, i, cause -> log.error('Failed to process mod event: {}', new EventBusErrorMessage(event, i, listeners, cause)) }
                    .markerType(IModBusEvent)
                    .allowPerPhasePost()
                    .build())

        this.module = layer.findModule(info.owningFile.moduleName()).orElseThrow()

        this.modClasses = new ArrayList()
        for (String entrypoint : entrypoints) {
            try {
                Class<?> cls = Class.forName(module, entrypoint)
                this.modClasses.add(cls)
                log.trace("Loaded modclass {} with {}", cls.getName(), cls.getClassLoader())
            } catch (Throwable e) {
                log.error("Failed to load class {}", entrypoint, e)
                throw new ModLoadingException(ModLoadingIssue.error("fml.modloading.failedtoloadmodclass").withCause(e).withAffectedMod(info))
            }
        }

        // TODO: re-enable script mods
        /*
        if (ScriptFileCompiler.isScriptMod(info.owningFile.file)) {
            // generate the pack.mcmeta for the script
            packMetaInjector(module.classLoader).accept(info)
        }
        */
    }

    private static Consumer<IModInfo> packMetaInjector(ClassLoader loader) {
        if (packMetaInjector !== null) return packMetaInjector
        final int[] formatVersions = (int[]) Class.forName('org.groovymc.gml.mod.PackMCMetaVersionsGetter', true, loader)
                .getDeclaredMethod('get')
                .invoke(null)
        return packMetaInjector = Reflections.<Consumer<IModInfo>>constructor(ClassLoader.forName('org.groovymc.gml.scriptmods.PackMetaInjector'), MethodType.methodType(void, int, int)).call(formatVersions[0], formatVersions[1])
    }

    protected void constructMod() {
        for (Class<?> modClass : modClasses) {
            constructMod(modClass)
        }
        injectEBS()
    }

    private void injectEBS() {
        AutomaticEventSubscriber.inject(this, scanData, module)

        scanData.annotations.findAll { it.annotationType() == GEBS }
                .each {
                    final modId = it.annotationData()['modId'] as String
                    final boolean isInMod = { ModFileScanData.AnnotationData data ->
                        if (modId !== null && !modId.isEmpty()) {
                            return modId == this.getModId()
                        }
                        return modClasses.any { data.clazz().getClassName().startsWith("${it.packageName}.") }
                    }.call(it)

                    if (!isInMod) return

                    if (!(AutomaticEventSubscriber.getSides(it.annotationData().get('dist')).contains(FMLLoader.getDist()) &&
                            GMLLangLoader.getEnvironments(it.annotationData().get('environment')).contains(Environment.current()))) {
                        return
                    }

                    log.info('Auto-Subscribing GEventBusSubscriber class {}', it.clazz().className)

                    final clazz = Class.forName(module, it.clazz().className)
                    final obj = enter(clazz).get()
                    if (clazz.getMethod('gml$registerListeners') !== null) {
                        try {
                            obj.invokeMethod('gml$registerListeners', [])
                        } catch (Throwable t) {
                            log.error('Failed to register listeners for class {}', clazz.name, t)
                            throw new ModLoadingException(ModLoadingIssue.error("fml.modloading.failedtoloadmod").withCause(t).withAffectedMod(this.modInfo))
                        }
                    } else {
                        log.error('Failed to find generated registration method in class {}; the @GEventBusSubscriber ASTT may not have ran', clazz.name)
                        throw new ModLoadingException(ModLoadingIssue.error("fml.modloading.failedtoloadmod").withAffectedMod(this.modInfo))
                    }
                }
    }

    @TupleConstructor(includeFields = true)
    private static class FoundCtor {
        public final Constructor<?> ctor
        private final List<Set<Class<?>>> argTypes
    }

    private synchronized Supplier<Object> enter(Class<?> clazz) {
        if (modInstances.containsKey(clazz)) {
            return modInstances[clazz]
        }
        var ctors = clazz.getDeclaredConstructors()
        Map<Class<?>, Object> allowedConstructorArgs = [
                (GModEventBus.class): this.eventBus,
                (GModContainer.class): this,
                (Dist.class): FMLLoader.getDist(),
                (Environment.class): Environment.current()
        ]
        var argTypes = ctors.collect {
            new FoundCtor(it, it.parameterTypes.collect { argType -> allowedConstructorArgs.keySet().findAll { t -> argType.isAssignableFrom(t) } })
        }.findAll {
            it.argTypes.every { it.size() == 1 }
        }
        if (argTypes.size() != 1) {
            Exception e = new RuntimeException("Ambiguous constructor for mod class ${clazz.name}")
            log.error("Failed to create mod instance for class {}", clazz.name, e)
            throw new ModLoadingException(ModLoadingIssue.error("fml.modloading.failedtoloadmod").withCause(e).withAffectedMod(this.modInfo))
        }
        var ctor = argTypes[0].ctor
        var args = argTypes[0].argTypes.collect { allowedConstructorArgs[it[0]] }
        // Groovy doesn't like varargs with Object...
        Supplier<Object> supplier = Suppliers.memoize {
            try {
                ctor.invokeMethod('newInstance', args.toArray())
            } catch (Throwable t) {
                log.error('Failed to create mod instance for class {}', clazz.name, t)
                throw new ModLoadingException(ModLoadingIssue.error("fml.modloading.failedtoloadmod").withCause(t).withAffectedMod(this.modInfo))
            }
        }
        modInstances[clazz] = supplier
    }

    GModEventBus getModBus() {
        modBus
    }

    @Override
    IEventBus getEventBus() {
        return modBus
    }

    void constructMod(Class<?> clazz) {
        try {
            log.debug('Loading mod class {} for {}', clazz.name, this.modId)
            def obj = enter(clazz).get()
            if (obj instanceof Script) obj.run()
            log.debug('Successfully loaded mod {}', this.modId)
        } catch (final Throwable t) {
            log.error('Failed to create mod from class {} for modid {}', clazz.name, modId, t)
            throw new ModLoadingException(ModLoadingIssue.error("fml.modloading.failedtoloadmod").withCause(t).withAffectedMod(this.modInfo))
        }
    }
}
