package com.bmc.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class Report {
    private String jobNbr;
    private String testName;
    private List<Ventilator> ventilators = new ArrayList<>();
    private int testCount;
}