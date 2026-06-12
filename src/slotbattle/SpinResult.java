package slotbattle;

public class SpinResult {
    public static final int LINE_TOP    = 0;
    public static final int LINE_MID    = 1;
    public static final int LINE_BOT    = 2;
    public static final int LINE_DIAG_D = 3;
    public static final int LINE_DIAG_U = 4;
    public static final int LINE_NONE   = -1;

    private final Symbol[] reels;
    private final Symbol[][] grid;
    private final int damage;
    private final boolean isReplay;
    private final boolean hasCherry;
    private final String description;
    private final int winLine;

    public SpinResult(Symbol[] reels, Symbol[][] grid, int damage,
                      boolean isReplay, boolean hasCherry, String description, int winLine) {
        this.reels       = reels;
        this.grid        = grid;
        this.damage      = damage;
        this.isReplay    = isReplay;
        this.hasCherry   = hasCherry;
        this.description = description;
        this.winLine     = winLine;
    }

    public SpinResult(Symbol[] reels, Symbol[][] grid, int damage,
                      boolean isReplay, boolean hasCherry, String description) {
        this(reels, grid, damage, isReplay, hasCherry, description, LINE_MID);
    }

    public Symbol[] getReels()      { return reels; }
    public Symbol[][] getGrid()     { return grid; }
    public int getDamage()          { return damage; }
    public boolean isReplay()       { return isReplay; }
    public boolean hasCherry()      { return hasCherry; }
    public String getDescription()  { return description; }
    public int getWinLine()         { return winLine; }

    public Symbol[] getDiagDown() {
        return new Symbol[]{ grid[0][0], grid[1][1], grid[2][2] };
    }

    public Symbol[] getDiagUp() {
        return new Symbol[]{ grid[0][2], grid[1][1], grid[2][0] };
    }

    @Override
    public String toString() {
        return description + (damage > 0 ? " (+" + damage + "ダメージ)" : "");
    }
}
