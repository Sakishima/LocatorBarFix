package dev.sakishima.locatorbarfix.application

import dev.sakishima.locatorbarfix.domain.model.PacketDecision
import dev.sakishima.locatorbarfix.domain.model.WaypointPacket
import dev.sakishima.locatorbarfix.domain.repository.WaypointStateRepository
import java.util.UUID

/**
 * Прикладной сервис защиты waypoint-пакетов.
 *
 * Координирует обработку пакетов и жизненный цикл состояния, не завися от PacketEvents
 * или Bukkit API.
 *
 * @param stateRepository хранилище, выполняющее атомарные переходы состояния waypoint.
 */
class WaypointGuardService(
    private val stateRepository: WaypointStateRepository
) {

    /**
     * Проверяет пакет и применяет соответствующий переход состояния.
     *
     * @param viewerId UUID получателя пакета; может отсутствовать у соединений до входа.
     * @param packet нормализованная операция waypoint.
     * @return разрешение отправки, если UUID отсутствует; иначе решение хранилища.
     */
    fun process(viewerId: UUID?, packet: WaypointPacket): PacketDecision =
        if (viewerId == null) {
            // У неизвестного соединения нет связанного реестра waypoint.
            // Не отменяем пакет: сохранение обычной работы Locator Bar важнее.
            PacketDecision.ALLOW
        } else {
            stateRepository.process(viewerId, packet)
        }

    /**
     * Удаляет состояние игрока после завершения его сетевого соединения.
     *
     * @param viewerId UUID отключившегося игрока или null у соединения без авторизации.
     */
    fun onViewerDisconnect(viewerId: UUID?) {
        // PacketEvents может вызвать UserDisconnectEvent до назначения UUID.
        // Такое соединение не имеет записей в хранилище.
        if (viewerId != null) {
            stateRepository.clear(viewerId)
        }
    }

    /**
     * Освобождает все записи состояния при выключении плагина.
     */
    fun shutdown() {
        stateRepository.clearAll()
    }
}
