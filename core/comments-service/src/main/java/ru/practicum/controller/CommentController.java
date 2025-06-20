package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.comment_service.dto.CommentPagedDto;
import ru.practicum.comment_service.entity.CommentsOrder;
import ru.practicum.service.CommentService;

@RestController
@RequestMapping("/comments")
@Validated
@RequiredArgsConstructor
@Slf4j
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/{eventId}")
    public ResponseEntity<CommentPagedDto> getCommentsByEvent(
            @PathVariable Long eventId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "NEWEST") CommentsOrder sort) {
        return ResponseEntity.status(HttpStatus.OK).body(commentService.getComments(eventId, page, size, sort));
    }
}
