public enum ToolMode {
    SELECT("select"),
    ASSOCIATION("association"),
    GENERALIZATION("generalization"),
    COMPOSITION("composition"),
    CLASS("rect"),
    USE_CASE("oval");

    private final String label;

    ToolMode(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean isLineMode() {
        return this == ASSOCIATION || this == GENERALIZATION || this == COMPOSITION;
    }

    public boolean isShapeMode() {
        return this == CLASS || this == USE_CASE;
    }
}
