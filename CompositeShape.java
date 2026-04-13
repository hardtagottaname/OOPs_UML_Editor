import java.awt.*;
import java.util.ArrayList;
import java.util.List;

// import org.w3c.dom.css.Rect;

// composite
public class CompositeShape extends Rectangle implements ShapeInterface {

    // composite 裡面的物件列表
    private List<Object> children;

    // constructor
    public CompositeShape(List<Object> children) {
        this.children = new ArrayList<>(children);
        updateBoundsOnly();
    }

    // 圈出 composite 的左上和右下點     
    public void updateBoundsOnly() {

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (Object obj : children) {
            Rectangle bounds = null;

            // 如果是矩形，取矩形的邊界
            if (obj instanceof Rectangle) {
                bounds = ((Rectangle) obj).getBounds();
                System.out.println("Updating bounds for child: " + obj.getClass().getSimpleName() + " with bounds: " + bounds);
            } 
            // 如果是橢圓形，取橢圓形外面的邊界矩形
            else if (obj instanceof MutableOval) {
                bounds = ((MutableOval) obj).getBounds();
            }

            // 將剛剛得到的 bounds 用來更新 composite 的邊界
            if (bounds != null) {
                minX = Math.min(minX, bounds.x);
                minY = Math.min(minY, bounds.y);
                maxX = Math.max(maxX, bounds.x + bounds.width);
                maxY = Math.max(maxY, bounds.y + bounds.height);
            }
        }

        // 更新 composite 的寬跟高及位置
        width = maxX - minX;
        height = maxY - minY;
        this.x = minX;
        this.y = minY;
    }


    // 點擊 composite 的時候，檢查點是否在 composite 的邊界內
    // override 掉 ShapeInterface 的 conatins
    @Override
    public boolean contains(int mx, int my) {
        return super.contains(mx, my);
    }


    // override 掉 ShapeInterface 的 updateBounds，改變
    // @Override
    // public void updateBounds(int x, int y, int width, int height) {
    //     this.x = x;
    //     this.y = y;
    //     this.width = width;
    //     this.height = height;
    // }


    //  getter 
    public List<Object> getChildren() {
        return children;
    }
    
    @Override
    public Rectangle getBounds() {
        return this;
    }

}