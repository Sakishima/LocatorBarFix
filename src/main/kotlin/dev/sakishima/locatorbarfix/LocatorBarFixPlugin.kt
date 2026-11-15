package dev.sakishima.locatorbarfix

import com.github.retrooper.packetevents.PacketEvents
import dev.sakishima.locatorbarfix.application.WaypointGuardService
import dev.sakishima.locatorbarfix.configuration.PluginSettingsLoader
import dev.sakishima.locatorbarfix.infrastructure.packet.WaypointPacketListener
import dev.sakishima.locatorbarfix.infrastructure.packet.WaypointStateObserver
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
    private var stateObserver: WaypointStateObserver? = null
    private var guardService: WaypointGuardService? = null

    /**
     * Инициализирует настройки, прикладной сервис и регистрирует обработчики PacketEvents.
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
        val guardListener = WaypointPacketListener(
            guardService = service,
            logger = logger,
            debug = settings.debug
        )
        val observer = WaypointStateObserver(
            guardService = service,
            logger = logger
        )

        packetEventsApi.eventManager.registerListener(guardListener)
        packetEventsApi.eventManager.registerListener(observer)

        guardService = service
        packetListener = guardListener
        stateObserver = observer

        logger.info("Защита Locator Bar активирована для Minecraft 26.2.")
    }

    /**
     * Снимает обработчики сетевых событий и очищает накопленное состояние.
     */
    override fun onDisable() {
        val eventManager = PacketEvents.getAPI().eventManager
        packetListener?.let(eventManager::unregisterListener)
        stateObserver?.let(eventManager::unregisterListener)

        guardService?.shutdown()
        packetListener = null
        stateObserver = null
        guardService = null
    }
}
