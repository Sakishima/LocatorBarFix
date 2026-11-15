package dev.sakishima.locatorbarfix.domain.model

/**
 * Операции протокола, применяемые к отслеживаемой метке.
 */
enum class WaypointOperation {
    /** Сообщает клиенту о начале отслеживания метки. */
    TRACK,

    /** Сообщает клиенту о прекращении отслеживания метки. */
    UNTRACK,

    /** Обновляет уже известную клиенту метку. */
    UPDATE
}
