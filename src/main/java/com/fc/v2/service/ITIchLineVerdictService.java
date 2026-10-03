package com.fc.v2.service;

import java.util.Date;
import java.util.List;

import com.fc.v2.model.auto.TIchLineVerdict;
import com.fc.v2.model.auto.TIchPreLine;
import com.fc.v2.model.custom.linebook.ApplicantInput;
import com.fc.v2.model.custom.linebook.BatchVerdict;
import com.fc.v2.model.custom.linebook.LineVerdict;
import com.fc.v2.model.custom.linebook.StaleFinding;

/**
 * 申报准入判定的**唯一判据出口**（linebook 形状）。
 *
 * <p>四问都只从这一个出口要说法，别处不再立第二道能给结论的门；
 * 页面也不许先摆一个答案再回头补依据：
 * <ol>
 *   <li>某位申报人在申请注明那一刻依哪条线判——{@link #judge} 按那一刻回看，
 *       手里最新那条不许顶上去；线代号与启用之日随结论钉库，换线不回头改。</li>
 *   <li>线册当刻一条也够不着时给不给结论——不给：回话 NO_LINE，两栏空着。</li>
 *   <li>几条线同顺位时听谁的——让位顺位高者，再平代号字数靠后者（口径在 LineBook）。</li>
 *   <li>两头线并到一处取哪一头——从艺年数、带徒人数各走各的线，两头都不越才过。</li>
 * </ol>
 *
 * <p>老的"按线代号取值"那条路（{@link ITIchPreLineService}）保持不动；
 * 要查"按某一刻该依哪条线"这条新道，另立本来承接，不掺进老路。
 *
 * @author fuce
 * @date 2026-10-03
 */
public interface ITIchLineVerdictService {

    /**
     * 单人判定：按申请注明那一刻回看线册，两头各取各的线。
     *
     * <p>结论落库只插不改；同一申报人同一申请注明时刻再问，回放的是上回钉死的那笔，
     * 不拿今天的线册重算。后来补过是另一刻的另一笔，旧的这一笔不抹。
     *
     * @param applicantCode  申报人代号（不许空）
     * @param applyAt        申请注明那一刻（不许空）
     * @param yearsInput     本人从艺年数（空着按这头数不像话）
     * @param disciplesInput 带徒人数
     */
    LineVerdict judge(String applicantCode, Date applyAt,
                      java.math.BigDecimal yearsInput, java.math.BigDecimal disciplesInput);

    /**
     * 整批判定：一批共用申请注明那一刻。线册快照只取一次，
     * 屏上在场名单与逐条判出来的名单同源于这一回算，不许分两次取数。
     * 已有落库结论的申报人，回放其旧笔而不是重算。
     */
    BatchVerdict judgeBatch(Date at, List<ApplicantInput> applicants);

    /** 回放某申报人某刻已落库的那笔结论（不回算）；没有返回 null */
    TIchLineVerdict findRecorded(String applicantCode, Date applyAt);

    /** at 那一刻在场的线（两种管项，决胜次序）——屏上在场名单，与判定同源 */
    List<TIchPreLine> listPresent(Date at);

    /**
     * 翻线册：未删的线全摆出来，<b>含已停用</b>——停用的在册里躺着给人翻，
     * 只是不再进场比对、不跟在现行结论后面。
     */
    List<TIchPreLine> listBookEntries();

    /**
     * 来历核查：旧结论屏上挂着线代号，线册里在申请注明那一天却查不到窗口罩得住的
     * 同名线（代号已删/窗口对不上），逐条列出。当年在场、如今停用的不算来历不对。
     */
    List<StaleFinding> listStale();
}
