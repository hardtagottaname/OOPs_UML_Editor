import java.awt.Rectangle;

/**
 * MutableOval類別：代表可變的橢圓形狀
 * 實現ShapeInterface介面，提供橢圓的基本操作
 */
public class MutableOval implements ShapeInterface {
    private int x, y, width, height;

    /**
     * 建構子：建立一個新的可變橢圓
     * @param x X座標
     * @param y Y座標
     * @param width 寬度
     * @param height 高度
     */
    public MutableOval(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /**
     * 檢查點是否在橢圓內
     * @param mx X座標
     * @param my Y座標
     * @return 如果點在橢圓內返回true
     */
    @Override
    public boolean contains(int mx, int my) {
        // 先檢查是否在矩形範圍內
        if (mx < x || mx > x + width || my < y || my > y + height) {
            return false;
        }

        // 將點轉換為以橢圓中心為原點的座標
        double rx = width / 2.0;
        double ry = height / 2.0;
        double cx = x + rx;
        double cy = y + ry;
        double dx = mx - cx;
        double dy = my - cy;

        // 橢圓公式: (dx^2 / rx^2) + (dy^2 / ry^2) <= 1
        return ((dx * dx) / (rx * rx) + (dy * dy) / (ry * ry)) <= 1;
    }

    /**
     * 獲取橢圓的邊界矩形
     * @return 邊界矩形
     */
    @Override
    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }

    /**
     * 更新橢圓的位置和大小
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

    // Getter 和 Setter 方法
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public void setX(int x) { this.x = x; }
    public void setY(int y) { this.y = y; }
    public void setWidth(int width) { this.width = width; }
    public void setHeight(int height) { this.height = height; }
}