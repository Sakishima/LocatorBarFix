package dev.sakishima.locatorbarfix.infrastructure.packet

import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWaypoint
import dev.sakishima.locatorbarfix.application.WaypointGuardService
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Наблюдатель неотменённых TRACK и UNTRACK.
 *
 * Работает на MONITOR и не изменяет пакет. Событие проверяется после
 * предыдущих обработчиков, но это не подтверждает доставку пакета клиенту.
 *
 * @param guardService сервис управления состоянием waypoint.
 * @param logger журнал плагина для сообщений об ошибках разбора.
 */
internal class WaypointStateObserver(
    private val guardService: WaypointGuardService,
    private val logger: Logger
) : PacketListenerAbstract(PacketListenerPriority.MONITOR) {

    private val mapper = PacketWaypointMapper()

    /**
     * Фиксирует TRACK и UNTRACK, которые действительно будут отправлены клиенту.
     *
     * @param event событие отправки пакета PacketEvents.
     */
    override fun onPacketSend(event: PacketSendEvent) {
        if (event.packetType != PacketType.Play.Server.WAYPOINT || event.isCancelled) {
            return
        }

        try {
            val wrapper = WrapperPlayServerWaypoint(event)
            if (
                wrapper.operation != WrapperPlayServerWaypoint.Operation.TRACK &&
                wrapper.operation != WrapperPlayServerWaypoint.Operation.UNTRACK
            ) {
                return
            }

            guardService.process(event.user.uuid, mapper.toDomain(wrapper))
        } catch (exception: Exception) {
            logger.log(
                Level.WARNING,
                "Не удалось обновить состояние отслеживания waypoint.",
                exception
            )
        }
    }
}
