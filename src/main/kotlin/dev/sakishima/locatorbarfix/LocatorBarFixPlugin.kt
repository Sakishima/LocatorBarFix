package dev.sakishima.locatorbarfix

import com.github.retrooper.packetevents.PacketEvents
import dev.sakishima.locatorbarfix.application.WaypointGuardService
import dev.sakishima.locatorbarfix.configuration.PluginSettingsLoader
import dev.sakishima.locatorbarfix.infrastructure.packet.WaypointPacketListener
import dev.sakishima.locatorbarfix.infrastructure.persistence.InMemoryWaypointStateRepository
import org.bukkit.plugin.java.JavaPlugin

/**
 * Главный класс плагина LocatorBarFix.
 *
 * Отвечает только за жизненный цикл и сборку зависимостей: прикладная логика,
 * хранение состояния и работа с PacketEvents вынесены в отдельные компоненты.
 */
class LocatorBarFixPlugin : JavaPlugin() {

    private var packetListener: WaypointPacketListener? = null
    private var guardService: WaypointGuardService? = null

    /**
     * Инициализирует настройки, прикладной сервис и регистрирует обработчик PacketEvents.
     */
    override fun onEnable() {
        val packetEventsApi = PacketEvents.getAPI()
        if (!packetEventsApi.isLoaded) {
            logger.severe("PacketEvents не завершил загрузку. LocatorBarFix будет отключён.")
            server.pluginManager.disablePlugin(this)
            return
        }

        val settings = PluginSettingsLoader(this).load()
        val service = WaypointGuardService(InMemoryWaypointStateRepository())
        val listener = WaypointPacketListener(
            guardService = service,
            logger = logger,
            debug = settings.debug
        )

        packetEventsApi.eventManager.registerListener(listener)

        guardService = service
        packetListener = listener

        logger.info("Защита Locator Bar активирована для Minecraft 26.2.")
    }

    /**
     * Снимает обработчик сетевых событий и очищает накопленное состояние.
     */
    override fun onDisable() {
        packetListener?.let { listener ->
            PacketEvents.getAPI().eventManager.unregisterListener(listener)
        }

        guardService?.shutdown()
        packetListener = null
        guardService = null
    }
}
