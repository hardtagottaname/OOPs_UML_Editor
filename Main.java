import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Main類別：UML編輯器的主視窗
 * 負責初始化應用程式介面和管理整體狀態
 */
public class Main extends JFrame {
    // UI元件
    private JPanel buttonPanel;
    private CanvasPanel canvasPanel;

    // 按鈕狀態管理
    private JButton lastActiveButton = null;
    private JButton previousActiveButton = null;

    /**
     * 主程式進入點
     * @param args 命令列參數
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new Main().setVisible(true);
        });
    }

    /**
     * 建構子：初始化主視窗
     */
    public Main() {
        setTitle("OOPS_UML_Editor");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 700);
        setLayout(new BorderLayout());

        initializeComponents();
        setupMenuBar();

        add(buttonPanel, BorderLayout.WEST);
        add(canvasPanel, BorderLayout.CENTER);
    }

    /**
     * 初始化UI元件
     */
    private void initializeComponents() {
        canvasPanel = new CanvasPanel();
        canvasPanel.setBackground(Color.LIGHT_GRAY);

        buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(6, 1, 5, 5));
        buttonPanel.setPreferredSize(new Dimension(120, 0));

        String[] buttonLabels = {"select", "association", "generalization", "composition", "rect", "oval"};

        for (String label : buttonLabels) {
            JButton btn = new JButton(label);
            btn.addActionListener(new ButtonClickListener());
            btn.addMouseListener(new ButtonMouseHandler(btn));
            buttonPanel.add(btn);
        }
    }

    /**
     * 設定選單列
     */
    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        Font menuFont = new Font("Arial", Font.PLAIN, 14);

        // 檔案選單
        JMenu fileMenu = new JMenu("file");
        fileMenu.setFont(menuFont);

        JMenuItem newItem = new JMenuItem("new");
        JMenuItem openItem = new JMenuItem("open");
        JMenuItem saveItem = new JMenuItem("save");
        fileMenu.add(newItem);
        fileMenu.add(openItem);
        fileMenu.add(saveItem);

        // 編輯選單
        JMenu editMenu = new JMenu("edit");
        editMenu.setFont(menuFont);

        JMenuItem groupItem = new JMenuItem("group");
        JMenuItem ungroupItem = new JMenuItem("ungroup");
        JMenuItem labelItem = new JMenuItem("label");

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

    /**
     * 顯示標籤設定對話框
     */
    private void showLabelDialog() {
        if (canvasPanel.getSelectedShapes().size() != 1) {
            JOptionPane.showMessageDialog(this, "Please select one object.");
            return;
        }

        Object shape = canvasPanel.getSelectedShapes().get(0);

        String currentName = canvasPanel.getShapeLabels().get(shape);
        Color currentColor = canvasPanel.getShapeLabelColors().get(shape);

        JTextField nameField = new JTextField(currentName);

        JButton colorButton = new JButton("Choose Color");
        final Color[] selectedColor = {currentColor};

        colorButton.addActionListener(e -> {
            Color c = JColorChooser.showDialog(this, "Pick Color", currentColor);
            if (c != null) {
                selectedColor[0] = c;
            }
        });

        JPanel panel = new JPanel(new GridLayout(2, 2));
        panel.add(new JLabel("Label Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Label Color:"));
        panel.add(colorButton);

        int result = JOptionPane.showConfirmDialog(
            this,
            panel,
            "Customize Label Style",
            JOptionPane.OK_CANCEL_OPTION
        );

        if (result == JOptionPane.OK_OPTION) {
            canvasPanel.getShapeLabels().put(shape, nameField.getText());
            canvasPanel.getShapeLabelColors().put(shape, selectedColor[0]);
            canvasPanel.repaint();
        }
    }

    /**
     * 重設按鈕顏色
     */
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
            lastActiveButton = null;
        }

        String mode = (previousActiveButton != null) ? previousActiveButton.getText() : "select";
        canvasPanel.setCurrentMode(mode);
        previousActiveButton = null;

        System.out.println("Mode reverted to: " + mode);
    }

    /**
     * ButtonMouseHandler類別：處理按鈕的滑鼠事件
     */
    private class ButtonMouseHandler extends MouseAdapter {
        private JButton button;

        public ButtonMouseHandler(JButton button) {
            this.button = button;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            String buttonText = button.getText();

            if ("rect".equals(buttonText) || "oval".equals(buttonText)) {
                previousActiveButton = lastActiveButton;

                if (lastActiveButton != null) {
                    System.out.println("Resetting button: " + lastActiveButton.getText());
                    lastActiveButton.setBackground(UIManager.getColor("Button.background"));
                    lastActiveButton.setForeground(UIManager.getColor("Button.foreground"));
                }

                button.setBackground(Color.BLACK);
                button.setForeground(Color.WHITE);
                lastActiveButton = button;
            }

            canvasPanel.setCurrentMode(buttonText);
            System.out.println("Mode changed to: " + buttonText);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            String mode = canvasPanel.getCurrentMode();
            if ("rect".equals(mode) || "oval".equals(mode)) {
                Point canvasPoint = SwingUtilities.convertPoint(button, e.getPoint(), canvasPanel);
                int width = 100;
                int height = 60;
                int x = canvasPoint.x - width / 2;
                int y = canvasPoint.y - height / 2;

                if ("oval".equals(mode)) {
                    canvasPanel.addShape(new MutableOval(x, y, width, height));
                } else if ("rect".equals(mode)) {
                    canvasPanel.addShape(new Rectangle(x, y, width, height));
                }
                resetButtonColors();
            }
        }
    }

    /**
     * ButtonClickListener類別：處理按鈕點擊事件
     */
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