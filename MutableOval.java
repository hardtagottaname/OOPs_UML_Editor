import java.awt.Rectangle;

/**
 * 可改變位置與大小的橢圓資料物件。
 *
 * 這是較簡化的舊版 shape 實作，只提供座標、大小、命中測試與外框資訊；
 * 目前新版 UML use case 圖形則由 UseCaseObject 負責。
 */
public class MutableOval implements ShapeInterface {

    // 橢圓外接矩形的左上角座標與寬高。
    private int x, y, width, height;

    /** 建立一個指定外接矩形的橢圓。 */
    public MutableOval(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /**
     * 判斷指定座標是否位於橢圓內。
     *
     * 先用外接矩形快速排除不可能命中的點，再用標準橢圓方程式做精準判斷。
     */
    @Override
    public boolean contains(int mx, int my) {
        if (mx < x || mx > x + width || my < y || my > y + height) {
            return false;
        }

        double rx = width / 2.0;
        double ry = height / 2.0;
        double cx = x + rx;
        double cy = y + ry;
        double dx = mx - cx;
        double dy = my - cy;

        return ((dx * dx) / (rx * rx) + (dy * dy) / (ry * ry)) <= 1;
    }

    /** 回傳橢圓的外接矩形。 */
    @Override
    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }

    /** 回傳左上角 x 座標。 */
    public int getX() {
        return x;
    }

    /** 回傳左上角 y 座標。 */
    public int getY() {
        return y;
    }

    /** 回傳寬度。 */
    public int getWidth() {
        return width;
    }

    /** 回傳高度。 */
    public int getHeight() {
        return height;
    }

    /** 設定左上角 x 座標。 */
    public void setX(int x) {
        this.x = x;
    }

    /** 設定左上角 y 座標。 */
    public void setY(int y) {
        this.y = y;
    }

    /** 設定寬度。 */
    public void setWidth(int width) {
        this.width = width;
    }

    /** 設定高度。 */
    public void setHeight(int height) {
        this.height = height;
    }
}
