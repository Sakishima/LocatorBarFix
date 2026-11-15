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
     * @param viewerId UUID получателя пакета.
     * @param packet нормализованная операция waypoint.
     * @return решение, разрешающее отправку пакета или отменяющее её.
     */
    fun process(viewerId: UUID, packet: WaypointPacket): PacketDecision =
        stateRepository.process(viewerId, packet)

    /**
     * Удаляет состояние игрока после завершения его сетевого соединения.
     *
     * @param viewerId UUID отключившегося игрока.
     */
    fun onViewerDisconnect(viewerId: UUID) {
        stateRepository.clear(viewerId)
    }

    /**
     * Освобождает все записи состояния при выключении плагина.
     */
    fun shutdown() {
        stateRepository.clearAll()
    }
}
