package fr.zbar.starter.errors;

import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
class ValidatedController {

  @GetMapping("/validated")
  String validated(@RequestParam @Min(1) int page) {
    return "ok";
  }
}
