package dev.sakishima.locatorbarfix.domain.repository

import dev.sakishima.locatorbarfix.domain.model.PacketDecision
import dev.sakishima.locatorbarfix.domain.model.WaypointPacket
import java.util.UUID

/**
 * Контракт атомарного хранилища состояния waypoint для каждого получателя пакетов.
 *
 * Реализация должна обрабатывать операцию и изменять состояние как единый переход:
 * это важно, поскольку сетевые события могут обрабатываться параллельно.
 */
interface WaypointStateRepository {

    /**
     * Атомарно применяет операцию waypoint и возвращает решение для пакета.
     *
     * @param viewerId UUID игрока, которому отправляется пакет.
     * @param packet нормализованная операция waypoint.
     * @return [PacketDecision.ALLOW], если пакет допустим, иначе [PacketDecision.CANCEL].
     */
    fun process(viewerId: UUID, packet: WaypointPacket): PacketDecision

    /**
     * Удаляет состояние одного получателя, например после его отключения.
     *
     * @param viewerId UUID игрока, состояние которого нужно очистить.
     */
    fun clear(viewerId: UUID)

    /**
     * Удаляет всё состояние при выключении плагина.
     */
    fun clearAll()
}
