package net.smackem.fxplayground;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * JavaFX App
 */
public class App extends Application {

    private Scene scene;
    private static App instance;

    public static App instance() {
        return instance;
    }

    public Scene scene() {
        return this.scene;
    }

    @Override
    public void start(Stage stage) throws IOException {
        if (App.instance != null) {
            throw new RuntimeException("multiple App instances started");
        }
        App.instance = this;
        this.scene = new Scene(loadFXML("main"), 800, 650);
        stage.setScene(this.scene);
        stage.show();
    }

    public static Parent loadFXML(String fxml) throws IOException {
        final FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        launch();
    }

}