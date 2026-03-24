import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;


public class Main extends JFrame {
    private CanvasPanel canvas;
    private JPanel toolBar;
    private ArrayList<JButton> modeButtons;
    private String currentMode = "Select";

    public Main() {
        setTitle("OOPs UML Editor");
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setSize(1200, 900);
        setResizable(false);
        
        canvas = new CanvasPanel();
        initToolBar();

        setLayout(new BorderLayout());
        add(canvas, BorderLayout.CENTER);
        add(toolBar, BorderLayout.WEST);

        setVisible(true);
    }

    private void initToolBar() {
        toolBar = new JPanel();

        toolBar.setLayout(new BoxLayout(toolBar, BoxLayout.Y_AXIS));
        toolBar.setPreferredSize(new Dimension(120, getHeight()));
        toolBar.setBorder(BorderFactory.createEtchedBorder());
        
        String[] modes = {"Select", "Association", "Generalization", "Composition", "Rect", "Oval"};
        modeButtons = new ArrayList<>();

        for (String mode: modes) {
            JButton button = new JButton(mode);
            button.setAlignmentX(Component.CENTER_ALIGNMENT);

            button.addActionListener(e -> {
                System.out.println(mode + " mode selected"); 
                setMode(mode);
            });

            modeButtons.add(button);
            toolBar.add(button);

            toolBar.add(Box.createVerticalStrut(10));
        }
    }

    private void setMode(String mode) {
        this.currentMode = mode;
        canvas.setMode(mode);
        updateButtonColors();
    }

    private void updateButtonColors() {
        for (JButton button : modeButtons) {
            if (button.getText().equals(currentMode)) {
                button.setBackground(Color.BLACK);
                button.setFont(new Font(button.getFont().getName(), Font.BOLD, 12));
                button.setForeground(Color.WHITE);
            } else {
                button.setBackground(null);
                button.setForeground(null);
                button.setFont(new Font(button.getFont().getName(), Font.PLAIN, 12));
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Main());    
    }
}

class CanvasPanel extends JPanel {
    private String currentMode = "Select";

    public CanvasPanel() {
        this.setBackground(Color.LIGHT_GRAY);
    }

    public void setCurrentMode(String mode) {
        this.currentMode = mode;
    }

    public void setMode(String mode) {
        this.currentMode = mode;
    }
}
