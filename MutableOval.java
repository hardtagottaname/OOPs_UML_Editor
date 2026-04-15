import java.awt.Rectangle;

// 自定義橢圓形
public class MutableOval implements ShapeInterface {

    // x, y 是橢圓的左上角座標(包住橢圓形的那塊矩形的最左上角的點)
    private int x, y, width, height;

    // contructor
    public MutableOval(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    // 點擊的點是否在橢圓範圍內 (繼承 ShapeInterface)
    @Override
    public boolean contains(int mx, int my) {
        if (mx < x || mx > x + width || my < y || my > y + height) {
            return false;
        }

        double rx = width / 2.0;
        double ry = height / 2.0;
        double cx = x + rx;
        double cy = y + ry;
        double dx = mx - cx;
        double dy = my - cy;

        return ((dx * dx) / (rx * rx) + (dy * dy) / (ry * ry)) <= 1;
    }

    
    // 取得橢圓形最外面的矩形邊界 (繼承 ShapeInterface)
    @Override
    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }

    // getter
    public int getX() { 
        return x; 
    }
    
    public int getY() { 
        return y; 
    }
    
    public int getWidth() { 
        return width; 
    }
    
    public int getHeight() { 
        return height; 
    }
    

    //setter
    public void setX(int x) { 
        this.x = x; 
    }
    
    public void setY(int y) { 
        this.y = y; 
    }
    
    public void setWidth(int width) { 
        this.width = width; 
    }
    
    public void setHeight(int height) { 
        this.height = height; 
    }
}