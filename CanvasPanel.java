import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CanvasPanel extends JPanel {
    private static final int MIN_SIZE = 30;

    private final List<UMLObject> objects = new ArrayList<>();
    private final List<Line> lines = new ArrayList<>();
    private final List<UMLObject> selectedObjects = new ArrayList<>();

    private String currentMode = "select";
    private UMLObject hoverObject;
    private Point lastMousePoint;
    private Point rubberBandStart;
    private Rectangle rubberBandRect;
    private Port linkStartPort;
    private Point linkPreviewEnd;
    private UMLObject resizeObject;
    private Port resizePort;
    private Point resizeAnchor;
    private Rectangle resizeStartBounds;
    private boolean movingSelection;

    public CanvasPanel() {
        MouseHandler mouseHandler = new MouseHandler();
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
        setFocusable(true);
        setRequestFocusEnabled(true);
    }

    public void createShapeAt(String mode, Point point) {
        int x = point.x - 55;
        int y = point.y - 35;
        UMLObject object;
        if ("oval".equals(mode)) {
            object = new UseCaseObject(x, y);
        } else {
            object = new ClassObject(x, y);
        }
        object.setDepth(nextBackDepth());
        objects.add(object);
        repaint();
    }

    public void setCurrentMode(String mode) {
        currentMode = mode;
        clearTemporaryState();
        repaint();
    }

    public String getCurrentMode() {
        return currentMode;
    }

    public List<UMLObject> getSelectedShapes() {
        return selectedObjects;
    }

    public void groupSelected() {
        if (!"select".equals(currentMode) || selectedObjects.size() < 2) {
            return;
        }

        List<UMLObject> children = new ArrayList<>();
        for (UMLObject object : objects) {
            if (selectedObjects.contains(object)) {
                children.add(object);
            }
        }
        objects.removeAll(children);
        CompositeShape group = new CompositeShape(children);
        objects.add(group);
        selectedObjects.clear();
        selectedObjects.add(group);
        bringToFront(group);
        repaint();
    }

    public void ungroupSelected() {
        if (!"select".equals(currentMode) || selectedObjects.size() != 1) {
            return;
        }

        UMLObject selected = selectedObjects.get(0);
        if (!(selected instanceof CompositeShape)) {
            return;
        }

        CompositeShape group = (CompositeShape) selected;
        objects.remove(group);
        int insertIndex = Math.max(0, objects.size());
        objects.addAll(insertIndex, group.getChildren());
        selectedObjects.clear();
        selectedObjects.addAll(group.getChildren());
        normalizeDepths();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (UMLObject object : objects) {
            object.draw(g2d);
        }

        for (Line line : lines) {
            drawLine(g2d, line);
        }

        for (UMLObject object : objects) {
            if (object == hoverObject || selectedObjects.contains(object)) {
                drawSelection(g2d, object);
            }
        }

        if (rubberBandRect != null) {
            g2d.setColor(new Color(40, 100, 210));
            g2d.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    10.0f, new float[]{5.0f}, 0.0f));
            g2d.draw(rubberBandRect);
        }

        if (linkStartPort != null && linkPreviewEnd != null) {
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    10.0f, new float[]{5.0f}, 0.0f));
            g2d.drawLine(linkStartPort.getX(), linkStartPort.getY(), linkPreviewEnd.x, linkPreviewEnd.y);
        }

        g2d.dispose();
    }

    private void drawSelection(Graphics2D g2d, UMLObject object) {
        if (object instanceof CompositeShape) {
            ((CompositeShape) object).drawSelection(g2d);
        } else if (object instanceof AbstractUMLObject) {
            ((AbstractUMLObject) object).drawSelection(g2d);
        }
    }

    private void drawLine(Graphics2D g2d, Line line) {
        Point start = line.getStart().getLocation();
        Point end = line.getEnd().getLocation();
        double angle = Math.atan2(end.y - start.y, end.x - start.x);
        int arrowSize = 12;
        int bodyEndX = end.x;
        int bodyEndY = end.y;

        if (!"association".equals(line.getType())) {
            bodyEndX = end.x - (int) (Math.cos(angle) * arrowSize);
            bodyEndY = end.y - (int) (Math.sin(angle) * arrowSize);
        }

        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawLine(start.x, start.y, bodyEndX, bodyEndY);

        if ("generalization".equals(line.getType())) {
            drawTriangle(g2d, end, angle, arrowSize, false);
        } else if ("composition".equals(line.getType())) {
            drawDiamond(g2d, end, angle, arrowSize);
        } else {
            drawAssociationArrow(g2d, end, angle, arrowSize);
        }
    }

    private void drawTriangle(Graphics2D g2d, Point end, double angle, int size, boolean filled) {
        Graphics2D copy = (Graphics2D) g2d.create();
        copy.translate(end.x, end.y);
        copy.rotate(angle);
        Polygon polygon = new Polygon(new int[]{0, -size, -size}, new int[]{0, -size, size}, 3);
        copy.setColor(Color.WHITE);
        if (filled) {
            copy.fillPolygon(polygon);
        }
        copy.setColor(Color.BLACK);
        copy.drawPolygon(polygon);
        copy.dispose();
    }

    private void drawDiamond(Graphics2D g2d, Point end, double angle, int size) {
        Graphics2D copy = (Graphics2D) g2d.create();
        copy.translate(end.x, end.y);
        copy.rotate(angle);
        Polygon polygon = new Polygon(new int[]{0, -size, -size * 2, -size},
                new int[]{0, -size / 2, 0, size / 2}, 4);
        copy.setColor(Color.BLACK);
        copy.fillPolygon(polygon);
        copy.dispose();
    }

    private void drawAssociationArrow(Graphics2D g2d, Point end, double angle, int size) {
        Graphics2D copy = (Graphics2D) g2d.create();
        copy.translate(end.x, end.y);
        copy.rotate(angle);
        copy.drawLine(0, 0, -size, -size);
        copy.drawLine(0, 0, -size, size);
        copy.dispose();
    }

    private Port findPortAt(Point point) {
        for (int i = objects.size() - 1; i >= 0; i--) {
            UMLObject object = objects.get(i);
            if (object instanceof CompositeShape) {
                continue;
            }
            for (Port port : object.getPorts()) {
                if (port.contains(point.x, point.y)) {
                    return port;
                }
            }
        }
        return null;
    }

    private UMLObject findObjectAt(Point point) {
        for (int i = objects.size() - 1; i >= 0; i--) {
            UMLObject object = objects.get(i);
            if (object.contains(point)) {
                return object;
            }
        }
        return null;
    }

    private void bringToFront(UMLObject object) {
        objects.remove(object);
        objects.add(object);
        object.setDepth(0);
        normalizeDepths();
    }

    private int nextBackDepth() {
        return Math.min(99, objects.size());
    }

    private void normalizeDepths() {
        for (int i = objects.size() - 1, depth = 0; i >= 0; i--, depth++) {
            objects.get(i).setDepth(Math.min(99, depth));
        }
    }

    private Point getOppositeAnchor(UMLObject object, Port port) {
        Rectangle b = object.getBounds();
        int cx = b.x + b.width / 2;
        int cy = b.y + b.height / 2;
        if (object.getPorts().size() == 8) {
            switch (port.getType()) {
                case 0:
                    return new Point(b.x + b.width, b.y + b.height);
                case 1:
                    return new Point(cx, b.y + b.height);
                case 2:
                    return new Point(b.x, b.y + b.height);
                case 3:
                    return new Point(b.x, cy);
                case 4:
                    return new Point(b.x, b.y);
                case 5:
                    return new Point(cx, b.y);
                case 6:
                    return new Point(b.x + b.width, b.y);
                case 7:
                    return new Point(b.x + b.width, cy);
                default:
                    return new Point(cx, cy);
            }
        }
        switch (port.getType()) {
            case 0:
                return new Point(cx, b.y + b.height);
            case 1:
                return new Point(b.x, cy);
            case 2:
                return new Point(cx, b.y);
            case 3:
                return new Point(b.x + b.width, cy);
            default:
                return new Point(cx, cy);
        }
    }

    private void selectSingle(UMLObject object) {
        selectedObjects.clear();
        selectedObjects.add(object);
        bringToFront(object);
    }

    private void clearTemporaryState() {
        hoverObject = null;
        rubberBandStart = null;
        rubberBandRect = null;
        linkStartPort = null;
        linkPreviewEnd = null;
        resizeObject = null;
        resizePort = null;
        resizeAnchor = null;
        resizeStartBounds = null;
        movingSelection = false;
    }

    private class MouseHandler extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            requestFocusInWindow();
            Point point = e.getPoint();
            lastMousePoint = point;

            if (isLineMode()) {
                linkStartPort = findPortAt(point);
                linkPreviewEnd = point;
                return;
            }

            if (!"select".equals(currentMode)) {
                return;
            }

            Port port = findPortAt(point);
            if (port != null) {
                resizePort = port;
                resizeObject = port.getParentShape();
                resizeAnchor = getOppositeAnchor(resizeObject, port);
                resizeStartBounds = resizeObject.getBounds();
                if (!selectedObjects.contains(resizeObject)) {
                    selectSingle(resizeObject);
                }
                repaint();
                return;
            }

            UMLObject hit = findObjectAt(point);
            if (hit != null) {
                selectSingle(hit);
                movingSelection = true;
            } else {
                selectedObjects.clear();
                rubberBandStart = point;
                rubberBandRect = null;
            }
            repaint();
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            Point point = e.getPoint();
            if (linkStartPort != null) {
                linkPreviewEnd = point;
                repaint();
                return;
            }

            if (resizeObject instanceof AbstractUMLObject && resizePort != null && resizeStartBounds != null) {
                ((AbstractUMLObject) resizeObject).resizeFromBounds(
                        resizeStartBounds, resizePort, resizeAnchor, point, MIN_SIZE);
                repaint();
                return;
            }

            if (movingSelection) {
                int dx = point.x - lastMousePoint.x;
                int dy = point.y - lastMousePoint.y;
                for (UMLObject object : selectedObjects) {
                    object.move(dx, dy);
                }
                lastMousePoint = point;
                repaint();
                return;
            }

            if (rubberBandStart != null) {
                int x = Math.min(rubberBandStart.x, point.x);
                int y = Math.min(rubberBandStart.y, point.y);
                int width = Math.abs(rubberBandStart.x - point.x);
                int height = Math.abs(rubberBandStart.y - point.y);
                rubberBandRect = new Rectangle(x, y, width, height);
                repaint();
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            Point point = e.getPoint();
            if (linkStartPort != null) {
                Port endPort = findPortAt(point);
                if (endPort != null && endPort.getParentShape() != linkStartPort.getParentShape()) {
                    lines.add(new Line(linkStartPort, endPort, currentMode));
                }
                linkStartPort = null;
                linkPreviewEnd = null;
                repaint();
                return;
            }

            if (rubberBandStart != null) {
                selectedObjects.clear();
                if (rubberBandRect != null) {
                    List<UMLObject> ordered = new ArrayList<>(objects);
                    ordered.sort(Comparator.comparingInt(UMLObject::getDepth).reversed());
                    for (UMLObject object : ordered) {
                        if (rubberBandRect.contains(object.getBounds())) {
                            selectedObjects.add(object);
                        }
                    }
                }
                rubberBandStart = null;
                rubberBandRect = null;
            }

            resizeObject = null;
            resizePort = null;
            resizeAnchor = null;
            resizeStartBounds = null;
            movingSelection = false;
            repaint();
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            if (!"select".equals(currentMode)) {
                return;
            }
            UMLObject hit = findObjectAt(e.getPoint());
            if (hoverObject != hit) {
                hoverObject = hit;
                repaint();
            }
            setCursor(hit == null ? Cursor.getDefaultCursor() : Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        private boolean isLineMode() {
            return "association".equals(currentMode)
                    || "generalization".equals(currentMode)
                    || "composition".equals(currentMode);
        }
    }
}
