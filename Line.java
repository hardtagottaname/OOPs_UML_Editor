/**
 * UML 物件之間的連線資料。
 *
 * Line 只保存起點 Port、終點 Port 與線段種類；實際繪製箭頭、三角形或菱形端點
 * 由 CanvasPanel.drawLine 負責。
 */
public class Line {

    // 連線起點與終點。Port 會依所屬圖形目前位置即時計算座標。
    private Port start;
    private Port end;
    // 線段種類，例如 association、generalization、composition。
    private String type;

    /** 建立一條指定起點、終點與種類的 UML 連線。 */
    public Line(Port start, Port end, String type) {
        this.start = start;
        this.end = end;
        this.type = type;
    }

    /** 回傳連線起點 Port。 */
    public Port getStart() { return start; }

    /** 回傳連線終點 Port。 */
    public Port getEnd() { return end; }

    /** 回傳線段種類。 */
    public String getType() { return type; }

    /** 設定連線起點 Port。 */
    public void setStart(Port start) { this.start = start; }

    /** 設定連線終點 Port。 */
    public void setEnd(Port end) { this.end = end; }

    /** 設定線段種類。 */
    public void setType(String type) { this.type = type; }
}
