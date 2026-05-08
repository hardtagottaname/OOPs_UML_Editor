import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 由多個 UMLObject 組成的群組物件。
 *
 * 群組本身不另外繪製圖形，而是把 draw、move 等操作轉交給所有子物件。
 * 它實作 UMLObject 是為了讓 CanvasPanel 可以把群組和一般圖形用同一種方式管理。
 */
public class CompositeShape implements UMLObject {
    // 群組內包含的物件。座標仍維持在畫布座標系中。
    private final List<UMLObject> children;
    // 群組在畫布上的顯示深度。
    private int depth;

    /** 建立群組，並複製傳入的子物件清單，避免外部直接改動。 */
    public CompositeShape(List<UMLObject> children) {
        this.children = new ArrayList<>(children);
    }

    /** 回傳唯讀子物件清單，供解散群組時取回原本物件。 */
    public List<UMLObject> getChildren() {
        return Collections.unmodifiableList(children);
    }

    /** 依序繪製群組內所有子物件。 */
    @Override
    public void draw(Graphics2D g2d) {
        for (UMLObject child : children) {
            child.draw(g2d);
        }
    }

    /**
     * 繪製群組選取外框。
     *
     * 群組沒有自己的 Port，因此只用虛線矩形表示整個群組範圍。
     */
    public void drawSelection(Graphics2D g2d) {
        Rectangle b = getBounds();
        g2d.setColor(new Color(40, 100, 210));
        g2d.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                10.0f, new float[]{5.0f}, 0.0f));
        g2d.drawRect(b.x - 3, b.y - 3, b.width + 6, b.height + 6);
    }

    /** 群組的點擊範圍採用所有子物件的聯集外框。 */
    @Override
    public boolean contains(Point p) {
        return getBounds().contains(p);
    }

    /**
     * 取得所有子物件外框的聯集，作為群組外框。
     */
    @Override
    public Rectangle getBounds() {
        if (children.isEmpty()) {
            return new Rectangle();
        }

        Rectangle bounds = new Rectangle(children.get(0).getBounds());
        for (int i = 1; i < children.size(); i++) {
            bounds = bounds.union(children.get(i).getBounds());
        }
        return bounds;
    }

    /** 移動群組時，實際上是把所有子物件一起移動。 */
    @Override
    public void move(int dx, int dy) {
        for (UMLObject child : children) {
            child.move(dx, dy);
        }
    }

    /** 群組沒有自己的標籤，因此固定回傳空字串。 */
    @Override
    public String getName() {
        return "";
    }

    /** 群組不支援設定標籤，這個方法刻意不做任何事。 */
    @Override
    public void setName(String name) {
    }

    /** 群組沒有標籤顏色，回傳一個預設值以符合介面。 */
    @Override
    public Color getLabelColor() {
        return Color.LIGHT_GRAY;
    }

    /** 群組不支援設定標籤顏色，這個方法刻意不做任何事。 */
    @Override
    public void setLabelColor(Color color) {
    }

    /** 群組本身沒有 Port，連線與縮放只能作用在基本物件上。 */
    @Override
    public List<Port> getPorts() {
        return Collections.emptyList();
    }

    /** 群組沒有 Port，因此找不到最近 Port。 */
    @Override
    public Port getNearestPort(Point p) {
        return null;
    }

    /** 群組不支援直接縮放，這個方法刻意不做任何事。 */
    @Override
    public void resize(Port port, Point anchor, Point draggedPoint, int minSize) {
    }

    /** 取得群組顯示深度。 */
    @Override
    public int getDepth() {
        return depth;
    }

    /** 設定群組顯示深度。 */
    @Override
    public void setDepth(int depth) {
        this.depth = depth;
    }
}
