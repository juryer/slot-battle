package slotbattle;

public class Fighter {
    private String name;
    private int maxHp;
    private int currentHp;

    public Fighter(String name, int maxHp) {
        this.name = name;
        this.maxHp = maxHp;
        this.currentHp = maxHp;
    }

    public void takeDamage(int damage) {
        currentHp = Math.max(0, currentHp - damage);
    }

    public void heal(int amount) {
        currentHp = Math.min(maxHp, currentHp + amount);
    }

    public boolean isAlive() {
        return currentHp > 0;
    }

    public String getName()     { return name; }
    public int getMaxHp()       { return maxHp; }
    public int getCurrentHp()   { return currentHp; }

    public double getHpRatio() {
        return (double) currentHp / maxHp;
    }
}
