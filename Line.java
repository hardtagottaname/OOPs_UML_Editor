/**
 * Line類別：代表形狀之間的連接線
 * 支援不同類型的關聯：association, generalization, composition
 */
public class Line {
    private Port start; // 起點Port
    private Port end;   // 終點Port
    private String type; // 線條類型

    /**
     * 建構子：建立一條新的線條
     * @param start 起點Port
     * @param end 終點Port
     * @param type 線條類型 ("association", "generalization", "composition")
     */
    public Line(Port start, Port end, String type) {
        this.start = start;
        this.end = end;
        this.type = type;
    }

    // Getter 和 Setter 方法
    public Port getStart() { return start; }
    public Port getEnd() { return end; }
    public String getType() { return type; }

    public void setStart(Port start) { this.start = start; }
    public void setEnd(Port end) { this.end = end; }
    public void setType(String type) { this.type = type; }
}