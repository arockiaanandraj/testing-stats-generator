package com.bmc.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class ConsolidatedReport {
    private String empName;
    private List<Report> reports = new ArrayList<>();
}