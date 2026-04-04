import java.awt.Rectangle;

/**
 * 形狀介面：定義所有形狀物件必須實現的方法
 * 這樣可以統一處理不同類型的形狀，如矩形、橢圓、複合形狀等
 */
public interface ShapeInterface {
    /**
     * 檢查給定點是否在形狀內
     * @param x X座標
     * @param y Y座標
     * @return 如果點在形狀內返回true，否則false
     */
    boolean contains(int x, int y);

    /**
     * 獲取形狀的邊界矩形
     * @return 形狀的邊界矩形
     */
    Rectangle getBounds();

    /**
     * 更新形狀的位置和大小
     * @param x 新X座標
     * @param y 新Y座標
     * @param width 新寬度
     * @param height 新高度
     */
    void updateBounds(int x, int y, int width, int height);
}