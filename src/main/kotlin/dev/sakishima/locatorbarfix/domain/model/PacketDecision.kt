package dev.sakishima.locatorbarfix.domain.model

/**
 * Решение фильтра о дальнейшей отправке исходящего пакета.
 */
enum class PacketDecision {
    /** Пакет можно отправить клиенту без изменений. */
    ALLOW,

    /** Пакет необходимо отменить до отправки клиенту. */
    CANCEL
}
