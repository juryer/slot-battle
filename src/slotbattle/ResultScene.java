package slotbattle;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.Stage;

import java.util.Map;

public class ResultScene {

    // キャラ画像パスを受け取るように変更
    public Scene buildScene(Stage stage, GameController gc,
                            String playerImagePath, String cpuImagePath) {
        boolean playerWon = gc.getPhase() == GameController.Phase.PLAYER_WIN;
        BattleStats pStats = gc.getPlayerStats();
        BattleStats cStats = gc.getCpuStats();

        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: #0d1b2a;");

        VBox content = new VBox(20);
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(30, 24, 24, 24));

        // ターン数
        Label turnLabel = new Label("累計スピン数：" + gc.getSpinNumber());
        turnLabel.setFont(Font.font("Monospaced", FontWeight.BOLD, 16));
        turnLabel.setTextFill(Color.LIGHTGRAY);

        // カード
        VBox playerCard = buildCard(
            playerWon ? "WIN" : "LOSE", playerWon,
            "プレイヤー", pStats, playerImagePath);
        Label vsLabel = new Label("VS");
        vsLabel.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        vsLabel.setTextFill(Color.ORANGE);
        vsLabel.setMinWidth(60);
        vsLabel.setAlignment(Pos.CENTER);

        VBox cpuCard = buildCard(
            playerWon ? "LOSE" : "WIN", !playerWon,
            "CPU", cStats, cpuImagePath);

        HBox cardsBox = new HBox(16, playerCard, vsLabel, cpuCard);
        cardsBox.setAlignment(Pos.CENTER);
        HBox.setHgrow(playerCard, Priority.ALWAYS);
        HBox.setHgrow(cpuCard,    Priority.ALWAYS);

        // ボタン
        Button retryButton = new Button("🔄  もう一度");
        retryButton.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        retryButton.setMinWidth(180); retryButton.setMinHeight(48);
        retryButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; " +
                             "-fx-background-radius: 8; -fx-cursor: hand;");
        retryButton.setOnAction(e -> stage.setScene(new BattleScene().buildScene(stage)));

        Button titleButton = new Button("🏠  タイトルに戻る");
        titleButton.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        titleButton.setMinWidth(180); titleButton.setMinHeight(48);
        titleButton.setStyle("-fx-background-color: #2c3e50; -fx-text-fill: white; " +
                             "-fx-background-radius: 8; -fx-cursor: hand;");
        titleButton.setOnAction(e -> stage.setScene(new TitleScene().buildScene(stage)));

        HBox buttonBox = new HBox(20, retryButton, titleButton);
        buttonBox.setAlignment(Pos.CENTER);

        content.getChildren().addAll(turnLabel, cardsBox, buttonBox);
        root.getChildren().add(content);

        return new Scene(root, 680, 560);
    }

    // キャラ画像パスなし版（後方互換）
    public Scene buildScene(Stage stage, GameController gc) {
        return buildScene(stage, gc,
            "/resources/images/player.png",
            "/resources/images/enemy.png");
    }

    private VBox buildCard(String resultText, boolean isWinner,
                           String name, BattleStats stats, String imagePath) {
        Label resultLabel = new Label(resultText);
        resultLabel.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        resultLabel.setTextFill(isWinner ? Color.GOLD : Color.LIGHTGRAY);
        resultLabel.setAlignment(Pos.CENTER);

        // キャラ画像
        ImageView charView = new ImageView();
        charView.setFitWidth(160);
        charView.setFitHeight(140);
        charView.setPreserveRatio(true);
        try {
            java.net.URL url = getClass().getResource(imagePath);
            if (url != null) charView.setImage(new Image(url.toExternalForm()));
        } catch (Exception e) {
            System.err.println("キャラ画像読み込み失敗: " + imagePath);
        }

        VBox statsBox = buildStatsBox(stats);

        VBox card = new VBox(8, resultLabel, charView, statsBox);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(8));
        card.setStyle("-fx-background-color: #1a2a3a; -fx-border-color: #2e4a6a; -fx-border-width: 1;");
        return card;
    }

    private VBox buildStatsBox(BattleStats stats) {
        VBox box = new VBox(4);
        box.setPadding(new Insets(8));
        box.setStyle("-fx-background-color: #0f1e2d; -fx-border-color: #2e4a6a; -fx-border-width: 1;");
        box.setMinWidth(200);

        Label header = new Label("スロットダメージ");
        header.setFont(Font.font("Monospaced", FontWeight.BOLD, 12));
        header.setTextFill(Color.LIGHTGRAY);
        box.getChildren().add(header);

        for (Map.Entry<String, int[]> entry : stats.getRoleStats().entrySet()) {
            int count  = entry.getValue()[0];
            int damage = entry.getValue()[1];
            if (count == 0) continue;
            Label row = new Label(String.format("%-14s %2d回  %3dダメージ",
                    entry.getKey(), count, damage));
            row.setFont(Font.font("Monospaced", 11));
            row.setTextFill(Color.LIGHTBLUE);
            box.getChildren().add(row);
        }

        Label total = new Label("累計ダメージ：" + stats.getTotalDamage());
        total.setFont(Font.font("Monospaced", FontWeight.BOLD, 12));
        total.setTextFill(Color.ORANGE);
        box.getChildren().add(new Separator());
        box.getChildren().add(total);
        return box;
    }
}
