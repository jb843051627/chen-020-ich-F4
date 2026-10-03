package com.fc.v2.linebook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.fc.v2.mapper.auto.TIchLineVerdictMapper;
import com.fc.v2.mapper.auto.TIchPreLineMapper;
import com.fc.v2.model.auto.TIchLineVerdict;
import com.fc.v2.model.auto.TIchPreLine;
import com.fc.v2.model.custom.linebook.ApplicantInput;
import com.fc.v2.model.custom.linebook.BatchVerdict;
import com.fc.v2.model.custom.linebook.LineSide;
import com.fc.v2.model.custom.linebook.LineVerdict;
import com.fc.v2.model.custom.linebook.SideOutcome;
import com.fc.v2.model.custom.linebook.StaleFinding;
import com.fc.v2.service.impl.TIchLineVerdictServiceImpl;

/**
 * 线册判据唯一出口的服务层规矩测试（两个 Mapper 全打桩，不碰库）。
 *
 * <p>盯死：线代号与启用之日随结论落库只插不改；同人同刻回放旧笔不重算；
 * 整批在场名单与逐条结论同源于一次取数；停用线可翻不进场；来历不对逐条列出。
 *
 * @author fuce
 * @date 2026-10-03
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class TIchLineVerdictServiceImplTest {

    @Mock
    private TIchPreLineMapper preLineMapper;

    @Mock
    private TIchLineVerdictMapper verdictMapper;

    @InjectMocks
    private TIchLineVerdictServiceImpl service;

    private final List<TIchPreLine> book = new ArrayList<>();
    private final List<TIchLineVerdict> recorded = new ArrayList<>();

    private static Date dt(String s) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(s);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    public void setUp() {
        book.clear();
        recorded.clear();
        when(preLineMapper.selectList(any())).thenAnswer(inv -> new ArrayList<>(book));
        // 按 Wrapper 条件过滤：findRecorded 带 applicant_code+apply_at，listStale 只带 del_flag
        when(verdictMapper.selectList(any())).thenAnswer(inv -> {
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<?> w = inv.getArgument(0);
            String seg = w.getSqlSegment();
            java.util.Collection<Object> vals = w.getParamNameValuePairs().values();
            if (seg != null && seg.contains("applicant_code")) {
                String code = null;
                Date at = null;
                for (Object val : vals) {
                    if (val instanceof String) {
                        code = (String) val;
                    } else if (val instanceof Date) {
                        at = (Date) val;
                    }
                }
                final String fcode = code;
                final Date fat = at;
                List<TIchLineVerdict> hit = new ArrayList<>();
                for (TIchLineVerdict e : recorded) {
                    if (fcode != null && fcode.equals(e.getApplicantCode())
                            && fat != null && fat.equals(e.getApplyAt())) {
                        hit.add(e);
                    }
                }
                return hit;
            }
            return new ArrayList<>(recorded);
        });
        // 落库只插：给插进来的快照补一个主键
        when(verdictMapper.insert(any())).thenAnswer(inv -> {
            TIchLineVerdict e = inv.getArgument(0);
            if (e.getId() == null) {
                e.setId((long) (recorded.size() + 1));
            }
            recorded.add(e);
            return 1;
        });
    }

    private TIchPreLine line(long id, String code, String kind, int priority, Integer status,
                             String start, String end) {
        TIchPreLine r = new TIchPreLine();
        r.setId(id);
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
        book.add(r);
        return r;
    }

    private void twoGoodLines() {
        line(1, "YL", "YEARS", 1, 0, null, null);
        line(2, "DL", "DISCIPLES", 1, 0, null, null);
    }

    @Test
    public void 判定通过时线代号与启用之日随结论落库() {
        TIchPreLine y = line(1, "YL", "YEARS", 1, 0, "2026-01-01 00:00:00", null);
        line(2, "DL", "DISCIPLES", 1, 0, "2026-02-01 00:00:00", null);
        Date at = dt("2026-10-03 09:00:00");
        LineVerdict v = service.judge("P1", at, new BigDecimal("30"), new BigDecimal("20"));

        assertEquals(LineVerdict.STATUS_PASS, v.getStatus());
        assertEquals(1, recorded.size());
        TIchLineVerdict e = recorded.get(0);
        assertEquals("P1", e.getApplicantCode());
        assertEquals("YL", e.getYearsLineCode());
        assertEquals(y.getEffStart(), e.getYearsEffStart());
        assertEquals("DL", e.getDisciplesLineCode());
        assertEquals(SideOutcome.STATE_JUDGED, e.getYearsState());
        assertEquals(2, e.getYearsLevel().intValue());
        verify(verdictMapper, times(1)).insert(any());
    }

    @Test
    public void 同人同刻再问回放旧笔不重算不覆盖() {
        twoGoodLines();
        Date at = dt("2026-10-03 09:00:00");
        LineVerdict first = service.judge("P1", at, new BigDecimal("30"), new BigDecimal("20"));
        assertEquals(LineVerdict.STATUS_PASS, first.getStatus());

        // 线册随后改了：旧线停用、加一条更严的新线——旧结论也不许回头改
        book.clear();
        TIchPreLine y2 = line(3, "YL2", "YEARS", 9, 0, "2026-10-02 00:00:00", null);
        y2.setTh3Max(new BigDecimal("35")); // 30 对新线本要落第4层
        line(4, "DL2", "DISCIPLES", 9, 0, "2026-10-02 00:00:00", null);

        LineVerdict again = service.judge("P1", at, new BigDecimal("30"), new BigDecimal("20"));
        // 回放的是上回钉死的 YL，不是今天的 YL2；库里仍只有一笔，未新增未覆盖
        assertEquals("YL", again.getYears().getLineCode());
        assertEquals(LineVerdict.STATUS_PASS, again.getStatus());
        assertEquals(1, recorded.size());
        verify(verdictMapper, times(1)).insert(any());
    }

    @Test
    public void 后来补过是另一刻的另一笔旧笔不抹() {
        twoGoodLines();
        Date firstAt = dt("2026-09-01 09:00:00");
        LineVerdict fail = service.judge("P1", firstAt, new BigDecimal("99"), new BigDecimal("20"));
        assertEquals(LineVerdict.STATUS_FAIL, fail.getStatus());

        Date laterAt = dt("2026-10-03 09:00:00");
        LineVerdict pass = service.judge("P1", laterAt, new BigDecimal("30"), new BigDecimal("20"));
        assertEquals(LineVerdict.STATUS_PASS, pass.getStatus());
        // 两笔各留各的
        assertEquals(2, recorded.size());
        assertEquals(LineVerdict.STATUS_FAIL, recorded.get(0).getVerdictStatus());
        assertTrue(recorded.get(0).getFailDetail().contains("从艺年数越线"));
        assertEquals(LineVerdict.STATUS_PASS, recorded.get(1).getVerdictStatus());
    }

    @Test
    public void 够不着线时不给结论两栏空且仍然落库留痕() {
        // 只有 DISCIPLES 线，YEARS 一头无线
        line(2, "DL", "DISCIPLES", 1, 0, null, null);
        Date at = dt("2026-10-03 09:00:00");
        LineVerdict v = service.judge("P1", at, new BigDecimal("30"), new BigDecimal("20"));
        assertEquals(LineVerdict.STATUS_NO_LINE, v.getStatus());
        assertNull(v.getYears().getLineCode());
        assertNull(v.getYears().getEffStart());
        TIchLineVerdict e = recorded.get(0);
        assertEquals(LineVerdict.STATUS_NO_LINE, e.getVerdictStatus());
        assertNull(e.getYearsLineCode());
        assertNull(e.getYearsEffStart());
        // 不抓邻近那条的线代号凑数
        assertEquals("DL", e.getDisciplesLineCode());
    }

    @Test
    public void 整批在场名单与逐条结论同源于一次取数() {
        line(1, "YL", "YEARS", 1, 0, null, null);
        line(2, "DL", "DISCIPLES", 1, 0, null, null);
        // 一条停用线：不在场名单，却在册里
        line(5, "DEAD", "YEARS", 9, 1, null, null);

        Date at = dt("2026-10-03 09:00:00");
        List<ApplicantInput> applicants = Arrays.asList(
                new ApplicantInput("P1", new BigDecimal("30"), new BigDecimal("20")),
                new ApplicantInput("P2", new BigDecimal("99"), new BigDecimal("5")));
        BatchVerdict report = service.judgeBatch(at, applicants);

        // 线册快照整批只取一次（present + judge 共用）
        verify(preLineMapper, times(1)).selectList(any());
        // 在场名单不含停用线
        assertEquals(2, report.getPresentCount());
        for (TIchPreLine r : report.getPresentLines()) {
            assertFalse("DEAD".equals(r.getRuleCode()));
        }
        // 逐条结论与入参同序，两头取线与在场名单同源
        assertEquals(2, report.size());
        assertEquals(LineVerdict.STATUS_PASS, report.getVerdicts().get(0).getStatus());
        assertEquals(LineVerdict.STATUS_FAIL, report.getVerdicts().get(1).getStatus());
        // 翻线册仍能翻到停用那条
        assertTrue(service.listBookEntries().stream().anyMatch(r -> "DEAD".equals(r.getRuleCode())));
        // 屏上在场名单里压根没有停用线
        assertTrue(service.listPresent(at).stream().noneMatch(r -> "DEAD".equals(r.getRuleCode())));
    }

    @Test
    public void 旧结论依停用线判的来历核查一眼认出() {
        // 当年一条 YEARS 线 2025 年在场，如今停用——旧卷来历仍对得上
        TIchPreLine old = line(1, "Y2025", "YEARS", 1, 1,
                "2025-01-01 00:00:00", "2026-01-01 00:00:00");
        // 带徒人数那一头当年也有一条开放窗口的线（如今停用无妨，复算不看停用栏）
        line(2, "DL", "DISCIPLES", 1, 1, "2024-01-01 00:00:00", null);
        TIchLineVerdict ok = verdict("V1", "P1", dt("2025-06-01 00:00:00"),
                "Y2025", old.getEffStart(), "DL", dt("2025-01-01 00:00:00"));
        recorded.add(ok);
        // 另两份挂着的线：一个代号已删、一个窗口对不上
        recorded.add(verdict("V2", "P2", dt("2025-06-01 00:00:00"),
                "GONE", dt("2025-01-01 00:00:00"), "DL", dt("2025-01-01 00:00:00")));
        recorded.add(verdict("V3", "P3", dt("2026-06-01 00:00:00"),
                "Y2025", old.getEffStart(), "DL", dt("2025-01-01 00:00:00")));

        List<StaleFinding> stale = service.listStale();
        // 当年在场、如今停用的 Y2025（V1）不算来历不对；V2 代号已删、V3 窗口对不上
        assertEquals(2, stale.size());
        StaleFinding gone = find(stale, "P2", LineSide.YEARS);
        StaleFinding window = find(stale, "P3", LineSide.YEARS);
        assertNotNull(gone);
        assertNotNull(window);
        assertEquals("GONE", gone.getLineCode());
        assertEquals(StaleFinding.Reason.CODE_GONE, gone.getReason());
        assertEquals("Y2025", window.getLineCode());
        assertEquals(StaleFinding.Reason.OUT_OF_WINDOW, window.getReason());
        // 停用线旧卷 V1 不在来历不对清单里
        assertNull(find(stale, "P1", LineSide.YEARS));
    }

    @Test
    public void 回放旧结论逐头字段以库里钉死的为准() {
        twoGoodLines();
        Date at = dt("2026-10-03 09:00:00");
        service.judge("P1", at, new BigDecimal("99"), new BigDecimal("20"));

        // 回放口：不回算，原样给库里钉死的那一笔
        TIchLineVerdict e = service.findRecorded("P1", at);
        assertNotNull(e);
        assertEquals(LineVerdict.STATUS_FAIL, e.getVerdictStatus());
        assertEquals("YL", e.getYearsLineCode());
        assertEquals(4, e.getYearsLevel().intValue());
        assertNotNull(e.getFailDetail());
        assertTrue(e.getFailDetail().contains("99"));
        // 查无此笔返回 null
        assertNull(service.findRecorded("NOBODY", at));
        assertNull(service.findRecorded("P1", null));
    }

    // ---- 辅助 ----

    private TIchLineVerdict verdict(String id, String applicant, Date applyAt,
                                   String yCode, Date yStart, String dCode, Date dStart) {
        TIchLineVerdict e = new TIchLineVerdict();
        e.setId(Long.valueOf(id.hashCode() & 0x7fffffff));
        e.setApplicantCode(applicant);
        e.setApplyAt(applyAt);
        e.setVerdictStatus(LineVerdict.STATUS_PASS);
        e.setYearsState(SideOutcome.STATE_JUDGED);
        e.setYearsLineCode(yCode);
        e.setYearsEffStart(yStart);
        e.setYearsLevel(2);
        e.setDisciplesState(SideOutcome.STATE_JUDGED);
        e.setDisciplesLineCode(dCode);
        e.setDisciplesEffStart(dStart);
        e.setDisciplesLevel(2);
        e.setDelFlag(0);
        return e;
    }

    private StaleFinding find(List<StaleFinding> list, String applicant, LineSide side) {
        for (StaleFinding f : list) {
            if (applicant.equals(f.getApplicantCode()) && f.getSide() == side) {
                return f;
            }
        }
        return null;
    }
}
