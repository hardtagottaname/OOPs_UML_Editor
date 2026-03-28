import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.util.List;

public class Main extends JFrame {
    public static class Port {
        public int x, y;
        public Object parentShape; // 可選：如果需要知道這個 Port 屬於哪個圖形
        public int type;

        public Port(Object parentShape, int type) {
            this.parentShape = parentShape;
            this.type = type;
            updatePosition();
        }

        public void updatePosition() {
            if (parentShape == null) {
                return; // 保持手動設置的 x, y 不變
            }


            // Rectangle bounds = ((Shape) parentShape).getBounds();
            // int cx = bounds.x + bounds.width / 2;
            // int cy = bounds.y + bounds.height / 2;
            
            // 簡單的 switch-case 來計算不同圖形的 Port 位置
            if (parentShape instanceof Rectangle) {
                Rectangle bounds = ((Rectangle) parentShape).getBounds();
                int cx = bounds.x + bounds.width / 2;
                int cy = bounds.y + bounds.height / 2;
               // 矩形：8個方向 (0-7)
                switch (type) {
                    case 0: // TopLeft (左上)
                        x = bounds.x;
                        y = bounds.y;
                        break;
                    case 1: // Top (上中)
                        x = cx;
                        y = bounds.y;
                        break;
                    case 2: // TopRight (右上)
                        x = bounds.x + bounds.width;
                        y = bounds.y;
                        break;
                    case 3: // Right (右中)
                        x = bounds.x + bounds.width;
                        y = cy;
                        break;
                    case 4: // BottomRight (右下)
                        x = bounds.x + bounds.width;
                        y = bounds.y + bounds.height;
                        break;
                    case 5: // Bottom (下中)
                        x = cx;
                        y = bounds.y + bounds.height;
                        break;
                    case 6: // BottomLeft (左下)
                        x = bounds.x;
                        y = bounds.y + bounds.height;
                        break;
                    case 7: // Left (左中)
                        x = bounds.x;
                        y = cy;
                        break;
                    default:
                        x = cx; y = cy; // 防呆
                }
            } else if (parentShape instanceof MutableOval) {
                MutableOval oval = (MutableOval) parentShape;

                int cx = oval.x + oval.width / 2;
                int cy = oval.y + oval.height / 2;
                // 橢圓：4個方向 (0-3)，基於橢圓的四個極點
                switch (type) {
                    case 0: x = cx; y = oval.y; break;
                    case 1: x = oval.x + oval.width; y = cy; break;
                    case 2: x = cx; y = oval.y + oval.height; break;
                    case 3: x = oval.x; y = cy; break;
                }
            } 
        }  

        public boolean contains(int mx, int my) {
            return Math.abs(mx - x) <= 5 && Math.abs(my - y) <= 5;
        }
    }

    public static class Line {
        public Port start, end;
        public String type; // 可選：用來區分 association/generalization/composition

        public Line(Port start, Port end, String type) {
            this.start = start;
            this.end = end;
            this.type = type;
        }
    }

    private String currentMode = "select";

    private JPanel buttonPanel;
    private CanvasPanel canvasPanel;

    private JButton lastActiveButton = null;
    private JButton previousActiveButton = null; 

    // --- 修正：改用 Object 作為 Key，這樣才能同時存放 Rectangle 和 MutableOval ---
    private java.util.Map<Object, ArrayList<Port>> shapePortsMap;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new Main().setVisible(true);
        });
    }

    public Main() {
        setTitle("OOPS_UML_Editor");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 700);
        setLayout(new BorderLayout());

        setCanvasPanel();
        setButtonPanel();
        setMenuBar();

        add(buttonPanel, BorderLayout.WEST);
        add(canvasPanel, BorderLayout.CENTER);
    }

    private class CanvasPanel extends JPanel {
        private java.util.List<Object> selectedShapes = new java.util.ArrayList<>();// 新的多选列表
        private Object hoverShape = null; // 新增：记录当前鼠标悬停的对象
        private boolean dragging = false;
        private Point rubberBandStart = null; // 框選起點
        private Rectangle rubberBandRect = null; // 框選矩形 (暫存)

        private java.util.List<Object> shapes;
        private ArrayList<Line> lines;

        private Port tempStartPort = null;      
        private Point tempLineEnd = null; 

        private Point lastMousePoint = new Point(0, 0); 
        
        public CanvasPanel() {
            shapes = new ArrayList<>();
            lines = new ArrayList<>();
            shapePortsMap = new java.util.HashMap<>();

            MouseHandler mouseHandler = new MouseHandler();
            addMouseListener(mouseHandler);
            addMouseMotionListener(mouseHandler);
            setFocusable(true); 
            setRequestFocusEnabled(true);
        }

        public void addShape(Object obj) {
            shapes.add(obj);
            updateShapePorts(obj); // 這裡會呼叫下方重載的 updateShapePorts
            repaint();
        }

        public void addLine(Line line) {
            lines.add(line);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(Color.BLACK);

            for (Object obj : shapes) {
                // --- 修正：強制轉型判斷 ---
                boolean isShapeSelected = selectedShapes.contains(obj);
                boolean isShapeHovered = (hoverShape == obj);

                if (obj instanceof Shape) {
                    Shape shape = (Shape) obj;
                    if (isShapeSelected || isShapeHovered) {
                        g2d.setColor(Color.BLUE);
                        g2d.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{5}, 0));
                        g2d.draw(shape);
                        
                        // Case C: 顯示 Ports
                        ArrayList<Port> ports = shapePortsMap.get(shape);
                        if (ports != null) {
                            for (Port port : ports) {
                                g2d.setColor(Color.WHITE);
                                g2d.fill(new Rectangle(port.x - 3, port.y - 3, 6, 6));
                                g2d.setColor(Color.BLACK);
                                g2d.draw(new Rectangle(port.x - 3, port.y - 3, 6, 6));
                            }
                        }
                    } else {
                        g2d.setColor(Color.BLACK);
                        g2d.setStroke(new BasicStroke(1.0f));
                        g2d.draw(shape);
                    }
                } 
                // --- 新增：處理 MutableOval 的繪製 ---
                else if (obj instanceof MutableOval) {
                    MutableOval oval = (MutableOval) obj;
                    // 建立一個臨時的 Ellipse2D 來繪製
                    Shape shape = new Ellipse2D.Float(oval.x, oval.y, oval.width, oval.height);
                    
                    if (isShapeSelected || isShapeHovered) {
                        g2d.setColor(Color.BLUE);
                        g2d.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{5}, 0));
                    } else {
                        g2d.setColor(Color.BLACK);
                        g2d.setStroke(new BasicStroke(1.0f));
                    }
                    g2d.draw(shape);
                    
                    // 畫 Port (如果需要)
                    // 注意：這部分取決於你的 Port 是否支援 Oval
                    if (isShapeSelected || isShapeHovered) {
                        ArrayList<Port> ports = shapePortsMap.get(obj);
                        if (ports != null) {
                            for (Port port : ports) {
                                g2d.setColor(Color.WHITE);
                                g2d.fill(new Rectangle(port.x - 3, port.y - 3, 6, 6));
                                g2d.setColor(Color.BLACK);
                                g2d.draw(new Rectangle(port.x - 3, port.y - 3, 6, 6));
                            }
                        }
                    }
                } else if (obj instanceof CompositeShape) {
                    CompositeShape cs = (CompositeShape) obj;

                    boolean isSelected = selectedShapes.contains(cs);
                    boolean isHovered = (hoverShape == cs);

                    // ⭐ 先畫 children（用正常樣式）
                    for (Object child : cs.getChildren()) {
                        if (child instanceof Shape) {
                            g2d.setColor(Color.BLACK);
                            g2d.setStroke(new BasicStroke(1.0f));
                            g2d.draw((Shape) child);
                        } else if (child instanceof MutableOval) {
                            MutableOval oval = (MutableOval) child;
                            Shape shape = new Ellipse2D.Float(oval.x, oval.y, oval.width, oval.height);
                            g2d.setColor(Color.BLACK);
                            g2d.setStroke(new BasicStroke(1.0f));
                            g2d.draw(shape);
                        }
                    }

                    // ⭐ 再畫 group 框（只有選到才藍色）
                    if (isSelected || isHovered) {
                        g2d.setColor(Color.BLUE);
                        g2d.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_BUTT,
                                BasicStroke.JOIN_MITER, 10.0f, new float[]{5}, 0));
                    } else {
                        g2d.setColor(Color.BLACK);
                        g2d.setStroke(new BasicStroke(1.0f));
                    }

                    g2d.draw(cs.getBounds());
                }
            }

            // --- 修正：框選邏輯 (Rubber Band) ---
            // 這部分通常不需要改，但為了安全檢查一下
            if (currentMode.equals("select") && rubberBandRect != null) {
                float[] dash = {5.0f};
                g2d.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f));
                g2d.setColor(Color.BLUE);
                g2d.draw(rubberBandRect);
            }

            // --- 修正：畫線邏輯 ---
            // 確保 line 的 start/end 處理正常
            for (Line line : lines) {
                if (line.start != null && line.end != null) {
                    drawCustomLine(g2d, line.start.x, line.start.y, line.end.x, line.end.y, line.type);
                }
            }

            if (tempStartPort != null && tempLineEnd != null) {
                float[] dash = {5.0f};
                g2d.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f));
                g2d.drawLine(tempStartPort.x, tempStartPort.y, tempLineEnd.x, tempLineEnd.y);
                g2d.setStroke(new BasicStroke(1.0f));
            }
        }

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
            if (type.equals("generalization")) {
                drawHollowArrow(g2d, x2, y2, angle, arrowSize);
            } else if (type.equals("composition")) {
                drawSolidDiamond(g2d, x2, y2, angle, arrowSize);
            } else if (type.equals("association")) {
                drawLineArrow(g2d, x2, y2, angle, arrowSize);
            }
        }

        private void drawLineArrow(Graphics2D g2d, int x, int y, double angle, int size) {
            Graphics2D g2dCopy = (Graphics2D) g2d.create();
            g2dCopy.translate(x, y);
            g2dCopy.rotate(angle);

            // 啟用抗鋸齒
            g2dCopy.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 設定線條粗細
            g2dCopy.setStroke(new BasicStroke(1.5f));

            // 定義箭頭尺寸
            int width = 6;  // 箭頭張開的寬度
            int length = 8; // 箭頭長度

            // 頂點
            int x1 = 0;
            int y1 = 0;

            // 左下點
            int x2 = -length;
            int y2 = -width / 2;

            // 右下點
            int x3 = -length;
            int y3 = width / 2;

            // 繪製兩條線：頂點 -> 左下點，頂點 -> 右下點
            g2dCopy.setColor(Color.BLACK);
            g2dCopy.drawLine(x1, y1, x2, y2); // 左邊
            g2dCopy.drawLine(x1, y1, x3, y3); // 右邊

            g2dCopy.dispose();
        }


        // 輔助方法：畫菱形 (組合)
        private void drawSolidDiamond(Graphics2D g2d, int x, int y, double angle, int size) {
            Graphics2D g2dCopy = (Graphics2D) g2d.create();
            g2dCopy.translate(x, y);
            g2dCopy.rotate(angle);

            // 啟用抗鋸齒，讓邊緣平滑
            g2dCopy.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 設定線條粗細（建議 1.5~2.0，與主線條一致）
            g2dCopy.setStroke(new BasicStroke(1.5f));

            // 定義菱形尺寸
            int width = 12;   // 菱形總寬度（左右頂點距離）
            int height = 18; // 菱形總高度（上下頂點距離）

            // 四個頂點：左頂點(0,0) -> 上頂點(width/2, height/4) -> 右頂點(0, height/2) -> 下頂點(-width/2, height/4)
            int[] xPoints = {0, width/2, 0, -width/2};
            int[] yPoints = {0, height/4, height/2, height/4};

            // 繪製空心菱形（只畫邊框）
            g2dCopy.setColor(Color.BLACK);
            g2dCopy.drawPolygon(xPoints, yPoints, 4);

            g2dCopy.dispose();
        }

        private void drawHollowArrow(Graphics2D g2d, int x, int y, double angle, int size) {
            Graphics2D g2dCopy = (Graphics2D) g2d.create();
            g2dCopy.translate(x, y);
            g2dCopy.rotate(angle);

            // 啟用抗鋸齒，讓邊緣平滑
            g2dCopy.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 設定線條粗細（建議 1.5~2.0，與主線條一致）
            g2dCopy.setStroke(new BasicStroke(1.5f));

            // 定義箭頭尺寸
            int width = 6; // 箭頭底部寬度
            int length = 8; // 箭頭長度（從頂點到底部的距離）

            // 頂點 (0, 0) -> 左下 (-length, -width/2) -> 右下 (-length, width/2)
            // 注意：這裡使用 drawPolygon，只畫邊框，不填充
            int[] xPoints = {0, -length, -length};
            int[] yPoints = {0, -width/2, width/2};

            // 繪製空心三角形箭頭
            g2dCopy.setColor(Color.BLACK);
            g2dCopy.drawPolygon(xPoints, yPoints, 3);

            // 恢復繪圖狀態
            g2dCopy.dispose();
        }

        private class MouseHandler extends MouseAdapter {
            @Override
            public void mousePressed(MouseEvent e) {
                if (currentMode.equals("association") || currentMode.equals("generalization") || currentMode.equals("composition")) {
                    Port port = findPortAt(e.getX(), e.getY());
                    
                    if (port != null) {
                        tempStartPort = port;
                        System.out.println("Start port set at: (" + port.x + ", " + port.y + ")");
                    }
                } else if (currentMode.equals("select")) {
                    Point p = e.getPoint();
                    Object targetShape = findShapeAt(p.x, p.y);
                    
                    // 2. 如果沒有點中群組，才去檢查一般的子物件
                    if (targetShape == null) {
                        for (int i = shapes.size() - 1; i >= 0; i--) {
                            Object shape = shapes.get(i);
                            // 跳過群組（群組已經檢查過了）
                            if (!(shape instanceof CompositeShape)) {
                                if (shape instanceof Shape) {
                                    if (((Shape) shape).contains(p.x, p.y)) {
                                        targetShape = shape;
                                        break;
                                    }
                                } else if (shape instanceof MutableOval) {
                                    if (((MutableOval) shape).contains(p.x, p.y)) {
                                        targetShape = shape;
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    boolean isCtrlPressed = (e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0;
        
                    // --- 修正選取邏輯 ---
                    if (targetShape != null) { 
                        // 點擊到了對象
                        if (isCtrlPressed) { 
                            // Ctrl 多選邏輯
                            if (selectedShapes.contains(targetShape)) {
                                selectedShapes.remove(targetShape);
                            } else {
                                // 關鍵：如果選的是群組，先清空裡面的子物件，只選群組
                                if (targetShape instanceof CompositeShape) {
                                    selectedShapes.clear(); // 或者 .removeAll(((CompositeShape) targetShape).getChildren()); 但為了簡單先清空
                                }
                                selectedShapes.add(targetShape);
                            }
                        } else { 
                            // 一般點擊：清空之前選擇，只選這個
                            selectedShapes.clear(); 
                            selectedShapes.add(targetShape); 
                        } 
                        rubberBandStart = null; 
                        rubberBandRect = null; 
                        System.out.println("Object selected: " + targetShape.getClass().getSimpleName());
                    } else { 
                        // 點擊到了空白處
                        selectedShapes.clear(); // 清空選擇
                        rubberBandStart = p; 
                        rubberBandRect = null; 
                        System.out.println("Start Rubber Band Selection"); 
                    } 
                    dragging = false; 
                    // 這行必須放在選擇邏輯之後，這樣 lastMousePoint 才會是滑鼠按下的瞬間座標
                    lastMousePoint = e.getPoint();
                    repaint(); 
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                Point currentPoint = e.getPoint();

                if (tempStartPort != null) {
                    // 画线模式（保持不变）
                    tempLineEnd = currentPoint;
                    repaint();
                    return;
                }

                if (currentMode.equals("select") && rubberBandStart == null && !selectedShapes.isEmpty()) {
                    
                    // --- 关键修正 1: 计算位移 ---
                    // deltaX/deltaY 是鼠标移动了多少距离，而不是要移动到哪里
                    // 注意：我们使用类成员变量 lastMousePoint 来记录上一次的位置
                    int deltaX = currentPoint.x - lastMousePoint.x;
                    int deltaY = currentPoint.y - lastMousePoint.y;

                    // 關鍵：只有當滑鼠移動了足夠距離，才視為拖動
                    if (Math.abs(deltaX) > 0 || Math.abs(deltaY) > 0) {
                        for (Object shape : selectedShapes) {
                            if (shape instanceof CompositeShape) {
                                // 交給上面的方法處理
                                moveCompositeShape((CompositeShape) shape, deltaX, deltaY);
                            } else {
                                // 普通物件移動邏輯 (保持不變)
                                if (shape instanceof Rectangle) {
                                    Rectangle rect = (Rectangle) shape;
                                    rect.x += deltaX;
                                    rect.y += deltaY;
                                } else if (shape instanceof MutableOval) {
                                    MutableOval oval = (MutableOval) shape;
                                    oval.x += deltaX;
                                    oval.y += deltaY;
                                    updateShapePorts(oval);
                                }
                                updateShapePorts(shape);
                            }
                        }
                        // 更新 lastMousePoint 必須在最後
                        // 這樣下一次計算 deltaX 時，才是基於「移動後」的位置
                        lastMousePoint = currentPoint; 
                        repaint();
                    }
                    return;
                }

                // 框选逻辑 (Rubber Band)
                if (currentMode.equals("select") && rubberBandStart != null) {
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
                dragging = false;

                if (tempStartPort != null) {
                    Port endPort = findPortAt(e.getX(), e.getY());
                    
                    // Alternatives B.2: 如果 endPort 是 null (不在任何圖形上) 或是同一個圖形，則不建立
                    if (endPort != null && endPort.parentShape != tempStartPort.parentShape) {
                        canvasPanel.addLine(new Line(tempStartPort, endPort, currentMode));
                        System.out.println("Line created between different shapes.");
                    } else {
                        System.out.println("Line creation cancelled (invalid end point or same shape).");
                    }
                    
                    // 無論是否建立線，都要重置狀態
                    tempStartPort = null;
                    tempLineEnd = null;
                    repaint();
                } else if (currentMode.equals("select") && rubberBandStart != null) {
                    // 處理框選結束
                    // 此時 rubberBandRect 就是 (x1, y1, x2, y2) 形成的矩形
                    // 需要檢查所有 shapes 是否「完全」落在這個矩形內
                    Rectangle finalRect = rubberBandRect;
                    
                    // 只有当框选范围足够大时才进行选择
                    if (finalRect != null && finalRect.width > 5 && finalRect.height > 5) {
                        // 关键修改：遍历所有图形，将范围内所有图形加入 selectedShapes
                        for (Object shape : shapes) {
                            // 判断图形中心点或边界是否在框选矩形内
                            // 这里使用 intersects 表示只要图形和框有重叠就算选中
                            // 如果需要完全包含，使用 finalRect.contains(shape.getBounds())
                            Rectangle bounds = null;
                            if (shape instanceof Shape) {
                                bounds = ((Shape) shape).getBounds();
                            } else if (shape instanceof MutableOval) {
                                bounds = ((MutableOval) shape).getBounds();
                            }
                            // 安全檢查並判斷
                            if (bounds != null && finalRect.intersects(bounds)) {
                                if (!selectedShapes.contains(shape)) {
                                    selectedShapes.add(shape);
                                }
                            }
                        }
                    } else {
                        // 矩形太小，视为取消选择
                        selectedShapes.clear();
                    }
                    
                    rubberBandStart = null;
                    rubberBandRect = null;
                    repaint(); // 更新选择状态的视觉效果
                    System.out.println("Selected " + selectedShapes.size() + " objects");
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                if (!currentMode.equals("select")) {
                    return;
                }
                Point p = e.getPoint();
                Object hitShape = findShapeAt(p.x, p.y);
                
                // 更新悬停状态
                if (hoverShape != hitShape) { // 只有当悬停对象改变时才更新
                    hoverShape = hitShape;
                    repaint(); // 必须重绘，否则 paintComponent 不知道要画 Ports
                }

                if (hitShape != null) {
                    canvasPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                } else {
                    canvasPanel.setCursor(Cursor.getDefaultCursor());
                }
            }

            private void moveCompositeShape(CompositeShape group, int deltaX, int deltaY) {
                // --- 1. 移動群組框本身 (讓藍框跟著滑鼠) ---
                group.x += deltaX;
                group.y += deltaY;

                // --- 2. 移動群組內的所有子物件 (關鍵修正) ---
                // 這裡要同時處理 Rectangle 和 MutableOval
                for (Object obj : group.getChildren()) {
                    
                    // --- 情況 A: 如果是矩形 (原本的邏輯) ---
                    if (obj instanceof Rectangle) {
                        Rectangle rect = (Rectangle) obj;
                        rect.x += deltaX;
                        rect.y += deltaY;
                        updateShapePorts(rect);
                    }
                    
                    // --- 情況 B: 如果是橢圓 (新增的關鍵邏輯) ---
                    // 注意：這裡直接強制轉型，因為我們知道它是 MutableOval
                    else if (obj instanceof MutableOval) {
                        MutableOval oval = (MutableOval) obj; // 把 obj 變成 oval
                        oval.x += deltaX; // 直接修改 oval 的 x 座標
                        oval.y += deltaY; // 直接修改 oval 的 y 座標
                        updateShapePorts(oval); // 更新它的 Port
                    }
                    
                    // --- 情況 C: 如果是群組 (CompositeShape) ---
                    // 如果你的群組可以「巢狀」（群組裡面還有群組），可以加上這段，但目前先不處理
                    // else if (obj instanceof CompositeShape) {
                    //     // 如果是群組，通常我們不移動它，因為它自己會處理
                    // }
                }

                // --- 3. 更新群組框的 Port ---
                updateShapePorts(group);
                repaint();
            }
        }

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

        private Object findShapeAt(int x, int y) {
            // ⭐ Step 1：先找「最上層的 group」（由上往下）
            for (int i = shapes.size() - 1; i >= 0; i--) {
                Object obj = shapes.get(i);

                if (obj instanceof CompositeShape) {
                    CompositeShape group = (CompositeShape) obj;

                    if (group.contains(x, y)) {
                        return group; // ⭐優先選 group
                    }
                }
            }

            // ⭐ Step 2：再找一般 shape（Rectangle / Oval）
            for (int i = shapes.size() - 1; i >= 0; i--) {
                Object obj = shapes.get(i);

                if (obj instanceof Rectangle) {
                    if (((Rectangle) obj).contains(x, y)) {
                        return obj;
                    }
                } else if (obj instanceof MutableOval) {
                    MutableOval oval = (MutableOval) obj;
                    Ellipse2D shape = new Ellipse2D.Float(
                        oval.x, oval.y, oval.width, oval.height
                    );

                    if (shape.contains(x, y)) {
                        return obj;
                    }
                }
            }

            return null;
        }

        private void updateShapePorts(Object obj) {
            ArrayList<Port> ports = new ArrayList<>();
            
            if (obj instanceof Rectangle) {
                for (int i = 0; i < 8; i++) {
                    ports.add(new Port(obj, i));
                }
            } else if (obj instanceof MutableOval) {
                
                for (int i = 0; i < 4; i++) {
                    ports.add(new Port(obj, i)); // ⭐ 核心
                }
                
                // 把這行移進來，確保 Oval 的資料被儲存
                shapePortsMap.put(obj, ports); 
                return; // 記得 return，否則下面會重複 put
            }
            
            // 如果是 Rectangle 或其他，執行這行
            shapePortsMap.put(obj, ports);
        }

        public void groupSelected() {
            if (selectedShapes.size() < 2) return;

            // 1. 直接使用 selectedShapes 的引用，不要 New 新物件
            List<Object> groupChildren = new ArrayList<>(selectedShapes); 
            CompositeShape group = new CompositeShape(groupChildren);
            
            // 2. 關鍵：先從畫布移除舊的子物件，否則畫布會畫兩次 (導致閃爍或消失)
            // (假設你的畫布有一個 List<Object> allShapes)
            // shapes.removeAll(selectedShapes); 
            
            // 3. 加入群組
            shapes.add(group);
            
            // 4. 更新選擇狀態
            selectedShapes.clear();
            selectedShapes.add(group);

            for (Object child : groupChildren) {
                updateShapePorts(child);
            }
            
            repaint();
        }

        public void ungroupSelected() {
            if (selectedShapes.size() != 1) return;

            Object sel = selectedShapes.get(0);
            if (sel instanceof CompositeShape) {
                CompositeShape group = (CompositeShape) sel;

                // ⭐ 1. 找出要刪掉的線
                ArrayList<Line> linesToRemove = new ArrayList<>();

                for (Line line : lines) {
                    Object startParent = line.start.parentShape;
                    Object endParent = line.end.parentShape;

                    // 👉 情況 1：線直接連到 group
                    if (startParent == group || endParent == group) {
                        linesToRemove.add(line);
                        continue;
                    }

                    // 👉 情況 2：線連到 group 裡面的 child
                    for (Object child : group.getChildren()) {
                        if (startParent == child || endParent == child) {
                            linesToRemove.add(line);
                            break;
                        }
                    }
                }

                // ⭐ 2. 刪掉這些線
                lines.removeAll(linesToRemove);

                // ⭐ 3. 還原子物件
                shapes.addAll(group.getChildren());

                // ⭐ 4. 移除 group
                shapes.remove(group);

                // ⭐ 5. 更新選取
                selectedShapes.clear();
                selectedShapes.addAll(group.getChildren());
            }

            repaint();
        }
    }

    private class ButtonMouseHandler extends MouseAdapter {
            private JButton button;

            public ButtonMouseHandler(JButton button) {
                this.button = button;
            }

            @Override
            public void mousePressed(MouseEvent e) {
                String buttonText = button.getText();

                if (buttonText.equals("rect") || buttonText.equals("oval")) {
                    previousActiveButton = lastActiveButton; // 儲存目前的 active button

                    if (lastActiveButton != null) {
                        System.out.println("Resetting button: " + lastActiveButton.getText());
                        lastActiveButton.setBackground(UIManager.getColor("Button.background"));
                        lastActiveButton.setForeground(UIManager.getColor("Button.foreground"));
                    }

                    button.setBackground(Color.BLACK);
                    button.setForeground(Color.WHITE);
                    lastActiveButton = button;
                }

                currentMode = buttonText;
                System.out.println("Mode changed to: " + currentMode);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (currentMode.equals("rect") || currentMode.equals("oval")) {
                    Point canvasPoint = SwingUtilities.convertPoint(button, e.getPoint(), canvasPanel);
                    int width = 100;
                    int height = 60;
                    int x = canvasPoint.x - width / 2;
                    int y = canvasPoint.y - height / 2;

                    if (currentMode.equals("oval")) {
                        // --- 修正：使用自定義的 MutableOval ---
                        canvasPanel.addShape(new MutableOval(x, y, width, height));
                    } else if (currentMode.equals("rect")) {
                        canvasPanel.addShape(new Rectangle(x, y, width, height));
                    }
                    resetButtonColors();
                }
            }
        }

    private void setCanvasPanel() {
        canvasPanel = new CanvasPanel();
        canvasPanel.setBackground(Color.LIGHT_GRAY);
    }
    
    private void setButtonPanel() {
        buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(6, 1, 5, 5));
        buttonPanel.setPreferredSize(new Dimension(120, 0));

        String[] buttonLabels = {"select", "association", "generalization", "composition", "rect", "oval"};

        for (String label: buttonLabels) {
            JButton btn = new JButton(label);
            btn.addActionListener(new ButtonClickListener());
            btn.addMouseListener(new ButtonMouseHandler(btn));
            buttonPanel.add(btn);
        }
    }

    private void resetButtonColors() {
        for (Component comp : buttonPanel.getComponents()) {
            if (comp instanceof JButton) {
                JButton btn = (JButton) comp;
                btn.setBackground(UIManager.getColor("Button.background"));
                btn.setForeground(UIManager.getColor("Button.foreground"));
            }
        }
        
        if (previousActiveButton != null) {
            lastActiveButton = previousActiveButton; 
        } else {
            // 如果之前沒人亮，那就預設回到 Select 或是 null
            lastActiveButton = null;
        }

        currentMode = (previousActiveButton != null) ? previousActiveButton.getText() : "select";
        previousActiveButton = null; // 清空暫存
        
        System.out.println("Mode reverted to: " + currentMode);    
    }

    private void setMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        Font menuFont = new Font("Arial", Font.PLAIN, 14);
        
        // file menu
        JMenu fileMenu = new JMenu("file");
        fileMenu.setFont(menuFont);

        JMenuItem newItem = new JMenuItem("new");
        JMenuItem openItem = new JMenuItem("open");
        JMenuItem saveItem = new JMenuItem("save");
        fileMenu.add(newItem);
        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        
        // edit menu
        JMenu editMenu = new JMenu("edit");
        editMenu.setFont(menuFont);

        JMenuItem groupItem = new JMenuItem("group");
        JMenuItem ungroupItem = new JMenuItem("ungroup");
        JMenuItem labelItem = new JMenuItem("label");

        groupItem.addActionListener(e -> canvasPanel.groupSelected());
        ungroupItem.addActionListener(e -> canvasPanel.ungroupSelected());
    
        editMenu.add(groupItem);
        editMenu.add(ungroupItem);
        editMenu.add(labelItem);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);

        setJMenuBar(menuBar);
    }

    private class ButtonClickListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String buttonName = e.getActionCommand();

            if (buttonName.equals("select")) {
                resetButtonColors();
                currentMode = "select";
            }
        }

    }
}

class CompositeShape extends java.awt.Rectangle {
    private java.util.List<Object> children;

    public CompositeShape(java.util.List<Object> children) {
        this.children = new java.util.ArrayList<>(children);
        // 初始化時只計算大小，位置由外面設定
        updateBoundsOnly(); // 改名為 Only，強調只改大小
    }

    public java.util.List<Object> getChildren() {
        return children;
    }

    // --- 修正：只更新大小，不更新位置 ---
    // 這個方法只在創建群組或新增物件時呼叫
    public void updateBoundsOnly() {
        if (children.isEmpty()) {
            // 如果沒東西，設個預設大小，不要改位置
            width = 50;
            height = 50;
            return;
        }

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (Object obj : children) {
            Rectangle bounds = null;

            if (obj instanceof java.awt.Shape) {
                bounds = ((java.awt.Shape) obj).getBounds();
            } else if (obj instanceof MutableOval) {
                bounds = ((MutableOval) obj).getBounds();
            }

            if (bounds != null) {
                minX = Math.min(minX, bounds.x);
                minY = Math.min(minY, bounds.y);
                maxX = Math.max(maxX, bounds.x + bounds.width);
                maxY = Math.max(maxY, bounds.y + bounds.height);
            }
        }

        // 只更新寬高
        // 關鍵：不要去動 this.x 和 this.y
        // 我們讓群組框的大小剛好包住子物件，但位置由拖曳邏輯控制
        width = maxX - minX;
        height = maxY - minY;

        this.x = minX; // 這行可以保留，讓群組框的初始位置是包住子物件的，但之後拖動時不會再改變
        this.y = minY; // 同上
        
        // 如果你希望群組框移動時，子物件相對位置不變，就不要在這裡改 x, y
    }
    
    // 強制提供一個方法，讓外部可以直接設定框框的位置和大小
    public void setGroupBounds(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.width = w;
        this.height = h;
    }
}

// 新增一個自定義的可變橢圓類
class MutableOval {
    public int x, y, width, height;
    
    public MutableOval(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    // 提供一個方法來檢查點是否在橢圓內 (用來處理點擊事件)
    public boolean contains(int mx, int my) {
        // 簡單的判斷：先檢查是否在矩形範圍內，再檢查橢圓公式
        if (mx < x || mx > x + width || my < y || my > y + height) {
            return false;
        }
        // 將點轉換為以橢圓中心為原點的座標
        double rx = width / 2.0;
        double ry = height / 2.0;
        double cx = x + rx;
        double cy = y + ry;
        double dx = mx - cx;
        double dy = my - cy;
        // 橢圓公式: (dx^2 / rx^2) + (dy^2 / ry^2) <= 1
        return ((dx*dx) / (rx*rx) + (dy*dy) / (ry*ry)) <= 1;
    }

    // 提供一個方法讓外部獲取它的 Bounds (用來計算 Port 和 群組框)
    public java.awt.Rectangle getBounds() {
        return new java.awt.Rectangle(x, y, width, height);
    }
}