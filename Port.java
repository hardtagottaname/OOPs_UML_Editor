import java.awt.*;

public class Port {

    // x, y => port 座標
    // parentShape => port 所屬的形狀 (矩形或橢圓)
    // type => port 的類型 (矩形有 8 個 port，橢圓有 4 個 port)
    private int x, y;
    private Object parentShape;
    private int type; 

    // constructor
    public Port(Object parentShape, int type) {
        this.parentShape = parentShape;
        this.type = type;
        updatePosition();
    }

    
    public void updatePosition() {

        // System.out.println("Updating port position for type " + type + " of parent shape: " + parentShape.getClass().getSimpleName());

        // 若為矩形
        if (parentShape instanceof Rectangle) {

            // 將 Object 轉型為 Rectangle 並獲取其邊界
            Rectangle bounds = ((Rectangle) parentShape).getBounds();
            int cx = bounds.x + bounds.width / 2;
            int cy = bounds.y + bounds.height / 2;

            // 矩形：8個方向 (0-7) => 順時鐘
            switch (type) {
                case 0: // 左上
                    x = bounds.x;
                    y = bounds.y;
                    break;
                case 1: // 上中
                    x = cx;
                    y = bounds.y;
                    break;
                case 2: // 右上
                    x = bounds.x + bounds.width;
                    y = bounds.y;
                    break;
                case 3: // 右中
                    x = bounds.x + bounds.width;
                    y = cy;
                    break;
                case 4: // 右下
                    x = bounds.x + bounds.width;
                    y = bounds.y + bounds.height;
                    break;
                case 5: // 下中
                    x = cx;
                    y = bounds.y + bounds.height;
                    break;
                case 6: // 左下
                    x = bounds.x;
                    y = bounds.y + bounds.height;
                    break;
                case 7: // 左中
                    x = bounds.x;
                    y = cy;
                    break;
                default: // 防呆
                    x = cx;
                    y = cy; 
            }
        } 
        // 若為橢圓形
        else if (parentShape instanceof MutableOval) {

            // 將 Object 轉型為 MutableOval 並獲取其中心點
            MutableOval oval = (MutableOval) parentShape;
            int cx = oval.getX() + oval.getWidth() / 2;
            int cy = oval.getY() + oval.getHeight() / 2;

            // 橢圓：4個方向 (0-3) => 順時鐘
            switch (type) {
                case 0: // 上
                    x = cx; 
                    y = oval.getY(); 
                    break; 
                case 1: // 右
                    x = oval.getX() + oval.getWidth(); 
                    y = cy; 
                    break;  
                case 2: // 下
                    x = cx; 
                    y = oval.getY() + oval.getHeight(); 
                    break; 
                case 3: // 左
                    x = oval.getX(); 
                    y = cy; 
                    break; 
                default: // 防呆
                    x = cx; 
                    y = cy;
            }
        }
    }

    
    public boolean contains(int mx, int my) {
        return Math.abs(mx - x) <= 5 && Math.abs(my - y) <= 5;
    }

    // Getter 和 Setter 方法
    public int getX() { return x; }
    public int getY() { return y; }
    public Object getParentShape() { return parentShape; }
    public int getType() { return type; }

    public void setX(int x) { this.x = x; }
    public void setY(int y) { this.y = y; }
    public void setParentShape(Object parentShape) { this.parentShape = parentShape; }
    public void setType(int type) { this.type = type; }
}