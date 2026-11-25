package com.spacedvocab.dto;

import com.spacedvocab.entity.StageTestAttempt;
import com.spacedvocab.entity.StageTestQuestion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** D25: the learner's answers. Positions left out count as unanswered. */
public record StageTestSubmitRequest(
        @NotNull(message = "Thiếu danh sách câu trả lời.")
        @Size(max = StageTestAttempt.QUESTION_COUNT, message = "Tối đa 20 câu trả lời.")
        List<@Valid @NotNull Answer> answers) {

    public record Answer(
            @NotNull(message = "Thiếu số thứ tự câu.")
            @Min(value = 1, message = "Số thứ tự câu từ 1 đến 20.")
            @Max(value = StageTestAttempt.QUESTION_COUNT, message = "Số thứ tự câu từ 1 đến 20.")
            Integer position,
            @Size(max = StageTestQuestion.MAX_ANSWER_LENGTH, message = "Câu trả lời tối đa 100 ký tự.")
            String answer) {
    }
}
