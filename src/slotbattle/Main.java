package slotbattle;

import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        TitleScene titleScene = new TitleScene();
        primaryStage.setScene(titleScene.buildScene(primaryStage));
        primaryStage.setTitle("🎰 スロットバトル 🎰");
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
