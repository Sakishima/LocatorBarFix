package dev.sakishima.locatorbarfix.configuration

import org.bukkit.plugin.java.JavaPlugin

/**
 * Загружает настройки плагина из стандартного config.yml.
 *
 * @param plugin экземпляр плагина, предоставляющий Bukkit-конфигурацию.
 */
class PluginSettingsLoader(
    private val plugin: JavaPlugin
) {

    /**
     * Создаёт файл конфигурации при первом запуске и читает настройки.
     *
     * @return неизменяемый набор настроек, используемый обработчиками пакетов.
     */
    fun load(): PluginSettings {
        plugin.saveDefaultConfig()

        return PluginSettings(
            debug = plugin.config.getBoolean("settings.debug", false)
        )
    }
}
