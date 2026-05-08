import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

/**
 * UML class 圖形。
 *
 * 在畫布上以白底黑框矩形呈現，並提供八個 Port 讓使用者可以從邊或角縮放、
 * 也可以作為 UML 連線端點。
 */
public class ClassObject extends AbstractUMLObject {
    /** 建立預設大小的 class 矩形。 */
    public ClassObject(int x, int y) {
        super(x, y, 110, 70, "", 8);
    }

    /**
     * 繪製 class 物件外框與中央標籤。
     */
    @Override
    public void draw(Graphics2D g2d) {
        Rectangle b = getBounds();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(b.x, b.y, b.width, b.height);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(b.x, b.y, b.width, b.height);
        drawCenteredName(g2d, b);
    }

    /**
     * 矩形物件的點擊範圍就是外框矩形。
     */
    @Override
    public boolean contains(Point p) {
        return getBounds().contains(p);
    }
}
