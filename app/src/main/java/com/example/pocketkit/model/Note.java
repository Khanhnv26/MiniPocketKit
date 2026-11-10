package com.example.pocketkit.model;

import java.io.Serializable;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Note implements Serializable {
    private int id;

    @NotBlank(message = "Tiêu đề ghi chú không được để trống")
    @Size(max = 100, message = "Tiêu đề tối đa 100 ký tự")
    private String title;

    @Size(max = 2000, message = "Nội dung tối đa 2000 ký tự")
    private String content;

    @NotBlank(message = "Vui lòng chọn môn học")
    private String subject;

    private int isPinned;
    private int isDone;
    private String updateAt;
}
