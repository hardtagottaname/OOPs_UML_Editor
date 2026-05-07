import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Color;
import java.util.List;

public interface UMLObject {
    void draw(Graphics2D g2d);

    boolean contains(Point p);

    Rectangle getBounds();

    void move(int dx, int dy);

    String getName();

    void setName(String name);

    Color getLabelColor();

    void setLabelColor(Color color);

    List<Port> getPorts();

    Port getNearestPort(Point p);

    void resize(Port port, Point anchor, Point draggedPoint, int minSize);

    int getDepth();

    void setDepth(int depth);
}
