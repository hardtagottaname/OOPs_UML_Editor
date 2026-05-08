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

/**
 * UML 編輯器的主要畫布。
 *
 * 這個類別管理所有圖形、連線、選取狀態與滑鼠互動，包括建立圖形、拖曳移動、
 * 框選、縮放、群組 / 解散群組，以及不同 UML 線段的繪製。
 */
public class CanvasPanel extends JPanel {
    // 圖形縮放時允許的最小寬高。
    private static final int MIN_SIZE = 30;

    // 目前畫布上的頂層 UML 物件；群組會以 CompositeShape 形式存在這份清單中。
    private final List<UMLObject> objects = new ArrayList<>();
    // 物件之間的 UML 連線。
    private final List<Line> lines = new ArrayList<>();
    // 目前被選取的物件。
    private final List<UMLObject> selectedObjects = new ArrayList<>();

    // 當前工具模式，對應左側按鈕文字。
    private String currentMode = "select";
    // 滑鼠目前 hover 到的物件，用於顯示選取提示。
    private UMLObject hoverObject;
    // 上一次滑鼠位置，用於計算拖曳位移。
    private Point lastMousePoint;
    // 框選起點與目前框選矩形。
    private Point rubberBandStart;
    private Rectangle rubberBandRect;
    // 建立連線時的起點 Port 與目前預覽線終點。
    private Port linkStartPort;
    private Point linkPreviewEnd;
    // 縮放圖形時的目標物件、被拖曳 Port、固定錨點與拖曳開始時外框。
    private UMLObject resizeObject;
    private Port resizePort;
    private Point resizeAnchor;
    private Rectangle resizeStartBounds;
    // 是否正在移動目前選取的物件。
    private boolean movingSelection;

    public CanvasPanel() {
        MouseHandler mouseHandler = new MouseHandler();
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
        setFocusable(true);
        setRequestFocusEnabled(true);
    }

    /**
     * 在指定畫布座標建立矩形類別物件或橢圓使用案例物件。
     */
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

    /**
     * 切換畫布目前工具模式，並清掉舊模式留下的暫時狀態。
     */
    public void setCurrentMode(String mode) {
        currentMode = mode;
        clearTemporaryState();
        repaint();
    }

    public String getCurrentMode() {
        return currentMode;
    }

    /**
     * 回傳目前選取的圖形清單，供主視窗選單功能使用。
     */
    public List<UMLObject> getSelectedShapes() {
        return selectedObjects;
    }

    /**
     * 將多個已選取物件包成 CompositeShape。
     *
     * 群組後畫布頂層只保留一個群組物件；群組內的子物件仍保留原本外觀與座標。
     */
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

    /**
     * 將目前選取的 CompositeShape 拆回多個基本物件。
     */
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

    /**
     * Swing 的繪圖入口。
     *
     * 繪製順序是：物件、連線、選取 / hover 外框、框選矩形、連線預覽。
     */
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

    /**
     * 依物件型別繪製不同選取外觀。
     */
    private void drawSelection(Graphics2D g2d, UMLObject object) {
        if (object instanceof CompositeShape) {
            ((CompositeShape) object).drawSelection(g2d);
        } else if (object instanceof AbstractUMLObject) {
            ((AbstractUMLObject) object).drawSelection(g2d);
        }
    }

    /**
     * 繪製 UML 連線本體與端點符號。
     */
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

    /** 繪製 generalization 的空心三角形箭頭。 */
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

    /** 繪製 composition 的黑色菱形端點。 */
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

    /** 繪製 association 的 V 型箭頭。 */
    private void drawAssociationArrow(Graphics2D g2d, Point end, double angle, int size) {
        Graphics2D copy = (Graphics2D) g2d.create();
        copy.translate(end.x, end.y);
        copy.rotate(angle);
        copy.drawLine(0, 0, -size, -size);
        copy.drawLine(0, 0, -size, size);
        copy.dispose();
    }

    /**
     * 從最上層物件往下尋找滑鼠點擊到的 Port。
     *
     * 群組本身沒有 Port，因此連線與縮放只作用在基本圖形上。
     */
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

    /**
     * 從最上層物件往下尋找滑鼠點擊到的物件。
     */
    private UMLObject findObjectAt(Point point) {
        for (int i = objects.size() - 1; i >= 0; i--) {
            UMLObject object = objects.get(i);
            if (object.contains(point)) {
                return object;
            }
        }
        return null;
    }

    /**
     * 將物件移到清單最後，讓它在繪製與點選上都位於最上層。
     */
    private void bringToFront(UMLObject object) {
        objects.remove(object);
        objects.add(object);
        object.setDepth(0);
        normalizeDepths();
    }

    /**
     * 新物件預設放在較後方的深度值。
     */
    private int nextBackDepth() {
        return Math.min(99, objects.size());
    }

    /**
     * 依目前 objects 清單順序重新編排深度。
     */
    private void normalizeDepths() {
        for (int i = objects.size() - 1, depth = 0; i >= 0; i--, depth++) {
            objects.get(i).setDepth(Math.min(99, depth));
        }
    }

    /**
     * 找出縮放時應固定不動的對側錨點。
     *
     * 八個 Port 的矩形可以拖角或邊；四個 Port 的橢圓只拖上、右、下、左四邊。
     */
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

    /**
     * 清除原本選取，改為單選指定物件，並把它帶到最上層。
     */
    private void selectSingle(UMLObject object) {
        selectedObjects.clear();
        selectedObjects.add(object);
        bringToFront(object);
    }

    /**
     * 清除模式切換時不該保留的暫時滑鼠互動狀態。
     */
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

    /**
     * 集中處理畫布上的滑鼠互動。
     */
    private class MouseHandler extends MouseAdapter {
        /**
         * 按下滑鼠時依目前模式決定開始連線、縮放、移動、單選或框選。
         */
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

        /**
         * 拖曳滑鼠時更新連線預覽、縮放、移動或框選矩形。
         */
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

        /**
         * 放開滑鼠時完成連線、完成框選，並清理拖曳狀態。
         */
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

        /**
         * 滑鼠移動時更新 hover 物件與游標樣式。
         */
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

        /**
         * 判斷目前工具是否為建立 UML 連線的模式。
         */
        private boolean isLineMode() {
            return "association".equals(currentMode)
                    || "generalization".equals(currentMode)
                    || "composition".equals(currentMode);
        }
    }
}
