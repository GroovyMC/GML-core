/*
 * Copyright (c) Matyrobbrt
 * SPDX-License-Identifier: MIT
 */

//file:noinspection GrPackage
import com.matyrobbrt.enhancedgroovy.dsl.ClassTransformer

((ClassTransformer) this.transformer).tap {
    addField name: 'modBus',
             type: 'org.groovymc.gml.bus.GModEventBus',
             modifiers: ['private', 'final']

    addField name: 'gameBus',
             type: 'net.neoforged.bus.api.IEventBus',
             modifiers: ['private', 'final']

    addMethod name: 'getModBus',
              returnType: 'org.groovymc.gml.bus.GModEventBus',
              modifiers: ['private', 'final']

    addMethod name: 'getGameBus',
              returnType: 'net.neoforged.bus.api.IEventBus',
              modifiers: ['private', 'final']
}
