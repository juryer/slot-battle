package slotbattle;

public class GameController {

    public enum Phase { PLAYER_TURN, CPU_TURN, PLAYER_WIN, CPU_WIN }

    private final Fighter player;
    private final Fighter cpu;
    private final SlotMachine playerSlot;
    private final SlotMachine cpuSlot;
    private Phase phase;
    private int spinNumber;

    private final BattleStats playerStats = new BattleStats();
    private final BattleStats cpuStats    = new BattleStats();

    // デフォルト（キャラ補正なし）
    public GameController() {
        this(null, null);
    }

    // キャラ指定コンストラクタ
    public GameController(CharacterData playerChara, CharacterData cpuChara) {
        player     = new Fighter("プレイヤー", 100);
        cpu        = new Fighter("CPU", 100);
        playerSlot = new SlotMachine(playerChara);
        cpuSlot    = new SlotMachine(cpuChara);
        phase      = Phase.PLAYER_TURN;
        spinNumber = 1;
    }

    public SpinResult spinOnce(boolean isPlayer) {
        SpinResult sr = isPlayer ? playerSlot.spin() : cpuSlot.spin();
        if (isPlayer) playerStats.record(sr);
        else          cpuStats.record(sr);
        return sr;
    }

    public boolean rollMeOshiChance(boolean isPlayer) {
        return isPlayer ? playerSlot.rollMeOshiChance() : cpuSlot.rollMeOshiChance();
    }

    public SpinResult getMeOshiSuccessResult(boolean isPlayer) {
        SlotMachine slot = isPlayer ? playerSlot : cpuSlot;
        SpinResult sr = slot.spinMeOshiSuccess();
        if (isPlayer) playerStats.record(sr); else cpuStats.record(sr);
        return sr;
    }

    public SpinResult getMeOshiFailResult(boolean isPlayer) {
        SlotMachine slot = isPlayer ? playerSlot : cpuSlot;
        SpinResult sr = slot.spinMeOshiFail();
        if (isPlayer) playerStats.record(sr); else cpuStats.record(sr);
        return sr;
    }

    // キャラ情報取得
    public CharacterData getPlayerChara() { return playerSlot.getChara(); }
    public CharacterData getCpuChara()    { return cpuSlot.getChara(); }

    // リール速度倍率取得
    public double getPlayerReelSpeed() {
        CharacterData c = playerSlot.getChara();
        return c != null ? c.getReelSpeedMultiplier() : 1.0;
    }

    // 目押し時★なし判定
    public boolean isNoStarInMeOshi(boolean isPlayer) {
        CharacterData c = isPlayer ? playerSlot.getChara() : cpuSlot.getChara();
        return c != null && c.isNoStarInMeOshi();
    }

    public void applySpinDamage(boolean isPlayer, int damage) {
        if (isPlayer) cpu.takeDamage(damage);
        else          player.takeDamage(damage);

        if (!cpu.isAlive())       phase = Phase.PLAYER_WIN;
        else if (!player.isAlive()) phase = Phase.CPU_WIN;
        else if (isPlayer)         phase = Phase.CPU_TURN;
        else {
            phase = Phase.PLAYER_TURN;
            playerStats.incrementTurn();
            cpuStats.incrementTurn();
            spinNumber++;
        }
    }

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
