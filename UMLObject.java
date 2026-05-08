import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Color;
import java.util.List;

/**
 * 畫布上所有可操作 UML 元件的共同介面。
 *
 * 基本圖形與群組都實作這個介面，讓 CanvasPanel 可以用同一套流程
 * 繪製、選取、移動、調整大小與管理顯示深度。
 */
public interface UMLObject {
    /** 將物件畫到畫布上。 */
    void draw(Graphics2D g2d);

    /** 判斷滑鼠點是否落在物件內，用於點選與 hover。 */
    boolean contains(Point p);

    /** 回傳物件外框範圍，用於選取框、群組邊界與碰撞判斷。 */
    Rectangle getBounds();

    /** 將物件水平移動 dx、垂直移動 dy。 */
    void move(int dx, int dy);

    /** 回傳顯示在物件中央的標籤文字。 */
    String getName();

    /** 設定顯示在物件中央的標籤文字。 */
    void setName(String name);

    /** 回傳標籤背景色。 */
    Color getLabelColor();

    /** 設定標籤背景色。 */
    void setLabelColor(Color color);

    /** 回傳可連線或可拖曳調整大小的控制點。 */
    List<Port> getPorts();

    /** 找出離指定點最近的 Port，通常用於連線吸附。 */
    Port getNearestPort(Point p);

    /** 依照被拖曳的 Port、固定錨點與滑鼠位置調整物件大小。 */
    void resize(Port port, Point anchor, Point draggedPoint, int minSize);

    /** 回傳顯示深度；數字越小代表越靠前。 */
    int getDepth();

    /** 設定顯示深度；CanvasPanel 會在物件前後順序改變時重新整理。 */
    void setDepth(int depth);
}
