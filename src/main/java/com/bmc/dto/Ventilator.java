package com.bmc.dto;

import lombok.Data;

@Data
public class Ventilator {
    private String jobNbr;
    private String serialNbr;
    private Boolean sentForRework;
    private String empName;
}