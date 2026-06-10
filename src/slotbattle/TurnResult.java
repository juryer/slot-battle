package slotbattle;

import java.util.List;

public class TurnResult {
    private final List<SpinResult> spins;
    private final int totalDamage;
    private final int spinCount; // リプレイ込みの実際のスピン数

    public TurnResult(List<SpinResult> spins, int totalDamage, int spinCount) {
        this.spins       = spins;
        this.totalDamage = totalDamage;
        this.spinCount   = spinCount;
    }

    public List<SpinResult> getSpins()  { return spins; }
    public int getTotalDamage()         { return totalDamage; }
    public int getSpinCount()           { return spinCount; }
}
