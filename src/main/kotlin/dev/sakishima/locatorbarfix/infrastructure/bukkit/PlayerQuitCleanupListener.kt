package dev.sakishima.locatorbarfix.infrastructure.bukkit

import dev.sakishima.locatorbarfix.application.WaypointGuardService
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

/**
 * Дополнительная очистка состояния после отключения вошедшего игрока.
 *
 * PacketEvents может не предоставлять UUID при UserDisconnectEvent, поэтому
 * используем Bukkit PlayerQuitEvent с известным идентификатором игрока.
 *
 * @param guardService сервис очистки состояния получателя.
 */
internal class PlayerQuitCleanupListener(
    private val guardService: WaypointGuardService
) : Listener {

    /**
     * Освобождает записи покинувшего сервер игрока по его реальному UUID.
     *
     * @param event событие отключения игрока Bukkit.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onQuit(event: PlayerQuitEvent) {
        guardService.onViewerDisconnect(event.player.uniqueId)
    }
}
