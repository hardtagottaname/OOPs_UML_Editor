import java.awt.Point;
import java.awt.Rectangle;

public class Port {
    private static final int HIT_SIZE = 8;

    private final UMLObject parentShape;
    private final int type;
    private int x;
    private int y;

    public Port(UMLObject parentShape, int type) {
        this.parentShape = parentShape;
        this.type = type;
    }

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

    public boolean contains(int mx, int my) {
        updatePosition();
        return Math.abs(mx - x) <= HIT_SIZE && Math.abs(my - y) <= HIT_SIZE;
    }

    public Point getLocation() {
        updatePosition();
        return new Point(x, y);
    }

    public int getX() {
        updatePosition();
        return x;
    }

    public int getY() {
        updatePosition();
        return y;
    }

    public UMLObject getParentShape() {
        return parentShape;
    }

    public int getType() {
        return type;
    }
}
