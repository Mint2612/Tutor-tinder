package com.tutormatch.controller;

import com.tutormatch.dto.lesson.TrialDtos.*;
import com.tutormatch.model.Account;
import com.tutormatch.service.TrialService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trials")
public class TrialController {
  private final TrialService trials;
  public TrialController(TrialService trials) { this.trials=trials; }
  @GetMapping public List<Trial> list(@AuthenticationPrincipal Account u) { return trials.list(u); }
  @GetMapping("/pairs/{student}/{tutor}")
  public List<Trial> pair(@AuthenticationPrincipal Account u,@PathVariable String student,@PathVariable String tutor) {
    return trials.pair(u,student,tutor);
  }
  @PostMapping public Trial propose(@AuthenticationPrincipal Account u,@Valid @RequestBody Propose r) {
    return trials.propose(u,r);
  }
  @PutMapping("/{id}") public Trial respond(@AuthenticationPrincipal Account u,@PathVariable String id,@Valid @RequestBody Respond r) {
    return trials.respond(u,id,r);
  }
}
