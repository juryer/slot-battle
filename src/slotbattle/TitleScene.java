package slotbattle;

import java.io.InputStream;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class TitleScene {

    public Scene buildScene(Stage stage) {

        // ===== 背景画像 =====
        StackPane root = new StackPane();

        // 背景画像（title_bg.png があれば使う、なければ黒背景）
        InputStream bgStream = getClass().getResourceAsStream("/resources/images/title_bg.png");
        if (bgStream != null) {
            ImageView bg = new ImageView(new Image(bgStream));
            bg.setFitWidth(680);
            bg.setFitHeight(520);
            bg.setPreserveRatio(false);
            root.getChildren().add(bg);
        } else {
            root.setStyle("-fx-background-color: #0d0d1a;");
        }

        // ===== コンテンツ（背景の上に重ねる） =====
        VBox content = new VBox(32);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(60, 40, 60, 40));

        // ===== タイトルテキスト =====
        Label titleLabel = new Label("スロットバトル");
        titleLabel.setFont(Font.font("Monospaced", FontWeight.BOLD, 48));
        titleLabel.setTextFill(Color.GOLD);
        titleLabel.setStyle(
            "-fx-effect: dropshadow(gaussian, rgba(255,200,0,0.9), 20, 0.5, 0, 0);"
        );

        Label subLabel = new Label("～ 運だけで決まる最悪のバトル ～");
        subLabel.setFont(Font.font("Monospaced", FontWeight.NORMAL, 16));
        subLabel.setTextFill(Color.LIGHTGRAY);

        VBox titleBox = new VBox(8, titleLabel, subLabel);
        titleBox.setAlignment(Pos.CENTER);
        titleBox.setPadding(new Insets(16, 32, 16, 32));
        titleBox.setStyle("-fx-background-color: rgba(0,0,0,0.6); -fx-background-radius: 12;");

        // ===== ボタンエリア =====
        Button startButton = new Button("▶  START  （CPU対戦）");
        startButton.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        startButton.setMinWidth(300);
        startButton.setMinHeight(56);
        startButton.setStyle(
            "-fx-background-color: #27ae60; -fx-text-fill: white; " +
            "-fx-background-radius: 10; -fx-cursor: hand;"
        );
        startButton.setOnMouseEntered(e ->
            startButton.setStyle(
                "-fx-background-color: #2ecc71; -fx-text-fill: white; " +
                "-fx-background-radius: 10; -fx-cursor: hand;")
        );
        startButton.setOnMouseExited(e ->
            startButton.setStyle(
                "-fx-background-color: #27ae60; -fx-text-fill: white; " +
                "-fx-background-radius: 10; -fx-cursor: hand;")
        );
        startButton.setOnAction(e -> {
            CharacterSelectScene select = new CharacterSelectScene(false);
            stage.setScene(select.buildScene(stage));
        });

        Button vsButton = new Button("⚔  VS  （対人戦）");
        vsButton.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        vsButton.setMinWidth(300);
        vsButton.setMinHeight(56);
        vsButton.setStyle(
            "-fx-background-color: #2c3e50; -fx-text-fill: #aaa; " +
            "-fx-background-radius: 10; -fx-cursor: hand;"
        );
        vsButton.setOnAction(e -> {
            CharacterSelectScene select = new CharacterSelectScene(true);
            stage.setScene(select.buildScene(stage));
        });

        VBox buttonBox = new VBox(16, startButton, vsButton);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(20, 32, 20, 32));
        buttonBox.setStyle("-fx-background-color: rgba(0,0,0,0.55); -fx-background-radius: 12;");

        // ===== 下部クレジット =====
        Label credit = new Label("Press START to play");
        credit.setFont(Font.font("Monospaced", 13));
        credit.setTextFill(Color.LIGHTGRAY);
        credit.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.9), 6, 0.5, 0, 0);");

        content.getChildren().addAll(titleBox, buttonBox, credit);
        root.getChildren().add(content);

        return new Scene(root, 680, 520);
    }
}
