package net.smackem.fxplayground.events;

import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicLong;

class SimpleEventSubscription<T> implements Flow.Subscription {
    final AtomicLong requestCount = new AtomicLong();
    final Flow.Subscriber<? super T> subscriber;
    final SimpleEventPublisher<T> publisher;

    SimpleEventSubscription(SimpleEventPublisher<T> publisher, Flow.Subscriber<? super T> subscriber) {
        this.publisher = publisher;
        this.subscriber = subscriber;
    }

    @Override
    public void request(long n) {
        this.requestCount.addAndGet(n);
    }

    @Override
    public void cancel() {
        this.publisher.cancelSubscription(this);
    }
}
