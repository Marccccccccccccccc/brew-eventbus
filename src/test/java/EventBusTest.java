import brewdevelopment.eventbus.EventBus;
import brewdevelopment.eventbus.EventListener;
import brewdevelopment.eventbus.event.Event;
import brewdevelopment.eventbus.event.Subscribe;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class EventBusTest {

    static class TestEvent implements Event {}
    static class SubTestEvent extends TestEvent {}

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
        EventBus eventBus = new EventBus();
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
        Module testModule = this.getClass().getModule();

        eventBus.subscribe(TestEvent.class, event -> callCount.incrementAndGet(), testModule);
        eventBus.subscribe(TestEvent.class, event -> callCount.incrementAndGet(), testModule);
        
        eventBus.post(new TestEvent());
        assertEquals(2, callCount.get());

        eventBus.unsubscribeAll(testModule);
        eventBus.post(new TestEvent());
        assertEquals(2, callCount.get(), "No listeners from testModule should be called");
    }

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
