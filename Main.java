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
            
            for (Shape shape : shapes) {
                g2d.draw(shape);
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
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (tempStartPort != null) {
                    tempLineEnd = e.getPoint();
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
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
                }
            }
        }

        private Port findPortAt(int x, int y) {
            // 反向遍歷，讓最上層的圖形優先被檢查
            for (Shape shape : shapes) {
                // 获取该 Shape 对应的 Ports 列表
                ArrayList<Port> ports = shapePortsMap.get(shape);
                if (ports != null) {
                    for (Port port : ports) {
                        if (port.contains(x, y)) {
                            return port; // 找到了，返回该 Port
                        }
                    }
                }
            }
            return null; // 没找到
        }

        private void updateShapePorts(Shape shape) {
            ArrayList<Port> ports = new ArrayList<>();
            Rectangle bounds = shape.getBounds();
            
            // 根据 Shape 类型创建不同数量的 Port
            if (shape instanceof Rectangle) {
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
