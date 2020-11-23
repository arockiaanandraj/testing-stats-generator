package com.bmc.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class Ventilator {
    private String jobNbr;
    private String serialNbr;
    private Boolean sentForRework;
    private String empName;
    private LocalDateTime testDate;
}