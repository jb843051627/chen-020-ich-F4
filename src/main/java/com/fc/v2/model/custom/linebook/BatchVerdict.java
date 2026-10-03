package com.fc.v2.model.custom.linebook;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.fc.v2.model.auto.TIchPreLine;

/**
 * 一批申报同一回算的战报：屏上那份在场名单与逐条判出来的名单同源于一次取数，
 * 两回的数不一样即错——故一个回话里两样都摆齐，不许分两次去取。
 *
 * @author fuce
 * @date 2026-10-03
 */
public class BatchVerdict {

    /** 整批回看的那一刻（申请注明那一刻） */
    private final Date at;

    /** 那一刻在场的全部线（两种管项都在，按决胜次序） */
    private final List<TIchPreLine> presentLines;

    /** 逐条判出来的名单，与入参同序 */
    private final List<LineVerdict> verdicts;

    public BatchVerdict(Date at, List<TIchPreLine> presentLines, List<LineVerdict> verdicts) {
        this.at = at;
        this.presentLines = presentLines == null
                ? new ArrayList<TIchPreLine>() : presentLines;
        this.verdicts = verdicts == null
                ? new ArrayList<LineVerdict>() : verdicts;
    }

    public Date getAt() {
        return at;
    }

    /** 在场名单只读，谁也别想在回话外另塞一条 */
    public List<TIchPreLine> getPresentLines() {
        return Collections.unmodifiableList(presentLines);
    }

    public List<LineVerdict> getVerdicts() {
        return Collections.unmodifiableList(verdicts);
    }

    public int getPresentCount() {
        return presentLines.size();
    }

    public int size() {
        return verdicts.size();
    }
}
