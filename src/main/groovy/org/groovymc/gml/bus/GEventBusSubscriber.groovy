package org.groovymc.gml.bus

import groovy.transform.CompileStatic
import net.neoforged.api.distmarker.Dist
import org.codehaus.groovy.transform.GroovyASTTransformationClass
import org.groovymc.gml.util.Environment

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

@CompileStatic
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@GroovyASTTransformationClass(value = 'org.groovymc.gml.transform.gmods.GEventBusSubscriberTransformer')
@interface GEventBusSubscriber {
    /**
     * Optional value, only necessary if this annotation is not on in a package of your mod. <br>
     * Needed to prevent early classloading of classes not owned by your mod.
     *
     * @return the modid to register listeners as
     */
    String modId() default ''

    /**
     * Specify targets to load this event subscriber on. Can be used to avoid loading Client specific events
     * on a dedicated server, for example.
     *
     * @return an array of Dist to load this event subscriber on
     */
    Dist[] dist() default [Dist.CLIENT, Dist.DEDICATED_SERVER]

    /**
     * Specify the environments this listener will be registered in.
     * @return an array of environments to load this event subscriber on
     */
    Environment[] environment() default [Environment.DEV, Environment.PRODUCTION]
}