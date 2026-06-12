import java.awt.Point;
import java.awt.Rectangle;

/**
 * UML 圖形周圍的連接 / 縮放控制點。
 *
 * Port 不直接儲存固定座標，而是每次被查詢時依照父圖形目前外框重新計算位置，
 * 因此圖形移動或縮放後，連線端點也會跟著更新。
 */
public class Port {
    // 滑鼠判定範圍。比畫出來的點稍大，讓使用者比較容易點到。
    private static final int HIT_SIZE = 8;

    // Port 所屬的 UML 圖形。
    // 用的是 polymorphism，只要有實作 UMLObject 這個 interface 就可以，不用管他是什麼類型的圖形。
    private final UMLObject parentShape;
    // Port 的位置編號；矩形有 8 個，橢圓有 4 個。
    private final int type;
    // 依父圖形外框計算出的目前座標。
    private int x;
    private int y;

    public Port(UMLObject parentShape, int type) {
        this.parentShape = parentShape;
        this.type = type;
    }

    /**
     * 依照父圖形外框與 Port 編號更新目前座標。
     *
     * 八個 Port 的順序是左上、上、右上、右、右下、下、左下、左；
     * 四個 Port 的順序是上、右、下、左。
     */
    public void updatePosition() {
        Rectangle b = parentShape.getBounds();
        int cx = b.x + b.width / 2;
        int cy = b.y + b.height / 2;

        if (parentShape.getPorts().size() == 8) {
            switch (type) {
                case 0:
                    x = b.x;
                    y = b.y;
                    break;
                case 1:
                    x = cx;
                    y = b.y;
                    break;
                case 2:
                    x = b.x + b.width;
                    y = b.y;
                    break;
                case 3:
                    x = b.x + b.width;
                    y = cy;
                    break;
                case 4:
                    x = b.x + b.width;
                    y = b.y + b.height;
                    break;
                case 5:
                    x = cx;
                    y = b.y + b.height;
                    break;
                case 6:
                    x = b.x;
                    y = b.y + b.height;
                    break;
                case 7:
                    x = b.x;
                    y = cy;
                    break;
                default:
                    x = cx;
                    y = cy;
                    break;
            }
        } else {
            switch (type) {
                case 0:
                    x = cx;
                    y = b.y;
                    break;
                case 1:
                    x = b.x + b.width;
                    y = cy;
                    break;
                case 2:
                    x = cx;
                    y = b.y + b.height;
                    break;
                case 3:
                    x = b.x;
                    y = cy;
                    break;
                default:
                    x = cx;
                    y = cy;
                    break;
            }
        }
    }

    /**
     * 判斷滑鼠座標是否落在 Port 的可點擊範圍內。
     */
    public boolean contains(int mx, int my) {
        updatePosition();
        return Math.abs(mx - x) <= HIT_SIZE && Math.abs(my - y) <= HIT_SIZE;
    }

    /** 回傳目前 Port 座標。 */
    public Point getLocation() {
        updatePosition();
        return new Point(x, y);
    }

    /** 回傳目前 x 座標。 */
    public int getX() {
        updatePosition();
        return x;
    }

    /** 回傳目前 y 座標。 */
    public int getY() {
        updatePosition();
        return y;
    }

    /** 回傳這個 Port 所屬的 UML 圖形。 */
    public UMLObject getParentShape() {
        return parentShape;
    }

    /** 回傳 Port 位置編號。 */
    public int getType() {
        return type;
    }
}
