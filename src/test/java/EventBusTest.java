import brewdevelopment.eventbus.Configuration;
import brewdevelopment.eventbus.EventBus;
import brewdevelopment.eventbus.EventListener;
import brewdevelopment.eventbus.event.Event;
import brewdevelopment.eventbus.event.Subscribe;
import brewdevelopment.eventbus.event.CancellableEvent;
import brewdevelopment.eventbus.event.MutableEventValue;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class EventBusTest {

    static class TestEvent implements Event {}

    @Test
    void testBasicSubscription() {
        EventBus eventBus = new EventBus();
        AtomicInteger callCount = new AtomicInteger(0);

        eventBus.subscribe(TestEvent.class, event -> callCount.incrementAndGet(), null);
        eventBus.post(new TestEvent());

        assertEquals(1, callCount.get());
    }

    @Test
    void testPriority() {
        EventBus eventBus = new EventBus();
        List<Integer> results = new ArrayList<>();

        eventBus.subscribe(TestEvent.class, event -> results.add(1), null, 1);
        eventBus.subscribe(TestEvent.class, event -> results.add(2), null, 2);
        eventBus.subscribe(TestEvent.class, event -> results.add(0), null, 0);

        eventBus.post(new TestEvent());

        assertEquals(List.of(2, 1, 0), results, "Listeners should be called in priority order (high to low)");
    }

    @Test
    void testAnnotation() {
        EventBus eventBus = new EventBus();
        AtomicBoolean called = new AtomicBoolean(false);

        Object container = new Object() {
            @Subscribe
            public void onEvent(TestEvent event) {
                called.set(true);
            }
        };

        eventBus.register(container, null);
        eventBus.post(new TestEvent());

        assertTrue(called.get(), "Annotated listener should have been called");
    }

    @Test
    void testExceptionHandling() {
        EventBus eventBus = new EventBus(Configuration.builder().enableErrorCallbacks(true).warnOnInvalidListenerMethod(true).build());
        AtomicBoolean exceptionHandled = new AtomicBoolean(false);

        eventBus.setExceptionHandler((event, listener, exception) -> {
            exceptionHandled.set(true);
        });

        eventBus.subscribe(TestEvent.class, event -> {
            throw new RuntimeException("Test exception");
        }, null);

        assertDoesNotThrow(() -> eventBus.post(new TestEvent()), "Post should not throw even if listener fails");
        assertTrue(exceptionHandled.get(), "Exception handler should have been notified");
    }

    @Test
    void testUnsubscribe() {
        EventBus eventBus = new EventBus();
        AtomicInteger callCount = new AtomicInteger(0);
        EventListener<TestEvent> listener = event -> callCount.incrementAndGet();

        eventBus.subscribe(TestEvent.class, listener, null);
        eventBus.post(new TestEvent());
        assertEquals(1, callCount.get());

        eventBus.unsubscribe(listener);
        eventBus.post(new TestEvent());
        assertEquals(1, callCount.get(), "Listener should not be called after unsubscription");
    }

    @Test
    void testUnsubscribeAll() {
        EventBus eventBus = new EventBus();
        AtomicInteger callCount = new AtomicInteger(0);
        Object testOwner = new Object();

        eventBus.subscribe(TestEvent.class, event -> callCount.incrementAndGet(), testOwner);
        eventBus.subscribe(TestEvent.class, event -> callCount.incrementAndGet(), testOwner);
        
        eventBus.post(new TestEvent());
        assertEquals(2, callCount.get());

        eventBus.unregister(testOwner);
        eventBus.post(new TestEvent());
        assertEquals(2, callCount.get(), "No listeners from testOwner should be called");
    }

    @Test
    void testAsyncDispatch() throws InterruptedException {
        EventBus eventBus = new EventBus();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean asyncCalled = new AtomicBoolean(false);

        eventBus.subscribe(TestEvent.class, event -> {
            asyncCalled.set(true);
            latch.countDown();
        }, null, 0, true);

        eventBus.post(new TestEvent());
        assertFalse(asyncCalled.get(), "Async listener should not have been called immediately");

        assertTrue(latch.await(1, TimeUnit.SECONDS), "Async listener should be called within 1 second");
        assertTrue(asyncCalled.get());
        
        eventBus.shutdown();
    }

    @Test
    void testAsyncConstraints() {
        EventBus eventBus = new EventBus();
        
        assertThrows(IllegalArgumentException.class, () -> {
            eventBus.subscribe(CancellableTestEvent.class, event -> {}, null, 0, true);
        }, "Should forbid async subscription to CancellableEvent");

        assertThrows(IllegalArgumentException.class, () -> {
            eventBus.subscribe(MutableTestEvent.class, event -> {}, null, 0, true);
        }, "Should forbid async subscription to MutableEventValue");
    }

    static class CancellableTestEvent implements CancellableEvent {
        @Override public boolean isCancelled() { return false; }
        @Override public boolean cancel() { return true; }
    }

    static class MutableTestEvent implements MutableEventValue<String> {
        private String val;
        @Override public String value() { return val; }
        @Override public void set(String value) { this.val = value; }
    }

    @Test
    void testRecordEvent() {
        EventBus eventBus = new EventBus();
        AtomicBoolean called = new AtomicBoolean(false);

        eventBus.subscribe(TestRecordEvent.class, event -> {
            if (event.message().equals("Hello")) {
                called.set(true);
            }
        }, null);

        eventBus.post(new TestRecordEvent("Hello"));
        assertTrue(called.get(), "Record event should be handled correctly");
    }

    record TestRecordEvent(String message) implements Event {}

    interface DeepInterface extends Event {}
    interface SubDeepInterface extends DeepInterface {}
    static class DeepEvent extends TestEvent implements SubDeepInterface {}

    @Test
    void testPolymorphicDispatch() {
        EventBus eventBus = new EventBus();
        AtomicInteger testEventCount = new AtomicInteger(0);
        AtomicInteger eventCount = new AtomicInteger(0);
        AtomicInteger interfaceCount = new AtomicInteger(0);

        eventBus.subscribe(TestEvent.class, event -> testEventCount.incrementAndGet(), null);
        eventBus.subscribe(Event.class, event -> eventCount.incrementAndGet(), null);
        eventBus.subscribe(DeepInterface.class, event -> interfaceCount.incrementAndGet(), null);

        eventBus.post(new DeepEvent());

        assertEquals(1, testEventCount.get(), "Should trigger TestEvent listener");
        assertEquals(1, eventCount.get(), "Should trigger Event listener");
        assertEquals(1, interfaceCount.get(), "Should trigger DeepInterface listener");
    }
}
