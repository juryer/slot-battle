package slotbattle;

import java.util.Random;

public class SlotMachine {

    private final Random random = new Random();
    private boolean cherryBoost = false;
    private final CharacterData chara;

    // 各リールのパターン（10コマ、★多めで複数ライン同時成立を抑制）
    public static final Symbol[][] REEL_PATTERNS = {
        { Symbol.SEVEN, Symbol.BELL, Symbol.CHERRY, Symbol.WATERMELON, Symbol.BAR,
          Symbol.STAR, Symbol.REPLAY, Symbol.STAR, Symbol.WATERMELON, Symbol.STAR },
        { Symbol.BELL, Symbol.WATERMELON, Symbol.SEVEN, Symbol.STAR, Symbol.CHERRY,
          Symbol.REPLAY, Symbol.BAR, Symbol.STAR, Symbol.STAR, Symbol.WATERMELON },
        { Symbol.WATERMELON, Symbol.BAR, Symbol.STAR, Symbol.BELL, Symbol.SEVEN,
          Symbol.CHERRY, Symbol.STAR, Symbol.REPLAY, Symbol.WATERMELON, Symbol.STAR }
    };

    private enum WinType {
        CHERRY(15), SEVEN_3(5), BAR_3(8), BELL_3(18),
        WATERMELON_3(10), REPLAY_3(12), STAR_3(8), MISS(40);
        final int weight;
        WinType(int w) { this.weight = w; }
    }

    public SlotMachine() { this.chara = null; }
    public SlotMachine(CharacterData chara) { this.chara = chara; }

    public boolean rollMeOshiChance() {
        double rate = (chara != null) ? chara.getMeOshiChanceRate() : 0.10;
        if (chara != null && chara.isMeOshiEveryTurn()) return true;
        return random.nextDouble() < rate;
    }

    // ――― 通常スピン：各リールの停止オフセットを返す ―――
    // 返値：int[3] = 各リールのpatternOffset（どこで止まるか）
    public int[] spinOffsets() {
        for (int attempt = 0; attempt < 50; attempt++) {
            int[] offsets = spinOffsetsRaw();
            Symbol[][] grid = offsetsToGrid(offsets);
            int matchCount = countMatchingLines(grid);
            boolean hasCherry = (grid[0][1] == Symbol.CHERRY);
            int totalSignals = matchCount + (hasCherry ? 1 : 0);

            // ハズレ目標：何も成立しない
            // 役目標：成立は1つだけ（チェリーと他ラインの併発も避ける）
            if (totalSignals <= 1) return offsets;
        }
        // 50回試しても見つからない場合は最後の結果を返す
        return spinOffsetsRaw();
    }

    private int[] spinOffsetsRaw() {
        WinType win = drawWinType();
        int[] offsets = new int[3];

        if (win == WinType.MISS) {
            offsets = randomOffsetsNoMatch();
        } else if (win == WinType.CHERRY) {
            offsets[0] = findOffsetForSymbolAtRow(0, Symbol.CHERRY, 1);
            offsets[1] = random.nextInt(REEL_PATTERNS[1].length);
            offsets[2] = random.nextInt(REEL_PATTERNS[2].length);
            cherryBoost = true;
        } else {
            Symbol target = getTargetSymbol(win);
            int line = randomLine();
            offsets = alignOffsetsToLine(target, line);
        }
        return offsets;
    }

    // grid内で成立してるライン（横3＋斜め2）の数を数える
    private int countMatchingLines(Symbol[][] grid) {
        int count = 0;
        if (checkLine(grid, 0,0, 1,0, 2,0)) count++; // 上段
        if (checkLine(grid, 0,1, 1,1, 2,1)) count++; // 中段
        if (checkLine(grid, 0,2, 1,2, 2,2)) count++; // 下段
        if (checkLine(grid, 0,0, 1,1, 2,2)) count++; // 斜めD
        if (checkLine(grid, 0,2, 1,1, 2,0)) count++; // 斜めU
        return count;
    }

    // ――― 目押し成功：7を中央に揃えるoffset ―――
    public int[] spinOffsetsMeOshiSuccess() {
        int[] offsets = new int[3];
        for (int col = 0; col < 3; col++) {
            offsets[col] = findOffsetForSymbolAtRow(col, Symbol.SEVEN, 1);
        }
        return offsets;
    }

    // ――― 目押し失敗：ランダム・揃わない ―――
    public int[] spinOffsetsMeOshiFail() {
        return randomOffsetsNoMatch();
    }

    // ――― ライン上に指定図柄を揃えるoffsetを計算 ―――
    private int[] alignOffsetsToLine(Symbol target, int line) {
        int[] offsets = new int[3];
        for (int col = 0; col < 3; col++) {
            int row = getRowForLine(line, col); // このラインでこの列が何行目か
            offsets[col] = findOffsetForSymbolAtRow(col, target, row);
        }
        return offsets;
    }

    // patternOffset停止時にrowに指定図柄が来るoffsetを探す
    private int findOffsetForSymbolAtRow(int col, Symbol target, int row) {
        Symbol[] pat = REEL_PATTERNS[col];
        int len = pat.length;
        // drawFrame: idx = (patternOffset + row + 1) % len でrow行目の図柄
        for (int i = 0; i < len; i++) {
            int symbolIdx = ((i + row + 1) % len + len) % len;
            if (pat[symbolIdx] == target) {
                return i;
            }
        }
        return random.nextInt(len);
    }

    // ライン種別と列から行番号を返す
    public static int getRowForLine(int line, int col) {
        switch (line) {
            case SpinResult.LINE_TOP:    return 0;
            case SpinResult.LINE_MID:    return 1;
            case SpinResult.LINE_BOT:    return 2;
            case SpinResult.LINE_DIAG_D: return col;
            case SpinResult.LINE_DIAG_U: return 2 - col;
            default:                     return 1;
        }
    }

    // ランダムライン
    private int randomLine() {
        int[] lines = {SpinResult.LINE_TOP, SpinResult.LINE_MID, SpinResult.LINE_BOT,
                       SpinResult.LINE_DIAG_D, SpinResult.LINE_DIAG_U};
        return lines[random.nextInt(lines.length)];
    }

    // 役の図柄を返す
    private Symbol getTargetSymbol(WinType win) {
        switch (win) {
            case SEVEN_3:      return Symbol.SEVEN;
            case BAR_3:        return Symbol.BAR;
            case BELL_3:       return Symbol.BELL;
            case WATERMELON_3: return Symbol.WATERMELON;
            case REPLAY_3:     return Symbol.REPLAY;
            case STAR_3:       return Symbol.STAR;
            default:           return Symbol.BELL;
        }
    }

    // どのラインも揃わないランダムoffset
    private int[] randomOffsetsNoMatch() {
        for (int t = 0; t < 200; t++) {
            int[] offsets = new int[3];
            for (int col = 0; col < 3; col++)
                offsets[col] = random.nextInt(REEL_PATTERNS[col].length);

            Symbol[][] grid = offsetsToGrid(offsets);
            if (!anyLineMatches(grid)) return offsets;
        }
        // 200回試しても見つからない場合：強制的に揃いを壊す
        int[] offsets = new int[3];
        for (int col = 0; col < 3; col++)
            offsets[col] = random.nextInt(REEL_PATTERNS[col].length);
        // 揃いを強制解除：col=2のoffsetをずらしながら揃わない組み合わせを探す
        for (int i = 0; i < REEL_PATTERNS[2].length; i++) {
            offsets[2] = (offsets[2] + 1) % REEL_PATTERNS[2].length;
            Symbol[][] grid = offsetsToGrid(offsets);
            if (!anyLineMatches(grid)) return offsets;
        }
        return offsets;
    }

    // offsetsからグリッドを生成（drawFrameの描画と一致）
    public static Symbol[][] offsetsToGrid(int[] offsets) {
        Symbol[][] grid = new Symbol[3][3];
        for (int col = 0; col < 3; col++) {
            Symbol[] pat = REEL_PATTERNS[col];
            int len = pat.length;
            for (int row = 0; row < 3; row++) {
                grid[col][row] = pat[((offsets[col] + row + 1) % len + len) % len];
            }
        }
        return grid;
    }

    // 任意のラインが揃ってるかチェック
    private boolean anyLineMatches(Symbol[][] grid) {
        // 横3ライン
        if (checkLine(grid, 0,0, 1,0, 2,0)) return true;
        if (checkLine(grid, 0,1, 1,1, 2,1)) return true;
        if (checkLine(grid, 0,2, 1,2, 2,2)) return true;
        // 斜め2本
        if (checkLine(grid, 0,0, 1,1, 2,2)) return true;
        if (checkLine(grid, 0,2, 1,1, 2,0)) return true;
        return false;
    }

    private boolean checkLine(Symbol[][] g, int c1, int r1, int c2, int r2, int c3, int r3) {
        Symbol s = g[c1][r1];
        return s != null && s != Symbol.MISS && s == g[c2][r2] && s == g[c3][r3];
    }

    // cherryBoostリセット
    public void resetCherryBoost() { cherryBoost = false; }
    public boolean isCherryBoost() { return cherryBoost; }
    public CharacterData getChara() { return chara; }

    private WinType drawWinType() {
        double koYaku = (chara != null) ? chara.getKoYakuMultiplier() : 1.0;
        double barMul = (chara != null) ? chara.getBarMultiplier()    : 1.0;

        int sevenW      = cherryBoost ? 25 : WinType.SEVEN_3.weight;
        int missW       = cherryBoost ?  3 : WinType.MISS.weight;
        int cherryW     = (int)(WinType.CHERRY.weight      * koYaku);
        int bellW       = (int)(WinType.BELL_3.weight      * koYaku);
        int watermelonW = (int)(WinType.WATERMELON_3.weight * koYaku);
        int barW        = (int)(WinType.BAR_3.weight       * barMul);

        int total = cherryW + sevenW + barW + bellW + watermelonW
                  + WinType.REPLAY_3.weight + WinType.STAR_3.weight + missW;
        if (total <= 0) total = 1;

        int r = random.nextInt(total), cum = 0;
        cum += cherryW;                      if (r < cum) return WinType.CHERRY;
        cum += sevenW;                       if (r < cum) return WinType.SEVEN_3;
        cum += barW;                         if (r < cum) return WinType.BAR_3;
        cum += bellW;                        if (r < cum) return WinType.BELL_3;
        cum += watermelonW;                  if (r < cum) return WinType.WATERMELON_3;
        cum += WinType.REPLAY_3.weight;      if (r < cum) return WinType.REPLAY_3;
        cum += WinType.STAR_3.weight;        if (r < cum) return WinType.STAR_3;
        return WinType.MISS;
    }
}
