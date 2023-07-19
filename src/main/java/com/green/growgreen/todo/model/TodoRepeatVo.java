package com.green.growgreen.todo.model;

import lombok.Data;

import java.time.LocalDate;

@Data
public class TodoRepeatVo {
    private int itodo;
    private String ctnt;
    private LocalDate deadlineDate;
    private String deadlineTime;
    private String nickNm;
    private String nm;
    private int finishYn;
    private String repeatDay;
}
