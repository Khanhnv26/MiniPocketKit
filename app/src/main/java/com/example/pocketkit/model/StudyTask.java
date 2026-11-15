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
public class StudyTask implements Serializable {

    private int id;
    private String title;
    private String subject;
    private String tag;
    private long dueAtMillis;
    private boolean done;
    private long createdAtMillis;

    public boolean hasDueAt() {
        return dueAtMillis > 0L;
    }
}
