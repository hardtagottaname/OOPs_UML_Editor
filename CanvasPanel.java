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

    // 標籤相關 (比記憶體位置是否相同)
    private Map<Object, String> shapeLabels = new IdentityHashMap<>();
    private Map<Object, Color> shapeLabelColors = new IdentityHashMap<>();
    private Map<Object, Boolean> shapeLabelFlipX = new IdentityHashMap<>();
    private Map<Object, Boolean> shapeLabelFlipY = new IdentityHashMap<>();

    // port 相關
    private Map<Object, ArrayList<Port>> shapePortsMap;

    // 滑鼠位置追蹤
    private Point lastMousePoint = new Point(0, 0);

    // 將目前的 mode 設為 select    
    private String currentMode = "select";

    //constructor
    public CanvasPanel() {
        shapes = new ArrayList<>();
        lines = new ArrayList<>();
        shapePortsMap = new IdentityHashMap<>();

        // 設定滑鼠事件監聽器
        MouseHandler mouseHandler = new MouseHandler(); // 負責處理滑鼠的按下、拖曳、放開、移動等等事件
        addMouseListener(mouseHandler); // 讓 CcanvasPanel 監聽滑鼠的按下、放開、點擊、進入、離開等等事件
        addMouseMotionListener(mouseHandler); // 讓 CanvasPanel 監聽滑鼠的郭義和移動等等事件
        
        // 設定可成為鍵盤事件焦點
        setFocusable(true); // 允許成為鍵盤焦點
        setRequestFocusEnabled(true); // 允許請求鍵盤焦點
    }

    // 新建一個 rect 或 oval
    public void addShape(Object obj) {
        shapes.add(obj);

        // label 預設空字串，顏色與背景一樣
        shapeLabels.put(obj, "");
        shapeLabelColors.put(obj, Color.LIGHT_GRAY);
        
        // 標籤翻轉
        // shapeLabelFlipX.put(obj, false);
        // shapeLabelFlipY.put(obj, false);

        updateShapePorts(obj); // 將新增的物件建立 ports
        repaint(); // 充新繪製圖形
    }

    // 將建立好的 line 新增進去 lines list
    public void addLine(Line line) {
        lines.add(line);
        repaint();
    }

    // 繪製 label
    private void drawLabel(Graphics2D g2d, Object obj) {
        String label = shapeLabels.get(obj);
        Color labelColor = shapeLabelColors.get(obj);
        // Boolean flipX = shapeLabelFlipX.get(obj);
        // Boolean flipY = shapeLabelFlipY.get(obj);

        if (label != null && labelColor != null) {
            Rectangle bounds = getShapeBounds(obj);

            int lx = bounds.x + bounds.width / 2;
            int ly = bounds.y + bounds.height / 2;

            // 管理字串的高及寬及 padding
            FontMetrics fm = g2d.getFontMetrics(); 
            int textWidth = fm.stringWidth(label);
            int textHeight = fm.getHeight();
            int padding = 4;

            // 儲存原始變換狀態
            // AffineTransform originalTransform = g2d.getTransform();

            // 應用翻轉
            // g2d.translate(lx, ly); // 暫時將座標原點移至 label 中心
            // // if (flipX != null && flipX) {
            // //     g2d.scale(-1, 1);
            // // }
            // // if (flipY != null && flipY) {
            // //     g2d.scale(1, -1);
            // // }
            // g2d.translate(-lx, -ly); // 將座標中心移回來

            // 繪製 label 背景
            g2d.setColor(labelColor);
            g2d.fillRect(
                lx - textWidth / 2 - padding,
                ly - textHeight / 2 - padding,
                textWidth + padding * 2,
                textHeight + padding * 2
            );

            // 繪製 label 文字
            g2d.setColor(Color.BLACK);
            g2d.drawString(
                label,
                lx - textWidth / 2,
                ly + fm.getAscent() / 2
            ); // drawString(要寫的字串, 最左下的 x 座標, 最左下的 y 座標)

            // 恢復原始狀態
            // g2d.setTransform(originalTransform);
        }
    }

    // 取得物件的邊界矩形的 x, y, width, height
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

    // 繪製三種不同的 line (generalization, composit)ion, association)
    private void drawCustomLine(Graphics2D g2d, int x1, int y1, int x2, int y2, String type) {
        double angle = Math.atan2(y2 - y1, x2 - x1); // 計算線的角度

        int arrowSize = 10; // 箭頭的大小

        // 因為箭頭的 arrow 也會占地方，所以要預留一些空間給 arrow
        int lineEndX = x2 - (int)(Math.cos(angle) * arrowSize);
        int lineEndY = y2 - (int)(Math.sin(angle) * arrowSize);
        if ("association".equals(type)) {
            g2d.drawLine(x1, y1, x2, y2); // association 的線段長度是 (x1, y1) 到 (x2, y2)，箭頭會畫在 (x2, y2) 的位置
        } else {
            g2d.drawLine(x1, y1, lineEndX, lineEndY); // 線段的真正長度是 (x1, y1) 到 (lineEndX, lineEndY)
        }
        
        // 根據 type 畫箭頭 (尖角、三角形、鑽石型)
        if ("generalization".equals(type)) {
            drawHollowArrow(g2d, x2, y2, angle, arrowSize);
        } else if ("composition".equals(type)) {
            drawSolidDiamond(g2d, x2, y2, angle, arrowSize);
        } else if ("association".equals(type)) {
            drawLineArrow(g2d, x2, y2, angle, arrowSize);
        }
    }

    // 三角形 (generalization)
    // x 和 y 是 arrow 的尖端
    private void drawHollowArrow(Graphics2D g2d, int x, int y, double angle, int size) {
        Graphics2D g2dCopy = (Graphics2D) g2d.create(); // 複製一個新的 g2d 狀態，之後對 g2dCopy 的變換不會影響到原本的 g2d
        g2dCopy.translate(x, y);
        g2dCopy.rotate(angle);

        // 開啟抗鋸齒，讓線條變更平滑
        g2dCopy.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); // setRenderingHint(要設定的渲染提示行為(要控制抗鋸齒), 設定提示行為的值(開啟抗鋸齒)) 
        g2dCopy.setStroke(new BasicStroke(1.5f)); // 線條粗細為 1.5 f

        int width = 2*size;
        int length = size;

        // 三個點，(0, 0), (-length, -width/2), (-length, width/2)
        int[] xPoints = {0, -length, -length};
        int[] yPoints = {0, -width/2, width/2};

        g2dCopy.setColor(Color.BLACK);
        g2dCopy.drawPolygon(xPoints, yPoints, 3);

        g2dCopy.dispose(); // 丟棄 g2dCopy，恢復到原本的 g2d 狀態
    }

    // 鑽石型 (composition)
    // x 和 y 是 arrow 的尖端
    private void drawSolidDiamond(Graphics2D g2d, int x, int y, double angle, int size) {
        Graphics2D g2dCopy = (Graphics2D) g2d.create(); // 複製一個新的 g2d 狀態，之後對 g2dCopy 的變換不會影響到原本的 g2d
        g2dCopy.translate(x, y);
        g2dCopy.rotate(angle);

        // 開啟抗鋸齒，讓線條變更平滑
        g2dCopy.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); // setRenderingHint(要設定的渲染提示行為(要控制抗鋸齒), 設定提示行為的值(開啟抗鋸齒))
        g2dCopy.setStroke(new BasicStroke(1.5f)); // 線條粗細為 1.5 f

        int width = size*2;
        int height = size;

        // 四個點，(width/2, 0), (0, height/2), (-width/2, 0), (0, -height/2)
        int[] xPoints = {width/2, 0, -width/2, 0};
        int[] yPoints = {0, height/2, 0, -height/2};

        g2dCopy.setColor(Color.BLACK);
        g2dCopy.drawPolygon(xPoints, yPoints, 4);

        g2dCopy.dispose(); // 丟棄 g2dCopy，恢復到原本的 g2d 狀態
    }

    // 尖角 (association)
    // x 和 y 是 arrow 的尖端
    private void drawLineArrow(Graphics2D g2d, int x, int y, double angle, int size) {
        Graphics2D g2dCopy = (Graphics2D) g2d.create(); // 複製一個新的 g2d 狀態，之後對 g2dCopy 的變換不會影響到原本的 g2d
        g2dCopy.translate(x, y);
        g2dCopy.rotate(angle);

        // 開啟抗鋸齒，讓線條變更平滑
        g2dCopy.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); // setRenderingHint(要設定的渲染提示行為(要控制抗鋸齒), 設定提示行為的值(開啟抗鋸齒))
        g2dCopy.setStroke(new BasicStroke(1.5f)); // 線條粗細為 1.5 f

        int width = size;
        int length = size;

        int x1 = 0;
        int y1 = 0;
        int x2 = -length;
        int y2 = -width;
        int x3 = -length;
        int y3 = width;

        g2dCopy.setColor(Color.BLACK);

        // 兩條線，(0, 0) 到 (-length, -width) 和 (0, 0) 到 (-length, width)
        g2dCopy.drawLine(x1, y1, x2, y2); 
        g2dCopy.drawLine(x1, y1, x3, y3);

        g2dCopy.dispose(); // 丟棄 g2dCopy，恢復到原本的 g2d 狀態
    }


    // 初始及更改畫布上的東西， repaint() 後都會被叫一次
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // 繼承 CanvasPanel 的 paintComponent method，清空原本的畫面，畫出元件的背景
        Graphics2D g2d = (Graphics2D) g; // 將 graphics 升成 graphics2d

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); // 抗鋸齒，讓線條變更平滑
        g2d.setColor(Color.BLACK); // 預設畫筆顏色為黑色

        // 更新所有Port位置
        for (ArrayList<Port> ports : shapePortsMap.values()) {
            for (Port p : ports) {
                p.updatePosition();
            }
        }

        // 繪製形狀
        for (Object obj : shapes) {
            boolean isShapeSelected = selectedShapes.contains(obj); // 物件是否被選中
            boolean isShapeHovered = (hoverShape == obj); // 配合滑鼠監聽器看看是否有 hover 該物件

            if (obj instanceof CompositeShape) {
                CompositeShape cs = (CompositeShape) obj;
                drawCompositeShape(g2d, cs, isShapeSelected, isShapeHovered);
            } else if (obj instanceof MutableOval) {
                MutableOval oval = (MutableOval) obj;
                drawOval(g2d, oval, isShapeSelected, isShapeHovered);
                drawLabel(g2d, obj);
            } else if (obj instanceof Shape) {
                Shape shape = (Shape) obj; // 因為 rectagle 繼承 rectangle2d，rectangle2d 繼承 shape
                drawShape(g2d, shape, isShapeSelected, isShapeHovered);
                drawLabel(g2d, obj);
            }
        }

        // 繪製框選矩形
        if ("select".equals(getCurrentMode()) && rubberBandRect != null) {
            float[] dash = {5.0f};
            g2d.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, dash, 0.0f)); // 設定 g2d 的筆觸，getStroke(筆觸寬度, 線段末端形狀, 線段轉角處接法, 斜角限制, 虛線規則, 虛線模式從哪個相位開始畫)
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
            g2d.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, dash, 0.0f)); // 設定 g2d 的筆觸，getStroke(筆觸寬度, 線段末端形狀, 線段轉角處接法, 斜角限制, 虛線規則, 虛線模式從哪個相位開始畫)
            g2d.drawLine(tempStartPort.getX(), tempStartPort.getY(), tempLineEnd.x, tempLineEnd.y);
            g2d.setStroke(new BasicStroke(1.0f));
        }
    }

    // 畫矩形
    private void drawShape(Graphics2D g2d, Shape shape, boolean selected, boolean hovered) {
        if (selected || hovered) {
            g2d.setColor(Color.BLUE);
            g2d.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, new float[]{5}, 0)); // 設定 g2d 的筆觸，getStroke(筆觸寬度, 線段末端形狀, 線段轉角處接法, 斜角限制, 虛線規則, 虛線模式從哪個相位開始畫)
            g2d.draw(shape); // 因為 shape 已經有要畫的圖的幾何資訊了，矩形是 x, y, width, height

            ArrayList<Port> ports = shapePortsMap.get(shape);

            // 如果 port 已存在才進入
            if (ports != null) {
                for (Port port : ports) {
                    // port 塗滿白色
                    g2d.setColor(Color.WHITE);
                    g2d.fillRect(port.getX() - 3, port.getY() - 3, 6, 6);
                    
                    // port 用黑色虛線矩形框起來
                    g2d.setColor(Color.BLACK);
                    g2d.drawRect(port.getX() - 3, port.getY() - 3, 6, 6);
                }
            }
        } else {
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(1.0f)); // 一般的筆，實線
            g2d.draw(shape);
        }
    }

    // 畫橢圓
    private void drawOval(Graphics2D g2d, MutableOval oval, boolean selected, boolean hovered) {
        // 
        Shape shape = new Ellipse2D.Float(oval.getX(), oval.getY(), oval.getWidth(), oval.getHeight()); // 取得橢圓的邊界矩形 x, y, width, height

        if (selected || hovered) {
            g2d.setColor(Color.BLUE);
            g2d.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, new float[]{5}, 0)); // 設定 g2d 的筆觸，getStroke(筆觸寬度, 線段末端形狀, 線段轉角處接法, 斜角限制, 虛線規則, 虛線模式從哪個相位開始畫)
        } else {
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(1.0f));
        }
        g2d.draw(shape);

        if (selected || hovered) {
            ArrayList<Port> ports = shapePortsMap.get(oval);

            if (ports != null) {
                for (Port port : ports) {
                    // port 塗滿白色
                    g2d.setColor(Color.WHITE);
                    g2d.fillRect(port.getX() - 3, port.getY() - 3, 6, 6);
                    
                    // port 用黑色虛線矩形框起來
                    g2d.setColor(Color.BLACK);
                    g2d.drawRect(port.getX() - 3, port.getY() - 3, 6, 6);
                }
            }
        }
    }

    // 畫 composite
    private void drawCompositeShape(Graphics2D g2d, CompositeShape cs, boolean selected, boolean hovered) {
        // 因為 oval 繼承 shapeinterface，而 shapeinterface 繼承 object 而非 shape，所以不能用 list<shape>
        for (Object child : cs.getChildren()) {
            if (child instanceof Shape) { // 也就是 rectangle (包含 composite，因為 composite 繼承 rectangle)
                g2d.setColor(Color.BLACK);
                g2d.setStroke(new BasicStroke(1.0f));
                g2d.draw((Shape) child);
            } else if (child instanceof MutableOval) {
                MutableOval oval = (MutableOval) child; // 因為 child 是 object，沒有 getX(), getY() 等等 methods
                Shape shape = new Ellipse2D.Float(oval.getX(), oval.getY(), oval.getWidth(), oval.getHeight());
                g2d.setColor(Color.BLACK);
                g2d.setStroke(new BasicStroke(1.0f));
                g2d.draw(shape);
            }
        }

        // 畫 composite 框
        if (selected || hovered) {
            g2d.setColor(Color.BLUE);
            g2d.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, new float[]{5}, 0)); // 設定 g2d 的筆觸，getStroke(筆觸寬度, 線段末端形狀, 線段轉角處接法, 斜角限制, 虛線規則, 虛線模式從哪個相位開始畫)
        } else {
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(1.0f));
        }
        g2d.draw(cs.getBounds()); // 將 compositebounds 算出的 minx, miny, maxx, maxy 畫出相對應的矩形
    }

    public void setCurrentMode(String mode) {
        this.currentMode = mode;
    }

    public String getCurrentMode() {
        return currentMode;
    }

    // 找找 (x, y) 所在的位置是哪個 port 佔的
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

    // 找找現在點擊的點是哪個物件 (rectangle, oval, composite) 在的地方
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
                Ellipse2D shape = new Ellipse2D.Float(oval.getX(), oval.getY(), oval.getWidth(), oval.getHeight()); // 因為要看看點擊的點是不是在 oval 裡面，所以還是用內建的 ellipse2d 比較好
                if (shape.contains(x, y)) {
                    return obj;
                }
            }
        }
        return null;
    }

    
    // 建立 Object 的 ports 或更新 ports 位置
    private void updateShapePorts(Object obj) {
        // CompositeShape 不應該有可連線的 ports
        if (obj instanceof CompositeShape) {
            shapePortsMap.remove(obj);
            return; 
        }

        ArrayList<Port> ports = shapePortsMap.get(obj); // 取 obj 的 ports list

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
            shapePortsMap.put(obj, ports); // 將建立好的 ports list 塞進 shapePortsMap
        } else {
            for (Port p : ports) {
                p.updatePosition();
            }
        }
    }

    // 組成 composite
    public void groupSelected() {
        if (selectedShapes.size() < 2) return;

        List<Object> groupChildren = new ArrayList<>(selectedShapes);
        CompositeShape group = new CompositeShape(groupChildren);

        // 將 composite 的子物件移除 shapes，並將 omposite 好的大物件塞進 selectedShapes 和 shapes 裡面
        shapes.removeAll(groupChildren);
        shapes.add(group);
        selectedShapes.clear();
        selectedShapes.add(group);

        for (Object child : groupChildren) {
            updateShapePorts(child);
        }
        repaint();
    }

    // 取消已組成的 composite
    public void ungroupSelected() {
        if (selectedShapes.size() != 1) return;

        Object sel = selectedShapes.get(0);
        if (sel instanceof CompositeShape) {
            CompositeShape group = (CompositeShape) sel;

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

    // 將 composite ungroup
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

    // 更新 shape 的大小或位置
    private void resizeShape(Object shape, Port port, int mx, int my) {
        int minSize = 30;

        if (shape instanceof Rectangle) {
            Rectangle rect = (Rectangle) shape;
            int x1 = rect.x;
            int y1 = rect.y;
            
            int x2 = rect.x + rect.width;
            int y2 = rect.y + rect.height;

            int newLeft = x1, newTop = y1, newRight = x2, newBottom = y2;

            switch (port.getType()) {
                case 0: 
                    newLeft = mx; 
                    newTop = my; 
                    break;
                case 1: 
                    newTop = my; 
                    break;
                case 2: 
                    newTop = my; 
                    newRight = mx; 
                    break;
                case 3: 
                    newRight = mx; 
                    break;
                case 4: 
                    newRight = mx; 
                    newBottom = my; 
                    break;
                case 5: 
                    newBottom = my; 
                    break;
                case 6: 
                    newLeft = mx; 
                    newBottom = my; 
                    break;
                case 7: 
                    newLeft = mx; 
                    break;
            }

            if (newRight < newLeft) {
                int temp = newLeft;
                newLeft = newRight;
                newRight = temp;
            }

            if (newBottom < newTop) {
                int temp = newTop;
                newTop = newBottom;
                newBottom = temp;
            }

            int newX = Math.min(newLeft, newRight);
            int newY = Math.min(newTop, newBottom);

            int newW = newRight - newLeft;
            int newH = newBottom - newTop;

            if (newW < minSize) {
                newW = minSize;
            } else {
                rect.x = newX;
            }

            if (newH < minSize) {
                newH = minSize;
            } else {
                rect.y = newY;
            }

            rect.width = newW;
            rect.height = newH;
            updateShapePorts(rect);

        } else if (shape instanceof MutableOval) {
            MutableOval oval = (MutableOval) shape;
            int x1 = oval.getX(), y1 = oval.getY();
            int x2 = oval.getX() + oval.getWidth(), y2 = oval.getY() + oval.getHeight();

            int newLeft = x1, newTop = y1, newRight = x2, newBottom = y2;

            switch (port.getType()) {
                case 0: // 上
                    newTop = my;
                    break;
                case 1: // 右
                    newRight = mx; 
                    break; 
                case 2: // 下
                    newBottom = my; 
                    break; 
                case 3: // 左
                    newLeft = mx; 
                    break; 
            }

            int newX = Math.min(newLeft, newRight);
            int newY = Math.min(newTop, newBottom);
            
            int newW = newRight - newLeft;
            int newH = newBottom - newTop;

            if (newW < minSize) {
                newW = minSize;
            } else {
                oval.setX(newX);
            }
                
            if (newH < minSize) {
                newH = minSize;
            } else {
                oval.setY(newY);
            }
                
            oval.setWidth(newW);
            oval.setHeight(newH);
            updateShapePorts(oval);
        }
    }

    // 移動 composite 物件 (裡面的 child 物件也要一起移動)
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
        group.updateBoundsOnly();
        updateShapePorts(group);
        repaint();
    }

    // 滑鼠監聽事件
    private class MouseHandler extends MouseAdapter {
        // 滑鼠被按下時
        @Override
        public void mousePressed(MouseEvent e) {
            if ("association".equals(currentMode) || "generalization".equals(currentMode) || "composition".equals(currentMode)) {
                Port port = findPortAt(e.getX(), e.getY()); // 找出現在點擊的點時哪個 port
                
                // 有可能要畫線
                if (port != null) {
                    tempStartPort = port;
                    System.out.println("Start port set at: (" + port.getX() + ", " + port.getY() + ")");
                }
            } else if ("select".equals(currentMode)) {
                Point p = e.getPoint();
                Object targetShape = findShapeAt(p.x, p.y); // 找找點擊的點所在的形狀是誰
                Port port = findPortAt(e.getX(), e.getY()); // 也有可能剛好點擊在 port 上

                // 如果剛好點擊在 port 上
                if (port != null) {
                    if (!(port.getParentShape() instanceof CompositeShape)) {
                        resizingPort = port; // 更新 Port resizingPort
                        resizingShape = port.getParentShape(); // 更新 Object resizingShape
                        return;
                    }
                }

                // 看看有沒有選中東西
                if (targetShape != null) {
                    selectedShapes.clear();
                    selectedShapes.add(targetShape);
                    rubberBandStart = null;
                    rubberBandRect = null;
                    System.out.println("Object selected: " + targetShape.getClass().getSimpleName());
                } else {
                    selectedShapes.clear();
                    rubberBandStart = p; // 預備可能要用拖曳選擇一大塊區域
                    rubberBandRect = null; // 清空選擇框
                    System.out.println("Start Rubber Band Selection");
                }
                dragging = false;
                lastMousePoint = e.getPoint(); // 先設定 lastMousePoint 是現在所點的位置，之後再實時更新
                repaint();
            }
        }

        // 滑鼠被拖曳時 (還沒看完)
        @Override
        public void mouseDragged(MouseEvent e) {
            Point currentPoint = e.getPoint();

            // 如果是要移動非 composite 的物件 (rectangle, oval)
            if (resizingPort != null && resizingShape != null) {
                resizeShape(resizingShape, resizingPort, e.getX(), e.getY());
                repaint();
                return;
            }

            // 如果要畫線，更新 line end
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

        // 在滑鼠移動時更新 hoverShape，並根據 hover 狀態改變游標
        @Override
        public void mouseMoved(MouseEvent e) {
            if (!"select".equals(currentMode)) return; // 要是 select mode 才有動作

            Point p = e.getPoint();
            Object hitShape = findShapeAt(p.x, p.y);

            // 更新 hoverShape 為使用者點擊的物件
            if (hoverShape != hitShape) {
                hoverShape = hitShape;
                repaint();
            }

            // 滑鼠 hover 到物件時，將游標形狀改成手指，否則改回預設
            if (hitShape != null) {
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            } else {
                setCursor(Cursor.getDefaultCursor());
            }
        }
    }

    // getters
    public List<Object> getSelectedShapes() { return selectedShapes; }
    public Map<Object, String> getShapeLabels() { return shapeLabels; }
    public Map<Object, Color> getShapeLabelColors() { return shapeLabelColors; }
}