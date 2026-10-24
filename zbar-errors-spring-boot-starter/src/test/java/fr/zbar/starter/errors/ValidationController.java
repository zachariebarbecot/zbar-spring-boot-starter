package fr.zbar.starter.errors;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ValidationController {

  @PostMapping("/signup")
  String signup(@Valid @RequestBody Signup signup) {
    return signup.username();
  }

  @GetMapping("/items")
  String items(@RequestParam @Min(1) int size) {
    return "ok";
  }

  @GetMapping("/items/{id}")
  String item(@PathVariable int id) {
    return "ok";
  }

  @PostMapping("/range")
  String range(@Valid @RequestBody Range range) {
    return "ok";
  }

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Constraint(validatedBy = OrderedRange.Validator.class)
  @interface OrderedRange {
    String message() default "from must not exceed to";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<OrderedRange, Range> {
      @Override
      public boolean isValid(Range range, ConstraintValidatorContext context) {
        return range.from() <= range.to();
      }
    }
  }

  @OrderedRange
  record Range(int from, int to) {}

  record Signup(@NotBlank String username, @Size(min = 8) String password) {}
}
