package slotbattle;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

public class BattleScene {

    private static final int CPU_STOP_MS = 400;
    private static final int COLS = 3;

    // リールパターン定義
    private static final Symbol[][] REEL_PATTERN = {
        { Symbol.SEVEN, Symbol.BELL, Symbol.CHERRY, Symbol.WATERMELON, Symbol.BAR,
          Symbol.STAR, Symbol.REPLAY, Symbol.BELL, Symbol.WATERMELON, Symbol.STAR },
        { Symbol.BELL, Symbol.WATERMELON, Symbol.SEVEN, Symbol.STAR, Symbol.CHERRY,
          Symbol.REPLAY, Symbol.BAR, Symbol.STAR, Symbol.BELL, Symbol.WATERMELON },
        { Symbol.WATERMELON, Symbol.BAR, Symbol.STAR, Symbol.BELL, Symbol.SEVEN,
          Symbol.CHERRY, Symbol.STAR, Symbol.REPLAY, Symbol.WATERMELON, Symbol.BELL }
    };

    // ゲーム状態
    private GameController gc;
    private Stage stage;
    private boolean isPlayerTurn = true;
    private boolean spinInProgress = false;
    private boolean isMeOshiChance = false;
    private boolean isVsMode = false;
    private CharacterData p1Chara, p2Chara;
    private int replayCount = 0; // リプレイ連続回数
    private static final int MAX_REPLAY = 3; // リプレイ上限

    // リール
    private ReelCanvas[] playerReels = new ReelCanvas[COLS]; // 1P/プレイヤーリール
    private ReelCanvas[] cpuReels    = new ReelCanvas[COLS]; // CPU/2Pリール
    private int stoppedCount = 0;
    private SpinResult pendingResult = null;
    private final Random rng = new Random();

    // 画像
    private final Map<Symbol, Image> symbolImages = new EnumMap<>(Symbol.class);

    // UI
    private Button[]    stopButtons = new Button[3];
    private Button      startButton;
    private Label       resultLabel;
    private Label       spinNumberLabel;
    private Label       phaseLabel;
    private ProgressBar playerHpBar, cpuHpBar;
    private Label       playerHpLabel, cpuHpLabel;
    private TextArea    logArea;
    private ImageView   playerCharView, cpuCharView;
    private Pane        overlayPane;
    private Label       playerReelLabel, cpuReelLabel;

    // CPU自動停止タイマー
    private final java.util.List<PauseTransition> cpuStopTimers = new java.util.ArrayList<>();
    private PauseTransition pendingCpuTurnPause = null;

    private void loadImages() {
        for (Symbol s : Symbol.values()) {
            if (s.getImageFile() == null) continue;
            try {
                InputStream is = getClass().getResourceAsStream("/resources/images/" + s.getImageFile());
                if (is != null) symbolImages.put(s, new Image(is, ReelCanvas.CELL_W, ReelCanvas.CELL_H, true, true));
            } catch (Exception e) { System.err.println("画像読み込み失敗: " + s.getImageFile()); }
        }
    }

    // ――― buildScene系 ―――
    public Scene buildScene(Stage stage) {
        return buildSceneVsCpu(stage, null, null);
    }

    public Scene buildSceneVsCpu(Stage stage, CharacterData playerChara, CharacterData cpuChara) {
        this.isVsMode = false;
        this.p1Chara  = playerChara;
        this.p2Chara  = cpuChara;
        gc = new GameController(playerChara, cpuChara);
        this.stage = stage;
        loadImages();
        return buildSceneInternal(stage,
            playerChara != null ? playerChara.getImagePath() : "/resources/images/player.png",
            cpuChara    != null ? cpuChara.getImagePath()    : "/resources/images/enemy.png",
            playerChara != null ? playerChara.getName()      : "プレイヤー",
            cpuChara    != null ? cpuChara.getName()         : "CPU");
    }

    public Scene buildSceneVs(Stage stage, CharacterData p1, CharacterData p2) {
        this.isVsMode = true;
        this.p1Chara  = p1;
        this.p2Chara  = p2;
        gc = new GameController(p1, p2);
        this.stage = stage;
        loadImages();
        return buildSceneInternal(stage,
            p1 != null ? p1.getImagePath() : "/resources/images/player.png",
            p2 != null ? p2.getImagePath() : "/resources/images/enemy.png",
            p1 != null ? p1.getName()      : "1P",
            p2 != null ? p2.getName()      : "2P");
    }

    private Scene buildSceneInternal(Stage stage, String p1Img, String p2Img,
                                      String p1Name, String p2Name) {
        // ===== HPバー =====
        playerHpBar   = buildHpBar();
        cpuHpBar      = buildHpBar();
        playerHpLabel = new Label(p1Name + "  HP: 100");
        cpuHpLabel    = new Label(p2Name + "  HP: 100");
        playerHpLabel.setFont(Font.font("Monospaced", FontWeight.BOLD, 13));
        cpuHpLabel.setFont(Font.font("Monospaced", FontWeight.BOLD, 13));
        HBox hpBox = new HBox(8, playerHpLabel, playerHpBar, new Region(), cpuHpBar, cpuHpLabel);
        HBox.setHgrow(((Region) hpBox.getChildren().get(2)), Priority.ALWAYS);
        hpBox.setAlignment(Pos.CENTER);
        hpBox.setPadding(new Insets(4, 8, 4, 8));

        // ===== キャラ画像 =====
        playerCharView = loadCharImage(p1Img);
        cpuCharView    = loadCharImage(p2Img);

        // ===== ターン表示 =====
        phaseLabel      = new Label("【" + (isVsMode ? p1Name : "あなた") + "のターン】");
        spinNumberLabel = new Label("スピン 1");
        phaseLabel.setFont(Font.font("Monospaced", FontWeight.BOLD, 14));
        spinNumberLabel.setFont(Font.font("Monospaced", 13));
        phaseLabel.setTextFill(Color.DARKBLUE);
        HBox turnBox = new HBox(16, phaseLabel, spinNumberLabel);
        turnBox.setAlignment(Pos.CENTER);

        // ===== ReelCanvas作成 =====
        for (int i = 0; i < COLS; i++) {
            playerReels[i] = new ReelCanvas(REEL_PATTERN[i], symbolImages);
            cpuReels[i]    = new ReelCanvas(REEL_PATTERN[i], symbolImages);
        }

        // プレイヤーリールボックス
        playerReelLabel = new Label(p1Name);
        playerReelLabel.setFont(Font.font("Monospaced", FontWeight.BOLD, 13));
        playerReelLabel.setTextFill(Color.LIGHTBLUE);
        HBox playerReelRow = new HBox(4,
            playerReels[0], playerReels[1], playerReels[2]);
        playerReelRow.setAlignment(Pos.CENTER);
        playerReelRow.setStyle("-fx-background-color: #111; -fx-padding: 4;");
        VBox playerReelBox = new VBox(4, playerReelLabel, playerReelRow);
        playerReelBox.setAlignment(Pos.TOP_CENTER);

        // CPUリールボックス
        cpuReelLabel = new Label(p2Name + "（前回）");
        cpuReelLabel.setFont(Font.font("Monospaced", FontWeight.BOLD, 13));
        cpuReelLabel.setTextFill(Color.SALMON);
        HBox cpuReelRow = new HBox(4,
            cpuReels[0], cpuReels[1], cpuReels[2]);
        cpuReelRow.setAlignment(Pos.CENTER);
        cpuReelRow.setStyle("-fx-background-color: #111; -fx-padding: 4;");
        VBox cpuReelBox = new VBox(4, cpuReelLabel, cpuReelRow);
        cpuReelBox.setAlignment(Pos.TOP_CENTER);

        Label vsLabel = new Label("VS");
        vsLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        vsLabel.setTextFill(Color.ORANGE);

        HBox reelsBox = new HBox(16, playerReelBox, vsLabel, cpuReelBox);
        reelsBox.setAlignment(Pos.CENTER);

        // キャラとリールを縦配置
        VBox playerSide = new VBox(6, playerCharView, playerReelBox);
        playerSide.setAlignment(Pos.TOP_CENTER);
        VBox cpuSide = new VBox(6, cpuCharView, cpuReelBox);
        cpuSide.setAlignment(Pos.TOP_CENTER);

        HBox charReelBox = new HBox(24, playerSide, vsLabel, cpuSide);
        charReelBox.setAlignment(Pos.CENTER);

        // ===== STOPボタン =====
        HBox stopBox = new HBox(4);
        stopBox.setAlignment(Pos.CENTER);
        for (int i = 0; i < COLS; i++) {
            final int idx = i;
            stopButtons[i] = new Button("STOP");
            stopButtons[i].setFont(Font.font("Arial", FontWeight.BOLD, 13));
            stopButtons[i].setMinWidth(ReelCanvas.CELL_W + 4);
            stopButtons[i].setStyle(stopBtnStyle());
            stopButtons[i].setDisable(true);
            stopButtons[i].setOnAction(e -> onStopPressed(idx));
            stopBox.getChildren().add(stopButtons[i]);
        }

        // ===== 役名表示 =====
        resultLabel = new Label("");
        resultLabel.setFont(Font.font("Monospaced", FontWeight.BOLD, 14));
        resultLabel.setTextFill(Color.GOLD);
        resultLabel.setMinHeight(22);

        // ===== STARTボタン =====
        startButton = new Button("🎰  START");
        startButton.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        startButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; " +
                             "-fx-background-radius: 8; -fx-padding: 8 28;");
        startButton.setOnAction(e -> onStartPressed());

        // ===== ログ =====
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setFont(Font.font("Monospaced", 12));
        logArea.setPrefHeight(100);
        logArea.setWrapText(true);

        // ===== レイアウト =====
        VBox content = new VBox(8,
            hpBox, new Separator(), turnBox,
            charReelBox, stopBox,
            resultLabel, startButton,
            new Separator(), logArea
        );
        content.setPadding(new Insets(12));
        content.setAlignment(Pos.TOP_CENTER);

        overlayPane = new Pane();
        overlayPane.setMouseTransparent(true);

        StackPane root = new StackPane(content, overlayPane);

        appendLog("=== スロットバトル開始！ ===");
        appendLog("STARTを押してスロットを回してください。\n");

        return new Scene(root, 820, 720);
    }

    // ――― START ―――
    private void onStartPressed() {
        if (gc.isGameOver() || spinInProgress) return;
        spinInProgress = true;
        stoppedCount = 0;
        pendingResult = null;
        resultLabel.setText("");
        startButton.setDisable(true);

        isMeOshiChance = gc.rollMeOshiChance(isPlayerTurn);

        if (isPlayerTurn || isVsMode) {
            if (isMeOshiChance) showMeOshiChanceEffect(isPlayerTurn);
            startPlayerSpin();
        } else {
            if (isMeOshiChance) showMeOshiChanceEffect(false);
            startCpuSpin();
        }
    }

    // ――― プレイヤースピン ―――
    private void startPlayerSpin() {
        // 使うリール（1Pターン=playerReels、2Pターン=cpuReels）
        ReelCanvas[] active = activeReels();
        for (int i = 0; i < COLS; i++) {
            stopButtons[i].setDisable(false);
            active[i].startSpin();
        }
    }

    // ――― CPUスピン ―――
    private void startCpuSpin() {
        SpinResult cpuSpin;
        if (isMeOshiChance) {
            boolean success = rng.nextInt(100) < 30;
            cpuSpin = success ? gc.getMeOshiSuccessResult(false) : gc.getMeOshiFailResult(false);
            appendLog("  CPU 目押しチャンス発生！→ " + (success ? "成功！" : "失敗..."));
        } else {
            cpuSpin = gc.spinOnce(false);
        }
        isMeOshiChance = false;
        pendingResult = cpuSpin;

        boolean cpuMeOshi = cpuSpin.getDescription().contains("目押し");

        // CPUリールを回してから自動停止
        for (int i = 0; i < COLS; i++) cpuReels[i].startSpin();

        cpuStopTimers.clear();
        for (int i = 0; i < COLS; i++) {
            final int col = i;
            final Symbol target = cpuSpin.getReels()[col];
            int delay = cpuMeOshi ? 200 * (col + 1) : CPU_STOP_MS * (col + 1);
            PauseTransition pt = new PauseTransition(Duration.millis(delay));
            pt.setOnFinished(e -> {
                // コールバックを先に設定してから停止
                cpuReels[col].setOnStopped(sym -> {
                    stoppedCount++;
                    if (stoppedCount >= COLS) onSpinComplete(pendingResult, false);
                });
                if (cpuMeOshi) {
                    cpuReels[col].requestStopImmediate(target);
                } else {
                    cpuReels[col].requestStop(target);
                }
            });
            cpuStopTimers.add(pt);
            pt.play();
        }
    }

    // ――― STOPボタン ―――
    private void onStopPressed(int col) {
        boolean canStop = isVsMode ? spinInProgress : (isPlayerTurn && spinInProgress);
        if (!canStop) return;

        ReelCanvas[] active = activeReels();
        if (!active[col].isSpinning()) return;

        stopButtons[col].setDisable(true);

        // 目押しチャンス：1リール目を止めた時点で全リールの出目を確定
        // 2・3リール目は別途STOPで止める
        if (isMeOshiChance && pendingResult == null) {
            // 全リールのアニメーションを即停止して出目を記録
            Symbol[] centers = new Symbol[COLS];
            for (int i = 0; i < COLS; i++) {
                centers[i] = active[i].freezeAndGetCenter();
            }
            // 出目からSpinResultを生成
            pendingResult = buildMeOshiResult(centers);
            isMeOshiChance = false;
            // 止めていないリールは再度スピン開始（見た目は回り続ける）
            for (int i = 0; i < COLS; i++) {
                if (i != col) active[i].startSpin();
            }
        } else if (pendingResult == null) {
            pendingResult = gc.spinOnce(isPlayerTurn);
        }

        // コールバックを先に設定してから停止
        active[col].setOnStopped(sym -> {
            stoppedCount++;
            if (stoppedCount >= COLS) onSpinComplete(pendingResult, isPlayerTurn);
        });

        Symbol target = pendingResult.getReels()[col];
        boolean wasMeOshi = pendingResult.getDescription().contains("目押し");
        if (wasMeOshi) {
            active[col].requestStopImmediate(target);
        } else {
            active[col].requestStop(target);
        }
    }

    private ReelCanvas[] activeReels() {
        return (isVsMode && !isPlayerTurn) ? cpuReels : playerReels;
    }

    // 目押しフリー：止めた出目から役を判定してSpinResultを生成
    private SpinResult buildMeOshiResult(Symbol[] centers) {
        Symbol r1 = centers[0], r2 = centers[1], r3 = centers[2];

        // 中央3揃い判定
        if (r1 == r2 && r2 == r3) {
            return buildTripleResult(r1, r2, r3);
        }

        // 斜め判定（左上→右下、左下→右上）は3×3グリッド必要なので
        // 各リールの現在の3行を取得
        ReelCanvas[] active = activeReels();
        Symbol[][] grid = new Symbol[3][3];
        for (int col = 0; col < COLS; col++) {
            Symbol[] col3 = active[col].getCurrentColumn();
            for (int row = 0; row < 3; row++) grid[col][row] = col3[row];
        }

        // 斜め左上→右下
        if (grid[0][0] == grid[1][1] && grid[1][1] == grid[2][2] && grid[0][0] != Symbol.STAR && grid[0][0] != Symbol.MISS) {
            return buildTripleResult(grid[0][0], grid[1][1], grid[2][2]);
        }
        // 斜め左下→右上
        if (grid[0][2] == grid[1][1] && grid[1][1] == grid[2][0] && grid[0][2] != Symbol.STAR && grid[0][2] != Symbol.MISS) {
            return buildTripleResult(grid[0][2], grid[1][1], grid[2][0]);
        }

        // 揃いなし→ハズレ
        Symbol[][] fullGrid = grid;
        return new SpinResult(new Symbol[]{r1, r2, r3}, fullGrid, 0, false, false, "目押し...ハズレ");
    }

    private SpinResult buildTripleResult(Symbol s1, Symbol s2, Symbol s3) {
        Symbol[][] grid = new Symbol[3][3];
        ReelCanvas[] active = activeReels();
        for (int col = 0; col < COLS; col++) {
            Symbol[] col3 = active[col].getCurrentColumn();
            for (int row = 0; row < 3; row++) grid[col][row] = col3[row];
        }

        int damage = 0;
        boolean isReplay = false;
        String desc;
        Symbol sym = s1;

        switch (sym) {
            case SEVEN:      damage = 30; desc = "！！７７７！！ BIG WIN！！（目押し成功！）"; break;
            case BAR:        damage = 25; desc = "BAR BAR BAR！！（目押し成功！）"; break;
            case BELL:       damage = 15; desc = "ベルベルベル！（目押し成功！）"; break;
            case WATERMELON: damage = 15; desc = "スイカスイカスイカ！（目押し成功！）"; break;
            case REPLAY:     isReplay = true; desc = "リプレイ！もう1スピン！（目押し成功！）"; break;
            default:         desc = "目押し...ハズレ"; break;
        }

        SpinResult sr = new SpinResult(new Symbol[]{s1, s2, s3}, grid, damage, isReplay, false, desc);
        if (isReplay || damage > 0) {
            gc.getPlayerStats().record(sr);
        }
        return sr;
    }

    // ――― 全停止後に成立行を一斉ハイライト ―――
    private void highlightReels(SpinResult sr, ReelCanvas[] reels) {
        // ハズレ・★ゴミ役は全部暗くする（highlightRow=-1扱いで全行暗く）
        String desc = sr.getDescription();
        if (desc.equals("ハズレ") || desc.contains("ゴミ") || desc.contains("失敗")) {
            for (ReelCanvas reel : reels) {
                reel.setHighlightRow(-1); // 全行暗く
            }
            return;
        }
        // それ以外は中央行（row=1）をハイライト
        for (ReelCanvas reel : reels) {
            reel.setHighlightRow(1);
        }
    }

    // ――― スピン完了 ―――
    private void onSpinComplete(SpinResult sr, boolean wasPlayerTurn) {
        // 全リール停止後に一斉ハイライト
        highlightReels(sr, activeReels());

        showResult(sr);
        appendLog(String.format("  スピン%d [%s]: %s",
            gc.getSpinNumber(), getTurnName(wasPlayerTurn), sr.toString()));

        if (sr.getDescription().contains("７７７")) playBigWinEffect();

        int waitMs = sr.getDescription().contains("７７７") ? 2800 : 900;
        PauseTransition pause = new PauseTransition(Duration.millis(waitMs));
        pause.setOnFinished(e -> {
            gc.applySpinDamage(wasPlayerTurn, sr.getDamage());
            updateHpDisplay();
            spinInProgress = false;
            pendingResult  = null;
            stoppedCount   = 0;

            if (gc.isGameOver()) { endGame(); return; }

            // リプレイ
            if (sr.isReplay() && replayCount < MAX_REPLAY) {
                replayCount++;
                appendLog("  ★ リプレイ！もう1スピン！（" + replayCount + "/" + MAX_REPLAY + "）");
                isPlayerTurn = wasPlayerTurn;
                spinNumberLabel.setText("スピン " + gc.getSpinNumber());
                if (wasPlayerTurn || isVsMode) {
                    phaseLabel.setText("【" + getTurnName(wasPlayerTurn) + "のターン】 ★リプレイ！");
                    phaseLabel.setTextFill(Color.CYAN);
                    startButton.setText("🎰  REPLAY START");
                    startButton.setDisable(false);
                } else {
                    scheduleCpuTurn(600);
                }
                return;
            }

            // リプレイ上限に達した場合はハズレ扱いでターン交代
            if (sr.isReplay() && replayCount >= MAX_REPLAY) {
                appendLog("  リプレイ上限（" + MAX_REPLAY + "回）に達しました");
            }

            replayCount = 0;
            isPlayerTurn = !wasPlayerTurn;
            spinNumberLabel.setText("スピン " + gc.getSpinNumber());

            if (isPlayerTurn || isVsMode) {
                cancelCpuStopTimers();
                cancelPendingCpuTurn();
                phaseLabel.setText("【" + getTurnName(isPlayerTurn) + "のターン】");
                phaseLabel.setTextFill(isPlayerTurn ? Color.DARKBLUE : Color.DARKRED);
                startButton.setText("🎰  START");
                startButton.setDisable(false);
            } else {
                scheduleCpuTurn(600);
            }
        });
        pause.play();
    }

    private void scheduleCpuTurn(int delayMs) {
        phaseLabel.setText("【CPU のターン】");
        phaseLabel.setTextFill(Color.DARKRED);
        startButton.setText("🤖  CPU ターン中...");
        pendingCpuTurnPause = new PauseTransition(Duration.millis(delayMs));
        pendingCpuTurnPause.setOnFinished(ev -> { pendingCpuTurnPause = null; onStartPressed(); });
        pendingCpuTurnPause.play();
    }

    private void cancelCpuStopTimers() {
        for (PauseTransition pt : cpuStopTimers) pt.stop();
        cpuStopTimers.clear();
    }

    private void cancelPendingCpuTurn() {
        if (pendingCpuTurnPause != null) { pendingCpuTurnPause.stop(); pendingCpuTurnPause = null; }
    }

    // ――― ゲーム終了 ―――
    private void endGame() {
        boolean win = gc.getPhase() == GameController.Phase.PLAYER_WIN;
        appendLog("\n" + (win ? "🎉 " + getTurnName(true) + "の勝利！！"
                              : "💀 " + getTurnName(false) + "の勝利！！"));
        CharacterData p1 = p1Chara != null ? p1Chara : CharacterData.getAllCharacters()[0];
        CharacterData p2 = p2Chara != null ? p2Chara : CharacterData.getAllCharacters()[1];
        PauseTransition pt = new PauseTransition(Duration.millis(1500));
        pt.setOnFinished(e -> stage.setScene(
            new ResultScene().buildScene(stage, gc, p1.getImagePath(), p2.getImagePath())));
        pt.play();
    }

    // ===== 7揃い演出 =====
    private void playBigWinEffect() { playParticles(); playBigWinText(); }

    private void playParticles() {
        double cx = 380, cy = 340;
        Color[] colors = {Color.GOLD, Color.ORANGE, Color.YELLOW, Color.WHITE, Color.ORANGERED};
        for (int i = 0; i < 60; i++) {
            double angle = rng.nextDouble() * 360;
            double speed = 80 + rng.nextDouble() * 220;
            double size  = 4 + rng.nextDouble() * 8;
            Circle p = new Circle(size, colors[rng.nextInt(colors.length)]);
            p.setLayoutX(cx); p.setLayoutY(cy);
            overlayPane.getChildren().add(p);
            double rad = Math.toRadians(angle);
            TranslateTransition move = new TranslateTransition(Duration.millis(900 + rng.nextInt(600)), p);
            move.setToX(Math.cos(rad) * speed); move.setToY(Math.sin(rad) * speed);
            FadeTransition fade = new FadeTransition(Duration.millis(900 + rng.nextInt(600)), p);
            fade.setFromValue(1.0); fade.setToValue(0.0);
            ScaleTransition scale = new ScaleTransition(Duration.millis(900 + rng.nextInt(600)), p);
            scale.setToX(0.1); scale.setToY(0.1);
            ParallelTransition pt2 = new ParallelTransition(p, move, fade, scale);
            pt2.setOnFinished(ev -> overlayPane.getChildren().remove(p));
            pt2.play();
        }
    }

    private void playBigWinText() {
        Label bigWin = new Label("🎰 BIG WIN!! 🎰");
        bigWin.setFont(Font.font("Arial", FontWeight.BOLD, 52));
        bigWin.setTextFill(Color.GOLD);
        bigWin.setStyle("-fx-effect: dropshadow(gaussian, rgba(255,180,0,1.0), 30, 0.7, 0, 0);");
        bigWin.setOpacity(0); bigWin.setScaleX(0.2); bigWin.setScaleY(0.2);
        bigWin.setLayoutX(160); bigWin.setLayoutY(300);
        overlayPane.getChildren().add(bigWin);
        ScaleTransition zoomIn = new ScaleTransition(Duration.millis(400), bigWin);
        zoomIn.setToX(1.1); zoomIn.setToY(1.1);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), bigWin);
        fadeIn.setFromValue(0.0); fadeIn.setToValue(1.0);
        ScaleTransition settle = new ScaleTransition(Duration.millis(150), bigWin);
        settle.setToX(1.0); settle.setToY(1.0);
        FadeTransition fadeOut = new FadeTransition(Duration.millis(600), bigWin);
        fadeOut.setFromValue(1.0); fadeOut.setToValue(0.0);
        fadeOut.setDelay(Duration.millis(1200));
        fadeOut.setOnFinished(e -> overlayPane.getChildren().remove(bigWin));
        new SequentialTransition(new ParallelTransition(zoomIn, fadeIn), settle, fadeOut).play();
    }

    // ===== 目押しチャンス演出 =====
    private void showMeOshiChanceEffect(boolean isPlayer) {
        javafx.scene.shape.Rectangle panel = new javafx.scene.shape.Rectangle(420, 80);
        panel.setFill(Color.color(0, 0, 0, 0.75));
        panel.setArcWidth(16); panel.setArcHeight(16);
        panel.setLayoutX(150); panel.setLayoutY(265);
        Label chance = new Label("🎯  目押しチャンス！！");
        chance.setFont(Font.font("Arial", FontWeight.BOLD, 34));
        chance.setTextFill(Color.CYAN);
        chance.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,255,255,1.0), 16, 0.7, 0, 0);");
        chance.setLayoutX(160); chance.setLayoutY(272);
        overlayPane.getChildren().addAll(panel, chance);
        panel.setOpacity(0); chance.setOpacity(0);
        FadeTransition panelIn = new FadeTransition(Duration.millis(150), panel); panelIn.setToValue(1);
        FadeTransition textIn  = new FadeTransition(Duration.millis(200), chance); textIn.setFromValue(0); textIn.setToValue(1);
        ScaleTransition zoom   = new ScaleTransition(Duration.millis(250), chance); zoom.setFromX(0.6); zoom.setFromY(0.6); zoom.setToX(1.0); zoom.setToY(1.0);
        FadeTransition panelOut = new FadeTransition(Duration.millis(400), panel); panelOut.setFromValue(1); panelOut.setToValue(0); panelOut.setDelay(Duration.millis(900)); panelOut.setOnFinished(e -> overlayPane.getChildren().remove(panel));
        FadeTransition textOut  = new FadeTransition(Duration.millis(400), chance); textOut.setFromValue(1); textOut.setToValue(0); textOut.setDelay(Duration.millis(900)); textOut.setOnFinished(e -> overlayPane.getChildren().remove(chance));
        new ParallelTransition(panelIn, textIn, zoom).play();
        panelOut.play(); textOut.play();
        if (isPlayer) { phaseLabel.setText("【あなたのターン】 🎯目押しチャンス！"); phaseLabel.setTextFill(Color.CYAN); }
    }

    // ===== HPアニメーション =====
    private void updateHpDisplay() {
        Fighter p = gc.getPlayer(); Fighter c = gc.getCpu();
        int prevPlayerHp = parseHpFromLabel(playerHpLabel);
        int prevCpuHp    = parseHpFromLabel(cpuHpLabel);
        int playerDmg = prevPlayerHp - p.getCurrentHp();
        int cpuDmg    = prevCpuHp    - c.getCurrentHp();
        if (playerDmg > 0) showDamagePopup(playerDmg, true);
        if (cpuDmg    > 0) showDamagePopup(cpuDmg,    false);
        animateHpBar(playerHpBar, playerHpLabel, prevPlayerHp, p.getCurrentHp(), p.getMaxHp(), playerHpLabel.getText().split("HP:")[0].trim());
        animateHpBar(cpuHpBar,    cpuHpLabel,    prevCpuHp,    c.getCurrentHp(), c.getMaxHp(), cpuHpLabel.getText().split("HP:")[0].trim());
    }

    private int parseHpFromLabel(Label label) {
        try {
            String[] parts = label.getText().split(":");
            if (parts.length >= 2) return Integer.parseInt(parts[parts.length - 1].trim());
        } catch (Exception ignored) {}
        return 100;
    }

    private void animateHpBar(ProgressBar bar, Label label, int fromHp, int toHp, int maxHp, String name) {
        if (fromHp == toHp) return;
        Timeline anim = new Timeline();
        int steps = 20;
        for (int i = 0; i <= steps; i++) {
            final double t = (double) i / steps;
            final double eased = 1 - Math.pow(1 - t, 2);
            final int hp = (int)(fromHp + (toHp - fromHp) * eased);
            final double ratio = (double) hp / maxHp;
            anim.getKeyFrames().add(new KeyFrame(Duration.millis(600 * t), e -> {
                bar.setProgress(ratio);
                label.setText(name + "  HP: " + hp);
                styleHpBar(bar, ratio);
            }));
        }
        anim.play();
    }

    private void showDamagePopup(int damage, boolean isPlayer) {
        Label popup = new Label("-" + damage);
        popup.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        popup.setTextFill(damage >= 25 ? Color.RED : damage >= 15 ? Color.ORANGE : Color.YELLOW);
        popup.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 6, 0.5, 0, 0);");
        popup.setLayoutX(isPlayer ? 120 : 520);
        popup.setLayoutY(160);
        overlayPane.getChildren().add(popup);
        TranslateTransition rise = new TranslateTransition(Duration.millis(900), popup); rise.setToY(-60);
        FadeTransition fade = new FadeTransition(Duration.millis(900), popup); fade.setFromValue(1.0); fade.setToValue(0.0); fade.setDelay(Duration.millis(200));
        ScaleTransition scale = new ScaleTransition(Duration.millis(200), popup); scale.setFromX(0.5); scale.setFromY(0.5); scale.setToX(1.2); scale.setToY(1.2);
        SequentialTransition seq = new SequentialTransition(scale, new ParallelTransition(rise, fade));
        seq.setOnFinished(e -> overlayPane.getChildren().remove(popup));
        seq.play();
    }

    // ===== ヘルパー =====
    private String getTurnName(boolean isP1Turn) {
        if (isVsMode) {
            CharacterData c = isP1Turn ? p1Chara : p2Chara;
            return c != null ? c.getName() : (isP1Turn ? "1P" : "2P");
        }
        return isP1Turn ? "あなた" : "CPU";
    }

    private void showResult(SpinResult sr) {
        String text = sr.getDescription() + (sr.getDamage() > 0 ? "  ダメージ: " + sr.getDamage() : "");
        resultLabel.setText(text);
        if      (sr.getDamage() >= 30) resultLabel.setTextFill(Color.RED);
        else if (sr.getDamage() >= 25) resultLabel.setTextFill(Color.ORANGERED);
        else if (sr.getDamage() >= 15) resultLabel.setTextFill(Color.ORANGE);
        else if (sr.getDamage() >   0) resultLabel.setTextFill(Color.GOLD);
        else                           resultLabel.setTextFill(Color.LIGHTGRAY);
    }

    private ImageView loadCharImage(String path) {
        ImageView iv = new ImageView();
        iv.setFitWidth(120); iv.setFitHeight(110); iv.setPreserveRatio(true);
        try {
            java.net.URL url = getClass().getResource(path);
            if (url != null) iv.setImage(new Image(url.toExternalForm()));
        } catch (Exception e) { System.err.println("キャラ画像読み込み失敗: " + path); }
        return iv;
    }

    private String stopBtnStyle() {
        return "-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 30;";
    }

    private void styleHpBar(ProgressBar bar, double ratio) {
        if      (ratio > 0.5)  bar.setStyle("-fx-accent: limegreen;");
        else if (ratio > 0.25) bar.setStyle("-fx-accent: orange;");
        else                   bar.setStyle("-fx-accent: red;");
    }

    private ProgressBar buildHpBar() {
        ProgressBar bar = new ProgressBar(1.0);
        bar.setPrefWidth(200); bar.setPrefHeight(20); return bar;
    }

    private void appendLog(String text) { logArea.appendText(text + "\n"); }
}
