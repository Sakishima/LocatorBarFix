package dev.sakishima.locatorbarfix.infrastructure.persistence

import dev.sakishima.locatorbarfix.domain.model.PacketDecision
import dev.sakishima.locatorbarfix.domain.model.WaypointIdentifier
import dev.sakishima.locatorbarfix.domain.model.WaypointOperation
import dev.sakishima.locatorbarfix.domain.model.WaypointPacket
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Проверяет переходы состояния waypoint и изоляцию данных разных получателей.
 */
class InMemoryWaypointStateRepositoryTest {

    private val repository = InMemoryWaypointStateRepository()
    private val viewer = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
    private val otherViewer = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
    private val waypoint = WaypointIdentifier.Uuid(
        UUID.fromString("11111111-1111-1111-1111-111111111111")
    )

    /**
     * Проверяет, что UPDATE без предварительного TRACK отменяется.
     */
    @Test
    fun `update without track is cancelled`() {
        assertEquals(PacketDecision.CANCEL, process(viewer, WaypointOperation.UPDATE))
    }

    /**
     * Проверяет, что TRACK регистрирует waypoint и разрешает последующие UPDATE.
     */
    @Test
    fun `tracked waypoint accepts update`() {
        assertEquals(PacketDecision.ALLOW, process(viewer, WaypointOperation.TRACK))
        assertEquals(PacketDecision.ALLOW, process(viewer, WaypointOperation.UPDATE))
    }

    /**
     * Проверяет, что UNTRACK удаляет waypoint из состояния получателя.
     */
    @Test
    fun `untracked waypoint rejects later update`() {
        process(viewer, WaypointOperation.TRACK)
        assertEquals(PacketDecision.ALLOW, process(viewer, WaypointOperation.UNTRACK))
        assertEquals(PacketDecision.CANCEL, process(viewer, WaypointOperation.UPDATE))
    }

    /**
     * Проверяет, что один и тот же waypoint отслеживается независимо для каждого игрока.
     */
    @Test
    fun `waypoint state is isolated per viewer`() {
        process(viewer, WaypointOperation.TRACK)

        assertEquals(PacketDecision.ALLOW, process(viewer, WaypointOperation.UPDATE))
        assertEquals(PacketDecision.CANCEL, process(otherViewer, WaypointOperation.UPDATE))
    }

    /**
     * Проверяет, что UUID и строковое имя не смешиваются даже при одинаковом тексте.
     */
    @Test
    fun `uuid and named identifiers are distinct`() {
        val namedWaypoint = WaypointIdentifier.Named(waypoint.value.toString())
        process(viewer, WaypointOperation.TRACK)

        assertEquals(
            PacketDecision.CANCEL,
            repository.process(viewer, packet(WaypointOperation.UPDATE, namedWaypoint))
        )
    }

    /**
     * Проверяет, что неизвестный UNTRACK не затрагивает другие метки.
     */
    @Test
    fun `unknown untrack does not affect tracked waypoint`() {
        val anotherWaypoint = WaypointIdentifier.Named("home")

        repository.process(viewer, packet(WaypointOperation.TRACK, waypoint))
        repository.process(viewer, packet(WaypointOperation.UNTRACK, anotherWaypoint))

        assertEquals(
            PacketDecision.ALLOW,
            repository.process(viewer, packet(WaypointOperation.UPDATE, waypoint))
        )
    }

    /**
     * Проверяет очистку состояния после отключения и выключения плагина.
     */
    @Test
    fun `clear removes viewer state`() {
        process(viewer, WaypointOperation.TRACK)
        repository.clear(viewer)

        assertEquals(PacketDecision.CANCEL, process(viewer, WaypointOperation.UPDATE))

        process(viewer, WaypointOperation.TRACK)
        repository.clearAll()

        assertEquals(PacketDecision.CANCEL, process(viewer, WaypointOperation.UPDATE))
    }


    /**
     * Повторный TRACK должен возобновить обновления после UNTRACK.
     */
    @Test
    fun `retrack after untrack restores updates`() {
        process(viewer, WaypointOperation.TRACK)
        process(viewer, WaypointOperation.UNTRACK)
        assertEquals(PacketDecision.CANCEL, process(viewer, WaypointOperation.UPDATE))

        process(viewer, WaypointOperation.TRACK)
        assertEquals(PacketDecision.ALLOW, process(viewer, WaypointOperation.UPDATE))
    }

    /**
     * UNTRACK должен удалять одну указанную метку, не затрагивая остальные.
     */
    @Test
    fun `untrack removes only selected waypoint`() {
        val another = WaypointIdentifier.Named("spawn")
        repository.process(viewer, packet(WaypointOperation.TRACK, waypoint))
        repository.process(viewer, packet(WaypointOperation.TRACK, another))

        repository.process(viewer, packet(WaypointOperation.UNTRACK, waypoint))

        assertEquals(PacketDecision.CANCEL, process(viewer, WaypointOperation.UPDATE))
        assertEquals(
            PacketDecision.ALLOW,
            repository.process(viewer, packet(WaypointOperation.UPDATE, another))
        )
    }

    /**
     * Очистка одного игрока не должна затрагивать состояние другого.
     */
    @Test
    fun `clear one viewer does not clear another`() {
        process(viewer, WaypointOperation.TRACK)
        process(otherViewer, WaypointOperation.TRACK)
        repository.clear(viewer)

        assertEquals(PacketDecision.CANCEL, process(viewer, WaypointOperation.UPDATE))
        assertEquals(PacketDecision.ALLOW, process(otherViewer, WaypointOperation.UPDATE))
    }

    /**
     * Проверяет атомарность переходов хранилища при параллельных вызовах.
     *
     * Тест не подтверждает порядок доставки реальных сетевых пакетов.
     */
    @Test
    fun `concurrent transitions preserve repository consistency`() {
        val identifiers = (0 until 128).map { WaypointIdentifier.Named("test-$it") }
        val executor = Executors.newFixedThreadPool(8)

        try {
            val tasks = identifiers.map { identifier ->
                Callable {
                    assertEquals(
                        PacketDecision.ALLOW,
                        repository.process(viewer, packet(WaypointOperation.TRACK, identifier))
                    )
                    assertEquals(
                        PacketDecision.ALLOW,
                        repository.process(viewer, packet(WaypointOperation.UPDATE, identifier))
                    )
                    assertEquals(
                        PacketDecision.ALLOW,
                        repository.process(viewer, packet(WaypointOperation.UNTRACK, identifier))
                    )
                }
            }
            executor.invokeAll(tasks).forEach { it.get(10, TimeUnit.SECONDS) }

            identifiers.forEach { identifier ->
                assertEquals(
                    PacketDecision.CANCEL,
                    repository.process(viewer, packet(WaypointOperation.UPDATE, identifier))
                )
            }
        } finally {
            executor.shutdownNow()
        }
    }

    /**
     * Создаёт тестовый пакет для заданной операции.
     *
     * @param viewerId получатель пакета.
     * @param operation операция протокола.
     * @return решение, полученное после обработки тестового пакета.
     */
    private fun process(
        viewerId: UUID,
        operation: WaypointOperation
    ): PacketDecision = repository.process(viewerId, packet(operation, waypoint))

    /**
     * Создаёт доменную модель waypoint-пакета.
     *
     * @param operation операция протокола.
     * @param identifier идентификатор метки.
     * @return пакет для передачи в тестируемое хранилище.
     */
    private fun packet(
        operation: WaypointOperation,
        identifier: WaypointIdentifier
    ) = WaypointPacket(operation, identifier)
}
