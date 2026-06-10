package slotbattle;

import java.util.Random;

public class SlotMachine {

    private final Random random = new Random();
    private boolean cherryBoost = false;
    private final CharacterData chara; // キャラ能力

    // デフォルトコンストラクタ（補正なし）
    public SlotMachine() {
        this.chara = null;
    }

    // キャラ能力付きコンストラクタ
    public SlotMachine(CharacterData chara) {
        this.chara = chara;
    }

    private static final Symbol[] DECO_POOL = {
        Symbol.BELL, Symbol.BELL, Symbol.WATERMELON, Symbol.WATERMELON,
        Symbol.REPLAY, Symbol.STAR, Symbol.STAR, Symbol.BAR, Symbol.CHERRY
    };

    private enum WinType {
        CHERRY(15), SEVEN_3(5), BAR_3(8), BELL_3(18),
        WATERMELON_3(10), REPLAY_3(12), STAR_3(8), MISS(40);
        final int weight;
        WinType(int w) { this.weight = w; }
    }

    // 目押しチャンス発生判定
    public boolean rollMeOshiChance() {
        double rate = (chara != null) ? chara.getMeOshiChanceRate() : 0.10;
        // 毎ターン目押しチャンスキャラ
        if (chara != null && chara.isMeOshiEveryTurn()) return true;
        return random.nextDouble() < rate;
    }

    // 目押し成功結果
    public SpinResult spinMeOshiSuccess() {
        Symbol[][] grid = buildGrid(Symbol.SEVEN, Symbol.SEVEN, Symbol.SEVEN);
        return new SpinResult(
            new Symbol[]{Symbol.SEVEN, Symbol.SEVEN, Symbol.SEVEN},
            grid, 30, false, false, "！！７７７！！ BIG WIN！！（目押し成功！）");
    }

    // 目押し失敗結果
    public SpinResult spinMeOshiFail() {
        Symbol[] pool = {Symbol.BELL, Symbol.WATERMELON, Symbol.REPLAY, Symbol.BAR};
        Symbol r1 = randomFrom(pool);
        Symbol r2; do { r2 = randomFrom(pool); } while (r2 == r1);
        Symbol r3; do { r3 = randomFrom(pool); } while (r3 == r1 || r3 == r2);
        Symbol[][] grid = buildGrid(r1, r2, r3);
        return new SpinResult(new Symbol[]{r1, r2, r3}, grid, 0, false, false, "目押し失敗...ハズレ");
    }

    public SpinResult spin() {
        WinType win = drawWinType();
        Symbol r1, r2, r3;
        int damage = 0;
        boolean isReplay = false;
        boolean hasCherry = false;
        String desc;

        switch (win) {
            case CHERRY:
                r1 = Symbol.CHERRY;
                r2 = randomFrom(Symbol.SEVEN, Symbol.BAR, Symbol.BELL, Symbol.WATERMELON, Symbol.REPLAY, Symbol.STAR);
                r3 = randomFrom(Symbol.SEVEN, Symbol.BAR, Symbol.BELL, Symbol.WATERMELON, Symbol.REPLAY, Symbol.STAR);
                damage = 5; hasCherry = true; cherryBoost = true;
                desc = "チェリー！次スピンは7が出やすい！";
                break;
            case SEVEN_3:
                r1 = r2 = r3 = Symbol.SEVEN;
                damage = 30; cherryBoost = false;
                desc = "！！７７７！！ BIG WIN！！";
                break;
            case BAR_3:
                r1 = r2 = r3 = Symbol.BAR;
                damage = 25; cherryBoost = false;
                desc = "BAR BAR BAR！！";
                break;
            case BELL_3:
                r1 = r2 = r3 = Symbol.BELL;
                damage = 15; cherryBoost = false;
                desc = "ベルベルベル！";
                break;
            case WATERMELON_3:
                r1 = r2 = r3 = Symbol.WATERMELON;
                damage = 15; cherryBoost = false;
                desc = "スイカスイカスイカ！";
                break;
            case REPLAY_3:
                r1 = r2 = r3 = Symbol.REPLAY;
                isReplay = true; cherryBoost = false;
                desc = "リプレイ！もう1スピン！";
                break;
            case STAR_3:
                r1 = r2 = r3 = Symbol.STAR;
                cherryBoost = false;
                desc = "★★★ …ハズレ（ゴミ）";
                break;
            default: {
                Symbol[] missPool = {Symbol.BELL, Symbol.WATERMELON, Symbol.REPLAY, Symbol.STAR, Symbol.STAR};
                r1 = randomFrom(missPool);
                do { r2 = randomFrom(missPool); } while (r2 == r1);
                do { r3 = randomFrom(missPool); } while (r3 == r1 || r3 == r2);
                cherryBoost = false;
                desc = "ハズレ";
                break;
            }
        }

        Symbol[] reels = {r1, r2, r3};
        Symbol[][] grid = buildGrid(r1, r2, r3);
        fixDiagonal(grid);
        return new SpinResult(reels, grid, damage, isReplay, hasCherry, desc);
    }

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

    private Symbol[][] buildGrid(Symbol r1, Symbol r2, Symbol r3) {
        Symbol[] reels = {r1, r2, r3};
        Symbol[][] grid = new Symbol[3][3];
        for (int col = 0; col < 3; col++) {
            grid[col][1] = reels[col];
            grid[col][0] = decoSymbol(reels[col]);
            grid[col][2] = decoSymbol(reels[col]);
        }
        return grid;
    }

    private Symbol decoSymbol(Symbol center) {
        if (random.nextInt(10) < 7) {
            Symbol s; int attempts = 0;
            do { s = DECO_POOL[random.nextInt(DECO_POOL.length)]; attempts++;
            } while (s == center && attempts < 5);
            return s;
        }
        return DECO_POOL[random.nextInt(DECO_POOL.length)];
    }

    private void fixDiagonal(Symbol[][] grid) {
        if (grid[0][0] == grid[1][1] && grid[1][1] == grid[2][2] && grid[0][0] != Symbol.STAR)
            grid[2][2] = decoSymbolForced(grid[2][2]);
        if (grid[0][2] == grid[1][1] && grid[1][1] == grid[2][0] && grid[0][2] != Symbol.STAR)
            grid[2][0] = decoSymbolForced(grid[2][0]);
    }

    private Symbol decoSymbolForced(Symbol exclude) {
        Symbol s;
        do { s = DECO_POOL[random.nextInt(DECO_POOL.length)]; } while (s == exclude);
        return s;
    }

    private Symbol randomFrom(Symbol... symbols) {
        return symbols[random.nextInt(symbols.length)];
    }

    private Symbol randomDifferent(Symbol exclude) {
        Symbol[] pool = {Symbol.SEVEN, Symbol.BAR, Symbol.BELL, Symbol.WATERMELON, Symbol.REPLAY};
        Symbol s;
        do { s = randomFrom(pool); } while (s == exclude);
        return s;
    }

    public CharacterData getChara() { return chara; }
    public void resetCherryBoost()  { cherryBoost = false; }
    public boolean isCherryBoost()  { return cherryBoost; }
}
