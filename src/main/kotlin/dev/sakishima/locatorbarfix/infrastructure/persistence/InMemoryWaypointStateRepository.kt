package dev.sakishima.locatorbarfix.infrastructure.persistence

import dev.sakishima.locatorbarfix.domain.model.PacketDecision
import dev.sakishima.locatorbarfix.domain.model.WaypointIdentifier
import dev.sakishima.locatorbarfix.domain.model.WaypointOperation
import dev.sakishima.locatorbarfix.domain.model.WaypointPacket
import dev.sakishima.locatorbarfix.domain.repository.WaypointStateRepository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Потокобезопасное хранилище waypoint-состояния в памяти процесса.
 *
 * Внешняя карта разделяет метки по UUID получателя. Для одного получателя все
 * переходы выполняются внутри [ConcurrentHashMap.compute], поэтому TRACK, UNTRACK
 * и UPDATE не могут одновременно наблюдать промежуточное состояние друг друга.
 */
class InMemoryWaypointStateRepository : WaypointStateRepository {

    private val trackedByViewer =
        ConcurrentHashMap<UUID, MutableSet<WaypointIdentifier>>()

    /**
     * Атомарно применяет операцию и определяет, допустимо ли отправлять пакет.
     *
     * @param viewerId UUID получателя пакета.
     * @param packet операция и идентификатор waypoint.
     * @return [PacketDecision.CANCEL] только для UPDATE неизвестной этому получателю метки.
     */
    override fun process(viewerId: UUID, packet: WaypointPacket): PacketDecision {
        var decision = PacketDecision.ALLOW

        trackedByViewer.compute(viewerId) { _, tracked ->
            when (packet.operation) {
                WaypointOperation.TRACK -> {
                    val identifiers = tracked ?: ConcurrentHashMap.newKeySet()
                    identifiers.add(packet.identifier)
                    identifiers
                }

                WaypointOperation.UNTRACK -> {
                    if (tracked == null) {
                        null
                    } else {
                        tracked.remove(packet.identifier)
                        // Пустые наборы не храним: это ограничивает расход памяти
                        // игроками, у которых больше нет отслеживаемых waypoint.
                        tracked.takeIf { it.isNotEmpty() }
                    }
                }

                WaypointOperation.UPDATE -> {
                    decision = if (tracked?.contains(packet.identifier) == true) {
                        PacketDecision.ALLOW
                    } else {
                        PacketDecision.CANCEL
                    }
                    tracked
                }
            }
        }

        return decision
    }

    /**
     * Удаляет все waypoint отключившегося получателя.
     *
     * @param viewerId UUID игрока, чьё состояние нужно удалить.
     */
    override fun clear(viewerId: UUID) {
        trackedByViewer.remove(viewerId)
    }

    /**
     * Очищает хранилище целиком.
     */
    override fun clearAll() {
        trackedByViewer.clear()
    }
}
