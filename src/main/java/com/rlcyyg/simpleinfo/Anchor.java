package com.rlcyyg.simpleinfo;

import java.util.Locale;

/**
 * 角锚点：信息项相对屏幕四角的贴靠方式。
 * <p>
 * 配置里存字符串枚举名（如 {@code "TOP_RIGHT"}）而不是裸 int，
 * 这样手改 toml 时不易写错，且 toml 文件自解释。
 */
public enum Anchor {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT;

    /** 语言文件 key：{@code gui.simpleinfo.anchor.top_left} */
    public String translationKey() {
        return "gui.simpleinfo.anchor." + name().toLowerCase(Locale.ROOT);
    }

    /** 水平方向是否靠右。 */
    public boolean isRight() {
        return this == TOP_RIGHT || this == BOTTOM_RIGHT;
    }

    /** 垂直方向是否靠下。 */
    public boolean isBottom() {
        return this == BOTTOM_LEFT || this == BOTTOM_RIGHT;
    }

    /**
     * 宽松解析字符串，非法值一律回退到 {@link #TOP_LEFT}。
     * <p>
     * 配置正常情况下已被 {@code defineInList} 校验过，这里只是兜底 ——
     * 配置文件被手改坏时不应该让 HUD 直接崩掉。
     */
    public static Anchor parse(String name) {
        if (name != null) {
            for (Anchor a : values()) {
                if (a.name().equalsIgnoreCase(name.trim())) {
                    return a;
                }
            }
        }
        return TOP_LEFT;
    }
}
