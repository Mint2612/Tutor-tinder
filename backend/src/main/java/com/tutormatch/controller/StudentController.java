package com.tutormatch.controller;

import com.tutormatch.dto.profile.*;
import com.tutormatch.mapper.StudentMapper;
import com.tutormatch.model.Account;
import com.tutormatch.service.StudentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/students")
public class StudentController {
  private final StudentService students;
  private final StudentMapper mapper;

  public StudentController(StudentService students, StudentMapper mapper) {
    this.students = students;
    this.mapper = mapper;
  }

  @GetMapping
  public List<StudentResponse> list(@AuthenticationPrincipal Account u) {
    return students.list(u.id()).stream().map(mapper::response).toList();
  }

  @GetMapping("/{id}")
  public StudentResponse get(@PathVariable String id, @AuthenticationPrincipal Account u) {
    return mapper.response(students.owned(id, u.id()));
  }

  @PostMapping
  public StudentResponse create(
      @Valid @RequestBody StudentRequest r, @AuthenticationPrincipal Account u) {
    return mapper.response(students.save(null, u.id(), r));
  }

  @PutMapping("/{id}")
  public StudentResponse update(
      @PathVariable String id,
      @Valid @RequestBody StudentRequest r,
      @AuthenticationPrincipal Account u) {
    return mapper.response(students.save(id, u.id(), r));
  }
}
