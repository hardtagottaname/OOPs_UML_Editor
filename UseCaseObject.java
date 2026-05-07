import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;

public class UseCaseObject extends AbstractUMLObject {
    public UseCaseObject(int x, int y) {
        super(x, y, 110, 70, "", 4);
    }

    @Override
    public void draw(Graphics2D g2d) {
        Rectangle b = getBounds();
        Ellipse2D ellipse = new Ellipse2D.Double(b.x, b.y, b.width, b.height);
        g2d.setColor(Color.WHITE);
        g2d.fill(ellipse);
        g2d.setColor(Color.BLACK);
        g2d.draw(ellipse);
        drawCenteredName(g2d, b);
    }

    @Override
    public boolean contains(Point p) {
        Rectangle b = getBounds();
        double rx = b.width / 2.0;
        double ry = b.height / 2.0;
        double cx = b.x + rx;
        double cy = b.y + ry;
        double dx = p.x - cx;
        double dy = p.y - cy;
        return (dx * dx) / (rx * rx) + (dy * dy) / (ry * ry) <= 1.0;
    }
}
