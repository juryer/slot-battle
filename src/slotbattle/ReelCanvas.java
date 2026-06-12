package slotbattle;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.util.Map;
import java.util.function.Consumer;

public class ReelCanvas extends Canvas {

    public static final int CELL_W     = 90;
    public static final int CELL_H     = 90;
    public static final int VISIBLE_ROWS = 3;
    public static final int CANVAS_H   = CELL_H * VISIBLE_ROWS;

    private static final double MAX_SPEED  = 16.0;
    private static final double SNAP_DIST  = 12.0; // この距離以下でスナップ停止

    private final Symbol[] pattern;
    private final Map<Symbol, Image> images;

    // スクロール状態
    private double scrollY     = 0;
    private double speed       = 0;
    private boolean spinning   = false;
    private boolean stopping   = false;
    private int stopTargetIndex = -1;
    private int patternOffset  = 0;

    private AnimationTimer timer;
    private Consumer<Symbol> onStopped;
    private Symbol stoppedSymbol = null;
    private boolean isHighlighted = false;

    // 成立ライン（止まった後に非成立セルを暗くする）
    // 0=上, 1=中, 2=下  -1=全部暗くする（ハズレ）
    private int highlightRow = 1; // デフォルト中央

    public ReelCanvas(Symbol[] pattern, Map<Symbol, Image> images) {
        super(CELL_W, CANVAS_H);
        this.pattern = pattern;
        this.images  = images;
        drawFrame();
    }

    // ――― スピン開始 ―――
    public void startSpin() { startSpin(1.0); }

    public void startSpin(double speedMultiplier) {
        spinning      = true;
        stopping      = false;
        stoppedSymbol = null;
        isHighlighted = false;
        speed         = MAX_SPEED * speedMultiplier;
        scrollY       = 0;
        onStopped     = null; // コールバックをリセット

        if (timer != null) timer.stop();
        timer = new AnimationTimer() {
            @Override public void handle(long now) {
                update();
                drawFrame();
            }
        };
        timer.start();
    }

    // ――― 停止要求（patternOffsetを指定して止める） ―――
    public void requestStopAtOffset(int targetOffset) {
        if (!spinning || stopping) return;
        stopping = true;
        // patternOffsetは減る方向にスクロール
        // targetOffsetに到達 = centerIndex = (targetOffset + 1) % len
        stopTargetIndex = (targetOffset + 1) % pattern.length;
        // findTargetIndexと同じ方向（減る方向）で探す
        // 現在のcenterIndexからtargetOffsetまでの距離を計算して正しい方向に設定
        int len = pattern.length;
        int currentCenter = getCenterIndex();
        int targetCenter  = (targetOffset + 1) % len;
        // 現在位置からtargetまで「減る方向」に何コマあるか
        int dist = (currentCenter - targetCenter + len) % len;
        if (dist == 0) dist = len; // 既に到達してる場合は1周させる
        stopTargetIndex = targetCenter;
    }

    // ――― 停止要求（中央行に止める・後方互換） ―――
    public void requestStop(Symbol targetCenter) {
        if (!spinning || stopping) return;
        stopping = true;
        stopTargetIndex = findTargetIndex(targetCenter);
    }

    // ――― 目押し判定用：タイマーを即停止して現在の中央シンボルを返す ―――
    public Symbol freezeAndGetCenter() {
        if (timer != null) timer.stop();
        return getCenterSymbol();
    }

    // ――― 目押しチャンス用：タイマーだけ止めてspinning状態は維持 ―――
    public void freezeReel() {
        if (timer != null) timer.stop();
        scrollY = 0; // スクロール位置をスナップ
        drawFrame();
    }

    // 現在のpatternOffsetを返す
    public int getCurrentOffset() {
        return patternOffset;
    }

    // ――― 即時停止（目押しチャンス用） ―――
    public void requestStopImmediate(Symbol targetCenter) {
        if (timer != null) timer.stop();
        int idx = findTargetIndex(targetCenter);
        patternOffset = (idx - 1 + pattern.length) % pattern.length;
        scrollY       = 0;
        spinning      = false;
        stopping      = false;
        stoppedSymbol = targetCenter;
        isHighlighted = false;
        drawFrame();
        if (onStopped != null) onStopped.accept(stoppedSymbol);
    }

    // ――― 毎フレーム更新 ―――
    private void update() {
        // 上から下にスクロール（scrollYが増えると図柄が下に流れる）
        scrollY += speed;
        while (scrollY >= CELL_H) {
            scrollY -= CELL_H;
            patternOffset = (patternOffset - 1 + pattern.length) % pattern.length;
        }

        if (!stopping) return;

        int centerIdx = getCenterIndex();
        boolean reached = (centerIdx == stopTargetIndex);

        if (reached) {
            if (scrollY < Math.max(SNAP_DIST, speed)) {
                scrollY       = 0;
                spinning      = false;
                stopping      = false;
                stoppedSymbol = pattern[centerIdx];
                isHighlighted = false;
                timer.stop();
                drawFrame();
                playStopShake();
                if (onStopped != null) onStopped.accept(stoppedSymbol);
            }
        }
    }

    // ――― 描画 ―――
    private void drawFrame() {
        GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0, 0, CELL_W, CANVAS_H);

        gc.setFill(Color.rgb(18, 18, 36));
        gc.fillRect(0, 0, CELL_W, CANVAS_H);

        for (int row = -1; row <= VISIBLE_ROWS; row++) {
            // 上から下に流れるのでrowが増えると下の図柄
            int idx = ((patternOffset + row) % pattern.length + pattern.length) % pattern.length;
            Symbol sym = pattern[idx];
            Image img  = images.get(sym);
            double y   = row * CELL_H + scrollY - CELL_H;

            double alpha = 1.0; // 常に全行均一

            gc.setGlobalAlpha(alpha);
            if (img != null) {
                gc.drawImage(img, 3, y + 3, CELL_W - 6, CELL_H - 6);
            } else {
                gc.setFill(Color.WHITE);
                gc.fillText(sym != null ? sym.getLabel() : "?",
                    CELL_W / 2.0 - 8, y + CELL_H / 2.0 + 5);
            }
            gc.setGlobalAlpha(1.0);
        }

        // セル区切り線
        gc.setStroke(Color.color(0.3, 0.3, 0.5, 0.5));
        gc.setLineWidth(1);
        for (int i = 1; i < VISIBLE_ROWS; i++) {
            gc.strokeLine(0, i * CELL_H, CELL_W, i * CELL_H);
        }

        // 有効ライン枠
        if (isHighlighted) {
            gc.setStroke(Color.GOLD);
            gc.setLineWidth(3);
            gc.strokeRect(2, highlightRow * CELL_H + 2, CELL_W - 4, CELL_H - 4);
        } else {
            gc.setStroke(Color.color(1.0, 0.85, 0.0, spinning ? 0.3 : 0.5));
            gc.setLineWidth(spinning ? 1.0 : 2.0);
            gc.strokeRect(2, CELL_H + 2, CELL_W - 4, CELL_H - 4);
        }

        // スピン中：上下グラデーション
        if (spinning) {
            for (int i = 0; i < 20; i++) {
                double t = i / 20.0;
                gc.setFill(Color.color(0.07, 0.07, 0.14, 0.6 * (1 - t)));
                gc.fillRect(0, i * (CELL_H / 20.0), CELL_W, CELL_H / 20.0);
                gc.fillRect(0, CANVAS_H - (i + 1) * (CELL_H / 20.0), CELL_W, CELL_H / 20.0);
            }
        }
    }

    // ――― ユーティリティ ―――

    // 中央行（row=1）に表示されてるインデックス
    private int getCenterIndex() {
        return ((patternOffset + 1) % pattern.length + pattern.length) % pattern.length;
    }

    public Symbol getCenterSymbol() {
        return pattern[getCenterIndex()];
    }

    // 現在表示中の3行分の図柄を取得（画面と完全一致）
    public Symbol[] getCurrentColumn() {
        int len = pattern.length;
        Symbol[] col = new Symbol[3];
        // drawFrameの描画: row=1→上段, row=2→中段, row=3→下段（scrollY=0時）
        for (int row = 0; row < 3; row++) {
            int idx = ((patternOffset + row + 1) % len + len) % len;
            col[row] = pattern[idx];
        }
        return col;
    }

    // targetに最も近い前方（下方向）インデックスを返す
    private int findTargetIndex(Symbol target) {
        int len = pattern.length;
        for (int i = 0; i < len; i++) {
            int idx = ((patternOffset + 1 + i) % len + len) % len;
            if (pattern[idx] == target) return idx;
        }
        return getCenterIndex();
    }

    public void setOnStopped(Consumer<Symbol> cb) { this.onStopped = cb; }
    public boolean isSpinning()      { return spinning; }
    public Symbol getStoppedSymbol() { return stoppedSymbol; }

    // ――― 停止時の震動演出 ―――
    private void playStopShake() {
        javafx.animation.TranslateTransition shake =
            new javafx.animation.TranslateTransition(javafx.util.Duration.millis(180), this);
        shake.setByY(-5);
        shake.setCycleCount(4);
        shake.setAutoReverse(true);
        shake.setOnFinished(e -> setTranslateY(0));
        shake.play();
    }

    // 成立行を設定（0=上, 1=中央, 2=下）
    public void setHighlightRow(int row) {
        this.highlightRow = row;
        this.isHighlighted = true;
        drawFrame();
    }
}
