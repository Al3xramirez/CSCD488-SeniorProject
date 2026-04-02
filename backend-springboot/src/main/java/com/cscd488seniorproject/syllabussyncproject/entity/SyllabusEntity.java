package com.cscd488seniorproject.syllabussyncproject.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "syllabus")
@IdClass(SyllabusId.class)
public class SyllabusEntity {

	@Id
	@Column(name = "ClassCode")
	private String classCode;

	@Id
	@Column(name = "Quarter")
	private String quarter;

	@Id
	@Column(name = "Year")
	private String year;

	@Column(name = "GradingScale", columnDefinition = "TEXT")
	private String gradingScale;

	@Column(name = "AttendancePolicy", columnDefinition = "TEXT")
	private String attendancePolicy;

	@Column(name = "LatePolicy", columnDefinition = "TEXT")
	private String latePolicy;

	@Column(name = "ExamInfo", columnDefinition = "TEXT")
	private String examInfo;

	public SyllabusEntity() {
	}

	public String getClassCode() {
		return classCode;
	}

	public void setClassCode(String classCode) {
		this.classCode = classCode;
	}

	public String getQuarter() {
		return quarter;
	}

	public void setQuarter(String quarter) {
		this.quarter = quarter;
	}

	public String getYear() {
		return year;
	}

	public void setYear(String year) {
		this.year = year;
	}

	public String getGradingScale() {
		return gradingScale;
	}

	public void setGradingScale(String gradingScale) {
		this.gradingScale = gradingScale;
	}

	public String getAttendancePolicy() {
		return attendancePolicy;
	}

	public void setAttendancePolicy(String attendancePolicy) {
		this.attendancePolicy = attendancePolicy;
	}

	public String getLatePolicy() {
		return latePolicy;
	}

	public void setLatePolicy(String latePolicy) {
		this.latePolicy = latePolicy;
	}

	public String getExamInfo() {
		return examInfo;
	}

	public void setExamInfo(String examInfo) {
		this.examInfo = examInfo;
	}
}


