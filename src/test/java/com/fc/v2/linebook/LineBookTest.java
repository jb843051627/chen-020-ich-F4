package com.fc.v2.linebook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fc.v2.model.auto.TIchPreLine;
import com.fc.v2.model.custom.linebook.LineBook;
import com.fc.v2.model.custom.linebook.LineSide;
import com.fc.v2.model.custom.linebook.LineVerdict;
import com.fc.v2.model.custom.linebook.SideOutcome;

/**
 * 线册唯一判定尺 LineBook 的规矩测试（纯算，不启 Spring、不碰库）。
 *
 * <p>把题面四问与边界一条条钉死：按哪一刻回看、够不着给不给结论、
 * 同顺位听谁的、两头并一处取哪头；外加停用线、缺栏点名、交替日、同源。
 *
 * @author fuce
 * @date 2026-10-03
 */
public class LineBookTest {

    private long seq = 1;

    private static Date dt(String s) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(s);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** 造一条立得住的线：起分10 过线50 优线90，除非另行覆盖 */
    private TIchPreLine line(String code, String kind, int priority, Integer status,
                             String start, String end) {
        TIchPreLine r = new TIchPreLine();
        r.setId(seq++);
        r.setRuleCode(code);
        r.setRuleName(code + "号线");
        r.setLineKind(kind);
        r.setPriority(priority);
        r.setStatus(status);
        r.setEffStart(start == null ? null : dt(start));
        r.setEffEnd(end == null ? null : dt(end));
        r.setTh1Max(new BigDecimal("10"));
        r.setTh2Max(new BigDecimal("50"));
        r.setTh3Max(new BigDecimal("90"));
        r.setDelFlag(0);
        return r;
    }

    private List<TIchPreLine> book(TIchPreLine... rs) {
        List<TIchPreLine> l = new ArrayList<>();
        for (TIchPreLine r : rs) {
            l.add(r);
        }
        return l;
    }

    // ---- 第一问：按申请注明那一刻回看，启用日含、交棒日不含；交替日一进一出 ----

    @Test
    public void 交替当日旧的出去新的进来() {
        TIchPreLine old = line("Y-OLD", "YEARS", 1, 0, "2026-09-01 00:00:00", "2026-10-01 00:00:00");
        TIchPreLine now = line("Y-NEW", "YEARS", 1, 0, "2026-10-01 00:00:00", null);
        List<TIchPreLine> b = book(old, now);
        // 交替前一刻：旧线说话
        assertEquals("Y-OLD", LineBook.selectHead(b, LineSide.YEARS, dt("2026-09-30 23:59:59")).getRuleCode());
        // 交替当日零点：旧的当日出去、新的当日进来——同一件事此刻两处问，都是新线
        assertEquals("Y-NEW", LineBook.selectHead(b, LineSide.YEARS, dt("2026-10-01 00:00:00")).getRuleCode());
        // 交棒那日不含：踩到交棒点旧线已不在场
        assertFalse(LineBook.activeAt(old, dt("2026-10-01 00:00:00")));
        // 启用那日含
        assertTrue(LineBook.activeAt(now, dt("2026-10-01 00:00:00")));
    }

    @Test
    public void 交棒栏空着按一直在场不认作没有() {
        TIchPreLine open = line("Y-OPEN", "YEARS", 1, 0, "2020-01-01 00:00:00", null);
        assertTrue(LineBook.activeAt(open, dt("2026-10-03 10:00:00")));
        // 旧卷复算：2020 年里它也在场，缺交棒一栏不把整条当没有
        assertTrue(LineBook.activeAt(open, dt("2020-06-01 00:00:00")));
    }

    @Test
    public void 前年那期按前年自己的日子回看最新那条不顶上去() {
        TIchPreLine old = line("Y-OLD", "YEARS", 1, 0, "2024-01-01 00:00:00", "2025-01-01 00:00:00");
        TIchPreLine now = line("Y-NEW", "YEARS", 9, 0, "2025-01-01 00:00:00", null);
        // 哪怕新线顺位更高，2024 年里复算也只能取旧线
        assertEquals("Y-OLD", LineBook.selectHead(book(old, now), LineSide.YEARS, dt("2024-06-01 00:00:00")).getRuleCode());
    }

    // ---- 第二问：够不着任何一条不给结论，两栏空着 ----

    @Test
    public void 此刻够不着线给无结论两栏空() {
        TIchPreLine dOnly = line("D", "DISCIPLES", 1, 0, null, null);
        LineVerdict v = LineBook.judgeBoth(book(dOnly), "P1", dt("2026-10-03 00:00:00"),
                new BigDecimal("20"), new BigDecimal("3"));
        assertEquals(LineVerdict.STATUS_NO_LINE, v.getStatus());
        // 从艺年数这头没有 YEARS 线：线代号与启用之日两栏空着
        assertEquals(SideOutcome.STATE_NO_LINE, v.getYears().getState());
        assertNull(v.getYears().getLineCode());
        assertNull(v.getYears().getEffStart());
    }

    // ---- 第三问：同顺位听谁的 ----

    @Test
    public void 让位顺位高者说话() {
        TIchPreLine low = line("LOW", "YEARS", 1, 0, null, null);
        TIchPreLine high = line("HIGH", "YEARS", 9, 0, null, null);
        high.setTh1Max(new BigDecimal("3")); // 5 对 HIGH 落二档，对 LOW 落一档
        SideOutcome o = LineBook.judge(book(low, high), LineSide.YEARS, dt("2026-10-03 00:00:00"),
                new BigDecimal("5"));
        assertEquals("HIGH", o.getLineCode());
        assertEquals(LineBook.LEVEL_PASS, o.getLevel().intValue());
    }

    @Test
    public void 同顺位代号字数靠后者赢() {
        TIchPreLine shortCode = line("A", "YEARS", 5, 0, null, null);
        TIchPreLine longCode = line("AA", "YEARS", 5, 0, null, null);
        longCode.setTh1Max(new BigDecimal("3"));
        SideOutcome o = LineBook.judge(book(shortCode, longCode), LineSide.YEARS,
                dt("2026-10-03 00:00:00"), new BigDecimal("5"));
        assertEquals("AA", o.getLineCode()); // 字数靠后（字数多）者赢
        assertEquals(LineBook.LEVEL_PASS, o.getLevel().intValue());
    }

    @Test
    public void 同顺位同字数字典序降序兜底给回两次同一句() {
        TIchPreLine a = line("AA", "YEARS", 5, 0, null, null);
        TIchPreLine b = line("AB", "YEARS", 5, 0, null, null);
        b.setTh1Max(new BigDecimal("3"));
        Date at = dt("2026-10-03 00:00:00");
        List<TIchPreLine> bk = book(a, b);
        assertEquals("AB", LineBook.selectHead(bk, LineSide.YEARS, at).getRuleCode());
        // 打乱入册顺序再问一次，答案同一句
        assertEquals("AB", LineBook.selectHead(book(b, a), LineSide.YEARS, at).getRuleCode());
    }

    // ---- 停用线：不进场，但在册里可翻、旧卷来历不算错 ----

    @Test
    public void 停用线不进场却可翻阅且旧窗口罩得住() {
        TIchPreLine dead = line("DEAD", "YEARS", 9, 1, null, null);
        Date at = dt("2026-10-03 00:00:00");
        assertFalse(LineBook.activeAt(dead, at));
        assertNull(LineBook.selectHead(book(dead), LineSide.YEARS, at));
        assertTrue(LineBook.present(book(dead), at).isEmpty());
        // 来历复算不看眼下停用栏：窗口罩得住就算对得上（按下不等于撕掉）
        assertTrue(LineBook.windowCovers(dead, at));
        assertTrue(LineBook.provenanceMatches(book(dead), "DEAD", at));
    }

    @Test
    public void 已删线与窗口外线来历不对() {
        TIchPreLine shortWindow = line("W", "YEARS", 1, 1, "2026-01-01 00:00:00", "2026-02-01 00:00:00");
        // 线还在册（停用），但申请那天不在其窗口内
        assertFalse(LineBook.provenanceMatches(book(shortWindow), "W", dt("2026-03-01 00:00:00")));
        // 代号已删
        assertFalse(LineBook.provenanceMatches(book(shortWindow), "GONE", dt("2026-01-10 00:00:00")));
    }

    // ---- 三条分界数：缺一立不起来，逐栏点名 ----

    @Test
    public void 分界数缺栏逐栏点名不光说数不对() {
        TIchPreLine bad = line("BAD", "YEARS", 1, 0, null, null);
        bad.setTh2Max(null);
        bad.setTh3Max(null);
        SideOutcome o = LineBook.judge(book(bad), LineSide.YEARS, dt("2026-10-03 00:00:00"),
                new BigDecimal("5"));
        assertEquals(SideOutcome.STATE_LINE_BAD, o.getState());
        assertEquals("BAD", o.getLineCode());
        assertTrue(o.getDetail().contains("过线一栏空着"));
        assertTrue(o.getDetail().contains("优线一栏空着"));
        assertFalse(o.getDetail().contains("起分一栏空着"));
    }

    @Test
    public void 分界数不像话负数与不递增点名() {
        TIchPreLine bad = line("BAD", "YEARS", 1, 0, null, null);
        bad.setTh1Max(new BigDecimal("-1"));
        bad.setTh2Max(new BigDecimal("50"));
        bad.setTh3Max(new BigDecimal("40")); // 过线 50 ≥ 优线 40，不层层抬高
        SideOutcome o = LineBook.judge(book(bad), LineSide.YEARS, dt("2026-10-03 00:00:00"),
                new BigDecimal("5"));
        assertEquals(SideOutcome.STATE_LINE_BAD, o.getState());
        assertTrue(o.getDetail().contains("起分数字不像话"));
        assertTrue(o.getDetail().contains("过线须低于优线"));
    }

    // ---- 分层与越 ----

    @Test
    public void 比到哪一段往哪一层等于分界取该层() {
        TIchPreLine y = line("Y", "YEARS", 1, 0, null, null);
        Date at = dt("2026-10-03 00:00:00");
        assertEquals(LineBook.LEVEL_START, LineBook.judge(book(y), LineSide.YEARS, at, new BigDecimal("10")).getLevel().intValue());
        assertEquals(LineBook.LEVEL_PASS, LineBook.judge(book(y), LineSide.YEARS, at, new BigDecimal("50")).getLevel().intValue());
        assertEquals(LineBook.LEVEL_EXCELLENT, LineBook.judge(book(y), LineSide.YEARS, at, new BigDecimal("90")).getLevel().intValue());
        assertEquals(LineBook.LEVEL_OVER, LineBook.judge(book(y), LineSide.YEARS, at, new BigDecimal("90.01")).getLevel().intValue());
    }

    // ---- 第四问：两头并一处，各走各的线，都不越才过 ----

    @Test
    public void 两头各走各的线都不越才过() {
        TIchPreLine y = line("YL", "YEARS", 1, 0, null, null);
        TIchPreLine d = line("DL", "DISCIPLES", 1, 0, null, null);
        LineVerdict v = LineBook.judgeBoth(book(y, d), "P1", dt("2026-10-03 00:00:00"),
                new BigDecimal("30"), new BigDecimal("20"));
        assertEquals(LineVerdict.STATUS_PASS, v.getStatus());
        assertEquals("YL", v.getYears().getLineCode());
        assertEquals("DL", v.getDisciples().getLineCode());
    }

    @Test
    public void 从艺年数越了亮出这头的数与线() {
        TIchPreLine y = line("YL", "YEARS", 1, 0, null, null);
        TIchPreLine d = line("DL", "DISCIPLES", 1, 0, null, null);
        LineVerdict v = LineBook.judgeBoth(book(y, d), "P1", dt("2026-10-03 00:00:00"),
                new BigDecimal("99"), new BigDecimal("5"));
        assertEquals(LineVerdict.STATUS_FAIL, v.getStatus());
        assertTrue(v.getYears().isOver());
        assertFalse(v.getDisciples().isOver());
        assertTrue(v.getFailDetail().contains("从艺年数越线"));
        assertTrue(v.getFailDetail().contains("99"));
        assertTrue(v.getFailDetail().contains("YL"));
    }

    @Test
    public void 带徒人数越了亮出那一头() {
        TIchPreLine y = line("YL", "YEARS", 1, 0, null, null);
        TIchPreLine d = line("DL", "DISCIPLES", 1, 0, null, null);
        LineVerdict v = LineBook.judgeBoth(book(y, d), "P1", dt("2026-10-03 00:00:00"),
                new BigDecimal("5"), new BigDecimal("99"));
        assertEquals(LineVerdict.STATUS_FAIL, v.getStatus());
        assertTrue(v.getFailDetail().contains("带徒人数越线"));
        assertTrue(v.getFailDetail().contains("99"));
        assertFalse(v.getFailDetail().contains("从艺年数越线"));
    }

    @Test
    public void 申报数不像话按失败逐头交代越优线不算不像话() {
        TIchPreLine y = line("YL", "YEARS", 1, 0, null, null);
        TIchPreLine d = line("DL", "DISCIPLES", 1, 0, null, null);
        // 从艺年数缺填、带徒负数
        LineVerdict v = LineBook.judgeBoth(book(y, d), "P1", dt("2026-10-03 00:00:00"),
                null, new BigDecimal("-2"));
        assertEquals(LineVerdict.STATUS_FAIL, v.getStatus());
        assertEquals(SideOutcome.STATE_INPUT_BAD, v.getYears().getState());
        assertEquals(SideOutcome.STATE_INPUT_BAD, v.getDisciples().getState());
        assertTrue(v.getFailDetail().contains("从艺年数申报数不像话"));
        assertTrue(v.getFailDetail().contains("带徒人数申报数不像话"));
        // 越过优线是 JUDGED 第4层，不是数不像话
        SideOutcome over = LineBook.judge(book(y), LineSide.YEARS, dt("2026-10-03 00:00:00"),
                new BigDecimal("999"));
        assertEquals(SideOutcome.STATE_JUDGED, over.getState());
        assertEquals(LineBook.LEVEL_OVER, over.getLevel().intValue());
    }

    // ---- 同源：在场名单的头一条就是逐条判定取的那条 ----

    @Test
    public void 在场名单头一条与判定取线同源同一句() {
        TIchPreLine y1 = line("A", "YEARS", 5, 0, null, null);
        TIchPreLine y2 = line("AA", "YEARS", 5, 0, null, null);
        TIchPreLine d1 = line("D", "DISCIPLES", 1, 0, null, null);
        Date at = dt("2026-10-03 00:00:00");
        List<TIchPreLine> bk = book(y1, y2, d1);
        List<TIchPreLine> present = LineBook.present(bk, at);
        // 在场名单按决胜次序，YEARS 头名 AA 必须与 selectHead 一致，两回算不是两个答案
        assertEquals("AA", present.get(0).getRuleCode());
        assertEquals(present.get(0).getRuleCode(), LineBook.selectHead(bk, LineSide.YEARS, at).getRuleCode());
        assertEquals("D", LineBook.selectHead(bk, LineSide.DISCIPLES, at).getRuleCode());
    }
}
