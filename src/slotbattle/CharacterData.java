package slotbattle;

public class CharacterData {

    public enum CharaId {
        KAKUTARO, ROJINURI, OBACHAN, JANSHI_GIRL, MEOSHI_TATSU
    }

    private final CharaId id;
    private final String name;
    private final String imagePath;
    private final String description;
    private final String abilityText;

    // 能力パラメータ
    private final double meOshiChanceRate;   // 目押しチャンス発生率(0.0~1.0)
    private final boolean meOshiEveryTurn;   // 毎ターン目押しチャンス
    private final double koYakuMultiplier;   // 子役確率倍率
    private final double barMultiplier;      // BAR確率倍率
    private final boolean noStarInMeOshi;    // 目押し時★なし
    private final double reelSpeedMultiplier;// リール速度倍率

    public CharacterData(CharaId id, String name, String imagePath,
                         String description, String abilityText,
                         double meOshiChanceRate, boolean meOshiEveryTurn,
                         double koYakuMultiplier, double barMultiplier,
                         boolean noStarInMeOshi, double reelSpeedMultiplier) {
        this.id                  = id;
        this.name                = name;
        this.imagePath           = imagePath;
        this.description         = description;
        this.abilityText         = abilityText;
        this.meOshiChanceRate    = meOshiChanceRate;
        this.meOshiEveryTurn     = meOshiEveryTurn;
        this.koYakuMultiplier    = koYakuMultiplier;
        this.barMultiplier       = barMultiplier;
        this.noStarInMeOshi      = noStarInMeOshi;
        this.reelSpeedMultiplier = reelSpeedMultiplier;
    }

    // ===== キャラクター定義 =====
    public static CharacterData[] getAllCharacters() {
        return new CharacterData[] {
            // 賭太郎
            new CharacterData(CharaId.KAKUTARO,
                "賭太郎",
                "/resources/images/player.png",
                "ただのスロットが好きな一般人",
                "補正なし",
                0.10, false, 1.0, 1.0, false, 1.0),

            // 路地裏の帝王
            new CharacterData(CharaId.ROJINURI,
                "路地裏の帝王",
                "/resources/images/enemy.png",
                "ギャンブルのために人生を捨てた伝説の男",
                "目押しチャンス17%、子役出づらい",
                0.17, false, 0.7, 1.0, false, 1.0),

            // おばちゃん
            new CharacterData(CharaId.OBACHAN,
                "覚醒するおばちゃん",
                "/resources/images/obachan.png",
                "アツい演出を生きがいにするギャンブラー",
                "子役確率低下、BAR1.5倍",
                0.10, false, 0.6, 1.5, false, 1.0),

            // スーパー雀士ガール
            new CharacterData(CharaId.JANSHI_GIRL,
                "スーパー雀士ガール",
                "/resources/images/janshi.png",
                "スロット初心者だが持前の勝負強さは本物",
                "目押しチャンス7%、発動時★消滅",
                0.07, false, 1.0, 1.0, true, 1.0),

            // 目押しのタツ
            new CharacterData(CharaId.MEOSHI_TATSU,
                "目押しのタツ",
                "/resources/images/tatsu.png",
                "7を左上段にビタ押しで正確に止める達人",
                "毎ターン目押しチャンス、リール速度2倍",
                1.0, true, 1.0, 1.0, false, 2.0),
        };
    }

    public CharaId getId()                  { return id; }
    public String getName()                 { return name; }
    public String getImagePath()            { return imagePath; }
    public String getDescription()          { return description; }
    public String getAbilityText()          { return abilityText; }
    public double getMeOshiChanceRate()     { return meOshiChanceRate; }
    public boolean isMeOshiEveryTurn()      { return meOshiEveryTurn; }
    public double getKoYakuMultiplier()     { return koYakuMultiplier; }
    public double getBarMultiplier()        { return barMultiplier; }
    public boolean isNoStarInMeOshi()       { return noStarInMeOshi; }
    public double getReelSpeedMultiplier()  { return reelSpeedMultiplier; }
}
