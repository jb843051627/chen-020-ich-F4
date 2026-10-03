package com.fc.v2.model.custom.linebook;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 一位申报人两头并到一处的判定回话——服务层那个唯一判据出口的原话，
 * 落库的线代号、启用之日、各层结果全以它为准，页面不许先摆答案再回头补依据。
 *
 * <p>三态：
 * <ul>
 *   <li>{@link #STATUS_PASS}：从艺年数、带徒人数两头各自都找到了线、数都像样，且都没越过优线；</li>
 *   <li>{@link #STATUS_FAIL}：两头都能判，但至少有一头越过优线；或某一头申报数不像话；
 *       或在场头一条线立不起来——逐头把那一头的数落/缺栏点出来；</li>
 *   <li>{@link #STATUS_NO_LINE}：有头够不着任何一条线。按题面这头两栏（线代号、启用之日）空着，
 *       不许抓邻近那条的数凑一个像样答复。</li>
 * </ul>
 *
 * @author fuce
 * @date 2026-10-03
 */
public class LineVerdict {

    /** 两头各自都不越，过 */
    public static final String STATUS_PASS = "PASS";

    /** 有头越了 / 申报数不像话 / 在场线立不起来 */
    public static final String STATUS_FAIL = "FAIL";

    /** 有头此刻够不着任何一条线，不给结论 */
    public static final String STATUS_NO_LINE = "NO_LINE";

    /** 申报人代号 */
    private final String applicantCode;

    /** 申请注明的那一刻（判定就按这一刻回看） */
    private final Date applyAt;

    /** 从艺年数那一头 */
    private final SideOutcome years;

    /** 带徒人数那一头 */
    private final SideOutcome disciples;

    /** 落库时钉死的逐头交代（回放旧笔用；新算为 null，现算现给） */
    private final String recordedDetail;

    private LineVerdict(String applicantCode, Date applyAt,
                        SideOutcome years, SideOutcome disciples, String recordedDetail) {
        this.applicantCode = applicantCode;
        this.applyAt = applyAt;
        this.years = years;
        this.disciples = disciples;
        this.recordedDetail = recordedDetail;
    }

    public static LineVerdict of(String applicantCode, Date applyAt,
                                 SideOutcome years, SideOutcome disciples) {
        return new LineVerdict(applicantCode, applyAt, years, disciples, null);
    }

    /**
     * 从落库快照原样还原整笔结论（不回算）：三态与逐头字段全以库里钉死的为准，
     * 线代号、启用之日不随今天的线册变动。
     */
    public static LineVerdict restore(String applicantCode, Date applyAt,
                                      SideOutcome years, SideOutcome disciples,
                                      String recordedDetail) {
        return new LineVerdict(applicantCode, applyAt, years, disciples, recordedDetail);
    }

    public String getApplicantCode() {
        return applicantCode;
    }

    public Date getApplyAt() {
        return applyAt;
    }

    public SideOutcome getYears() {
        return years;
    }

    public SideOutcome getDisciples() {
        return disciples;
    }

    public SideOutcome side(LineSide side) {
        return side == LineSide.DISCIPLES ? disciples : years;
    }

    /**
     * 三态怎么落：只要有一头够不着线，整笔不给结论（NO_LINE）；
     * 其余情形下有一头没判成（数不像话/线立不起来）或越过优线，即 FAIL；两头都不越才 PASS。
     */
    public String getStatus() {
        if (SideOutcome.STATE_NO_LINE.equals(years.getState())
                || SideOutcome.STATE_NO_LINE.equals(disciples.getState())) {
            return STATUS_NO_LINE;
        }
        if (years.isOver() || disciples.isOver()
                || !SideOutcome.STATE_JUDGED.equals(years.getState())
                || !SideOutcome.STATE_JUDGED.equals(disciples.getState())) {
            return STATUS_FAIL;
        }
        return STATUS_PASS;
    }

    public boolean isPass() {
        return STATUS_PASS.equals(getStatus());
    }

    /**
     * 逐头交代：哪头越了亮哪头的数与所在层；没判成的把缺栏/不像话原话摆出。
     * 全过或整笔无线（无线那头各栏已空）时，这里给一句人头能对上的总述。
     */
    public String getFailDetail() {
        // 回放旧笔：逐头提示是哪一笔的就摆哪一笔，不许因换了线而在页面上走样
        if (recordedDetail != null) {
            return recordedDetail;
        }
        List<String> parts = new ArrayList<>();
        appendDetail(parts, years);
        appendDetail(parts, disciples);
        if (parts.isEmpty()) {
            return null;
        }
        return String.join("；", parts);
    }

    private void appendDetail(List<String> parts, SideOutcome o) {
        String state = o.getState();
        BigDecimal input = o.getInput();
        String num = input == null ? "空" : input.toPlainString();
        if (SideOutcome.STATE_JUDGED.equals(state)) {
            if (o.isOver()) {
                parts.add(o.getSide().label() + "越线：申报 " + num + " 已越过优线（第4层），依线 "
                        + o.getLineCode());
            }
        } else if (SideOutcome.STATE_INPUT_BAD.equals(state)) {
            parts.add(o.getSide().label() + "申报数不像话：" + num + "（" + o.getDetail() + "）");
        } else if (SideOutcome.STATE_LINE_BAD.equals(state)) {
            parts.add(o.getSide().label() + "在场线 " + o.getLineCode() + " 立不起来：" + o.getDetail());
        }
        // NO_LINE 不塞进来：那头线代号/启用之日两栏空着即是交代，不凑邻近线的数
    }
}
