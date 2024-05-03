package me.pandras.color_names

import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.ScalableIcon

/**
 * Singleton object that holds the icons used in the plugin.
 */
object PluginIcons {
    /**
     * Icon representing the action or notification related to color names.
     */
    @JvmField
    val ColorNameIcon = IconLoader.getIcon("color-names.svg", PluginIcons::class.java.classLoader) as ScalableIcon
}
