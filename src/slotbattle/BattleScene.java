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

    // ゲーム状態
    private GameController gc;
    private Stage stage;
    private boolean isPlayerTurn = true;
    private boolean spinInProgress = false;
    private boolean isMeOshiChance = false;
    private boolean isVsMode = false;
    private CharacterData p1Chara, p2Chara;
    private int replayCount = 0;
    private static final int MAX_REPLAY = 3;

    // リール
    private ReelCanvas[] playerReels = new ReelCanvas[COLS];
    private ReelCanvas[] cpuReels    = new ReelCanvas[COLS];
    private int stoppedCount = 0;
    private int[] pendingOffsets = null; // 停止オフセット
    private boolean meOshiOffsetDecided = false; // 目押しチャンス時のoffset確定フラグ
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
        playerHpLabel.setTextFill(Color.WHITE);
        cpuHpLabel.setTextFill(Color.WHITE);
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
        spinNumberLabel.setTextFill(Color.WHITE);
        phaseLabel.setTextFill(Color.SKYBLUE);
        HBox turnBox = new HBox(16, phaseLabel, spinNumberLabel);
        turnBox.setAlignment(Pos.CENTER);

        // ===== ReelCanvas作成 =====
        for (int i = 0; i < COLS; i++) {
            playerReels[i] = new ReelCanvas(SlotMachine.REEL_PATTERNS[i], symbolImages);
            cpuReels[i]    = new ReelCanvas(SlotMachine.REEL_PATTERNS[i], symbolImages);
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
        charReelBox.setStyle("-fx-background-color: rgba(0,0,0,0.4); -fx-background-radius: 12; -fx-padding: 8;");

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
        logArea.setPrefHeight(75);
        logArea.setWrapText(true);

        // ===== レイアウト =====
        VBox content = new VBox(5,
            hpBox, new Separator(), turnBox,
            charReelBox, stopBox,
            resultLabel, startButton,
            new Separator(), logArea
        );
        content.setPadding(new Insets(8));
        content.setAlignment(Pos.TOP_CENTER);

        // 主要UI要素を半透明パネルで囲んで視認性確保
        hpBox.setStyle("-fx-background-color: rgba(0,0,0,0.55); -fx-background-radius: 8; -fx-padding: 6 10;");
        turnBox.setStyle("-fx-background-color: rgba(0,0,0,0.55); -fx-background-radius: 8; -fx-padding: 4 12;");
        resultLabel.setStyle("-fx-background-color: rgba(0,0,0,0.55); -fx-background-radius: 6; -fx-padding: 2 14;");
        logArea.setStyle("-fx-control-inner-background: rgba(20,20,20,0.75); -fx-text-fill: white;");

        // ===== 背景画像 =====
        ImageView bgView = new ImageView();
        try {
            java.net.URL bgUrl = getClass().getResource("/resources/images/casino_bg.png");
            if (bgUrl != null) {
                bgView.setImage(new Image(bgUrl.toExternalForm()));
                bgView.setPreserveRatio(false);
                bgView.setFitWidth(820);
                bgView.setFitHeight(660);
            }
        } catch (Exception e) { System.err.println("背景画像読み込み失敗"); }

        // 背景を少し暗くする半透明オーバーレイ
        javafx.scene.shape.Rectangle dimOverlay = new javafx.scene.shape.Rectangle(820, 660);
        dimOverlay.setFill(Color.color(0, 0, 0, 0.3));

        overlayPane = new Pane();
        overlayPane.setMouseTransparent(true);

        StackPane root = new StackPane(bgView, dimOverlay, content, overlayPane);

        appendLog("=== スロットバトル開始！ ===");
        appendLog("STARTを押してスロットを回してください。\n");

        return new Scene(root, 820, 660);
    }

    // ――― START ―――
    private void onStartPressed() {
        if (gc.isGameOver() || spinInProgress) return;
        spinInProgress = true;
        stoppedCount = 0;
        pendingOffsets = null;
        meOshiOffsetDecided = false;
        judgeInProgress = false;
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
        // 目押しチャンス時はハズレoffset（どの役も成立しない状態から始める）
        pendingOffsets = isMeOshiChance
            ? gc.spinOffsetsMeOshiFail(isPlayerTurn)
            : gc.spinOffsets(isPlayerTurn);
        ReelCanvas[] active = activeReels();
        double speedMul = gc.getReelSpeed(isPlayerTurn);
        for (int i = 0; i < COLS; i++) {
            stopButtons[i].setDisable(false);
            active[i].startSpin(speedMul);
        }
    }

    // ――― CPUスピン ―――
    private void startCpuSpin() {
        int[] offsets;
        boolean cpuMeOshi = isMeOshiChance;
        if (cpuMeOshi) {
            boolean success = rng.nextInt(100) < 30;
            offsets = success
                ? gc.spinOffsetsMeOshiSuccess(false)
                : gc.spinOffsetsMeOshiFail(false);
            appendLog("  CPU 目押しチャンス発生！→ " + (success ? "成功！" : "失敗..."));
        } else {
            offsets = gc.spinOffsets(false);
        }
        isMeOshiChance = false;
        pendingOffsets = offsets;

        for (int i = 0; i < COLS; i++) cpuReels[i].startSpin(gc.getReelSpeed(false));

        cpuStopTimers.clear();
        for (int i = 0; i < COLS; i++) {
            final int col = i;
            final int targetOffset = offsets[col];
            int delay = cpuMeOshi ? 200 * (col + 1) : CPU_STOP_MS * (col + 1);
            PauseTransition pt = new PauseTransition(Duration.millis(delay));
            pt.setOnFinished(e -> {
                cpuReels[col].setOnStopped(sym -> {
                    boolean allStopped = true;
                    for (ReelCanvas r : cpuReels) {
                        if (r.isSpinning()) { allStopped = false; break; }
                    }
                    if (allStopped && !judgeInProgress) onAllStopped(false);
                });
                if (cpuMeOshi) {
                    cpuReels[col].requestStopImmediate(SlotMachine.REEL_PATTERNS[col][targetOffset]);
                } else {
                    cpuReels[col].requestStopAtOffset(targetOffset);
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

        final boolean turnSnapshot = isPlayerTurn;
        active[col].setOnStopped(sym -> {
            // 全リールが止まったか確認
            ReelCanvas[] activeNow = (isVsMode && !turnSnapshot) ? cpuReels : playerReels;
            boolean allStopped = true;
            for (ReelCanvas r : activeNow) {
                if (r.isSpinning()) { allStopped = false; break; }
            }
            if (allStopped && !judgeInProgress) onAllStopped(turnSnapshot);
        });

        active[col].requestStopAtOffset(pendingOffsets[col]);
    }

    private boolean judgeInProgress = false; // 判定の二重呼び出し防止

    // ――― 全リール停止後の処理 ―――
    private void onAllStopped(boolean wasPlayerTurn) {
        if (judgeInProgress) return;
        judgeInProgress = true;

        ReelCanvas[] reels = wasPlayerTurn ? playerReels : cpuReels;
        Symbol[][] grid = new Symbol[COLS][3];
        for (int col = 0; col < COLS; col++) {
            Symbol[] column = reels[col].getCurrentColumn();
            for (int row = 0; row < 3; row++) grid[col][row] = column[row];
        }

        SpinResult sr = gc.judgeGrid(grid, wasPlayerTurn);

        // 目押しチャンスフラグをリセット
        if (isMeOshiChance) isMeOshiChance = false;

        highlightReels(sr, reels);
        onSpinComplete(sr, wasPlayerTurn);
    }

    private ReelCanvas[] activeReels() {
        return (isVsMode && !isPlayerTurn) ? cpuReels : playerReels;
    }

    // ――― 全停止後に成立ラインを一斉ハイライト ―――
    private void highlightReels(SpinResult sr, ReelCanvas[] reels) {
        int line = sr.getWinLine();

        if (line == SpinResult.LINE_NONE) {
            // ハズレ：全行暗く
            for (ReelCanvas reel : reels) reel.setHighlightRow(-1);
            return;
        }

        switch (line) {
            case SpinResult.LINE_TOP:
                for (ReelCanvas reel : reels) reel.setHighlightRow(0);
                break;
            case SpinResult.LINE_MID:
                for (ReelCanvas reel : reels) reel.setHighlightRow(1);
                break;
            case SpinResult.LINE_BOT:
                for (ReelCanvas reel : reels) reel.setHighlightRow(2);
                break;
            case SpinResult.LINE_DIAG_D:
                reels[0].setHighlightRow(0);
                reels[1].setHighlightRow(1);
                reels[2].setHighlightRow(2);
                break;
            case SpinResult.LINE_DIAG_U:
                reels[0].setHighlightRow(2);
                reels[1].setHighlightRow(1);
                reels[2].setHighlightRow(0);
                break;
            default:
                for (ReelCanvas reel : reels) reel.setHighlightRow(1);
                break;
        }
    }

    // ――― スピン完了 ―――
    private void onSpinComplete(SpinResult sr, boolean wasPlayerTurn) {
        showResult(sr);
        appendLog(String.format("  スピン%d [%s]: %s",
            gc.getSpinNumber(), getTurnName(wasPlayerTurn), sr.toString()));

        if (sr.getDescription().contains("７７７")) playBigWinEffect();

        int waitMs = sr.getDescription().contains("７７７") ? 2800 : 900;
        PauseTransition pause = new PauseTransition(Duration.millis(waitMs));
        pause.setOnFinished(e -> {
            spinInProgress  = false;
            pendingOffsets  = null;
            stoppedCount    = 0;
            judgeInProgress = false;

            // ===== エクストラゲーム中 =====
            if (gc.getPhase() == GameController.Phase.EXTRA_P1_TURN
                || gc.getPhase() == GameController.Phase.EXTRA_P2_TURN) {
                handleExtraSpinComplete(sr, wasPlayerTurn);
                return;
            }

            // ===== リプレイ =====
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
            if (sr.isReplay() && replayCount >= MAX_REPLAY) {
                appendLog("  リプレイ上限（" + MAX_REPLAY + "回）に達しました");
            }
            replayCount = 0;

            // ===== メインゲーム：1P→2Pの順でスピン、両者完了後に同時ダメージ =====
            if (wasPlayerTurn) {
                // 1Pのダメージを保留して2Pターンへ
                gc.recordPlayerDamage(sr.getDamage());
                isPlayerTurn = false;

                if (isVsMode) {
                    phaseLabel.setText("【" + getTurnName(false) + "のターン】");
                    phaseLabel.setTextFill(Color.SALMON);
                    startButton.setText("🎰  START");
                    startButton.setDisable(false);
                } else {
                    scheduleCpuTurn(600);
                }
            } else {
                // 2P側のダメージを記録→両者同時適用
                gc.recordCpuDamageAndApply(sr.getDamage());
                updateHpDisplay();

                if (gc.getPhase() == GameController.Phase.EXTRA_P1_TURN) {
                    // 両者同時KO → エクストラゲーム開始
                    startExtraGame();
                    return;
                }
                if (gc.isGameOver()) { endGame(); return; }

                // 通常進行：次のターンへ
                spinNumberLabel.setText("スピン " + gc.getSpinNumber());
                isPlayerTurn = true;
                cancelCpuStopTimers();
                cancelPendingCpuTurn();
                phaseLabel.setText("【" + getTurnName(true) + "のターン】");
                phaseLabel.setTextFill(Color.SKYBLUE);
                startButton.setText("🎰  START");
                startButton.setDisable(false);
            }
        });
        pause.play();
    }

    // ===== エクストラゲーム開始 =====
    private void startExtraGame() {
        appendLog("\n=== 両者同時に倒れた！エクストラゲーム開始！ ===");
        appendLog("先に７７７またはBAR BAR BARを引いた方が勝利！");
        isPlayerTurn = true;
        phaseLabel.setText("【エクストラゲーム：" + getTurnName(true) + "のターン】");
        phaseLabel.setTextFill(Color.GOLD);
        spinNumberLabel.setText("EXTRA " + gc.getSpinNumber());
        startButton.setText("🎰 EXTRA START");
        startButton.setDisable(false);
        cancelCpuStopTimers();
        cancelPendingCpuTurn();
    }

    // ===== エクストラゲームのスピン完了処理 =====
    private void handleExtraSpinComplete(SpinResult sr, boolean wasPlayerTurn) {
        boolean won = gc.isExtraWinSymbol(sr);
        appendLog(won ? "  🎯 ７か BAR を引いた！" : "  ハズレ...");

        if (wasPlayerTurn) {
            gc.recordExtraP1(sr);
            isPlayerTurn = false;

            if (isVsMode) {
                phaseLabel.setText("【エクストラゲーム：" + getTurnName(false) + "のターン】");
                phaseLabel.setTextFill(Color.GOLD);
                startButton.setText("🎰 EXTRA START");
                startButton.setDisable(false);
            } else {
                scheduleCpuTurn(600);
            }
        } else {
            boolean decided = gc.recordExtraP2AndJudge(sr);
            if (decided) {
                endGame();
                return;
            }
            // 相殺：もう一度1Pから
            appendLog("  両者の結果が同じ（相殺）！もう一度！");
            spinNumberLabel.setText("EXTRA " + gc.getSpinNumber());
            isPlayerTurn = true;
            cancelCpuStopTimers();
            cancelPendingCpuTurn();
            phaseLabel.setText("【エクストラゲーム：" + getTurnName(true) + "のターン】");
            phaseLabel.setTextFill(Color.GOLD);
            startButton.setText("🎰 EXTRA START");
            startButton.setDisable(false);
        }
    }

    private void scheduleCpuTurn(int delayMs) {
        phaseLabel.setText("【CPU のターン】");
        phaseLabel.setTextFill(Color.SALMON);
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
        else                           resultLabel.setTextFill(Color.DARKSLATEGRAY);
    }

    private ImageView loadCharImage(String path) {
        ImageView iv = new ImageView();
        iv.setFitWidth(100); iv.setFitHeight(92); iv.setPreserveRatio(true);
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
