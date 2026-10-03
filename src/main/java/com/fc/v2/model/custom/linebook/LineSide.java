package com.fc.v2.model.custom.linebook;

/**
 * 线册管项：一位申报人身上牵着两头，各走各的线。
 *
 * <p>字面量与线册 {@code t_ich_pre_line.line_kind} 一栏同文，
 * 空管项的旧线不进这两头判定，只在老的"按线代号取值"那条路上躺着。
 *
 * @author fuce
 * @date 2026-10-03
 */
public enum LineSide {

    /** 本人从艺年数那一头 */
    YEARS("YEARS", "从艺年数"),

    /** 带徒人数那一头 */
    DISCIPLES("DISCIPLES", "带徒人数");

    private final String kindCode;
    private final String label;

    LineSide(String kindCode, String label) {
        this.kindCode = kindCode;
        this.label = label;
    }

    /** 与线册 line_kind 栏同文 */
    public String kindCode() {
        return kindCode;
    }

    /** 人话名目，回执里点名哪一头用 */
    public String label() {
        return label;
    }
}
