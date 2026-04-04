import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * CompositeShape類別：代表複合形狀（群組）
 * 包含多個子形狀，可以將它們作為一個整體操作
 */
public class CompositeShape extends Rectangle implements ShapeInterface {
    private List<Object> children; // 子形狀列表

    /**
     * 建構子：建立一個新的複合形狀
     * @param children 子形狀列表
     */
    public CompositeShape(List<Object> children) {
        this.children = new ArrayList<>(children);
        updateBoundsOnly(); // 初始化時計算邊界
    }

    /**
     * 獲取子形狀列表
     * @return 子形狀列表
     */
    public List<Object> getChildren() {
        return children;
    }

    /**
     * 只更新邊界大小，不改變位置
     * 用於創建群組或新增物件時
     */
    public void updateBoundsOnly() {
        if (children.isEmpty()) {
            width = 50;
            height = 50;
            return;
        }

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (Object obj : children) {
            Rectangle bounds = null;

            if (obj instanceof java.awt.Shape) {
                bounds = ((java.awt.Shape) obj).getBounds();
            } else if (obj instanceof MutableOval) {
                bounds = ((MutableOval) obj).getBounds();
            }

            if (bounds != null) {
                minX = Math.min(minX, bounds.x);
                minY = Math.min(minY, bounds.y);
                maxX = Math.max(maxX, bounds.x + bounds.width);
                maxY = Math.max(maxY, bounds.y + bounds.height);
            }
        }

        // 只更新寬高，位置由外部控制
        width = maxX - minX;
        height = maxY - minY;
        this.x = minX;
        this.y = minY;
    }

    /**
     * 設定群組邊界
     * @param x X座標
     * @param y Y座標
     * @param w 寬度
     * @param h 高度
     */
    public void setGroupBounds(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.width = w;
        this.height = h;
    }

    /**
     * 檢查點是否在複合形狀內
     * @param mx X座標
     * @param my Y座標
     * @return 如果點在形狀內返回true
     */
    @Override
    public boolean contains(int mx, int my) {
        return super.contains(mx, my);
    }

    /**
     * 獲取邊界矩形
     * @return 邊界矩形
     */
    @Override
    public Rectangle getBounds() {
        return this;
    }

    /**
     * 更新邊界
     * @param x 新X座標
     * @param y 新Y座標
     * @param width 新寬度
     * @param height 新高度
     */
    @Override
    public void updateBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
}