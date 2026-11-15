package dev.sakishima.locatorbarfix.application

import dev.sakishima.locatorbarfix.domain.model.PacketDecision
import dev.sakishima.locatorbarfix.domain.model.WaypointIdentifier
import dev.sakishima.locatorbarfix.domain.model.WaypointOperation
import dev.sakishima.locatorbarfix.domain.model.WaypointPacket
import dev.sakishima.locatorbarfix.infrastructure.persistence.InMemoryWaypointStateRepository
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Проверяет безопасное поведение сервиса для соединений без UUID.
 */
class WaypointGuardServiceTest {

    private val service = WaypointGuardService(InMemoryWaypointStateRepository())
    private val viewer = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
    private val waypoint = WaypointIdentifier.Named("player")

    /**
     * Неизвестное соединение не должно вызывать исключение или блокировку пакета.
     */
    @Test
    fun `missing viewer uuid permits packet without tracking state`() {
        assertEquals(PacketDecision.ALLOW, service.process(null, packet(WaypointOperation.TRACK)))
        assertEquals(PacketDecision.ALLOW, service.process(null, packet(WaypointOperation.UPDATE)))
        assertEquals(PacketDecision.ALLOW, service.process(null, packet(WaypointOperation.UNTRACK)))

        // Операции для анонимного соединения не создают запись реальному игроку.
        assertEquals(PacketDecision.CANCEL, service.process(viewer, packet(WaypointOperation.UPDATE)))
    }

    /**
     * Отключение соединения без UUID не должно очищать чужие метки.
     */
    @Test
    fun `disconnect without uuid keeps active viewer state`() {
        service.process(viewer, packet(WaypointOperation.TRACK))
        service.onViewerDisconnect(null)

        assertEquals(PacketDecision.ALLOW, service.process(viewer, packet(WaypointOperation.UPDATE)))

        service.onViewerDisconnect(viewer)
        assertEquals(PacketDecision.CANCEL, service.process(viewer, packet(WaypointOperation.UPDATE)))
    }

    /**
     * Пакет с выбранной операцией для проверки состояния.
     *
     * @param operation операция протокола.
     * @return доменная модель waypoint-пакета.
     */
    private fun packet(operation: WaypointOperation): WaypointPacket =
        WaypointPacket(operation, waypoint)
}
