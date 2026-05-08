/**
 * UML 連線種類的列舉。
 *
 * 目前畫布邏輯主要用按鈕文字字串判斷模式，這個 enum 可作為型別安全的線段種類定義。
 */
public enum LineType {
    // 一般關聯線。
    ASSOCIATION,
    // 泛化 / 繼承線。
    GENERALIZATION,
    // 組合線。
    COMPOSITION
}
