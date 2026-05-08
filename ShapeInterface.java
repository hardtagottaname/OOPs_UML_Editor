import java.awt.Rectangle;

/**
 * 舊版可點選圖形的簡單介面。
 *
 * 目前主要 UML 編輯器已改用 UMLObject；這個介面仍保留給 MutableOval 這類
 * 較簡單的形狀資料結構使用。
 */
public interface ShapeInterface {

    /** 判斷指定座標是否落在圖形內。 */
    boolean contains(int x, int y);

    /** 回傳圖形外接矩形。 */
    Rectangle getBounds();
}
