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

import java.util.Random;

public class CharacterSelectScene {

    private final boolean isVsMode;
    private CharacterData p1Selected = null;
    private CharacterData p2Selected = null;
    private boolean p1Confirmed = false; // 対人戦：1Pが確定済みか

    // UI部品
    private ImageView p1Icon, p2Icon;
    private Label p1NameLabel, p2NameLabel;
    private Label descLabel;
    private Button startButton;
    private final java.util.List<VBox> charCards = new java.util.ArrayList<>();

    public CharacterSelectScene(boolean isVsMode) {
        this.isVsMode = isVsMode;
    }

    public Scene buildScene(Stage stage) {
        CharacterData[] chars = CharacterData.getAllCharacters();

        // ===== 背景 =====
        VBox root = new VBox(16);
        root.setStyle("-fx-background-color: #0d1b2a;");
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.TOP_CENTER);

        // ===== タイトル =====
        Label title = new Label(isVsMode ? "VS モード　キャラクター選択" : "キャラクター選択");
        title.setFont(Font.font("Monospaced", FontWeight.BOLD, 20));
        title.setTextFill(Color.GOLD);

        // ===== 上部：1P vs 2P 表示 =====
        p1Icon = buildIconView();
        p2Icon = buildIconView();
        p1NameLabel = buildPlayerLabel("1P", Color.LIGHTBLUE);
        p2NameLabel = buildPlayerLabel(isVsMode ? "2P" : "CPU", Color.SALMON);

        VBox p1Box = new VBox(6, p1NameLabel, p1Icon);
        p1Box.setAlignment(Pos.CENTER);

        VBox p2Box = new VBox(6, p2NameLabel, p2Icon);
        p2Box.setAlignment(Pos.CENTER);

        Label vsLabel = new Label("VS");
        vsLabel.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        vsLabel.setTextFill(Color.ORANGE);

        HBox vsBox = new HBox(40, p1Box, vsLabel, p2Box);
        vsBox.setAlignment(Pos.CENTER);
        vsBox.setPadding(new Insets(12, 20, 12, 20));
        vsBox.setStyle("-fx-background-color: #1a2a3a; -fx-background-radius: 10;");

        // ===== 中部：キャラカード一覧 =====
        HBox cardRow = new HBox(12);
        cardRow.setAlignment(Pos.CENTER);
        charCards.clear();

        for (CharacterData c : chars) {
            VBox card = buildCharCard(c);
            charCards.add(card);
            card.setOnMouseClicked(e -> onCardClicked(c, card, stage, chars));
            card.setOnMouseEntered(e -> {
                descLabel.setText(c.getName() + "\n" + c.getDescription() + "\n特性：" + c.getAbilityText());
                if (!isCardSelected(card)) card.setStyle(cardHoverStyle());
            });
            card.setOnMouseExited(e -> {
                if (!isCardSelected(card)) card.setStyle(cardNormalStyle());
            });
            cardRow.getChildren().add(card);
        }

        // ===== 下部：説明テキスト =====
        descLabel = new Label("キャラクターを選んでください");
        descLabel.setFont(Font.font("Monospaced", 13));
        descLabel.setTextFill(Color.LIGHTGRAY);
        descLabel.setWrapText(true);
        descLabel.setMinHeight(70);
        descLabel.setPadding(new Insets(10));
        descLabel.setStyle("-fx-background-color: #1a2a3a; -fx-background-radius: 8;");
        descLabel.setMaxWidth(680);

        // ===== ボタン =====
        startButton = new Button("⚔  バトル開始！");
        startButton.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        startButton.setMinWidth(240); startButton.setMinHeight(50);
        startButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-background-radius: 8;");
        startButton.setDisable(true);
        startButton.setOnAction(e -> startBattle(stage, chars));

        Button backButton = new Button("← 戻る");
        backButton.setStyle("-fx-background-color: #2c3e50; -fx-text-fill: white; -fx-background-radius: 6;");
        backButton.setOnAction(e -> stage.setScene(new TitleScene().buildScene(stage)));

        HBox btnBox = new HBox(16, backButton, startButton);
        btnBox.setAlignment(Pos.CENTER);

        // 対人戦の操作説明
        Label hint = new Label(isVsMode
            ? "1P がキャラを選んで確定 → 2P がキャラを選んでバトル開始"
            : "キャラを選んでバトル開始（CPU はランダム）");
        hint.setFont(Font.font("Monospaced", 12));
        hint.setTextFill(Color.DARKGRAY);

        root.getChildren().addAll(title, vsBox, cardRow, descLabel, hint, btnBox);

        return new Scene(root, 760, 520);
    }

    // ――― カードクリック処理 ―――
    private void onCardClicked(CharacterData c, VBox card, Stage stage, CharacterData[] chars) {
        if (isVsMode) {
            if (!p1Confirmed) {
                // 1P選択中
                p1Selected = c;
                updateIcon(p1Icon, c);
                p1NameLabel.setText("1P：" + c.getName());
                refreshCardStyles();
                // 確定ボタン表示
                startButton.setText("1P 確定 →");
                startButton.setDisable(false);
                startButton.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; -fx-background-radius: 8;");
            } else {
                // 2P選択中
                p2Selected = c;
                updateIcon(p2Icon, c);
                p2NameLabel.setText("2P：" + c.getName());
                refreshCardStyles();
                startButton.setText("⚔  バトル開始！");
                startButton.setDisable(false);
                startButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-background-radius: 8;");
            }
        } else {
            // CPU戦：1Pのみ選択
            p1Selected = c;
            updateIcon(p1Icon, c);
            p1NameLabel.setText("1P：" + c.getName());
            refreshCardStyles();
            startButton.setDisable(false);
        }
    }

    // ――― バトル開始 ―――
    private void startBattle(Stage stage, CharacterData[] chars) {
        if (isVsMode && !p1Confirmed) {
            // 1P確定
            p1Confirmed = true;
            p2Selected = null;
            p2NameLabel.setText("2P を選んでください");
            p2Icon.setImage(null);
            startButton.setText("⚔  バトル開始！");
            startButton.setDisable(true);
            // 全カードのスタイルをリセット（2P選択用）
            refreshCardStyles();
            descLabel.setText("2P のキャラクターを選んでください");
            return;
        }

        BattleScene battle = new BattleScene();
        if (isVsMode) {
            stage.setScene(battle.buildSceneVs(stage, p1Selected, p2Selected));
        } else {
            // CPU戦：CPUはランダム
            CharacterData cpuChara = chars[new Random().nextInt(chars.length)];
            updateIcon(p2Icon, cpuChara);
            p2NameLabel.setText("CPU：" + cpuChara.getName());
            stage.setScene(battle.buildSceneVsCpu(stage, p1Selected, cpuChara));
        }
    }

    // ――― アイコン画像更新 ―――
    private void updateIcon(ImageView iv, CharacterData c) {
        try {
            java.net.URL url = getClass().getResource(c.getImagePath());
            if (url != null) iv.setImage(new Image(url.toExternalForm()));
        } catch (Exception e) { /* ignore */ }
    }

    // ――― カードの選択状態スタイル更新 ―――
    private void refreshCardStyles() {
        CharacterData[] chars = CharacterData.getAllCharacters();
        for (int i = 0; i < charCards.size() && i < chars.length; i++) {
            VBox card = charCards.get(i);
            CharacterData c = chars[i];
            boolean isP1 = (c == p1Selected && (!isVsMode || !p1Confirmed || p1Selected == c));
            boolean isP2 = isVsMode && p1Confirmed && c == p2Selected;

            if (isP1 && !p1Confirmed) card.setStyle(cardSelectedP1Style());
            else if (isP2)            card.setStyle(cardSelectedP2Style());
            else if (isP1 && p1Confirmed) card.setStyle(cardConfirmedStyle());
            else                      card.setStyle(cardNormalStyle());
        }
    }

    private boolean isCardSelected(VBox card) {
        CharacterData[] chars = CharacterData.getAllCharacters();
        int idx = charCards.indexOf(card);
        if (idx < 0 || idx >= chars.length) return false;
        return chars[idx] == p1Selected || chars[idx] == p2Selected;
    }

    // ――― UIビルダー ―――
    private ImageView buildIconView() {
        ImageView iv = new ImageView();
        iv.setFitWidth(80); iv.setFitHeight(72); iv.setPreserveRatio(true);
        return iv;
    }

    private Label buildPlayerLabel(String text, Color color) {
        Label l = new Label(text);
        l.setFont(Font.font("Monospaced", FontWeight.BOLD, 14));
        l.setTextFill(color);
        return l;
    }

    private VBox buildCharCard(CharacterData c) {
        ImageView img = new ImageView();
        img.setFitWidth(90); img.setFitHeight(80); img.setPreserveRatio(true);
        try {
            java.net.URL url = getClass().getResource(c.getImagePath());
            if (url != null) img.setImage(new Image(url.toExternalForm()));
        } catch (Exception e) { /* ignore */ }

        Label name = new Label(c.getName());
        name.setFont(Font.font("Monospaced", FontWeight.BOLD, 11));
        name.setTextFill(Color.WHITE);
        name.setWrapText(true);
        name.setMaxWidth(110);
        name.setAlignment(Pos.CENTER);

        VBox card = new VBox(6, img, name);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(10));
        card.setMinWidth(120);
        card.setStyle(cardNormalStyle());
        return card;
    }

    // ――― スタイル ―――
    private String cardNormalStyle() {
        return "-fx-background-color: #1a2a3a; -fx-border-color: #2e4a6a; " +
               "-fx-border-width: 2; -fx-background-radius: 8; -fx-border-radius: 8; -fx-cursor: hand;";
    }
    private String cardHoverStyle() {
        return "-fx-background-color: #2a3a4a; -fx-border-color: #5a8aaa; " +
               "-fx-border-width: 2; -fx-background-radius: 8; -fx-border-radius: 8; -fx-cursor: hand;";
    }
    private String cardSelectedP1Style() {
        return "-fx-background-color: #1a3a5a; -fx-border-color: #00aaff; " +
               "-fx-border-width: 3; -fx-background-radius: 8; -fx-border-radius: 8; -fx-cursor: hand;";
    }
    private String cardSelectedP2Style() {
        return "-fx-background-color: #3a1a1a; -fx-border-color: #ff6655; " +
               "-fx-border-width: 3; -fx-background-radius: 8; -fx-border-radius: 8; -fx-cursor: hand;";
    }
    private String cardConfirmedStyle() {
        return "-fx-background-color: #1a3a2a; -fx-border-color: #00cc88; " +
               "-fx-border-width: 3; -fx-background-radius: 8; -fx-border-radius: 8; -fx-cursor: hand;";
    }
}
