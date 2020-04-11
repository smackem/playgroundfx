package net.smackem.fxplayground.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Flow;
import java.util.function.Consumer;

public class UnboundedSubscriber<T> implements Flow.Subscriber<T> {
    private final Logger log = LoggerFactory.getLogger(UnboundedSubscriber.class);
    private final Consumer<T> consumer;
    private Flow.Subscription subscription;

    public UnboundedSubscriber(Consumer<T> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        log.debug("onSubscribe");
        subscription.request(1);
        this.subscription = subscription;
    }

    @Override
    public void onNext(T item) {
        log.debug("onNext {}", item);
        this.consumer.accept(item);
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
