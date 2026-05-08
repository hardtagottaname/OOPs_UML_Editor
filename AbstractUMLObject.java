import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 基本 UML 圖形的抽象父類別。
 *
 * ClassObject 與 UseCaseObject 共用位置、大小、標籤、連接點、移動與縮放邏輯；
 * 子類別只需要負責自己的外觀繪製與點擊範圍判斷。
 */
public abstract class AbstractUMLObject implements UMLObject {
    // Port 在畫面上繪製成正方形控制點時的邊長。
    protected static final int PORT_SIZE = 8;

    // 圖形外框的位置與大小。
    protected int x;
    protected int y;
    protected int width;
    protected int height;
    // 顯示在圖形中央的標籤文字與標籤背景色。
    protected String name;
    protected Color labelColor = new Color(235, 235, 235);
    // 顯示深度，數字越小代表越接近畫面上層。
    protected int depth;
    // 圖形周圍的連接 / 縮放控制點。
    private final List<Port> ports = new ArrayList<>();

    /**
     * 建立預設有四個 Port 的基本圖形。
     */
    protected AbstractUMLObject(int x, int y, int width, int height, String name) {
        this(x, y, width, height, name, 4);
    }

    /**
     * 建立指定位置、大小、標籤與 Port 數量的基本圖形。
     */
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

    /**
     * 取得圖形目前的矩形外框。
     */
    @Override
    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }

    /**
     * 移動圖形。Port 位置不需要另外更新，因為 Port 每次讀取時會依外框重算。
     */
    @Override
    public void move(int dx, int dy) {
        x += dx;
        y += dy;
    }

    /** 取得標籤文字。 */
    @Override
    public String getName() {
        return name;
    }

    /** 設定標籤文字；null 會被轉成空字串，避免繪圖時發生錯誤。 */
    @Override
    public void setName(String name) {
        this.name = name == null ? "" : name;
    }

    /** 取得標籤背景色。 */
    @Override
    public Color getLabelColor() {
        return labelColor;
    }

    /** 設定標籤背景色；null 會回到預設淺灰色。 */
    @Override
    public void setLabelColor(Color color) {
        labelColor = color == null ? new Color(235, 235, 235) : color;
    }

    /** 回傳唯讀 Port 清單，避免外部直接改動內部資料。 */
    @Override
    public List<Port> getPorts() {
        return Collections.unmodifiableList(ports);
    }

    /**
     * 從所有 Port 中找出距離指定點最近的一個。
     */
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

    /**
     * 以目前外框為基準調整大小。
     */
    @Override
    public void resize(Port port, Point anchor, Point draggedPoint, int minSize) {
        resizeFromBounds(getBounds(), port, anchor, draggedPoint, minSize);
    }

    /**
     * 從指定的原始外框開始縮放。
     *
     * CanvasPanel 在拖曳開始時會記住原始外框，之後每次拖曳都用同一個基準計算，
     * 這樣可以避免反覆累加造成尺寸漂移。四個 Port 只改單邊，八個 Port 則可改邊
     * 或角，並用 anchor 固定對側位置。
     */
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

    /** 取得顯示深度。 */
    @Override
    public int getDepth() {
        return depth;
    }

    /** 設定顯示深度。 */
    @Override
    public void setDepth(int depth) {
        this.depth = depth;
    }

    /**
     * 繪製選取外框與所有 Port 控制點。
     */
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

    /**
     * 在指定區域中央繪製標籤。
     *
     * 標籤會先畫一塊背景色矩形，再畫文字，讓文字在白色圖形內更清楚。
     */
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
