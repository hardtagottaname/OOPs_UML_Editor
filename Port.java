/**
 * Port類別：代表形狀上的連接點
 * 用於連接線條的起點和終點
 */
public class Port {
    private int x, y; // Port的位置座標
    private Object parentShape; // 父形狀物件
    private int type; // Port類型（方向）

    /**
     * 建構子：建立一個新的Port
     * @param parentShape 父形狀物件
     * @param type Port類型（0-7 for Rectangle, 0-3 for Oval）
     */
    public Port(Object parentShape, int type) {
        this.parentShape = parentShape;
        this.type = type;
        updatePosition();
    }

    /**
     * 更新Port的位置，根據父形狀的邊界計算
     */
    public void updatePosition() {
        if (parentShape == null) {
            return; // 保持手動設置的 x, y 不變
        }

        if (parentShape instanceof java.awt.Rectangle) {
            java.awt.Rectangle bounds = ((java.awt.Rectangle) parentShape).getBounds();
            int cx = bounds.x + bounds.width / 2;
            int cy = bounds.y + bounds.height / 2;

            // 矩形：8個方向 (0-7)
            switch (type) {
                case 0: // TopLeft (左上)
                    x = bounds.x;
                    y = bounds.y;
                    break;
                case 1: // Top (上中)
                    x = cx;
                    y = bounds.y;
                    break;
                case 2: // TopRight (右上)
                    x = bounds.x + bounds.width;
                    y = bounds.y;
                    break;
                case 3: // Right (右中)
                    x = bounds.x + bounds.width;
                    y = cy;
                    break;
                case 4: // BottomRight (右下)
                    x = bounds.x + bounds.width;
                    y = bounds.y + bounds.height;
                    break;
                case 5: // Bottom (下中)
                    x = cx;
                    y = bounds.y + bounds.height;
                    break;
                case 6: // BottomLeft (左下)
                    x = bounds.x;
                    y = bounds.y + bounds.height;
                    break;
                case 7: // Left (左中)
                    x = bounds.x;
                    y = cy;
                    break;
                default:
                    x = cx;
                    y = cy; // 防呆
            }
        } else if (parentShape instanceof MutableOval) {
            MutableOval oval = (MutableOval) parentShape;
            int cx = oval.getX() + oval.getWidth() / 2;
            int cy = oval.getY() + oval.getHeight() / 2;

            // 橢圓：4個方向 (0-3)
            switch (type) {
                case 0: x = cx; y = oval.getY(); break; // Top
                case 1: x = oval.getX() + oval.getWidth(); y = cy; break; // Right
                case 2: x = cx; y = oval.getY() + oval.getHeight(); break; // Bottom
                case 3: x = oval.getX(); y = cy; break; // Left
            }
        }
    }

    /**
     * 檢查給定點是否在Port範圍內
     * @param mx X座標
     * @param my Y座標
     * @return 如果點在Port內返回true
     */
    public boolean contains(int mx, int my) {
        return Math.abs(mx - x) <= 5 && Math.abs(my - y) <= 5;
    }

    // Getter 和 Setter 方法
    public int getX() { return x; }
    public int getY() { return y; }
    public Object getParentShape() { return parentShape; }
    public int getType() { return type; }

    public void setX(int x) { this.x = x; }
    public void setY(int y) { this.y = y; }
    public void setParentShape(Object parentShape) { this.parentShape = parentShape; }
    public void setType(int type) { this.type = type; }
}