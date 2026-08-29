package ws.sodiunplugin.feature;

public enum HighlightColor {

    WHITE(0xFFFFFF),
    RED(0xFF5555),
    GOLD(0xFFAA00),
    YELLOW(0xFFFF55),
    GREEN(0x55FF55),
    AQUA(0x55FFFF),
    BLUE(0x5588FF),
    PURPLE(0xAA55FF),
    MAGENTA(0xFF55FF);

    private final int colorValue;

    HighlightColor(int colorValue) {
        this.colorValue = colorValue;
    }

    public int getColorValue() {
        return colorValue;
    }

    public String getTranslationSuffix() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
