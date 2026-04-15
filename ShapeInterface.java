import java.awt.Rectangle;

// 矩形或橢圓形或組合要實作的 methods
public interface ShapeInterface {
    
    // contains => 點擊的點是否在形狀內
    boolean contains(int x, int y);

    // getBounds => 取得矩形或橢圓形或組合的邊界矩形
    Rectangle getBounds();
}