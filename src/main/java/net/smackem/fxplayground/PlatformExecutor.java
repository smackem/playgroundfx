package net.smackem.fxplayground;

import javafx.application.Platform;

import java.util.concurrent.Executor;

public enum PlatformExecutor implements Executor {

    INSTANCE;

    @SuppressWarnings("NullableProblems")
    @Override
    public void execute(Runnable command) {
        if (Platform.isFxApplicationThread()) {
            command.run();
        } else {
            Platform.runLater(command);
        }
    }
}
