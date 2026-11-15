package dev.sakishima.locatorbarfix.configuration

/**
 * Неизменяемый набор настроек LocatorBarFix.
 *
 * @property debug включает диагностические сообщения о заблокированных пакетах.
 */
data class PluginSettings(
    val debug: Boolean
)
