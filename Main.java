import javax.swing.*;
import java.awt.*;
import java.awt.event.*;


// 初始化介面和整體的狀態
public class Main extends JFrame {
    // UI
    private JPanel buttonPanel;
    private CanvasPanel canvasPanel;

    // 按鈕
    // 紀錄上一個按的是誰
    private JButton lastActiveButton = null;
    private JButton previousActiveButton = null;

    // main function 
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new Main().setVisible(true); // 讓整個 UI visible
        });
    }

    // constructor
    public Main() {
        setTitle("OOPS_UML_Editor");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // 按下叉叉就離開 
        setSize(1200, 700);
        setLayout(new BorderLayout()); // 把介面分成北中南西東

        initializeComponents();
        setupMenuBar();

        add(buttonPanel, BorderLayout.WEST);
        add(canvasPanel, BorderLayout.CENTER);
    }

    // 初始化 UI
    private void initializeComponents() {
        canvasPanel = new CanvasPanel();
        canvasPanel.setBackground(Color.LIGHT_GRAY);

        buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(6, 1, 5, 5)); // 讓 button 變成一個 column，六個元素，六個平均分配在 column 中，每個垂直水平間隔 5 pixel
        buttonPanel.setPreferredSize(new Dimension(120, 0)); // 讓 buttons 的寬度是 120 pixel，高度充滿父元素

        String[] buttonLabels = {"select", "association", "generalization", "composition", "rect", "oval"};

        // 把 button 加進 buttonPanel
        for (String label : buttonLabels) {
            JButton btn = new JButton(label);
            btn.addActionListener(new ButtonClickListener());
            btn.addMouseListener(new ButtonMouseHandler(btn));
            buttonPanel.add(btn);
        }
    }

    // 初始化 menuBar
    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        Font menuFont = new Font("Arial", Font.PLAIN, 14);

        // 檔案選單
        JMenu fileMenu = new JMenu("File");
        fileMenu.setFont(menuFont);

        // JMenuItem newItem = new JMenuItem("new");
        // JMenuItem openItem = new JMenuItem("open");
        // JMenuItem saveItem = new JMenuItem("save");
        
        // fileMenu.add(newItem);
        // fileMenu.add(openItem);
        // fileMenu.add(saveItem);

        // 編輯選單
        JMenu editMenu = new JMenu("Edit");
        editMenu.setFont(menuFont);

        JMenuItem groupItem = new JMenuItem("Group");
        JMenuItem ungroupItem = new JMenuItem("Ungroup");
        JMenuItem labelItem = new JMenuItem("Label");

        groupItem.addActionListener(e -> canvasPanel.groupSelected());
        ungroupItem.addActionListener(e -> canvasPanel.ungroupSelected());
        labelItem.addActionListener(e -> showLabelDialog());

        editMenu.add(groupItem);
        editMenu.add(ungroupItem);
        editMenu.add(labelItem);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);

        setJMenuBar(menuBar);
    }

    // 顯示 edit -> label 時
    private void showLabelDialog() {
        if (canvasPanel.getSelectedShapes().size() != 1) {
            JOptionPane.showMessageDialog(this, "要選一個物件!");
            return;
        }

        Object shape = canvasPanel.getSelectedShapes().get(0);

        String currentName = canvasPanel.getShapeLabels().get(shape);
        Color currentColor = canvasPanel.getShapeLabelColors().get(shape);

        JTextField nameField = new JTextField(currentName);

        JButton colorButton = new JButton("選色");
        final Color[] selectedColor = {currentColor}; // 要讓後面的 lambda 能夠修改 selectedColor，所以要用 final

        colorButton.addActionListener(e -> {
            Color c = JColorChooser.showDialog(this, "選色", currentColor);
            if (c != null) {
                selectedColor[0] = c;
            }
        });

        JPanel panel = new JPanel(new GridLayout(2, 2));
        
        panel.add(new JLabel("Name"));
        panel.add(nameField);

        panel.add(new JLabel("Color"));
        panel.add(colorButton);

        int result = JOptionPane.showConfirmDialog(
            this,
            panel,
            "Customize Label Style",
            JOptionPane.OK_CANCEL_OPTION
        ); // 選顏色的小視窗

        // 如果確認，就更新 label string 和 color
        if (result == JOptionPane.OK_OPTION) {
            canvasPanel.getShapeLabels().put(shape, nameField.getText());
            canvasPanel.getShapeLabelColors().put(shape, selectedColor[0]);
            canvasPanel.repaint();
        }
    }

    // 處理因為被按下 rect 或 oval 而變色的按鈕
    private void resetButtonColors() {
        for (Component comp : buttonPanel.getComponents()) {
            if (comp instanceof JButton) {
                JButton btn = (JButton) comp;

                // 將按鈕變回預設
                btn.setBackground(UIManager.getColor("Button.background"));
                btn.setForeground(UIManager.getColor("Button.foreground"));
            }
        }

        // 如果前一個被按下的按鈕不是 null，則更新 lasActiveButton 為他，否則為 null
        if (previousActiveButton != null) {
            lastActiveButton = previousActiveButton;
        } else {
            lastActiveButton = null;
        }

        String mode = (previousActiveButton != null) ? previousActiveButton.getText() : "select"; // 將 mode 設為 previousActiveButton 否則設為 select
        canvasPanel.setCurrentMode(mode);
        previousActiveButton = null; // 更新 previousActiveButton

        System.out.println("Mode reverted to: " + mode);
    }

    // 這裡只處理 rect 和 oval， CanvasPanel 才處理其他
    private class ButtonMouseHandler extends MouseAdapter {
        private JButton button;

        public ButtonMouseHandler(JButton button) {
            this.button = button;
        }

        // 滑鼠按下時
        @Override
        public void mousePressed(MouseEvent e) {
            String buttonText = button.getText();

            // 如果是按下 rect 或 oval，要復原回前一個按鈕
            if ("rect".equals(buttonText) || "oval".equals(buttonText)) {
                previousActiveButton = lastActiveButton;

                // 其他按鈕維持預設樣式
                if (lastActiveButton != null) {
                    System.out.println("Resetting button: " + lastActiveButton.getText());
                    lastActiveButton.setBackground(UIManager.getColor("Button.background"));
                    lastActiveButton.setForeground(UIManager.getColor("Button.foreground"));
                }

                // rect 或 oval 變黑色
                button.setBackground(Color.BLACK);
                button.setForeground(Color.WHITE);
                lastActiveButton = button;
            }

            canvasPanel.setCurrentMode(buttonText);
            System.out.println("Mode changed to: " + buttonText);
        }

        // 滑鼠放開時
        @Override
        public void mouseReleased(MouseEvent e) {
            String mode = canvasPanel.getCurrentMode();
            
            if ("rect".equals(mode) || "oval".equals(mode)) {
                Point canvasPoint = SwingUtilities.convertPoint(button, e.getPoint(), canvasPanel); // 把滑鼠在 button 上的位置轉成在 canvas 上的位置 convertPoint(來源元素, 點, 目標元素)

                int width = 100;
                int height = 60;
                int x = canvasPoint.x - width / 2;
                int y = canvasPoint.y - height / 2;

                if ("oval".equals(mode)) {
                    canvasPanel.addShape(new MutableOval(x, y, width, height));
                } else if ("rect".equals(mode)) {
                    canvasPanel.addShape(new Rectangle(x, y, width, height));
                }

                resetButtonColors(); // 更新 button 顏色和 previousbutton, lastbutton 狀態
            }
        }
    }

    // 處理滑鼠完整點擊 (因為 select 相對簡單，不需要紀錄按下和放開等等，用 actionlistener 就好)
    private class ButtonClickListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String buttonName = e.getActionCommand();

            if ("select".equals(buttonName)) {
                resetButtonColors();
                canvasPanel.setCurrentMode("select");
            }
        }
    }
}