package slotbattle;

public class GameController {

    public enum Phase {
        PLAYER_TURN, CPU_TURN,        // メインゲーム
        EXTRA_P1_TURN, EXTRA_P2_TURN, // エクストラゲーム
        PLAYER_WIN, CPU_WIN, DRAW_PENDING
    }

    private final Fighter player;
    private final Fighter cpu;
    private final SlotMachine playerSlot;
    private final SlotMachine cpuSlot;
    private Phase phase;
    private int spinNumber;
    private boolean extraMode = false;

    // 保留ダメージ（両者スピン後に同時適用するため）
    private int pendingPlayerDamage = 0;
    private int pendingCpuDamage    = 0;

    private final BattleStats playerStats = new BattleStats();
    private final BattleStats cpuStats    = new BattleStats();

    public GameController() { this(null, null); }

    public GameController(CharacterData playerChara, CharacterData cpuChara) {
        player     = new Fighter("プレイヤー", 100);
        cpu        = new Fighter("CPU", 100);
        playerSlot = new SlotMachine(playerChara);
        cpuSlot    = new SlotMachine(cpuChara);
        phase      = Phase.PLAYER_TURN;
        spinNumber = 1;
    }

    // 通常スピン：停止オフセットを返す
    public int[] spinOffsets(boolean isPlayer) {
        return isPlayer ? playerSlot.spinOffsets() : cpuSlot.spinOffsets();
    }

    public int[] spinOffsetsMeOshiSuccess(boolean isPlayer) {
        SlotMachine slot = isPlayer ? playerSlot : cpuSlot;
        return slot.spinOffsetsMeOshiSuccess();
    }

    public int[] spinOffsetsMeOshiFail(boolean isPlayer) {
        SlotMachine slot = isPlayer ? playerSlot : cpuSlot;
        return slot.spinOffsetsMeOshiFail();
    }

    public SpinResult judgeGrid(Symbol[][] grid, boolean isPlayer) {
        SpinResult sr = GridJudge.judge(grid);
        if (isPlayer) playerStats.record(sr);
        else          cpuStats.record(sr);
        return sr;
    }

    public boolean rollMeOshiChance(boolean isPlayer) {
        return isPlayer ? playerSlot.rollMeOshiChance() : cpuSlot.rollMeOshiChance();
    }

    public boolean isNoStarInMeOshi(boolean isPlayer) {
        CharacterData c = isPlayer ? playerSlot.getChara() : cpuSlot.getChara();
        return c != null && c.isNoStarInMeOshi();
    }

    public double getPlayerReelSpeed() {
        CharacterData c = playerSlot.getChara();
        return c != null ? c.getReelSpeedMultiplier() : 1.0;
    }

    public double getReelSpeed(boolean isPlayer) {
        CharacterData c = isPlayer ? playerSlot.getChara() : cpuSlot.getChara();
        return c != null ? c.getReelSpeedMultiplier() : 1.0;
    }

    // ――― メインゲーム：1Pのスピン結果を保留 ―――
    public void recordPlayerDamage(int damage) {
        pendingPlayerDamage = damage;
    }

    // ――― メインゲーム：2Pのスピン結果を保留してダメージ同時適用 ―――
    // 戻り値: true = 両者のスピンが完了してダメージ適用済み
    public void recordCpuDamageAndApply(int cpuDamage) {
        pendingCpuDamage = cpuDamage;
        applyBothDamage();
    }

    // 両者のダメージを同時適用してHP判定
    private void applyBothDamage() {
        cpu.takeDamage(pendingPlayerDamage);
        player.takeDamage(pendingCpuDamage);

        boolean playerDead = !player.isAlive();
        boolean cpuDead    = !cpu.isAlive();

        pendingPlayerDamage = 0;
        pendingCpuDamage    = 0;

        if (playerDead && cpuDead) {
            // 両者同時KO → エクストラゲームへ
            extraMode = true;
            phase = Phase.EXTRA_P1_TURN;
        } else if (cpuDead) {
            phase = Phase.PLAYER_WIN;
        } else if (playerDead) {
            phase = Phase.CPU_WIN;
        } else {
            phase = Phase.PLAYER_TURN;
            playerStats.incrementTurn();
            cpuStats.incrementTurn();
            spinNumber++;
        }
    }

    // ――― エクストラゲーム：7/BARを引いたか判定 ―――
    public boolean isExtraWinSymbol(SpinResult sr) {
        String desc = sr.getDescription();
        return desc.contains("７７７") || desc.contains("BAR BAR BAR");
    }

    // エクストラゲーム：1P結果を保留
    private boolean extraP1Won = false;

    public void recordExtraP1(SpinResult sr) {
        extraP1Won = isExtraWinSymbol(sr);
    }

    // エクストラゲーム：2P結果を判定して勝敗決定
    // 戻り値: true = 決着, false = 相殺・引き続き
    public boolean recordExtraP2AndJudge(SpinResult sr) {
        boolean extraP2Won = isExtraWinSymbol(sr);

        if (extraP1Won && !extraP2Won) {
            phase = Phase.PLAYER_WIN;
            return true;
        } else if (!extraP1Won && extraP2Won) {
            phase = Phase.CPU_WIN;
            return true;
        } else {
            // 両方引いた or 両方引かなかった → 相殺、次のターンへ
            extraP1Won = false;
            phase = Phase.EXTRA_P1_TURN;
            spinNumber++;
            return false;
        }
    }

    public boolean isExtraMode() { return extraMode; }

    public Phase getPhase()             { return phase; }
    public Fighter getPlayer()          { return player; }
    public Fighter getCpu()             { return cpu; }
    public int getSpinNumber()          { return spinNumber; }
    public int getTurnNumber()          { return spinNumber; }
    public BattleStats getPlayerStats() { return playerStats; }
    public BattleStats getCpuStats()    { return cpuStats; }
    public boolean isGameOver() {
        return phase == Phase.PLAYER_WIN || phase == Phase.CPU_WIN;
    }
}
