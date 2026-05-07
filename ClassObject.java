import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

public class ClassObject extends AbstractUMLObject {
    public ClassObject(int x, int y) {
        super(x, y, 110, 70, "", 8);
    }

    @Override
    public void draw(Graphics2D g2d) {
        Rectangle b = getBounds();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(b.x, b.y, b.width, b.height);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(b.x, b.y, b.width, b.height);
        drawCenteredName(g2d, b);
    }

    @Override
    public boolean contains(Point p) {
        return getBounds().contains(p);
    }
}
