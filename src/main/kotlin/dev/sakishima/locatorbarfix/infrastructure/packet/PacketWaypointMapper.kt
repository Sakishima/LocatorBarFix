package dev.sakishima.locatorbarfix.infrastructure.packet

import com.github.retrooper.packetevents.protocol.world.waypoint.TrackedWaypoint
import com.github.retrooper.packetevents.util.Either
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWaypoint
import dev.sakishima.locatorbarfix.domain.model.WaypointIdentifier
import dev.sakishima.locatorbarfix.domain.model.WaypointOperation
import dev.sakishima.locatorbarfix.domain.model.WaypointPacket
import java.util.UUID

/**
 * Преобразует модели PacketEvents в независимые от библиотеки доменные модели.
 */
internal class PacketWaypointMapper {

    /**
     * Преобразует обёртку PacketEvents в минимальную модель для прикладного слоя.
     *
     * @param wrapper разобранный исходящий waypoint-пакет.
     * @return доменное представление операции и идентификатора.
     */
    fun toDomain(wrapper: WrapperPlayServerWaypoint): WaypointPacket =
        WaypointPacket(
            operation = when (wrapper.operation) {
                WrapperPlayServerWaypoint.Operation.TRACK -> WaypointOperation.TRACK
                WrapperPlayServerWaypoint.Operation.UNTRACK -> WaypointOperation.UNTRACK
                WrapperPlayServerWaypoint.Operation.UPDATE -> WaypointOperation.UPDATE
            },
            identifier = toDomain(wrapper.waypoint)
        )

    /**
     * Преобразует идентификатор PacketEvents в типобезопасную доменную модель.
     *
     * @param waypoint метка, извлечённая из сетевого пакета.
     * @return UUID- или строковый идентификатор метки.
     */
    private fun toDomain(waypoint: TrackedWaypoint): WaypointIdentifier =
        toDomain(waypoint.identifier)

    /**
     * Разбирает Either, сохраняя различие между UUID и строковым идентификатором.
     *
     * @param identifier идентификатор PacketEvents, в котором задан ровно один вариант.
     * @return соответствующий доменный идентификатор.
     * @throws IllegalStateException если PacketEvents вернул некорректный пустой вариант.
     */
    private fun toDomain(identifier: Either<UUID, String>): WaypointIdentifier =
        if (identifier.isLeft) {
            WaypointIdentifier.Uuid(
                checkNotNull(identifier.left) {
                    "PacketEvents вернул пустой UUID waypoint."
                }
            )
        } else {
            WaypointIdentifier.Named(
                checkNotNull(identifier.right) {
                    "PacketEvents вернул пустое строковое имя waypoint."
                }
            )
        }
}
