package fr.zbar.starter.errors;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class DocumentedController {

  @GetMapping("/documented")
  @ApiResponse(responseCode = "400", description = "custom bad request")
  @ApiResponse(responseCode = "500", description = "custom failure")
  String documented() {
    return "ok";
  }
}
