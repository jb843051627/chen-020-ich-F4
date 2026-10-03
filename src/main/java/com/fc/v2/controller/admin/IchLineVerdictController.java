package com.fc.v2.controller.admin;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.custom.linebook.ApplicantInput;
import com.fc.v2.model.custom.linebook.BatchVerdict;
import com.fc.v2.model.custom.linebook.LineVerdict;
import com.fc.v2.service.ITIchLineVerdictService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * 申报准入线册判据 Controller（linebook 形状：只读判定，无线册增删改）。
 *
 * <p>这一层只透传服务层那个<b>唯一判据出口</b>的回话，不在这里另立第二道能给结论的门，
 * 也不先摆一个答案再回头补依据——回话里线代号、启用之日、各层结果与结论是同一回算给的。
 * 翻线册、看在场名单、判单人、判整批、查来历不对，各走各的只读口。
 *
 * @author fuce
 * @date 2026-10-03
 */
@Api(value = "申报准入线册判据")
@Controller
@RequestMapping("/ichLineVerdict")
public class IchLineVerdictController extends BaseController {

    @Autowired
    private ITIchLineVerdictService ichLineVerdictService;

    @Log(title = "线册判据·单人判定", action = "judge")
    @ApiOperation(value = "单人判定", notes = "按申请注明那一刻回看，两头各取各的线，结论随线代号与启用之日钉库")
    @PostMapping("/judge")
    @RequiresPermissions("ich:lineVerdict:judge")
    @ResponseBody
    public AjaxResult judge(String applicantCode, Date applyAt,
                            BigDecimal yearsInput, BigDecimal disciplesInput) {
        LineVerdict v = ichLineVerdictService.judge(applicantCode, applyAt, yearsInput, disciplesInput);
        return success(200, v.getStatus(), v);
    }

    @Log(title = "线册判据·整批判定", action = "judgeBatch")
    @ApiOperation(value = "整批判定", notes = "在场名单与逐条结论同源于一次取数")
    @PostMapping("/judgeBatch")
    @RequiresPermissions("ich:lineVerdict:judge")
    @ResponseBody
    public AjaxResult judgeBatch(Date at, @RequestBody(required = false) List<ApplicantInput> applicants) {
        BatchVerdict report = ichLineVerdictService.judgeBatch(at, applicants);
        return AjaxResult.successData(200, report);
    }

    @Log(title = "线册判据·在场名单", action = "present")
    @ApiOperation(value = "某刻在场名单", notes = "停用线不在内；与判定同源")
    @GetMapping("/present")
    @RequiresPermissions("ich:lineVerdict:list")
    @ResponseBody
    public AjaxResult present(Date at) {
        return AjaxResult.successData(200, ichLineVerdictService.listPresent(at));
    }

    @Log(title = "线册判据·翻线册", action = "book")
    @ApiOperation(value = "翻线册", notes = "含已停用：在册里躺着给人翻，只是不进场比对")
    @GetMapping("/book")
    @RequiresPermissions("ich:lineVerdict:list")
    @ResponseBody
    public AjaxResult book() {
        return AjaxResult.successData(200, ichLineVerdictService.listBookEntries());
    }

    @Log(title = "线册判据·旧结论来历核查", action = "stale")
    @ApiOperation(value = "来历不对清单", notes = "屏上挂着线代号、线册里那天却查不到在场同名线的，逐条列出")
    @GetMapping("/stale")
    @RequiresPermissions("ich:lineVerdict:list")
    @ResponseBody
    public AjaxResult stale() {
        return AjaxResult.successData(200, ichLineVerdictService.listStale());
    }

    @Log(title = "线册判据·回放旧结论", action = "recorded")
    @ApiOperation(value = "回放已落库结论", notes = "不回算，原样给钉死的那一笔")
    @GetMapping("/recorded")
    @RequiresPermissions("ich:lineVerdict:list")
    @ResponseBody
    public AjaxResult recorded(String applicantCode, Date applyAt) {
        return AjaxResult.successData(200, ichLineVerdictService.findRecorded(applicantCode, applyAt));
    }
}
