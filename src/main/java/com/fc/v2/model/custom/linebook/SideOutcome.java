package com.fc.v2.model.custom.linebook;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 一头的判定回话：从艺年数、带徒人数各算各的，各钉各的线。
 *
 * <p>四种落点：
 * <ul>
 *   <li>{@link #STATE_JUDGED}：此刻在场的线找到了、申报数也像样，落在哪一层写明；</li>
 *   <li>{@link #STATE_INPUT_BAD}：这一头没填数或数字不像话，原话点名；</li>
 *   <li>{@link #STATE_NO_LINE}：这一刻够不着任何一条线，线代号与启用之日两栏空着；</li>
 *   <li>{@link #STATE_LINE_BAD}：在场头一条线立不起来（分界数缺栏/不像话），逐栏点名。</li>
 * </ul>
 *
 * @author fuce
 * @date 2026-10-03
 */
public class SideOutcome {

    /** 落点：判出来了（层级见 level） */
    public static final String STATE_JUDGED = "JUDGED";

    /** 落点：申报这头的数缺失/不像话 */
    public static final String STATE_INPUT_BAD = "INPUT_BAD";

    /** 落点：此刻够不着任何一条在场的线 */
    public static final String STATE_NO_LINE = "NO_LINE";

    /** 落点：在场头一条线三条分界数立不起来 */
    public static final String STATE_LINE_BAD = "LINE_BAD";

    /** 哪一头 */
    private final LineSide side;

    /** 申报原数（没填为 null） */
    private final BigDecimal input;

    /** 落点状态 */
    private final String state;

    /** 依哪条线判（无可用线时为空） */
    private final String lineCode;

    /** 那条线的启用之日（无可用线时为空） */
    private final Date effStart;

    /** 落在哪一层 1起分段 2过线段 3优线段 4越过优线（没判成时为 null） */
    private final Integer level;

    /** 给人的交代：缺哪栏、数怎么不像话、越了亮哪头的数 */
    private final String detail;

    private SideOutcome(LineSide side, BigDecimal input, String state,
                        String lineCode, Date effStart, Integer level, String detail) {
        this.side = side;
        this.input = input;
        this.state = state;
        this.lineCode = lineCode;
        this.effStart = effStart;
        this.level = level;
        this.detail = detail;
    }

    public static SideOutcome judged(LineSide side, BigDecimal input,
                                     String lineCode, Date effStart, int level) {
        return new SideOutcome(side, input, STATE_JUDGED, lineCode, effStart,
                Integer.valueOf(level), null);
    }

    public static SideOutcome inputBad(LineSide side, BigDecimal input, String detail) {
        return new SideOutcome(side, input, STATE_INPUT_BAD, null, null, null, detail);
    }

    public static SideOutcome noLine(LineSide side, BigDecimal input) {
        return new SideOutcome(side, input, STATE_NO_LINE, null, null, null,
                side.label() + "那一头此刻没有可用线");
    }

    public static SideOutcome lineBad(LineSide side, BigDecimal input,
                                      String lineCode, Date effStart, String detail) {
        return new SideOutcome(side, input, STATE_LINE_BAD, lineCode, effStart, null, detail);
    }

    /**
     * 从落库快照原样还原一头（回放旧结论用，不重新判）。字段以库中钉死的为准。
     */
    public static SideOutcome restore(LineSide side, BigDecimal input, String state,
                                      String lineCode, Date effStart, Integer level, String detail) {
        return new SideOutcome(side, input, state, lineCode, effStart, level, detail);
    }

    public LineSide getSide() {
        return side;
    }

    public BigDecimal getInput() {
        return input;
    }

    public String getState() {
        return state;
    }

    public String getLineCode() {
        return lineCode;
    }

    public Date getEffStart() {
        return effStart;
    }

    public Integer getLevel() {
        return level;
    }

    public String getDetail() {
        return detail;
    }

    /** 是不是越过了优线（落第 4 层） */
    public boolean isOver() {
        return STATE_JUDGED.equals(state) && level != null && level.intValue() == LineBook.LEVEL_OVER;
    }
}
