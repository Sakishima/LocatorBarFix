package dev.sakishima.locatorbarfix.domain.model

/**
 * Минимальное представление waypoint-пакета, необходимое для проверки состояния.
 *
 * @property operation тип операции протокола.
 * @property identifier идентификатор метки, к которой относится операция.
 */
data class WaypointPacket(
    val operation: WaypointOperation,
    val identifier: WaypointIdentifier
)
