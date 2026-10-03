package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TIchLineVerdictMapper;
import com.fc.v2.mapper.auto.TIchPreLineMapper;
import com.fc.v2.model.auto.TIchLineVerdict;
import com.fc.v2.model.auto.TIchPreLine;
import com.fc.v2.model.custom.linebook.ApplicantInput;
import com.fc.v2.model.custom.linebook.BatchVerdict;
import com.fc.v2.model.custom.linebook.LineBook;
import com.fc.v2.model.custom.linebook.LineSide;
import com.fc.v2.model.custom.linebook.LineVerdict;
import com.fc.v2.model.custom.linebook.SideOutcome;
import com.fc.v2.model.custom.linebook.StaleFinding;
import com.fc.v2.service.ITIchLineVerdictService;

/**
 * 线册判据唯一出口的业务层。判定怎么算全在 {@link LineBook} 里（纯算），
 * 这里只做四件事：取一次线册快照、向唯一出口要回话、把回话原样钉库、回放旧笔。
 *
 * <p>不在这里另立第二套装法；老的按代号取值路 {@link TIchPreLineMapper} 照旧供老路用，
 * 本出口读同一本册子，但不改动老路一个字。
 *
 * @author fuce
 * @date 2026-10-03
 */
@Service
public class TIchLineVerdictServiceImpl implements ITIchLineVerdictService {

    @javax.annotation.Resource
    private TIchPreLineMapper ichPreLineMapper;

    @javax.annotation.Resource
    private TIchLineVerdictMapper ichLineVerdictMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LineVerdict judge(String applicantCode, Date applyAt,
                             BigDecimal yearsInput, BigDecimal disciplesInput) {
        String code = requireApplicant(applicantCode);
        if (applyAt == null) {
            throw new IllegalArgumentException("申请注明那一刻不能为空：不晓得按哪一刻回看线册");
        }
        // 同人同刻再问：旧结论钉在库里，回放，不拿今天的线册重算
        TIchLineVerdict recorded = findRecorded(code, applyAt);
        if (recorded != null) {
            return restore(recorded);
        }
        List<TIchPreLine> snapshot = loadSnapshot();
        LineVerdict verdict = LineBook.judgeBoth(snapshot, code, applyAt, yearsInput, disciplesInput);
        persist(verdict);
        return verdict;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchVerdict judgeBatch(Date at, List<ApplicantInput> applicants) {
        if (at == null) {
            throw new IllegalArgumentException("申请注明那一刻不能为空：整批不晓得按哪一刻回看线册");
        }
        // 线册快照只取一次：在场名单与逐条结论同源于它，两回的数不一样即错
        List<TIchPreLine> snapshot = loadSnapshot();
        List<TIchPreLine> present = LineBook.present(snapshot, at);

        List<LineVerdict> verdicts = new ArrayList<>();
        if (applicants != null) {
            for (ApplicantInput a : applicants) {
                if (a == null) {
                    continue;
                }
                String code = requireApplicant(a.getApplicantCode());
                TIchLineVerdict recorded = findRecorded(code, at);
                LineVerdict v;
                if (recorded != null) {
                    // 这一笔先前判过，回放旧话，不重算、不覆盖
                    v = restore(recorded);
                } else {
                    v = LineBook.judgeBoth(snapshot, code, at, a.getYearsInput(), a.getDisciplesInput());
                    persist(v);
                }
                verdicts.add(v);
            }
        }
        return new BatchVerdict(at, present, verdicts);
    }

    @Override
    public TIchLineVerdict findRecorded(String applicantCode, Date applyAt) {
        if (applicantCode == null || applyAt == null) {
            return null;
        }
        List<TIchLineVerdict> rows = this.ichLineVerdictMapper.selectList(
                new QueryWrapper<TIchLineVerdict>()
                        .eq("applicant_code", applicantCode.trim())
                        .eq("apply_at", applyAt)
                        .eq("del_flag", 0));
        return (rows == null || rows.isEmpty()) ? null : rows.get(0);
    }

    @Override
    public List<TIchPreLine> listPresent(Date at) {
        if (at == null) {
            return new ArrayList<>();
        }
        return LineBook.present(loadSnapshot(), at);
    }

    @Override
    public List<TIchPreLine> listBookEntries() {
        // 含已停用：在册里躺着给人翻；只是不再进场比对
        List<TIchPreLine> all = this.ichPreLineMapper.selectList(
                new QueryWrapper<TIchPreLine>().eq("del_flag", 0));
        return all == null ? new ArrayList<TIchPreLine>() : all;
    }

    @Override
    public List<StaleFinding> listStale() {
        List<TIchLineVerdict> verdicts = this.ichLineVerdictMapper.selectList(
                new QueryWrapper<TIchLineVerdict>().eq("del_flag", 0));
        if (verdicts == null || verdicts.isEmpty()) {
            return new ArrayList<>();
        }
        // 复算用整册（未删，含停用）：当年在场、如今停用的不算来历不对
        List<TIchPreLine> book = this.ichPreLineMapper.selectList(
                new QueryWrapper<TIchPreLine>().eq("del_flag", 0));
        List<StaleFinding> findings = new ArrayList<>();
        for (TIchLineVerdict v : verdicts) {
            checkSide(findings, book, v, LineSide.YEARS, v.getYearsLineCode());
            checkSide(findings, book, v, LineSide.DISCIPLES, v.getDisciplesLineCode());
        }
        return findings;
    }

    /** 线册快照：未删的全取（含停用），在场/选取由 LineBook 同一谓词裁。 */
    private List<TIchPreLine> loadSnapshot() {
        List<TIchPreLine> all = this.ichPreLineMapper.selectList(
                new QueryWrapper<TIchPreLine>().eq("del_flag", 0));
        return all == null ? Collections.<TIchPreLine>emptyList() : all;
    }

    private void checkSide(List<StaleFinding> findings, List<TIchPreLine> book,
                           TIchLineVerdict v, LineSide side, String lineCode) {
        if (lineCode == null || lineCode.trim().isEmpty()) {
            return;
        }
        if (LineBook.provenanceMatches(book, lineCode, v.getApplyAt())) {
            return;
        }
        // 来历不对分两说：代号还在册子里是窗口对不上，否则是代号已删
        boolean codeExists = false;
        for (TIchPreLine r : book) {
            if (lineCode.equals(r.getRuleCode())) {
                codeExists = true;
                break;
            }
        }
        findings.add(new StaleFinding(v.getId(), v.getApplicantCode(), side, lineCode,
                codeExists ? StaleFinding.Reason.OUT_OF_WINDOW : StaleFinding.Reason.CODE_GONE));
    }

    /** 把唯一出口的回话原样钉库：只插不改，换线不回头改这一段 */
    private void persist(LineVerdict v) {
        TIchLineVerdict e = new TIchLineVerdict();
        e.setApplicantCode(v.getApplicantCode());
        e.setApplyAt(v.getApplyAt());
        e.setVerdictStatus(v.getStatus());
        fillSide(e, v.getYears(), true);
        fillSide(e, v.getDisciples(), false);
        e.setFailDetail(v.getFailDetail());
        e.setDelFlag(0);
        this.ichLineVerdictMapper.insert(e);
    }

    private void fillSide(TIchLineVerdict e, SideOutcome o, boolean years) {
        if (years) {
            e.setYearsInput(o.getInput());
            e.setYearsState(o.getState());
            e.setYearsLineCode(o.getLineCode());
            e.setYearsEffStart(o.getEffStart());
            e.setYearsLevel(o.getLevel());
        } else {
            e.setDisciplesInput(o.getInput());
            e.setDisciplesState(o.getState());
            e.setDisciplesLineCode(o.getLineCode());
            e.setDisciplesEffStart(o.getEffStart());
            e.setDisciplesLevel(o.getLevel());
        }
    }

    /** 从库里钉死的字段还原回话（不回算）：线代号、启用之日、状态一概以旧笔为准 */
    private LineVerdict restore(TIchLineVerdict e) {
        SideOutcome years = SideOutcome.restore(LineSide.YEARS, e.getYearsInput(),
                e.getYearsState(), e.getYearsLineCode(), e.getYearsEffStart(), e.getYearsLevel(), null);
        SideOutcome disciples = SideOutcome.restore(LineSide.DISCIPLES, e.getDisciplesInput(),
                e.getDisciplesState(), e.getDisciplesLineCode(), e.getDisciplesEffStart(),
                e.getDisciplesLevel(), null);
        return LineVerdict.restore(e.getApplicantCode(), e.getApplyAt(), years, disciples,
                e.getFailDetail());
    }

    private static String requireApplicant(String applicantCode) {
        if (applicantCode == null || applicantCode.trim().isEmpty()) {
            throw new IllegalArgumentException("申报人代号不能为空：结论落不了户");
        }
        return applicantCode.trim();
    }
}
