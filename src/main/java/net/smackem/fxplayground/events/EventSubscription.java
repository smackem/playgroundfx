package net.smackem.fxplayground.events;

import java.util.concurrent.Flow;

public interface EventSubscription extends Flow.Subscription {
    void suspend();
    void resume();
}
