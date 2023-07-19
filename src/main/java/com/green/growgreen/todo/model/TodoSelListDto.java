package com.green.growgreen.todo.model;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class TodoSelListDto {
    private String sDate;
    private String eDate;
}
