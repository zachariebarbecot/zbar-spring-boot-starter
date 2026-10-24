package fr.zbar.starter.errors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
class SampleController {

  @GetMapping("/conflict")
  String conflict() {
    throw new ResponseStatusException(HttpStatus.CONFLICT, "already there");
  }

  @GetMapping("/boom")
  String boom() {
    throw new IllegalStateException("select * from users where password='hunter2'");
  }

  @GetMapping("/unavailable")
  String unavailable() {
    throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "database host db-internal");
  }

  @GetMapping("/teapot")
  String teapot() {
    throw new TeapotException();
  }

  @GetMapping("/read-only")
  String readOnly() {
    return "ok";
  }

  @PostMapping(path = "/json", consumes = "application/json")
  String json(@RequestBody Payload payload) {
    return payload.name();
  }

  @GetMapping("/param")
  String param(@RequestParam String name) {
    return name;
  }

  static class TeapotException extends RuntimeException {}

  record Payload(String name) {}
}
