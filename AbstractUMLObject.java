import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AbstractUMLObject implements UMLObject {
    protected static final int PORT_SIZE = 8;

    protected int x;
    protected int y;
    protected int width;
    protected int height;
    protected String name;
    protected Color labelColor = new Color(235, 235, 235);
    protected int depth;
    private final List<Port> ports = new ArrayList<>();

    protected AbstractUMLObject(int x, int y, int width, int height, String name) {
        this(x, y, width, height, name, 4);
    }

    protected AbstractUMLObject(int x, int y, int width, int height, String name, int portCount) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.name = name;
        for (int i = 0; i < portCount; i++) {
            ports.add(new Port(this, i));
        }
    }

    @Override
    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }

    @Override
    public void move(int dx, int dy) {
        x += dx;
        y += dy;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name == null ? "" : name;
    }

    @Override
    public Color getLabelColor() {
        return labelColor;
    }

    @Override
    public void setLabelColor(Color color) {
        labelColor = color == null ? new Color(235, 235, 235) : color;
    }

    @Override
    public List<Port> getPorts() {
        return Collections.unmodifiableList(ports);
    }

    @Override
    public Port getNearestPort(Point p) {
        Port nearest = null;
        double bestDistance = Double.MAX_VALUE;
        for (Port port : ports) {
            Point location = port.getLocation();
            double distance = location.distanceSq(p);
            if (distance < bestDistance) {
                bestDistance = distance;
                nearest = port;
            }
        }
        return nearest;
    }

    @Override
    public void resize(Port port, Point anchor, Point draggedPoint, int minSize) {
        resizeFromBounds(getBounds(), port, anchor, draggedPoint, minSize);
    }

    public void resizeFromBounds(Rectangle originalBounds, Port port, Point anchor, Point draggedPoint, int minSize) {
        Rectangle b = new Rectangle(originalBounds);
        int left = b.x;
        int top = b.y;
        int right = b.x + b.width;
        int bottom = b.y + b.height;
        boolean changesHorizontal = false;
        boolean changesVertical = false;

        if (ports.size() == 8) {
            switch (port.getType()) {
                case 0:
                case 6:
                case 7:
                    left = draggedPoint.x;
                    changesHorizontal = true;
                    break;
                case 2:
                case 3:
                case 4:
                    right = draggedPoint.x;
                    changesHorizontal = true;
                    break;
                default:
                    break;
            }
            switch (port.getType()) {
                case 0:
                case 1:
                case 2:
                    top = draggedPoint.y;
                    changesVertical = true;
                    break;
                case 4:
                case 5:
                case 6:
                    bottom = draggedPoint.y;
                    changesVertical = true;
                    break;
                default:
                    break;
            }
        } else {
            switch (port.getType()) {
                case 0:
                    top = draggedPoint.y;
                    changesVertical = true;
                    break;
                case 1:
                    right = draggedPoint.x;
                    changesHorizontal = true;
                    break;
                case 2:
                    bottom = draggedPoint.y;
                    changesVertical = true;
                    break;
                case 3:
                    left = draggedPoint.x;
                    changesHorizontal = true;
                    break;
                default:
                    break;
            }
        }

        int newLeft = Math.min(left, right);
        int newRight = Math.max(left, right);
        int newTop = Math.min(top, bottom);
        int newBottom = Math.max(top, bottom);

        if (changesHorizontal && newRight - newLeft < minSize) {
            if (draggedPoint.x < anchor.x) {
                newLeft = anchor.x - minSize;
                newRight = anchor.x;
            } else {
                newLeft = anchor.x;
                newRight = anchor.x + minSize;
            }
        }
        if (changesVertical && newBottom - newTop < minSize) {
            if (draggedPoint.y < anchor.y) {
                newTop = anchor.y - minSize;
                newBottom = anchor.y;
            } else {
                newTop = anchor.y;
                newBottom = anchor.y + minSize;
            }
        }

        x = newLeft;
        y = newTop;
        width = newRight - newLeft;
        height = newBottom - newTop;
    }

    @Override
    public int getDepth() {
        return depth;
    }

    @Override
    public void setDepth(int depth) {
        this.depth = depth;
    }

    public void drawSelection(Graphics2D g2d) {
        Rectangle b = getBounds();
        g2d.setColor(Color.BLACK);
        for (Port port : ports) {
            Point p = port.getLocation();
            g2d.setColor(Color.WHITE);
            g2d.fillRect(p.x - PORT_SIZE / 2, p.y - PORT_SIZE / 2, PORT_SIZE, PORT_SIZE);
            g2d.setColor(Color.BLACK);
            g2d.drawRect(p.x - PORT_SIZE / 2, p.y - PORT_SIZE / 2, PORT_SIZE, PORT_SIZE);
        }
        g2d.setColor(new Color(40, 100, 210));
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawRect(b.x - 2, b.y - 2, b.width + 4, b.height + 4);
    }

    protected void drawCenteredName(Graphics2D g2d, Rectangle area) {
        if (name == null || name.isEmpty()) {
            return;
        }
        FontMetrics fm = g2d.getFontMetrics();
        int padding = 5;
        int textX = area.x + (area.width - fm.stringWidth(name)) / 2;
        int textY = area.y + (area.height - fm.getHeight()) / 2 + fm.getAscent();
        g2d.setColor(labelColor);
        g2d.fillRect(textX - padding, textY - fm.getAscent() - padding,
                fm.stringWidth(name) + padding * 2, fm.getHeight() + padding * 2);
        g2d.setColor(Color.BLACK);
        g2d.drawString(name, textX, textY);
    }
}
