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
public class Note implements Serializable {

    public static final String EXTRA_NOTE_ITEM = "note_item";

    private int id;
    private String title;
    private String content;
    private String subject;
    private int isPinned;
    private int isDone;
    private String updatedAt;
}
