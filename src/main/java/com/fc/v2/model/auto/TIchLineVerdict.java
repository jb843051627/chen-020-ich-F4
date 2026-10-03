package com.fc.v2.model.auto;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 申报准入判定结论快照对象 t_ich_line_verdict
 *
 * <p>这是判定出口回话原样落库的一本账：线代号与启用之日随结论一并钉死，
 * 往后换了线也不回头改这一段；这一回没过与后来补过是两笔，各留各的。
 *
 * @author fuce
 * @date 2026-10-03
 */
@TableName("t_ich_line_verdict")
@ApiModel(value = "TIchLineVerdict", description = "申报准入判定结论快照")
public class TIchLineVerdict implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 申报人代号 */
    @TableField("applicant_code")
    @ApiModelProperty(value = "申报人代号")
    private String applicantCode;

    /** 申请注明的那一刻（按此刻回看线册，不用最新那条顶替） */
    @TableField("apply_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "申请注明的那一刻")
    private Date applyAt;

    /** 判定回话 PASS / FAIL / NO_LINE */
    @TableField("verdict_status")
    @ApiModelProperty(value = "判定回话 PASS FAIL NO_LINE")
    private String verdictStatus;

    /** 本人从艺年数（申报原数） */
    @TableField("years_input")
    @ApiModelProperty(value = "本人从艺年数")
    private BigDecimal yearsInput;

    /** 从艺年数这头落点 JUDGED INPUT_BAD NO_LINE LINE_BAD */
    @TableField("years_state")
    @ApiModelProperty(value = "从艺年数这头落点")
    private String yearsState;

    /** 从艺年数那一头依哪条线判（钉线代号） */
    @TableField("years_line_code")
    @ApiModelProperty(value = "从艺年数依哪条线")
    private String yearsLineCode;

    /** 从艺年数那条线的启用之日 */
    @TableField("years_eff_start")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "从艺年数那条线的启用之日")
    private Date yearsEffStart;

    /** 从艺年数落在哪一层 1起分段 2过线段 3优线段 4越 */
    @TableField("years_level")
    @ApiModelProperty(value = "从艺年数落层 1起分 2过线 3优线 4越")
    private Integer yearsLevel;

    /** 带徒人数（申报原数） */
    @TableField("disciples_input")
    @ApiModelProperty(value = "带徒人数")
    private BigDecimal disciplesInput;

    /** 带徒人数这头落点 JUDGED INPUT_BAD NO_LINE LINE_BAD */
    @TableField("disciples_state")
    @ApiModelProperty(value = "带徒人数这头落点")
    private String disciplesState;

    /** 带徒人数那一头依哪条线判（钉线代号） */
    @TableField("disciples_line_code")
    @ApiModelProperty(value = "带徒人数依哪条线")
    private String disciplesLineCode;

    /** 带徒人数那条线的启用之日 */
    @TableField("disciples_eff_start")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "带徒人数那条线的启用之日")
    private Date disciplesEffStart;

    /** 带徒人数落在哪一层 1起分段 2过线段 3优线段 4越 */
    @TableField("disciples_level")
    @ApiModelProperty(value = "带徒人数落层 1起分 2过线 3优线 4越")
    private Integer disciplesLevel;

    /** 哪头越了就把那一头的数落在哪；无线/不像话逐头写明 */
    @TableField("fail_detail")
    @ApiModelProperty(value = "越界/缺栏交代")
    private String failDetail;

    /** 删除标记 0正常 1删除 */
    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记 0正常 1删除")
    private Integer delFlag;

    /** 创建者 */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "创建者")
    private String createBy;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    /** 更新者 */
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    @ApiModelProperty(value = "更新者")
    private String updateBy;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

    /** 备注 */
    @TableField("remark")
    @ApiModelProperty(value = "备注")
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getApplicantCode() {
        return applicantCode;
    }

    public void setApplicantCode(String applicantCode) {
        this.applicantCode = applicantCode;
    }

    public Date getApplyAt() {
        return applyAt;
    }

    public void setApplyAt(Date applyAt) {
        this.applyAt = applyAt;
    }

    public String getVerdictStatus() {
        return verdictStatus;
    }

    public void setVerdictStatus(String verdictStatus) {
        this.verdictStatus = verdictStatus;
    }

    public BigDecimal getYearsInput() {
        return yearsInput;
    }

    public void setYearsInput(BigDecimal yearsInput) {
        this.yearsInput = yearsInput;
    }

    public String getYearsState() {
        return yearsState;
    }

    public void setYearsState(String yearsState) {
        this.yearsState = yearsState;
    }

    public String getYearsLineCode() {
        return yearsLineCode;
    }

    public void setYearsLineCode(String yearsLineCode) {
        this.yearsLineCode = yearsLineCode;
    }

    public Date getYearsEffStart() {
        return yearsEffStart;
    }

    public void setYearsEffStart(Date yearsEffStart) {
        this.yearsEffStart = yearsEffStart;
    }

    public Integer getYearsLevel() {
        return yearsLevel;
    }

    public void setYearsLevel(Integer yearsLevel) {
        this.yearsLevel = yearsLevel;
    }

    public BigDecimal getDisciplesInput() {
        return disciplesInput;
    }

    public void setDisciplesInput(BigDecimal disciplesInput) {
        this.disciplesInput = disciplesInput;
    }

    public String getDisciplesState() {
        return disciplesState;
    }

    public void setDisciplesState(String disciplesState) {
        this.disciplesState = disciplesState;
    }

    public String getDisciplesLineCode() {
        return disciplesLineCode;
    }

    public void setDisciplesLineCode(String disciplesLineCode) {
        this.disciplesLineCode = disciplesLineCode;
    }

    public Date getDisciplesEffStart() {
        return disciplesEffStart;
    }

    public void setDisciplesEffStart(Date disciplesEffStart) {
        this.disciplesEffStart = disciplesEffStart;
    }

    public Integer getDisciplesLevel() {
        return disciplesLevel;
    }

    public void setDisciplesLevel(Integer disciplesLevel) {
        this.disciplesLevel = disciplesLevel;
    }

    public String getFailDetail() {
        return failDetail;
    }

    public void setFailDetail(String failDetail) {
        this.failDetail = failDetail;
    }

    public Integer getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Integer delFlag) {
        this.delFlag = delFlag;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
