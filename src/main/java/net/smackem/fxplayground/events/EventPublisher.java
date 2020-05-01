package net.smackem.fxplayground.events;

import java.util.concurrent.Flow;
import java.util.function.Consumer;

public interface EventPublisher<T> extends Flow.Publisher<T> {
    EventSubscription subscribe(Consumer<T> handler);
}
