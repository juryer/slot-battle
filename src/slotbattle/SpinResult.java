package slotbattle;

public class SpinResult {
    // reels[0..2] = 横中央ライン（有効ライン・判定に使う）
    private final Symbol[] reels;
    // grid[col][row]: col=0..2(左中右), row=0..2(上中下)
    // grid[col][1] == reels[col]（中央行）
    private final Symbol[][] grid;

    private final int damage;
    private final boolean isReplay;
    private final boolean hasCherry;
    private final String description;

    public SpinResult(Symbol[] reels, Symbol[][] grid, int damage,
                      boolean isReplay, boolean hasCherry, String description) {
        this.reels       = reels;
        this.grid        = grid;
        this.damage      = damage;
        this.isReplay    = isReplay;
        this.hasCherry   = hasCherry;
        this.description = description;
    }

    public Symbol[] getReels()       { return reels; }
    public Symbol[][] getGrid()      { return grid; }
    public int getDamage()           { return damage; }
    public boolean isReplay()        { return isReplay; }
    public boolean hasCherry()       { return hasCherry; }
    public String getDescription()   { return description; }

    // 斜めライン取得（左上→右下）
    public Symbol[] getDiagDown() {
        return new Symbol[]{ grid[0][0], grid[1][1], grid[2][2] };
    }

    // 斜めライン取得（左下→右上）
    public Symbol[] getDiagUp() {
        return new Symbol[]{ grid[0][2], grid[1][1], grid[2][0] };
    }

    @Override
    public String toString() {
        return String.format("[%s|%s|%s] %s (+%dダメージ)",
            reels[0].getLabel(), reels[1].getLabel(), reels[2].getLabel(),
            description, damage);
    }
}
