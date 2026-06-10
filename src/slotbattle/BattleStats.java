package slotbattle;

import java.util.EnumMap;
import java.util.Map;

public class BattleStats {

    // 役ごとの成立回数とダメージ
    private final Map<String, int[]> roleStats = new java.util.LinkedHashMap<>();
    // [0]=回数, [1]=累計ダメージ

    private int totalTurns   = 0;
    private int totalDamage  = 0;

    public BattleStats() {
        roleStats.put("７７７",         new int[]{0, 0});
        roleStats.put("BAR BAR BAR",    new int[]{0, 0});
        roleStats.put("ベルベルベル",    new int[]{0, 0});
        roleStats.put("スイカ３揃い",    new int[]{0, 0});
        roleStats.put("リプレイ",        new int[]{0, 0});
        roleStats.put("チェリー",        new int[]{0, 0});
        roleStats.put("★★★（ゴミ）",  new int[]{0, 0});
        roleStats.put("ハズレ",         new int[]{0, 0});
    }

    public void record(SpinResult sr) {
        String key = resolveKey(sr);
        if (roleStats.containsKey(key)) {
            roleStats.get(key)[0]++;
            roleStats.get(key)[1] += sr.getDamage();
        }
        totalDamage += sr.getDamage();
    }

    private String resolveKey(SpinResult sr) {
        String desc = sr.getDescription();
        if (desc.contains("７７７"))        return "７７７";
        if (desc.contains("BAR"))           return "BAR BAR BAR";
        if (desc.contains("ベルベルベル"))  return "ベルベルベル";
        if (desc.contains("スイカスイカ"))  return "スイカ３揃い";
        if (desc.contains("リプレイ"))      return "リプレイ";
        if (desc.contains("チェリー"))      return "チェリー";
        if (desc.contains("ゴミ"))          return "★★★（ゴミ）";
        return "ハズレ";
    }

    public void incrementTurn() { totalTurns++; }

    public int getTotalTurns()  { return totalTurns; }
    public int getTotalDamage() { return totalDamage; }
    public Map<String, int[]> getRoleStats() { return roleStats; }
}
