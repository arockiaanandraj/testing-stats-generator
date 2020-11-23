package com.bmc.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Helper {

    public static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss z yyyy");

    private Helper() {
    }

    public static LocalDateTime convertToLocalDateTime(String dateStr) {
        try {
            return LocalDateTime.parse(dateStr, DTF);
        } catch (Exception E) {
            return null;
        }
    }
}