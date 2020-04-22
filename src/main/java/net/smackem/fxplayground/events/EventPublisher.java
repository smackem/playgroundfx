package net.smackem.fxplayground.events;

import java.util.concurrent.Flow;
import java.util.function.Consumer;

public interface EventPublisher<T> extends Flow.Publisher<T> {
    Flow.Subscription subscribe(Consumer<T> handler);
}
