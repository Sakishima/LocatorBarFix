package dev.sakishima.locatorbarfix.infrastructure.packet

import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWaypoint
import dev.sakishima.locatorbarfix.application.WaypointGuardService
import dev.sakishima.locatorbarfix.domain.model.WaypointOperation
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Наблюдатель финального результата отправки TRACK и UNTRACK.
 *
 * Работает на MONITOR и не изменяет пакет. Благодаря этому состояние обновляется
 * только после того, как остальные обработчики приняли окончательное решение
 * об отмене события.
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
            val packet = mapper.toDomain(WrapperPlayServerWaypoint(event))
            if (
                packet.operation == WaypointOperation.TRACK ||
                packet.operation == WaypointOperation.UNTRACK
            ) {
                guardService.process(event.user.uuid, packet)
            }
        } catch (exception: Exception) {
            logger.log(
                Level.WARNING,
                "Не удалось обновить состояние отслеживания waypoint.",
                exception
            )
        }
    }
}
