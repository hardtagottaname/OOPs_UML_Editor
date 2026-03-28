import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;

public class Main extends JFrame {
    public static class Port {
        public int x, y;
        public Shape parentShape; // 可選：如果需要知道這個 Port 屬於哪個圖形
        public int type;

        public Port(Shape parentShape, int type) {
            this.parentShape = parentShape;
            this.type = type;
            updatePosition();
        }

        public void updatePosition() {
            Rectangle bounds = parentShape.getBounds();
            int cx = bounds.x + bounds.width / 2;
            int cy = bounds.y + bounds.height / 2;
            
            // 簡單的 switch-case 來計算不同圖形的 Port 位置
            if (parentShape instanceof Rectangle) {
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
            } else if (parentShape instanceof Ellipse2D) {
                // 橢圓：4個方向 (0-3)，基於橢圓的四個極點
                switch (type) {
                    case 0: // Top (正上)
                        x = cx;
                        y = bounds.y;
                        break;
                    case 1: // Right (正右)
                        x = bounds.x + bounds.width;
                        y = cy;
                        break;
                    case 2: // Bottom (正下)
                        x = cx;
                        y = bounds.y + bounds.height;
                        break;
                    case 3: // Left (正左)
                        x = bounds.x;
                        y = cy;
                        break;
                    default:
                        x = cx; y = cy; // 防呆
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

    private java.util.Map<Shape, ArrayList<Port>> shapePortsMap;

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

        CanvasPanel.MouseHandler mouseHandler = canvasPanel.new MouseHandler();
        canvasPanel.addMouseListener(mouseHandler);
        canvasPanel.addMouseMotionListener(mouseHandler);

        add(buttonPanel, BorderLayout.WEST);
        add(canvasPanel, BorderLayout.CENTER);
    }

    private class CanvasPanel extends JPanel {
        private java.util.List<Shape> selectedShapes = new java.util.ArrayList<>(); // 新的多选列表
        private Shape hoverShape = null; // 新增：记录当前鼠标悬停的对象
        private boolean dragging = false;
        private Point rubberBandStart = null; // 框選起點
        private Rectangle rubberBandRect = null; // 框選矩形 (暫存)

        private ArrayList<Shape> shapes;
        private ArrayList<Line> lines;

        private Port tempStartPort = null;      
        private Point tempLineEnd = null; 
        
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

        public void addShape(Shape shape) {
            shapes.add(shape);
            updateShapePorts(shape);
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
            
            
            for (Object obj : shapes) { // 注意：這裡迴圈變數改為 Object
                boolean isShapeSelected = selectedShapes.contains(obj);
                boolean isShapeHovered = (hoverShape == obj);

                if (obj instanceof Shape) {
                    Shape shape = (Shape) obj;
                    // 這是原本的基本物件繪製邏輯
                    if (isShapeSelected || isShapeHovered) {
                        g2d.setColor(Color.BLUE);
                        g2d.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{5}, 0));
                        g2d.draw(shape);
                        
                        // Case C: 顯示 Ports (只有基本物件顯示 Port)
                        boolean showPort = true;
                        for (Shape s : shapes) {
                            if (s instanceof CompositeShape) {
                                CompositeShape group = (CompositeShape) s;
                                if (group.getChildren().contains(shape)) {
                                    showPort = false; // 如果 shape 在群組內，隱藏 Port
                                    break;
                                }
                            }
                        }

                        if (showPort) {
                            ArrayList<Port> ports = shapePortsMap.get(shape);
                            if (ports != null) {
                                for (Port port : ports) {
                                    g2d.setColor(Color.WHITE); 
                                    g2d.fill(new Rectangle(port.x - 3, port.y - 3, 6, 6));
                                    g2d.setColor(Color.BLACK); 
                                    g2d.draw(new Rectangle(port.x - 3, port.y - 3, 6, 6));
                                }
                            }
                        }
                    } else {
                        g2d.setColor(Color.BLACK);
                        g2d.setStroke(new BasicStroke(1.0f));
                        g2d.draw(shape);
                    }
                } 
                else if (obj instanceof CompositeShape) {
                    // 這是 CompositeShape 的繪製邏輯
                    CompositeShape group = (CompositeShape) obj;
                    Rectangle bounds = group.getBounds();
                    
                    if (isShapeSelected || isShapeHovered) {
                        g2d.setColor(Color.BLUE);
                        g2d.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{5}, 0));
                    } else {
                        g2d.setColor(Color.GRAY); // Composite 用灰色表示
                        g2d.setStroke(new BasicStroke(1.0f));
                    }
                    // Requirement: 僅顯示組合物件外框
                    g2d.draw(bounds);
                    // 注意：Composite 不畫內部的 Port，只畫框
                }
            }

            if (currentMode.equals("select") && rubberBandRect != null) {
                // 設定虛線樣式
                float[] dash = {5.0f};
                g2d.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f));
                g2d.setColor(Color.BLUE);
                
                // 畫矩形 (注意：rubberBandRect 可能是 null，所以要判斷)
                g2d.draw(rubberBandRect);
                
                // (可選) 畫一個半透明的背景
                // AlphaComposite alpha = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.1f);
                // g2d.setComposite(alpha);
                // g2d.fill(rubberBandRect);
            }

            for (Line line : lines) {
                // 必須確認 line.start 和 line.end 的 owner 還存在，且不是 null
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
                    Shape targetShape = null;

                    for (int i = shapes.size() - 1; i >= 0; i--) {
                        Shape shape = shapes.get(i);
                        // 特別處理：如果是 CompositeShape
                        if (shape instanceof CompositeShape) {
                            // 注意：CompositeShape 繼承自 Rectangle，所以可以直接用 contains
                            if (shape.contains(p.x, p.y)) {
                                targetShape = shape;
                                break; // 一旦點中群組，就停止搜尋，不要去理會裡面的子物件
                            }
                        }
                    }
                    
                    // 2. 如果沒有點中群組，才去檢查一般的子物件
                    if (targetShape == null) {
                        for (int i = shapes.size() - 1; i >= 0; i--) {
                            Shape shape = shapes.get(i);
                            // 跳過群組（群組已經檢查過了）
                            if (!(shape instanceof CompositeShape)) {
                                if (shape.contains(p.x, p.y)) {
                                    targetShape = shape;
                                    break;
                                }
                            }
                        }
                    }

                    boolean isCtrlPressed = (e.getModifiersEx() & ActionEvent.CTRL_MASK) != 0; 
        
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
                    repaint(); 
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                dragging = true; // 開始拖曳

                if (tempStartPort != null) {
                    // 這是原本的畫線邏輯
                    tempLineEnd = e.getPoint();
                    repaint();
                } else if (currentMode.equals("select") && rubberBandStart != null) {
                    Point start = rubberBandStart;
                    Point current = e.getPoint();
                    int x = Math.min(start.x, current.x);
                    int y = Math.min(start.y, current.y);
                    int width = Math.abs(start.x - current.x);
                    int height = Math.abs(start.y - current.y);
                    rubberBandRect = new Rectangle(x, y, width, height);
                    repaint(); // 实时绘制框选矩形
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
                        for (Shape shape : shapes) {
                            // 判断图形中心点或边界是否在框选矩形内
                            // 这里使用 intersects 表示只要图形和框有重叠就算选中
                            // 如果需要完全包含，使用 finalRect.contains(shape.getBounds())
                            if (finalRect.intersects(shape.getBounds())) {
                                // 防止重复添加
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
                Shape hitShape = findShapeAt(p.x, p.y);
                
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
        }

        private Port findPortAt(int x, int y) {
            // 反向遍歷，讓最上層的圖形優先被檢查
            for (Shape shape : shapes) {
                
                // --- 邏輯 A: 檢查是否為 "群組內的子物件" ---
                boolean isInsideGroup = false;
                for (Shape s : shapes) {
                    if (s instanceof CompositeShape) {
                        CompositeShape group = (CompositeShape) s;
                        if (group.getChildren().contains(shape)) {
                            isInsideGroup = true;
                            break;
                        }
                    }
                }
                // 如果是群組內的子物件，跳過，不檢查它的 Port
                if (isInsideGroup) {
                    continue;
                }
                
                // --- 邏輯 B: 如果是群組 (CompositeShape) 或 普通物件 ---
                // 獲取該 Shape 的 Port 列表 (這裡會包含 CompositeShape，因為它也是 Shape)
                ArrayList<Port> ports = shapePortsMap.get(shape);
                if (ports != null) {
                    for (Port port : ports) {
                        if (port.contains(x, y)) {
                            return port; // 找到了！
                        }
                    }
                }
            }
            return null;
        }

        private Shape findShapeAt(int x, int y) {
            // 需求：深度值小的在上層 -> 後加入的通常深度值小 (或者我們遍歷時從最後一個開始)
            // 你的代碼已經是倒序，這符合需求 (Alternative C.1: 最上層先接收事件)
            for (int i = shapes.size() - 1; i >= 0; i--) {
                Shape shape = shapes.get(i);
                if (shape.contains(x, y)) {
                    return shape;
                }
            }

            // 再檢查 CompositeShape (如果基本物件沒點到，再看是否點到 Composite 的外框)
            // 注意：這只是一個簡易實現，實際上 Composite 應該優先於底層物件被選取
            for (int i = shapes.size() - 1; i >= 0; i--) {
                Object obj = shapes.get(i);
                if (obj instanceof CompositeShape) {
                    CompositeShape group = (CompositeShape) obj;
                    if (group.contains(x, y)) {
                        // 這裡我們無法返回 CompositeShape (因為類型是 Shape)，所以這個函數設計有侷限
                        // 這暗示了你最好將 Shape 和 CompositeShape 都統一為一個介面 (例如：GraphicalNode)
                        // 但為了讓你先跑起來，我們在 MouseHandler 裡直接重寫邏輯
                        // 這裡暫時返回 null，我們將在 MouseHandler 的 mousePressed 裡直接寫邏輯
                    }
                }
            }

            return null;
        }

        private void updateShapePorts(Shape shape) {
            ArrayList<Port> ports = new ArrayList<>();

            // 這裡的 logic 會自動適用於 CompositeShape，因為它繼承自 Rectangle
            if (shape instanceof Rectangle) { 
                // 這會包含普通的 Rectangle 和 CompositeShape
                for (int i = 0; i < 8; i++) {
                    ports.add(new Port(shape, i));
                }
            } else if (shape instanceof Ellipse2D) {
                for (int i = 0; i < 4; i++) {
                    ports.add(new Port(shape, i));
                }
            }   
            shapePortsMap.put(shape, ports);
        }

        public void groupSelected() {
            // Case D.1: 當有大於 2 (含) 個物件處於被 select 的狀態時。
            if (selectedShapes.size() >= 2) {
                // Step 1: 創建群組
                // 將目前選取的物件列表複製給群組作為子物件
                java.util.List<Object> tempChildren = new java.util.ArrayList<>(selectedShapes);
                CompositeShape group = new CompositeShape(tempChildren);
                
                // --- Step 2: 關鍵修改 - 不要移除子物件 ---
                // 原本的代碼會做 shapes.removeAll(selectedShapes)，這會讓子物件消失。
                // 現在，我們只做一件事：把 "群組框" 加到畫布上。
                shapes.add(group); // 修正點：只添加群組，不刪除子物件
                
                // --- Step 3: 更新選取狀態 ---
                // 清空原本的選擇（因為它們現在屬於群組了）
                selectedShapes.clear();
                // 選取這個新創建的群組框
                selectedShapes.add(group);
                
                // --- Step 4: 確保群組有 Port ---
                updateShapePorts(group); // 確保群組框有連接點
                
                repaint();
                System.out.println("Grouped " + tempChildren.size() + " objects. Sub-shapes remain on canvas.");
            }
            // Alternatives D.1: 當只有 1 個物件被選取時，點擊 Group 選項不會有任何動作。
        }

        public void ungroupSelected() {
            // 1. 確保只有一個物件被選取，且該物件是 CompositeShape
            if (selectedShapes.size() == 1) {
                Shape selected = selectedShapes.get(0);
                
                if (selected instanceof CompositeShape) {
                    CompositeShape group = (CompositeShape) selected;
                    
                    // --- 關鍵新增：刪除與該群組相關的線 ---
                    // 創建一個臨時列表來存放需要刪除的線
                    // (不能在遍歷時直接修改原列表，否則會拋出 ConcurrentModificationException)
                    java.util.List<Line> linesToRemove = new java.util.ArrayList<>();
                    
                    // 遍歷畫布上所有的線
                    for (Line line : lines) {
                        // 檢查線的起點或終點是否屬於這個群組
                        // Port 裡有一個 parentShape 屬性可以讓我們追溯
                        if ((line.start != null && line.start.parentShape == group) ||
                            (line.end != null && line.end.parentShape == group)) {
                            linesToRemove.add(line);
                        }
                    }
                    
                    // 從畫布的 lines 列表中移除這些線
                    lines.removeAll(linesToRemove);
                    // --- 關鍵新增結束 ---

                    // 2. 創建一個 List<Shape> 來存放子物件
                    java.util.List<Shape> validShapes = new java.util.ArrayList<>();
                    for (Object child : group.getChildren()) {
                        if (child instanceof Shape) {
                            validShapes.add((Shape) child);
                        }
                    }

                    // 3. 從畫布移除群組
                    shapes.remove(group); 
                    
                    // 4. 加回子物件
                    shapes.addAll(validShapes); 
                    
                    // 5. 更新選取狀態
                    selectedShapes.clear();
                    selectedShapes.addAll(validShapes); 
                    
                    repaint();
                    System.out.println("Ungrouped and removed " + linesToRemove.size() + " connected lines.");
                    return;
                }
            }
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
                    
                    // 計算矩形大小 (這裡用固定大小，或可以根據拖曳距離計算)
                    int width = 100;
                    int height = 60;
                    int x = canvasPoint.x - width / 2;
                    int y = canvasPoint.y - height / 2;
                    
                    if (currentMode.equals("oval")) {
                        canvasPanel.addShape(new Ellipse2D.Float(x, y, width, height));
                    } else if (currentMode.equals("rect")) {
                        canvasPanel.addShape(new Rectangle(x, y, width, height));
                    }

                    // 復原按鈕顏色
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
    // 存放這個群組裡的所有基本物件 (或巢狀群組)
    private java.util.List<Object> children;

    public CompositeShape(java.util.List<Object> children) {
        this.children = new java.util.ArrayList<>(children);
        // 修正：直接計算並設置自己的座標 (因為繼承了 Rectangle)
        calculateBounds();
    }

    public java.util.List<Object> getChildren() {
        return children;
    }

    // 計算包含所有子物件的最小矩形
    private void calculateBounds() {
        if (children.isEmpty()) {
            setBounds(0, 0, 0, 0); // 直接設置自己 (this)
            return;
        }
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (Object obj : children) {
            Rectangle childBounds = null;
            if (obj instanceof Shape) {
                // Shape 介面通常有 getBounds()
                childBounds = ((Shape) obj).getBounds();
            } else if (obj instanceof CompositeShape) {
                // CompositeShape 繼承自 Rectangle，所以它自己就是 bounds
                // 或者你可以強制轉型後調用 getBounds()，但直接轉型為 Rectangle 更簡單
                childBounds = (Rectangle) obj;
            }
            if (childBounds != null) {
                minX = Math.min(minX, childBounds.x);
                minY = Math.min(minY, childBounds.y);
                maxX = Math.max(maxX, childBounds.x + childBounds.width);
                maxY = Math.max(maxY, childBounds.y + childBounds.height);
            }
        }
        // 修正：直接設置當前對象 (this) 的大小
        setBounds(minX, minY, maxX - minX, maxY - minY);
    }
}