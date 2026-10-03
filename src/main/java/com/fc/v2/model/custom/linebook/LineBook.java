package com.fc.v2.model.custom.linebook;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import com.fc.v2.model.auto.TIchPreLine;

/**
 * 线册的**唯一一把判定尺**（纯算，不碰库）。
 *
 * <p>判定只有一条路，规矩全收在这里，服务层那个判据出口只负责取一次线册快照、落库快照，
 * 不再各判各的：
 * <ul>
 *   <li>先定按哪一刻回看。那一刻在场的线才算数：已到启用之日（含）、未到交棒之日（不含）；
 *       交棒之日空着的按一直在场认——旧卷复算要靠它，缺这一栏不许把整条当没有。</li>
 *   <li>前后两条交替那一日，旧的当日出去、新的当日进来，同一件事在两个地方问答案同一句，
 *       靠的就是 [启用之日含, 交棒之日不含) 这同一把界尺。</li>
 *   <li>同时在场两条：让位顺位高的说话；顺位相同，<b>代号字数靠后（字数多）</b>的赢；
 *       字数也相同，按代号字典序降序兜底——给回两次必须是同一句。</li>
 *   <li>够不着任何一条线不给结论：线代号、启用之日两栏空着，不抓邻近那条凑数。</li>
 *   <li>已经停用（status=1）的线不进场，只在册里躺着给人翻。</li>
 *   <li>三条分界数（起分/过线/优线）一条都缺不得、且须层层抬高，缺了/不像话就立不起来，
 *       回执逐栏点名，不光回一句"数不对"。</li>
 *   <li>两头看：本人从艺年数走 YEARS 线、带徒人数走 DISCIPLES 线，两头各自都不越过优线
 *       （第4层）才算过；哪头越了亮哪头的数。</li>
 * </ul>
 *
 * <p>在场名单与逐条结论同源：服务层把一次取出的线册快照同时喂给 {@link #present} 与
 * {@link #judge} / {@link #judgeBoth}，不分两次取数。
 *
 * @author fuce
 * @date 2026-10-03
 */
public final class LineBook {

    /** 层1：起分段（含起分） */
    public static final int LEVEL_START = 1;

    /** 层2：过线段 */
    public static final int LEVEL_PASS = 2;

    /** 层3：优线段 */
    public static final int LEVEL_EXCELLENT = 3;

    /** 层4：越过优线——两头各自都不许落进这一层 */
    public static final int LEVEL_OVER = 4;

    private LineBook() {
    }

    /**
     * 在场决胜次序：让位顺位降序；并列时代号字数靠后（字数多）者赢；
     * 字数也相同按代号字典序降序兜底，保证回回同一句。
     */
    public static final Comparator<TIchPreLine> WIN_ORDER = new Comparator<TIchPreLine>() {
        @Override
        public int compare(TIchPreLine a, TIchPreLine b) {
            int pa = a.getPriority() == null ? 0 : a.getPriority().intValue();
            int pb = b.getPriority() == null ? 0 : b.getPriority().intValue();
            if (pa != pb) {
                return pb - pa;
            }
            String ca = a.getRuleCode() == null ? "" : a.getRuleCode();
            String cb = b.getRuleCode() == null ? "" : b.getRuleCode();
            if (ca.length() != cb.length()) {
                return cb.length() - ca.length();
            }
            return cb.compareTo(ca);
        }
    };

    /**
     * 一条线在 at 那一刻在不在场：未删、眼下情形为在场（空值按在场）、
     * 已到启用之日（含）、未到交棒之日（不含；交棒日空着按一直在场）。
     * 判定与在场名单共用这一个谓词，不分两套。
     */
    public static boolean activeAt(TIchPreLine r, Date at) {
        if (r == null || at == null) {
            return false;
        }
        if (r.getDelFlag() != null && r.getDelFlag().intValue() != 0) {
            return false;
        }
        // 已停用的线不进场：它在册里躺着给人翻，现行这一路不再拿它比对
        if (r.getStatus() != null && r.getStatus().intValue() != 0) {
            return false;
        }
        if (r.getEffStart() != null && at.before(r.getEffStart())) {
            return false;
        }
        if (r.getEffEnd() != null && !at.before(r.getEffEnd())) {
            return false;
        }
        return true;
    }

    /**
     * 来历复算专用：只问那一天窗口罩不罩得住 [启用之日含, 交棒之日不含)，不看眼下停用栏。
     *
     * <p>一条线当年在场期间判过的旧卷，如今它虽停用，来历仍对得上——"按下不等于撕掉"；
     * 唯有代号已从册里删掉、或窗口根本没罩住申请注明那刻的，才算来历不对。
     */
    public static boolean windowCovers(TIchPreLine r, Date at) {
        if (r == null || at == null) {
            return false;
        }
        if (r.getDelFlag() != null && r.getDelFlag().intValue() != 0) {
            return false;
        }
        if (r.getEffStart() != null && at.before(r.getEffStart())) {
            return false;
        }
        if (r.getEffEnd() != null && !at.before(r.getEffEnd())) {
            return false;
        }
        return true;
    }

    /**
     * 同一份线册快照上，at 那一刻在场的全部线（含两种管项，按决胜次序排好）。
     * 屏上在场名单与逐条判定都从这里取，不许分两次回算。
     */
    public static List<TIchPreLine> present(List<TIchPreLine> snapshot, Date at) {
        List<TIchPreLine> hits = new ArrayList<>();
        if (snapshot == null || at == null) {
            return hits;
        }
        for (TIchPreLine r : snapshot) {
            if (activeAt(r, at)) {
                hits.add(r);
            }
        }
        Collections.sort(hits, WIN_ORDER);
        return hits;
    }

    /** 这一头在 at 那一刻该由哪条线说话：在场 + 管项对得上，再按决胜次序取头一条。 */
    public static TIchPreLine selectHead(List<TIchPreLine> snapshot, LineSide side, Date at) {
        if (snapshot == null || side == null || at == null) {
            return null;
        }
        TIchPreLine head = null;
        for (TIchPreLine r : snapshot) {
            if (!activeAt(r, at)) {
                continue;
            }
            if (!side.kindCode().equals(r.getLineKind())) {
                continue;
            }
            if (head == null || WIN_ORDER.compare(r, head) < 0) {
                head = r;
            }
        }
        return head;
    }

    /**
     * 一头的判定。线册快照须由服务层一次性取好，两头共用。
     */
    public static SideOutcome judge(List<TIchPreLine> snapshot, LineSide side,
                                    Date at, BigDecimal input) {
        if (at == null) {
            return SideOutcome.inputBad(side, input, "申请注明那一刻空着，无法回看线册");
        }
        String inputProblem = inputProblem(input);
        if (inputProblem != null) {
            return SideOutcome.inputBad(side, input, inputProblem);
        }
        TIchPreLine head = selectHead(snapshot, side, at);
        if (head == null) {
            // 够不着任何一条：两栏空着，不给结论，不抓邻近那条凑数
            return SideOutcome.noLine(side, input);
        }
        String lineProblem = lineProblem(head);
        if (lineProblem != null) {
            return SideOutcome.lineBad(side, input, head.getRuleCode(), head.getEffStart(),
                    lineProblem);
        }
        int level = levelOf(head, input);
        return SideOutcome.judged(side, input, head.getRuleCode(), head.getEffStart(), level);
    }

    /**
     * 两头并到一处：本人从艺年数走 YEARS、带徒人数走 DISCIPLES，
     * 各取各的线、各判各的层，再由 {@link LineVerdict} 合成一句回话。
     */
    public static LineVerdict judgeBoth(List<TIchPreLine> snapshot, String applicantCode,
                                        Date at, BigDecimal yearsInput, BigDecimal disciplesInput) {
        SideOutcome years = judge(snapshot, LineSide.YEARS, at, yearsInput);
        SideOutcome disciples = judge(snapshot, LineSide.DISCIPLES, at, disciplesInput);
        return LineVerdict.of(applicantCode, at, years, disciples);
    }

    /**
     * 来历核查：屏上结论钉的线代号，线册里在申请注明那一天有没有一条窗口罩得住的同名线。
     * 当年在场、如今停用的不算来历不对（旧卷要靠它复算）；删掉的、窗口对不上的逐条点名。
     */
    public static boolean provenanceMatches(List<TIchPreLine> snapshot, String lineCode, Date at) {
        if (snapshot == null || lineCode == null || at == null) {
            return false;
        }
        for (TIchPreLine r : snapshot) {
            if (lineCode.equals(r.getRuleCode()) && windowCovers(r, at)) {
                return true;
            }
        }
        return false;
    }

    /** 申报这头的数像不像话：非空、不可为负；越过优线是"越"，不是数不像话，另由层级亮出来。 */
    private static String inputProblem(BigDecimal input) {
        if (input == null) {
            return "这一头没填数";
        }
        if (input.compareTo(BigDecimal.ZERO) < 0) {
            return "不可为负";
        }
        return null;
    }

    /**
     * 三条分界数立不立得起来：一栏都缺不得，且起分 &lt; 过线 &lt; 优线层层抬高。
     * 返回 null 为立得住；否则逐栏点名叫回执交代。
     */
    static String lineProblem(TIchPreLine r) {
        List<String> problems = new ArrayList<>();
        BigDecimal t1 = r.getTh1Max();
        BigDecimal t2 = r.getTh2Max();
        BigDecimal t3 = r.getTh3Max();
        if (t1 == null) {
            problems.add("起分一栏空着");
        } else if (t1.compareTo(BigDecimal.ZERO) < 0) {
            problems.add("起分数字不像话（不可为负）");
        }
        if (t2 == null) {
            problems.add("过线一栏空着");
        } else if (t2.compareTo(BigDecimal.ZERO) < 0) {
            problems.add("过线数字不像话（不可为负）");
        }
        if (t3 == null) {
            problems.add("优线一栏空着");
        } else if (t3.compareTo(BigDecimal.ZERO) < 0) {
            problems.add("优线数字不像话（不可为负）");
        }
        if (t1 != null && t2 != null && t1.compareTo(t2) >= 0) {
            problems.add("起分须低于过线（今 " + t1.toPlainString() + " ≥ " + t2.toPlainString() + "）");
        }
        if (t2 != null && t3 != null && t2.compareTo(t3) >= 0) {
            problems.add("过线须低于优线（今 " + t2.toPlainString() + " ≥ " + t3.toPlainString() + "）");
        }
        return problems.isEmpty() ? null : String.join("；", problems);
    }

    /**
     * 比到哪一段往哪一层去：等于分界取该层（≤起分→1，≤过线→2，≤优线→3），
     * 高过优线即第4层"越"。与老路 levelOf 同一把尺，守住旧卷复算兼容。
     */
    static int levelOf(TIchPreLine r, BigDecimal input) {
        if (input.compareTo(r.getTh1Max()) <= 0) {
            return LEVEL_START;
        }
        if (input.compareTo(r.getTh2Max()) <= 0) {
            return LEVEL_PASS;
        }
        if (input.compareTo(r.getTh3Max()) <= 0) {
            return LEVEL_EXCELLENT;
        }
        return LEVEL_OVER;
    }
}
