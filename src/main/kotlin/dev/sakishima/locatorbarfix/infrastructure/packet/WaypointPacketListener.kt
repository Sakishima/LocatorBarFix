package dev.sakishima.locatorbarfix.infrastructure.packet

import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.event.UserDisconnectEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWaypoint
import dev.sakishima.locatorbarfix.application.WaypointGuardService
import dev.sakishima.locatorbarfix.domain.model.PacketDecision
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Адаптер сетевого слоя: извлекает waypoint-пакеты и передаёт их прикладному сервису.
 *
 * @param guardService сервис, принимающий решение о допустимости waypoint-операции.
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
     * Обрабатывает только исходящие waypoint-пакеты.
     *
     * Приоритет HIGHEST выбран, чтобы учитывать отмену пакета предыдущими обработчиками:
     * отменённый TRACK не должен попадать в локальный реестр, иначе последующий UPDATE
     * будет ошибочно принят за безопасный.
     *
     * @param event событие отправки пакета PacketEvents.
     */
    override fun onPacketSend(event: PacketSendEvent) {
        if (event.packetType != PacketType.Play.Server.WAYPOINT) {
            return
        }

        // Не меняем состояние по пакету, который уже отменил другой обработчик.
        if (event.isCancelled) {
            return
        }

        try {
            val wrapper = WrapperPlayServerWaypoint(event)
            val packet = mapper.toDomain(wrapper)
            val viewerId = event.user.uuid

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
        guardService.onViewerDisconnect(event.user.uuid)
    }
}
