import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * UML 編輯器的主視窗。
 *
 * 負責建立 Swing 視窗、左側工具列、上方選單，以及把使用者在工具列上的操作
 * 轉交給 CanvasPanel 執行。
 */
public class Main extends JFrame {
    // 左側垂直工具列，放 select、line、shape 等工具按鈕。
    private JPanel buttonPanel;
    // 中央畫布，所有 UML 圖形、連線、選取與拖曳都在這裡處理。
    private CanvasPanel canvasPanel;
    // 目前常駐啟用的工具按鈕，例如 select 或 association。
    private JButton lastActiveButton;
    // 建立圖形時暫存前一個工具，放開滑鼠後會切回它。
    private JButton previousActiveButton;

    public static void main(String[] args) {
        // Swing 元件必須在 Event Dispatch Thread 上建立與更新。
        SwingUtilities.invokeLater(() -> new Main().setVisible(true));
    }

    public Main() {
        setTitle("OOPS UML Editor");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 700);
        setLocationRelativeTo(null);
        setResizable(false);
        setLayout(new BorderLayout());

        initializeComponents();
        setupMenuBar();

        add(buttonPanel, BorderLayout.WEST);
        add(canvasPanel, BorderLayout.CENTER);
    }

    /**
     * 建立畫布與左側工具按鈕。
     *
     * rect / oval 採用「按住工具按鈕、拖到畫布放開」的建立方式，
     * 其他工具則是點一下後持續切換模式。
     */
    private void initializeComponents() {
        canvasPanel = new CanvasPanel();
        canvasPanel.setBackground(Color.LIGHT_GRAY);

        buttonPanel = new JPanel(new GridLayout(6, 1, 5, 5));
        buttonPanel.setPreferredSize(new Dimension(120, 0));

        String[] labels = {"select", "association", "generalization", "composition", "rect", "oval"};
        for (String label : labels) {
            JButton button = new JButton(label);
            button.addMouseListener(new ToolButtonHandler(button));
            buttonPanel.add(button);
            if ("select".equals(label)) {
                lastActiveButton = button;
                setButtonActive(button, true);
            }
        }
    }

    /**
     * 建立選單列，目前 Edit 選單提供群組、解散群組與修改標籤樣式。
     */
    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        Font menuFont = new Font("Arial", Font.PLAIN, 14);

        JMenu fileMenu = new JMenu("File");
        fileMenu.setFont(menuFont);

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

    /**
     * 顯示標籤設定對話框，讓使用者修改單一基本圖形的名稱與標籤底色。
     */
    private void showLabelDialog() {
        if (canvasPanel.getSelectedShapes().size() != 1) {
            JOptionPane.showMessageDialog(this, "Please select exactly one basic object.");
            return;
        }

        UMLObject object = canvasPanel.getSelectedShapes().get(0);
        if (object instanceof CompositeShape) {
            JOptionPane.showMessageDialog(this, "Label can only be customized for a basic object.");
            return;
        }

        JTextField nameField = new JTextField(object.getName());
        JButton colorButton = new JButton("Choose");
        final Color[] selectedColor = {object.getLabelColor()};
        colorButton.setBackground(selectedColor[0]);
        colorButton.addActionListener(e -> {
            Color color = JColorChooser.showDialog(this, "Choose Label Color", selectedColor[0]);
            if (color != null) {
                selectedColor[0] = color;
                colorButton.setBackground(color);
            }
        });

        JPanel panel = new JPanel(new GridLayout(2, 2, 8, 8));
        panel.add(new JLabel("Label Name"));
        panel.add(nameField);
        panel.add(new JLabel("Label Color"));
        panel.add(colorButton);

        int result = JOptionPane.showConfirmDialog(
                this,
                panel,
                "Customize Label Style",
                JOptionPane.OK_CANCEL_OPTION
        );

        if (result == JOptionPane.OK_OPTION) {
            object.setName(nameField.getText());
            object.setLabelColor(selectedColor[0]);
            canvasPanel.repaint();
        }
    }

    /**
     * 用黑底白字表示工具按鈕啟用，否則恢復系統預設按鈕顏色。
     */
    private void setButtonActive(JButton button, boolean active) {
        if (active) {
            button.setBackground(Color.BLACK);
            button.setForeground(Color.WHITE);
        } else {
            button.setBackground(UIManager.getColor("Button.background"));
            button.setForeground(UIManager.getColor("Button.foreground"));
        }
    }

    /**
     * 清除所有工具按鈕的視覺啟用狀態。
     */
    private void clearButtonStates() {
        for (Component component : buttonPanel.getComponents()) {
            if (component instanceof JButton) {
                setButtonActive((JButton) component, false);
            }
        }
    }

    /**
     * 切換目前工具，並同步更新按鈕狀態與畫布模式。
     */
    private void activateTool(JButton button) {
        clearButtonStates();
        setButtonActive(button, true);
        lastActiveButton = button;
        canvasPanel.setCurrentMode(button.getText());
    }

    /**
     * 工具按鈕的滑鼠處理器。
     *
     * 一般工具按下後直接切換模式；圖形工具則在按下時暫時啟用，
     * 放開時若位置落在畫布內就建立圖形，最後回復到前一個工具。
     */
    private class ToolButtonHandler extends MouseAdapter {
        private final JButton button;

        ToolButtonHandler(JButton button) {
            this.button = button;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            String mode = button.getText();
            if ("rect".equals(mode) || "oval".equals(mode)) {
                previousActiveButton = lastActiveButton;
                clearButtonStates();
                setButtonActive(button, true);
                canvasPanel.setCurrentMode(mode);
            } else {
                previousActiveButton = null;
                activateTool(button);
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            String mode = button.getText();
            if (!"rect".equals(mode) && !"oval".equals(mode)) {
                return;
            }

            Point canvasPoint = SwingUtilities.convertPoint(button, e.getPoint(), canvasPanel);
            if (canvasPanel.contains(canvasPoint)) {
                canvasPanel.createShapeAt(mode, canvasPoint);
            }

            clearButtonStates();
            if (previousActiveButton != null) {
                setButtonActive(previousActiveButton, true);
                lastActiveButton = previousActiveButton;
                canvasPanel.setCurrentMode(previousActiveButton.getText());
            } else {
                Component first = buttonPanel.getComponent(0);
                if (first instanceof JButton) {
                    lastActiveButton = (JButton) first;
                    setButtonActive(lastActiveButton, true);
                    canvasPanel.setCurrentMode("select");
                }
            }
            previousActiveButton = null;
        }
    }
}
