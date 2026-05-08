/**
 * 左側工具列可切換的工具模式。
 *
 * 每個 enum 值都對應一個按鈕文字；雖然目前 CanvasPanel 多數地方仍使用字串，
 * 這個 enum 可以集中描述哪些模式屬於連線工具、哪些屬於建立圖形工具。
 */
public enum ToolMode {
    SELECT("select"),
    ASSOCIATION("association"),
    GENERALIZATION("generalization"),
    COMPOSITION("composition"),
    CLASS("rect"),
    USE_CASE("oval");

    private final String label;

    /** 建立工具模式與其畫面顯示文字。 */
    ToolMode(String label) {
        this.label = label;
    }

    /** 回傳工具按鈕顯示文字。 */
    public String getLabel() {
        return label;
    }

    /** 判斷是否為建立連線的工具模式。 */
    public boolean isLineMode() {
        return this == ASSOCIATION || this == GENERALIZATION || this == COMPOSITION;
    }

    /** 判斷是否為建立基本圖形的工具模式。 */
    public boolean isShapeMode() {
        return this == CLASS || this == USE_CASE;
    }
}
