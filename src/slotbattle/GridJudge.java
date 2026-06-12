package slotbattle;

public class GridJudge {

    // grid[col][row] の9マスから役を判定してSpinResultを生成
    public static SpinResult judge(Symbol[][] grid) {

        // チェリー：左リール（col=0）の中段
        if (grid[0][1] == Symbol.CHERRY) {
            return new SpinResult(
                new Symbol[]{grid[0][1], grid[1][1], grid[2][1]},
                grid, 5, false, true,
                "チェリー！次スピンは7が出やすい！", SpinResult.LINE_NONE);
        }

        // 横3ライン（上・中・下）
        for (int row = 0; row < 3; row++) {
            Symbol s = grid[0][row];
            if (s != Symbol.MISS && s == grid[1][row] && s == grid[2][row]) {
                int line = row; // LINE_TOP=0, LINE_MID=1, LINE_BOT=2
                return buildResult(grid, s, line);
            }
        }

        // 斜め左上→右下
        {
            Symbol s = grid[0][0];
            if (s != Symbol.MISS && s == grid[1][1] && s == grid[2][2]) {
                return buildResult(grid, s, SpinResult.LINE_DIAG_D);
            }
        }

        // 斜め左下→右上
        {
            Symbol s = grid[0][2];
            if (s != Symbol.MISS && s == grid[1][1] && s == grid[2][0]) {
                return buildResult(grid, s, SpinResult.LINE_DIAG_U);
            }
        }

        // ハズレ
        return new SpinResult(
            new Symbol[]{grid[0][1], grid[1][1], grid[2][1]},
            grid, 0, false, false, "ハズレ", SpinResult.LINE_NONE);
    }

    private static SpinResult buildResult(Symbol[][] grid, Symbol sym, int line) {
        int damage = 0;
        boolean isReplay = false;
        String desc;

        switch (sym) {
            case SEVEN:      damage = 30; desc = "！！７７７！！ BIG WIN！！"; break;
            case BAR:        damage = 25; desc = "BAR BAR BAR！！"; break;
            case BELL:       damage = 15; desc = "ベルベルベル！"; break;
            case WATERMELON: damage = 20; desc = "スイカスイカスイカ！"; break;
            case REPLAY:     isReplay = true; desc = "リプレイ！もう1スピン！"; break;
            case STAR:       desc = "★★★ …ハズレ（ゴミ）"; break;
            default:         desc = "ハズレ"; break;
        }

        return new SpinResult(
            new Symbol[]{grid[0][1], grid[1][1], grid[2][1]},
            grid, damage, isReplay, false, desc, line);
    }
}
