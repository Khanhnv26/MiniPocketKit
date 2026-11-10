package com.example.pocketkit.dto;

import com.example.pocketkit.model.Note;

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
public class NoteRequestDto {

    private int id;

    private String title;
    private String content;
    private String subject;
    private int isPinned;
    private int isDone;
    private String updateAt;

    public Note toEntity () {
        return Note.builder()
                .id(this.id)
                .title(this.title)
                .content(this.content)
                .subject(this.subject)
                .isPinned(this.isPinned)
                .isDone(this.isDone)
                .updateAt(this.updateAt)
                .build();
    }
}
