package fr.zbar.starter.errors;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

/** Publishes the problem response schema in the generated OpenAPI document. */
class ErrorsOpenApiCustomizer implements OpenApiCustomizer {

  static final String PROBLEM_DETAIL = "ProblemDetail";

  private static final List<HttpStatus> ERROR_STATUSES =
      List.of(HttpStatus.BAD_REQUEST, HttpStatus.NOT_FOUND, HttpStatus.INTERNAL_SERVER_ERROR);

  @Override
  public void customise(OpenAPI openApi) {
    if (openApi.getComponents() == null) {
      openApi.setComponents(new Components());
    }
    openApi.getComponents().addSchemas(PROBLEM_DETAIL, problemDetail());
    if (openApi.getPaths() != null) {
      openApi
          .getPaths()
          .values()
          .forEach(path -> path.readOperations().forEach(ErrorsOpenApiCustomizer::addErrors));
    }
  }

  private static void addErrors(Operation operation) {
    for (HttpStatus status : ERROR_STATUSES) {
      operation
          .getResponses()
          .computeIfAbsent(String.valueOf(status.value()), code -> problemResponse(status));
    }
  }

  private static ApiResponse problemResponse(HttpStatus status) {
    Schema<?> reference = new Schema<>().$ref("#/components/schemas/" + PROBLEM_DETAIL);
    return new ApiResponse()
        .description(status.getReasonPhrase())
        .content(
            new Content()
                .addMediaType(
                    MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    new io.swagger.v3.oas.models.media.MediaType().schema(reference)));
  }

  private static Schema<?> problemDetail() {
    return new ObjectSchema()
        .addProperty("type", new StringSchema().example("about:blank"))
        .addProperty("title", new StringSchema())
        .addProperty("status", new IntegerSchema())
        .addProperty("detail", new StringSchema())
        .addProperty("instance", new StringSchema())
        .addProperty("traceId", new StringSchema())
        .addProperty("errors", new ArraySchema().items(violation()));
  }

  private static Schema<?> violation() {
    return new ObjectSchema()
        .addProperty("field", new StringSchema())
        .addProperty("message", new StringSchema());
  }
}
