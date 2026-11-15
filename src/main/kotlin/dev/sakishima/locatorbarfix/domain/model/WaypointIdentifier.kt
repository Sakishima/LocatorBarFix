package dev.sakishima.locatorbarfix.domain.model

import java.util.UUID

/**
 * Идентификатор waypoint, независимый от конкретной сетевой библиотеки.
 *
 * Minecraft допускает два представления идентификатора: UUID и строковое имя.
 * Разные варианты намеренно представлены разными типами, чтобы исключить
 * совпадение UUID и строки с одинаковым текстовым содержимым.
 */
sealed interface WaypointIdentifier {

    /**
     * Идентификатор waypoint в формате UUID.
     *
     * @property value исходный UUID метки.
     */
    data class Uuid(val value: UUID) : WaypointIdentifier

    /**
     * Идентификатор waypoint в строковом формате.
     *
     * @property value исходное имя метки без нормализации.
     */
    data class Named(val value: String) : WaypointIdentifier
}
