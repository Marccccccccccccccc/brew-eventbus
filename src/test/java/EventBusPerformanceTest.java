import brewdevelopment.eventbus.EventBus;
import brewdevelopment.eventbus.IEventBus;
import brewdevelopment.eventbus.event.Event;
import brewdevelopment.eventbus.event.Subscribe;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class EventBusPerformanceTest {

    public static class BenchEvent implements Event {}

    @Test
    void benchmarkLambdaDispatch() {
        IEventBus eventBus = new EventBus();
        int listenerCount = 100;
        int iterations = 1_000_000;
        AtomicInteger counter = new AtomicInteger(0);

        for (int i = 0; i < listenerCount; i++) {
            eventBus.subscribe(BenchEvent.class, event -> counter.incrementAndGet());
        }

        // Warmup
        for (int i = 0; i < 200_000; i++) {
            eventBus.post(new BenchEvent());
        }

        counter.set(0);
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            eventBus.post(new BenchEvent());
        }
        long end = System.nanoTime();

        double totalMs = (end - start) / 1_000_000.0;
        double nsPerPost = (double) (end - start) / iterations;
        double nsPerListener = nsPerPost / listenerCount;

        System.out.println("--- Lambda Dispatch Benchmark ---");
        System.out.println("Listeners: " + listenerCount);
        System.out.println("Iterations: " + iterations);
        System.out.println("Total Time: " + String.format("%.2f", totalMs) + "ms");
        System.out.println("Avg Time per post(): " + String.format("%.2f", nsPerPost) + "ns");
        System.out.println("Avg Time per listener: " + String.format("%.2f", nsPerListener) + "ns");
    }

    public static class AnnotatedContainer {
        public int count = 0;
        @Subscribe
        public void onEvent(BenchEvent e) {
            count++;
        }
    }

    @Test
    void benchmarkAnnotatedDispatch() {
        IEventBus eventBus = new EventBus();
        int iterations = 10_000_000; 
        
        AnnotatedContainer container = new AnnotatedContainer();
        eventBus.register(container);

        // Warmup
        for (int i = 0; i < 1_000_000; i++) {
            eventBus.post(new BenchEvent());
        }

        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            eventBus.post(new BenchEvent());
        }
        long end = System.nanoTime();

        double nsPerPost = (double) (end - start) / iterations;
        System.out.println("--- Annotated Dispatch Benchmark (ASM Caller) ---");
        System.out.println("Iterations: " + iterations);
        System.out.println("Avg Time per post(): " + String.format("%.2f", nsPerPost) + "ns");
        System.out.println("Result Count: " + container.count);
    }
}
