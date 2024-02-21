/*
 * Copyright (c) Matyrobbrt
 * SPDX-License-Identifier: MIT
 */

package org.groovymc.gml.bus.type

/**
 * Indicates a type of event bus.
 * @see GameBus
 * @see ModBus
 */
sealed interface BusType permits GameBus, ModBus {
}
