package dev.sakishima.locatorbarfix.infrastructure.packet

import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.event.UserDisconnectEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWaypoint
import dev.sakishima.locatorbarfix.application.WaypointGuardService
import dev.sakishima.locatorbarfix.domain.model.PacketDecision
import java.util.UUID
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Фильтр исходящих UPDATE-пакетов Locator Bar.
 *
 * @param guardService сервис, принимающий решение о допустимости UPDATE.
 * @param logger журнал плагина для диагностических сообщений.
 * @param debug включает подробное логирование заблокированных UPDATE.
 */
internal class WaypointPacketListener(
    private val guardService: WaypointGuardService,
    private val logger: Logger,
    private val debug: Boolean
) : PacketListenerAbstract(PacketListenerPriority.HIGHEST) {

    private val mapper = PacketWaypointMapper()

    /**
     * Отменяет UPDATE, если waypoint ещё не зарегистрирован для получателя.
     *
     * TRACK и UNTRACK здесь не меняют состояние: их окончательный статус может
     * измениться в другом обработчике с тем же приоритетом. Фактические переходы
     * фиксирует [WaypointStateObserver] на MONITOR после завершения отмены события.
     *
     * @param event событие отправки пакета PacketEvents.
     */
    override fun onPacketSend(event: PacketSendEvent) {
        if (event.packetType != PacketType.Play.Server.WAYPOINT || event.isCancelled) {
            return
        }

        try {
            val wrapper = WrapperPlayServerWaypoint(event)
            // Остальные операции не относятся к защите UPDATE.
            if (wrapper.operation != WrapperPlayServerWaypoint.Operation.UPDATE) {
                return
            }

            val packet = mapper.toDomain(wrapper)

            // PacketEvents.getUUID() возвращает Java platform type и иногда null.
            // Явная nullable-переменная исключает неявный Kotlin NPE.
            val viewerId: UUID? = event.user.uuid
            if (guardService.process(viewerId, packet) == PacketDecision.CANCEL) {
                event.isCancelled = true

                if (debug) {
                    logger.info(
                        "Заблокирован UPDATE неизвестного waypoint для получателя $viewerId."
                    )
                }
            }
        } catch (exception: Exception) {
            // При ошибке разбора не вмешиваемся в пакетный поток: лучше пропустить
            // потенциально проблемный пакет, чем сломать отправку других данных игроку.
            logger.log(
                Level.WARNING,
                "Не удалось проверить waypoint-пакет; он будет пропущен без фильтрации.",
                exception
            )
        }
    }

    /**
     * Очищает состояние waypoint после отключения сетевого пользователя.
     *
     * @param event событие разрыва соединения PacketEvents.
     */
    override fun onUserDisconnect(event: UserDisconnectEvent) {
        // При отключении до завершения входа UUID может ещё отсутствовать.
        val viewerId: UUID? = event.user.uuid
        guardService.onViewerDisconnect(viewerId)
    }
}
