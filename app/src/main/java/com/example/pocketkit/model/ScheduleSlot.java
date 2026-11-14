package com.example.pocketkit.model;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ScheduleSlot implements Serializable {

    public static final int DAY_MIN = 2;
    public static final int DAY_MAX = 7;

    private int id;
    private int dayOfWeek;
    private String startTime;
    private String endTime;
    private String subject;
    private String room;
    private String lecturer;
    private String note;

    public static int toMinutes(String time) {
        if (time == null) {
            return -1;
        }
        String trimmed = time.trim();
        int colon = trimmed.indexOf(':');
        if (colon <= 0 || colon >= trimmed.length() - 1) {
            return -1;
        }
        try {
            int hour = Integer.parseInt(trimmed.substring(0, colon));
            int minute = Integer.parseInt(trimmed.substring(colon + 1));
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                return -1;
            }
            return hour * 60 + minute;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
