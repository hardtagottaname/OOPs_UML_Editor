import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.util.*;
import java.util.List;

public class CanvasPanel extends JPanel {
    // 所有線條和 Object 都在這兩個 list 裡面
    private List<Object> shapes;
    private List<Line> lines; 

    // 選擇 (Object List) 或懸浮 (Object)
    private List<Object> selectedShapes = new ArrayList<>(); 
    private Object hoverShape = null; 

    // 繪圖相關
    private boolean dragging = false;
    private Point rubberBandStart = null; // 框選起點
    private Rectangle rubberBandRect = null; // 框選矩形

    // Line 繪製相關
    private Port tempStartPort = null;
    private Point tempLineEnd = null;

    // 調整大小相關
    private Port resizingPort = null;
    private Object resizingShape = null;

    // 標籤相關
    private Map<Object, String> shapeLabels = new IdentityHashMap<>();
    private Map<Object, Color> shapeLabelColors = new IdentityHashMap<>();
    private Map<Object, Boolean> shapeLabelFlipX = new IdentityHashMap<>();
    private Map<Object, Boolean> shapeLabelFlipY = new IdentityHashMap<>();

    // port 相關
    private Map<Object, ArrayList<Port>> shapePortsMap;

    // 滑鼠位置追蹤
    private Point lastMousePoint = new Point(0, 0);

    //constructor
    public CanvasPanel() {
        shapes = new ArrayList<>();
        lines = new ArrayList<>();
        shapePortsMap = new IdentityHashMap<>();

        // 設定滑鼠事件監聽器
        MouseHandler mouseHandler = new MouseHandler();
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
        setFocusable(true);
        setRequestFocusEnabled(true);
    }

    /**
     * 添加形狀到畫布
     * @param obj 要添加的形狀物件
     */
    public void addShape(Object obj) {
        shapes.add(obj);

        // 設定預設標籤
        shapeLabels.put(obj, "Default");
        shapeLabelColors.put(obj, Color.YELLOW);
        shapeLabelFlipX.put(obj, false);
        shapeLabelFlipY.put(obj, false);

        updateShapePorts(obj);
        repaint();
    }

    /**
     * 添加線條到畫布
     * @param line 要添加的線條
     */
    public void addLine(Line line) {
        lines.add(line);
        repaint();
    }

    /**
     * 繪製形狀標籤
     * @param g2d 圖形上下文
     * @param obj 形狀物件
     */
    private void drawLabel(Graphics2D g2d, Object obj) {
        String label = shapeLabels.get(obj);
        Color labelColor = shapeLabelColors.get(obj);
        Boolean flipX = shapeLabelFlipX.get(obj);
        Boolean flipY = shapeLabelFlipY.get(obj);

        if (label != null && labelColor != null) {
            Rectangle bounds = getShapeBounds(obj);

            int lx = bounds.x + bounds.width / 2;
            int ly = bounds.y + bounds.height / 2;

            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(label);
            int textHeight = fm.getHeight();
            int padding = 4;

            // 儲存原始變換狀態
            AffineTransform originalTransform = g2d.getTransform();

            // 應用翻轉
            g2d.translate(lx, ly);
            if (flipX != null && flipX) {
                g2d.scale(-1, 1);
            }
            if (flipY != null && flipY) {
                g2d.scale(1, -1);
            }
            g2d.translate(-lx, -ly);

            // 繪製背景
            g2d.setColor(labelColor);
            g2d.fillRect(
                lx - textWidth / 2 - padding,
                ly - textHeight / 2,
                textWidth + padding * 2,
                textHeight
            );

            // 繪製文字
            g2d.setColor(Color.BLACK);
            g2d.drawString(
                label,
                lx - textWidth / 2,
                ly + fm.getAscent() / 2
            );

            // 恢復原始狀態
            g2d.setTransform(originalTransform);
        }
    }

    /**
     * 獲取形狀的邊界矩形
     * @param obj 形狀物件
     * @return 邊界矩形
     */
    private Rectangle getShapeBounds(Object obj) {
        if (obj instanceof Shape) {
            return ((Shape) obj).getBounds();
        } else if (obj instanceof MutableOval) {
            return ((MutableOval) obj).getBounds();
        } else if (obj instanceof CompositeShape) {
            return ((CompositeShape) obj).getBounds();
        }
        return new Rectangle();
    }

    /**
     * 繪製自訂線條（含箭頭）
     * @param g2d 圖形上下文
     * @param x1 起點X
     * @param y1 起點Y
     * @param x2 終點X
     * @param y2 終點Y
     * @param type 線條類型
     */
    private void drawCustomLine(Graphics2D g2d, int x1, int y1, int x2, int y2, String type) {
        // 計算線的角度
        double angle = Math.atan2(y2 - y1, x2 - x1);

        // 箭頭的大小
        int arrowSize = 10;

        // 畫線身 (從起點畫到離終點一段距離的地方，避免蓋到箭頭)
        int lineEndX = x2 - (int)(Math.cos(angle) * arrowSize);
        int lineEndY = y2 - (int)(Math.sin(angle) * arrowSize);
        g2d.drawLine(x1, y1, lineEndX, lineEndY);

        // 根據類型畫箭頭
        if ("generalization".equals(type)) {
            drawHollowArrow(g2d, x2, y2, angle, arrowSize);
        } else if ("composition".equals(type)) {
            drawSolidDiamond(g2d, x2, y2, angle, arrowSize);
        } else if ("association".equals(type)) {
            drawLineArrow(g2d, x2, y2, angle, arrowSize);
        }
    }

    /**
     * 繪製空心箭頭（一般化）
     */
    private void drawHollowArrow(Graphics2D g2d, int x, int y, double angle, int size) {
        Graphics2D g2dCopy = (Graphics2D) g2d.create();
        g2dCopy.translate(x, y);
        g2dCopy.rotate(angle);

        g2dCopy.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2dCopy.setStroke(new BasicStroke(1.5f));

        int width = 6;
        int length = 8;

        int[] xPoints = {0, -length, -length};
        int[] yPoints = {0, -width/2, width/2};

        g2dCopy.setColor(Color.BLACK);
        g2dCopy.drawPolygon(xPoints, yPoints, 3);

        g2dCopy.dispose();
    }

    /**
     * 繪製實心菱形（組合）
     */
    private void drawSolidDiamond(Graphics2D g2d, int x, int y, double angle, int size) {
        Graphics2D g2dCopy = (Graphics2D) g2d.create();
        g2dCopy.translate(x, y);
        g2dCopy.rotate(angle);

        g2dCopy.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2dCopy.setStroke(new BasicStroke(1.5f));

        int width = 12;
        int height = 18;

        int[] xPoints = {0, width/2, 0, -width/2};
        int[] yPoints = {0, height/4, height/2, height/4};

        g2dCopy.setColor(Color.BLACK);
        g2dCopy.drawPolygon(xPoints, yPoints, 4);

        g2dCopy.dispose();
    }

    /**
     * 繪製線箭頭（關聯）
     */
    private void drawLineArrow(Graphics2D g2d, int x, int y, double angle, int size) {
        Graphics2D g2dCopy = (Graphics2D) g2d.create();
        g2dCopy.translate(x, y);
        g2dCopy.rotate(angle);

        g2dCopy.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2dCopy.setStroke(new BasicStroke(1.5f));

        int width = 6;
        int length = 8;

        int x1 = 0;
        int y1 = 0;
        int x2 = -length;
        int y2 = -width / 2;
        int x3 = -length;
        int y3 = width / 2;

        g2dCopy.setColor(Color.BLACK);
        g2dCopy.drawLine(x1, y1, x2, y2);
        g2dCopy.drawLine(x1, y1, x3, y3);

        g2dCopy.dispose();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(Color.BLACK);

        // 更新所有Port位置
        for (ArrayList<Port> ports : shapePortsMap.values()) {
            for (Port p : ports) {
                p.updatePosition();
            }
        }

        // 繪製形狀
        for (Object obj : shapes) {
            boolean isShapeSelected = selectedShapes.contains(obj);
            boolean isShapeHovered = (hoverShape == obj);

            if (obj instanceof Shape) {
                Shape shape = (Shape) obj;
                drawShape(g2d, shape, isShapeSelected, isShapeHovered);
                drawLabel(g2d, obj);
            } else if (obj instanceof MutableOval) {
                MutableOval oval = (MutableOval) obj;
                drawOval(g2d, oval, isShapeSelected, isShapeHovered);
                drawLabel(g2d, obj);
            } else if (obj instanceof CompositeShape) {
                CompositeShape cs = (CompositeShape) obj;
                drawCompositeShape(g2d, cs, isShapeSelected, isShapeHovered);
            }
        }

        // 繪製框選矩形
        if ("select".equals(getCurrentMode()) && rubberBandRect != null) {
            float[] dash = {5.0f};
            g2d.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f));
            g2d.setColor(Color.BLUE);
            g2d.draw(rubberBandRect);
        }

        // 繪製線條
        for (Line line : lines) {
            if (line.getStart() != null && line.getEnd() != null) {
                drawCustomLine(g2d, line.getStart().getX(), line.getStart().getY(),
                             line.getEnd().getX(), line.getEnd().getY(), line.getType());
            }
        }

        // 繪製臨時線條
        if (tempStartPort != null && tempLineEnd != null) {
            float[] dash = {5.0f};
            g2d.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f));
            g2d.drawLine(tempStartPort.getX(), tempStartPort.getY(), tempLineEnd.x, tempLineEnd.y);
            g2d.setStroke(new BasicStroke(1.0f));
        }
    }

    /**
     * 繪製普通形狀
     */
    private void drawShape(Graphics2D g2d, Shape shape, boolean selected, boolean hovered) {
        if (selected || hovered) {
            g2d.setColor(Color.BLUE);
            g2d.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{5}, 0));
            g2d.draw(shape);

            // 顯示Ports
            ArrayList<Port> ports = shapePortsMap.get(shape);
            if (ports != null) {
                for (Port port : ports) {
                    g2d.setColor(Color.WHITE);
                    g2d.fillRect(port.getX() - 3, port.getY() - 3, 6, 6);
                    g2d.setColor(Color.BLACK);
                    g2d.drawRect(port.getX() - 3, port.getY() - 3, 6, 6);
                }
            }
        } else {
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(1.0f));
            g2d.draw(shape);
        }
    }

    /**
     * 繪製橢圓
     */
    private void drawOval(Graphics2D g2d, MutableOval oval, boolean selected, boolean hovered) {
        Shape shape = new Ellipse2D.Float(oval.getX(), oval.getY(), oval.getWidth(), oval.getHeight());

        if (selected || hovered) {
            g2d.setColor(Color.BLUE);
            g2d.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{5}, 0));
        } else {
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(1.0f));
        }
        g2d.draw(shape);

        if (selected || hovered) {
            ArrayList<Port> ports = shapePortsMap.get(oval);
            if (ports != null) {
                for (Port port : ports) {
                    g2d.setColor(Color.WHITE);
                    g2d.fillRect(port.getX() - 3, port.getY() - 3, 6, 6);
                    g2d.setColor(Color.BLACK);
                    g2d.drawRect(port.getX() - 3, port.getY() - 3, 6, 6);
                }
            }
        }
    }

    /**
     * 繪製複合形狀
     */
    private void drawCompositeShape(Graphics2D g2d, CompositeShape cs, boolean selected, boolean hovered) {
        // 先畫子物件
        for (Object child : cs.getChildren()) {
            if (child instanceof Shape) {
                g2d.setColor(Color.BLACK);
                g2d.setStroke(new BasicStroke(1.0f));
                g2d.draw((Shape) child);
            } else if (child instanceof MutableOval) {
                MutableOval oval = (MutableOval) child;
                Shape shape = new Ellipse2D.Float(oval.getX(), oval.getY(), oval.getWidth(), oval.getHeight());
                g2d.setColor(Color.BLACK);
                g2d.setStroke(new BasicStroke(1.0f));
                g2d.draw(shape);
            }
        }

        // 再畫群組框
        if (selected || hovered) {
            g2d.setColor(Color.BLUE);
            g2d.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{5}, 0));
        } else {
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(1.0f));
        }
        g2d.draw(cs.getBounds());
    }

    // 其他方法將在後續添加...

    /**
     * 獲取當前模式（需要由外部設定）
     */
    private String currentMode = "select";

    public void setCurrentMode(String mode) {
        this.currentMode = mode;
    }

    public String getCurrentMode() {
        return currentMode;
    }

    /**
     * 尋找指定位置的Port
     * @param x X座標
     * @param y Y座標
     * @return 找到的Port，如果沒有則返回null
     */
    private Port findPortAt(int x, int y) {
        // 反向遍歷，讓最上層的圖形優先被檢查
        for (int i = shapes.size() - 1; i >= 0; i--) {
            Object obj = shapes.get(i);
            ArrayList<Port> ports = shapePortsMap.get(obj);
            if (ports != null) {
                for (Port port : ports) {
                    if (port.contains(x, y)) {
                        return port;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 尋找指定位置的形狀
     * @param x X座標
     * @param y Y座標
     * @return 找到的形狀物件，如果沒有則返回null
     */
    private Object findShapeAt(int x, int y) {
        // 先找群組
        for (int i = shapes.size() - 1; i >= 0; i--) {
            Object obj = shapes.get(i);
            if (obj instanceof CompositeShape) {
                CompositeShape group = (CompositeShape) obj;
                if (group.contains(x, y)) {
                    return group;
                }
            }
        }

        // 再找一般形狀
        for (int i = shapes.size() - 1; i >= 0; i--) {
            Object obj = shapes.get(i);
            if (obj instanceof Rectangle) {
                if (((Rectangle) obj).contains(x, y)) {
                    return obj;
                }
            } else if (obj instanceof MutableOval) {
                MutableOval oval = (MutableOval) obj;
                Ellipse2D shape = new Ellipse2D.Float(oval.getX(), oval.getY(), oval.getWidth(), oval.getHeight());
                if (shape.contains(x, y)) {
                    return obj;
                }
            }
        }
        return null;
    }

    /**
     * 更新形狀的Ports
     * @param obj 形狀物件
     */
    private void updateShapePorts(Object obj) {
        ArrayList<Port> ports = shapePortsMap.get(obj);

        if (ports == null) {
            ports = new ArrayList<>();
            if (obj instanceof Rectangle) {
                for (int i = 0; i < 8; i++) {
                    ports.add(new Port(obj, i));
                }
            } else if (obj instanceof MutableOval) {
                for (int i = 0; i < 4; i++) {
                    ports.add(new Port(obj, i));
                }
            }
            shapePortsMap.put(obj, ports);
        } else {
            for (Port p : ports) {
                p.updatePosition();
            }
        }
    }

    /**
     * 將選中的形狀組成群組
     */
    public void groupSelected() {
        if (selectedShapes.size() < 2) return;

        List<Object> groupChildren = new ArrayList<>(selectedShapes);
        CompositeShape group = new CompositeShape(groupChildren);

        shapes.add(group);
        selectedShapes.clear();
        selectedShapes.add(group);

        for (Object child : groupChildren) {
            updateShapePorts(child);
        }
        repaint();
    }

    /**
     * 取消選中形狀的群組
     * 遞歸地解開所有巢狀群組，直到只剩下基本形狀
     */
    public void ungroupSelected() {
        if (selectedShapes.size() != 1) return;

        Object sel = selectedShapes.get(0);
        if (sel instanceof CompositeShape) {
            CompositeShape group = (CompositeShape) sel;

            // 移除相關的線條
            ArrayList<Line> linesToRemove = new ArrayList<>();
            for (Line line : lines) {
                Object startParent = line.getStart().getParentShape();
                Object endParent = line.getEnd().getParentShape();

                if (startParent == group || endParent == group) {
                    linesToRemove.add(line);
                    continue;
                }

                // 檢查是否連接到任何將被解開的形狀
                List<Object> allUngroupedShapes = getAllUngroupedShapes(group);
                for (Object ungroupedShape : allUngroupedShapes) {
                    if (startParent == ungroupedShape || endParent == ungroupedShape) {
                        linesToRemove.add(line);
                        break;
                    }
                }
            }
            lines.removeAll(linesToRemove);

            // 遞歸地解開群組並獲取所有基本形狀
            List<Object> ungroupedShapes = getAllUngroupedShapes(group);

            // 從shapes列表中移除群組
            shapes.remove(group);

            // 添加所有解開後的基本形狀
            shapes.addAll(ungroupedShapes);

            // 設定選中狀態
            selectedShapes.clear();
            selectedShapes.addAll(ungroupedShapes);
        }
        repaint();
    }

    /**
     * 遞歸地獲取CompositeShape中所有非群組的基本形狀
     * @param shape 要解開的形狀（可能是CompositeShape）
     * @return 所有基本形狀的列表
     */
    private List<Object> getAllUngroupedShapes(Object shape) {
        List<Object> result = new ArrayList<>();

        if (shape instanceof CompositeShape) {
            CompositeShape group = (CompositeShape) shape;
            for (Object child : group.getChildren()) {
                // 遞歸處理巢狀群組
                result.addAll(getAllUngroupedShapes(child));
            }
        } else {
            // 基本形狀，直接添加
            result.add(shape);
        }

        return result;
    }

    /**
     * 調整形狀大小
     * @param shape 要調整的形狀
     * @param port 調整的Port
     * @param mx 滑鼠X座標
     * @param my 滑鼠Y座標
     */
    private void resizeShape(Object shape, Port port, int mx, int my) {
        int minSize = 30;

        if (shape instanceof Rectangle) {
            Rectangle rect = (Rectangle) shape;
            int x1 = rect.x, y1 = rect.y;
            int x2 = rect.x + rect.width, y2 = rect.y + rect.height;

            int newLeft = x1, newTop = y1, newRight = x2, newBottom = y2;

            switch (port.getType()) {
                case 0: newLeft = mx; newTop = my; break; // TL
                case 1: newTop = my; break; // T
                case 2: newTop = my; newRight = mx; break; // TR
                case 3: newRight = mx; break; // R
                case 4: newRight = mx; newBottom = my; break; // BR
                case 5: newBottom = my; break; // B
                case 6: newLeft = mx; newBottom = my; break; // BL
                case 7: newLeft = mx; break; // L
            }

            boolean flipX = newRight < newLeft;
            boolean flipY = newBottom < newTop;

            if (flipX) {
                int temp = newLeft; newLeft = newRight; newRight = temp;
                shapeLabelFlipX.put(shape, !shapeLabelFlipX.get(shape));
            }
            if (flipY) {
                int temp = newTop; newTop = newBottom; newBottom = temp;
                shapeLabelFlipY.put(shape, !shapeLabelFlipY.get(shape));
            }

            int newX = Math.min(newLeft, newRight);
            int newY = Math.min(newTop, newBottom);
            int newW = Math.abs(newRight - newLeft);
            int newH = Math.abs(newBottom - newTop);

            if (newW < minSize) newW = minSize;
            if (newH < minSize) newH = minSize;

            rect.x = newX;
            rect.y = newY;
            rect.width = newW;
            rect.height = newH;
            updateShapePorts(rect);

        } else if (shape instanceof MutableOval) {
            MutableOval oval = (MutableOval) shape;
            int x1 = oval.getX(), y1 = oval.getY();
            int x2 = oval.getX() + oval.getWidth(), y2 = oval.getY() + oval.getHeight();

            int newLeft = x1, newTop = y1, newRight = x2, newBottom = y2;

            switch (port.getType()) {
                case 0: newTop = my; break; // Top
                case 1: newRight = mx; break; // Right
                case 2: newBottom = my; break; // Bottom
                case 3: newLeft = mx; break; // Left
            }

            boolean flipX = newRight < newLeft;
            boolean flipY = newBottom < newTop;

            if (flipX) {
                int temp = newLeft; newLeft = newRight; newRight = temp;
                shapeLabelFlipX.put(shape, !shapeLabelFlipX.get(shape));
            }
            if (flipY) {
                int temp = newTop; newTop = newBottom; newBottom = temp;
                shapeLabelFlipY.put(shape, !shapeLabelFlipY.get(shape));
            }

            int newX = Math.min(newLeft, newRight);
            int newY = Math.min(newTop, newBottom);
            int newW = Math.abs(newRight - newLeft);
            int newH = Math.abs(newBottom - newTop);

            if (newW < minSize) newW = minSize;
            if (newH < minSize) newH = minSize;

            oval.setX(newX);
            oval.setY(newY);
            oval.setWidth(newW);
            oval.setHeight(newH);
            updateShapePorts(oval);
        }
    }

    /**
     * 移動複合形狀
     * @param group 群組
     * @param deltaX X方向位移
     * @param deltaY Y方向位移
     */
    private void moveCompositeShape(CompositeShape group, int deltaX, int deltaY) {
        group.x += deltaX;
        group.y += deltaY;

        for (Object obj : group.getChildren()) {
            if (obj instanceof Rectangle) {
                Rectangle rect = (Rectangle) obj;
                rect.x += deltaX;
                rect.y += deltaY;
                updateShapePorts(rect);
            } else if (obj instanceof MutableOval) {
                MutableOval oval = (MutableOval) obj;
                oval.setX(oval.getX() + deltaX);
                oval.setY(oval.getY() + deltaY);
                updateShapePorts(oval);
            }
        }
        updateShapePorts(group);
        repaint();
    }

    // Getter 方法
    public List<Object> getSelectedShapes() { return selectedShapes; }
    public Map<Object, String> getShapeLabels() { return shapeLabels; }
    public Map<Object, Color> getShapeLabelColors() { return shapeLabelColors; }

    /**
     * MouseHandler 內部類別：處理滑鼠事件
     */
    private class MouseHandler extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            if ("association".equals(currentMode) || "generalization".equals(currentMode) || "composition".equals(currentMode)) {
                Port port = findPortAt(e.getX(), e.getY());
                if (port != null) {
                    tempStartPort = port;
                    System.out.println("Start port set at: (" + port.getX() + ", " + port.getY() + ")");
                }
            } else if ("select".equals(currentMode)) {
                Point p = e.getPoint();
                Object targetShape = findShapeAt(p.x, p.y);
                Port port = findPortAt(e.getX(), e.getY());

                if (port != null) {
                    if (!(port.getParentShape() instanceof CompositeShape)) {
                        resizingPort = port;
                        resizingShape = port.getParentShape();
                        return;
                    }
                }

                if (targetShape == null) {
                    for (int i = shapes.size() - 1; i >= 0; i--) {
                        Object shape = shapes.get(i);
                        if (!(shape instanceof CompositeShape)) {
                            if (shape instanceof Shape && ((Shape) shape).contains(p.x, p.y)) {
                                targetShape = shape;
                                break;
                            } else if (shape instanceof MutableOval && ((MutableOval) shape).contains(p.x, p.y)) {
                                targetShape = shape;
                                break;
                            }
                        }
                    }
                }

                boolean isCtrlPressed = (e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0;

                if (targetShape != null) {
                    if (isCtrlPressed) {
                        if (selectedShapes.contains(targetShape)) {
                            selectedShapes.remove(targetShape);
                        } else {
                            if (targetShape instanceof CompositeShape) {
                                selectedShapes.clear();
                            }
                            selectedShapes.add(targetShape);
                        }
                    } else {
                        selectedShapes.clear();
                        selectedShapes.add(targetShape);
                    }
                    rubberBandStart = null;
                    rubberBandRect = null;
                    System.out.println("Object selected: " + targetShape.getClass().getSimpleName());
                } else {
                    selectedShapes.clear();
                    rubberBandStart = p;
                    rubberBandRect = null;
                    System.out.println("Start Rubber Band Selection");
                }
                dragging = false;
                lastMousePoint = e.getPoint();
                repaint();
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            Point currentPoint = e.getPoint();

            if (resizingPort != null && resizingShape != null) {
                resizeShape(resizingShape, resizingPort, e.getX(), e.getY());
                repaint();
                return;
            }

            if (tempStartPort != null) {
                tempLineEnd = currentPoint;
                repaint();
                return;
            }

            if ("select".equals(currentMode) && rubberBandStart == null && !selectedShapes.isEmpty()) {
                int deltaX = currentPoint.x - lastMousePoint.x;
                int deltaY = currentPoint.y - lastMousePoint.y;

                if (Math.abs(deltaX) > 0 || Math.abs(deltaY) > 0) {
                    for (Object shape : selectedShapes) {
                        if (shape instanceof CompositeShape) {
                            moveCompositeShape((CompositeShape) shape, deltaX, deltaY);
                        } else {
                            if (shape instanceof Rectangle) {
                                Rectangle rect = (Rectangle) shape;
                                rect.x += deltaX;
                                rect.y += deltaY;
                            } else if (shape instanceof MutableOval) {
                                MutableOval oval = (MutableOval) shape;
                                oval.setX(oval.getX() + deltaX);
                                oval.setY(oval.getY() + deltaY);
                                updateShapePorts(oval);
                            }
                            updateShapePorts(shape);
                        }
                    }
                    lastMousePoint = currentPoint;
                    repaint();
                }
                return;
            }

            if ("select".equals(currentMode) && rubberBandStart != null) {
                Point start = rubberBandStart;
                int x = Math.min(start.x, currentPoint.x);
                int y = Math.min(start.y, currentPoint.y);
                int width = Math.abs(start.x - currentPoint.x);
                int height = Math.abs(start.y - currentPoint.y);
                rubberBandRect = new Rectangle(x, y, width, height);
                repaint();
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            resizingPort = null;
            resizingShape = null;
            dragging = false;

            if (tempStartPort != null) {
                Port endPort = findPortAt(e.getX(), e.getY());
                if (endPort != null && endPort.getParentShape() != tempStartPort.getParentShape()) {
                    addLine(new Line(tempStartPort, endPort, currentMode));
                    System.out.println("Line created between different shapes.");
                } else {
                    System.out.println("Line creation cancelled (invalid end point or same shape).");
                }
                tempStartPort = null;
                tempLineEnd = null;
                repaint();
            } else if ("select".equals(currentMode) && rubberBandStart != null) {
                Rectangle finalRect = rubberBandRect;
                if (finalRect != null && finalRect.width > 5 && finalRect.height > 5) {
                    for (Object shape : shapes) {
                        Rectangle bounds = getShapeBounds(shape);
                        if (bounds != null && finalRect.intersects(bounds)) {
                            if (!selectedShapes.contains(shape)) {
                                selectedShapes.add(shape);
                            }
                        }
                    }
                } else {
                    selectedShapes.clear();
                }
                rubberBandStart = null;
                rubberBandRect = null;
                repaint();
                System.out.println("Selected " + selectedShapes.size() + " objects");
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            if (!"select".equals(currentMode)) return;

            Point p = e.getPoint();
            Object hitShape = findShapeAt(p.x, p.y);

            if (hoverShape != hitShape) {
                hoverShape = hitShape;
                repaint();
            }

            if (hitShape != null) {
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            } else {
                setCursor(Cursor.getDefaultCursor());
            }
        }
    }
}