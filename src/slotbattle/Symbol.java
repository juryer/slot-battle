package slotbattle;

public enum Symbol {
    SEVEN      ("７",    5,  "seven.png"),
    BAR        ("BAR",   8,  "bar.png"),
    BELL       ("ベル",  20,  "bell.png"),
    WATERMELON ("スイカ", 10, "watermelon.png"),
    REPLAY     ("RP",   15,  "replay.png"),
    CHERRY     ("チェリー", 10, "cherry.png"),
    STAR       ("★",   12,  "star.png"),
    MISS       ("ハズレ", 20, null);

    private final String label;
    private final int baseWeight;
    private final String imageFile;

    Symbol(String label, int baseWeight, String imageFile) {
        this.label      = label;
        this.baseWeight = baseWeight;
        this.imageFile  = imageFile;
    }

    public String getLabel()      { return label; }
    public int getBaseWeight()    { return baseWeight; }
    public String getImageFile()  { return imageFile; }
}
