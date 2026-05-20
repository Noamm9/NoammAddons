import com.github.noamm9.event.Event
import com.github.noamm9.event.EventBus
import com.github.noamm9.event.EventListener
import com.github.noamm9.event.EventPriority
import kotlin.system.exitProcess
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

object EventBusTest {
    open class TestEvent(cancelable: Boolean = false): Event(cancelable) {
        private var _isCanceled = false
        override var isCanceled: Boolean
            get() = _isCanceled
            set(value) {
                if (cancelable) {
                    _isCanceled = value
                }
            }

        override fun cancel() {
            if (cancelable) {
                _isCanceled = true
            }
        }
    }

    class AnotherTestEvent: TestEvent()

    @JvmStatic
    fun main(args: Array<String>) {
        println("--- Running EventBus Tests ---\n")
        var failed = false

        fun runTest(name: String, testBlock: () -> Unit) {
            EventBus.listeners.clear()
            try {
                testBlock()
                println("[PASS] $name")
            }
            catch (t: Throwable) {
                println("[FAIL] $name: ${t.message}")
                t.printStackTrace()
                failed = true
            }
        }

        runTest("testRegisterAndPost", ::testRegisterAndPost)
        runTest("testPriorityOrdering", ::testPriorityOrdering)
        runTest("testCancellation", ::testCancellation)
        runTest("testUnregister", ::testUnregister)
        runTest("testDeduplication", ::testDeduplication)
        runTest("testEventTargeting", ::testEventTargeting)
        runTest("testSpeed", ::testSpeed)
        runTest("testListenerSpeed", ::testListenerSpeed)
        runTest("testAllSpeed", ::testAllSpeed)

        println("\n--------------------------------------------------")
        if (failed) {
            println("Status: FAILED")
            exitProcess(1)
        }
        else {
            println("Status: SUCCESS")
        }
    }

    fun testRegisterAndPost() {
        var called = false
        var receivedEvent: TestEvent? = null

        val listener = EventBus.register<TestEvent> {
            called = true
            receivedEvent = event
        }

        assertTrue(listener.isRegistered(), "Listener should be registered")

        val eventInstance = TestEvent()
        val canceled = EventBus.post(eventInstance)

        assertTrue(called, "Listener callback should be executed")
        assertEquals(eventInstance, receivedEvent, "Received event should match posted event")
        assertFalse(canceled, "Non-cancelable event should not be reported as canceled")
    }

    fun testPriorityOrdering() {
        val executionOrder = mutableListOf<EventPriority>()

        EventBus.register<TestEvent>(EventPriority.LOW) {
            executionOrder.add(EventPriority.LOW)
        }
        EventBus.register<TestEvent>(EventPriority.HIGHEST) {
            executionOrder.add(EventPriority.HIGHEST)
        }
        EventBus.register<TestEvent>(EventPriority.LOWEST) {
            executionOrder.add(EventPriority.LOWEST)
        }
        EventBus.register<TestEvent>(EventPriority.NORMAL) {
            executionOrder.add(EventPriority.NORMAL)
        }
        EventBus.register<TestEvent>(EventPriority.HIGH) {
            executionOrder.add(EventPriority.HIGH)
        }

        EventBus.post(TestEvent())

        val expectedOrder = listOf(
            EventPriority.HIGHEST,
            EventPriority.HIGH,
            EventPriority.NORMAL,
            EventPriority.LOW,
            EventPriority.LOWEST
        )
        assertEquals(expectedOrder, executionOrder, "Callbacks should be executed in priority order (Highest to Lowest)")
    }

    fun testCancellation() {
        var secondListenerCalled = false

        EventBus.register<TestEvent>(EventPriority.HIGH) {
            event.cancel()
        }

        EventBus.register<TestEvent>(EventPriority.NORMAL) {
            secondListenerCalled = true
            assertTrue(event.isCanceled, "Event should be marked as canceled in subsequent listener")
        }

        val eventInstance = TestEvent(cancelable = true)
        val isCanceledResult = EventBus.post(eventInstance)

        assertTrue(secondListenerCalled, "Subsequent listener should still be called even if event is canceled")
        assertTrue(eventInstance.isCanceled, "Event instance should be canceled")
        assertTrue(isCanceledResult, "EventBus.post should return true for a canceled event")
    }

    fun testUnregister() {
        var callCount = 0

        val listener = EventBus.register<TestEvent> {
            callCount ++
        }

        EventBus.post(TestEvent())
        assertEquals(1, callCount, "Listener should be called once")

        listener.unregister()
        assertFalse(listener.isRegistered(), "Listener should no longer be registered")

        EventBus.post(TestEvent())
        assertEquals(1, callCount, "Listener should not be called after unregistering")
    }

    fun testDeduplication() {
        var callCount = 0

        val listener = EventListener.create<TestEvent> {
            callCount ++
        }
        
        EventBus.register(listener)
        EventBus.register(listener)

        val listenersList = EventBus.listeners[TestEvent::class.java]
        assertNotNull(listenersList, "Listeners list should not be null")
        assertEquals(1, listenersList.size, "Listener should only be present once")

        EventBus.post(TestEvent())
        assertEquals(1, callCount, "Listener should only be called once")
    }

    fun testEventTargeting() {
        var testEventCalled = false
        var anotherEventCalled = false

        EventBus.register<TestEvent> {
            testEventCalled = true
        }

        EventBus.register<AnotherTestEvent> {
            anotherEventCalled = true
        }

        EventBus.post(TestEvent())
        assertTrue(testEventCalled, "TestEvent listener should be called")
        assertFalse(anotherEventCalled, "AnotherTestEvent listener should not be called")

        testEventCalled = false
        EventBus.post(AnotherTestEvent())
        assertFalse(testEventCalled, "TestEvent listener should not be called")
        assertTrue(anotherEventCalled, "AnotherTestEvent listener should be called")
    }

    fun testSpeed() {
        var count = 0
        val listener = EventBus.register<TestEvent> {
            count ++
        }

        val event = TestEvent()
        val iterations = 1_000_000

        val startTime = System.nanoTime()
        for (i in 0 until iterations) {
            EventBus.post(event)
        }
        val endTime = System.nanoTime()

        val durationMs = (endTime - startTime) / 1_000_000.0
        val opsPerSec = (iterations.toDouble() / (endTime - startTime)) * 1_000_000_000

        println("Speed test (single listener): $iterations posts took $durationMs ms (${opsPerSec.toLong()} ops/sec)")
        assertEquals(iterations, count, "Listener should be called exactly $iterations times")

        listener.unregister()
    }

    fun testListenerSpeed() {
        val iterations = 10_000

        var count = 0
        repeat(iterations) {
            EventBus.register<TestEvent> {
                count ++
            }
        }

        val event = TestEvent()

        val startTime = System.nanoTime()
        EventBus.post(event)
        val endTime = System.nanoTime()

        val durationMs = (endTime - startTime) / 1_000_000.0
        val opsPerSec = (iterations.toDouble() / (endTime - startTime)) * 1_000_000_000

        println("Speed test (multi listener): $iterations listeners took $durationMs ms (${opsPerSec.toLong()} ops/sec)")
        assertEquals(iterations, count, "Listener should be called exactly $iterations times")
    }

    fun testAllSpeed() {
        val postIterations = 100_000
        val listenerIterations = 100

        var count = 0
        repeat(listenerIterations) {
            EventBus.register<TestEvent> {
                count ++
            }
        }

        val event = TestEvent()

        val startTime = System.nanoTime()
        repeat(postIterations) { EventBus.post(event) }
        val endTime = System.nanoTime()

        val durationMs = (endTime - startTime) / 1_000_000.0

        println("Speed test (all) listeners took $durationMs ms")
        assertEquals(postIterations * listenerIterations, count, "Listener should be called exactly ${postIterations * listenerIterations} times")
    }
}