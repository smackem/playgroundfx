package net.smackem.fxplayground.server;

import java.util.concurrent.Flow;
import java.util.function.Consumer;

public class UnboundedSubscriber<T> implements Flow.Subscriber<T> {
    private final Consumer<T> consumer;

    public UnboundedSubscriber(Consumer<T> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        subscription.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(T item) {
        this.consumer.accept(item);
    }

    @Override
    public void onError(Throwable throwable) {
        throwable.printStackTrace();
    }

    @Override
    public void onComplete() {
    }
}
