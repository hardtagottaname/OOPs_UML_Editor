import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CompositeShape implements UMLObject {
    private final List<UMLObject> children;
    private int depth;

    public CompositeShape(List<UMLObject> children) {
        this.children = new ArrayList<>(children);
    }

    public List<UMLObject> getChildren() {
        return Collections.unmodifiableList(children);
    }

    @Override
    public void draw(Graphics2D g2d) {
        for (UMLObject child : children) {
            child.draw(g2d);
        }
    }

    public void drawSelection(Graphics2D g2d) {
        Rectangle b = getBounds();
        g2d.setColor(new Color(40, 100, 210));
        g2d.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                10.0f, new float[]{5.0f}, 0.0f));
        g2d.drawRect(b.x - 3, b.y - 3, b.width + 6, b.height + 6);
    }

    @Override
    public boolean contains(Point p) {
        return getBounds().contains(p);
    }

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

    @Override
    public void move(int dx, int dy) {
        for (UMLObject child : children) {
            child.move(dx, dy);
        }
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public void setName(String name) {
    }

    @Override
    public Color getLabelColor() {
        return Color.LIGHT_GRAY;
    }

    @Override
    public void setLabelColor(Color color) {
    }

    @Override
    public List<Port> getPorts() {
        return Collections.emptyList();
    }

    @Override
    public Port getNearestPort(Point p) {
        return null;
    }

    @Override
    public void resize(Port port, Point anchor, Point draggedPoint, int minSize) {
    }

    @Override
    public int getDepth() {
        return depth;
    }

    @Override
    public void setDepth(int depth) {
        this.depth = depth;
    }
}
