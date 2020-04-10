package net.smackem.fxplayground.server;

import javafx.application.Platform;

import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.function.Consumer;

public class OneByOneSubscriber<T> implements Flow.Subscriber<T> {
    private final Consumer<T> consumer;
    private Flow.Subscription subscription;

    public OneByOneSubscriber(Consumer<T> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        subscription.request(1);
    }

    @Override
    public void onNext(T item) {
        this.consumer.accept(item);
        //Platform.runLater(() -> this.consumer.accept(item));
        this.subscription.request(1);
    }

    @Override
    public void onError(Throwable throwable) {
        throwable.printStackTrace();
    }

    @Override
    public void onComplete() {
    }
}
