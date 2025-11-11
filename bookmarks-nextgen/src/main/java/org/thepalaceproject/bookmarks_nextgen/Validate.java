package org.thepalaceproject.bookmarks_nextgen;

import com.networknt.schema.ExecutionConfig;
import com.networknt.schema.InputFormat;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public final class Validate
{
  private Validate()
  {

  }

  public static void main(
    final String[] args)
    throws Exception
  {
    if (args.length != 2) {
      System.err.println("Usage: schema.json file.json");
      throw new IllegalArgumentException();
    }

    final var schemaFile =
      Paths.get(args[0]);
    final var sourceFile =
      Paths.get(args[1]);

    final var jsonSchemaFactory =
      JsonSchemaFactory.getInstance(
        SpecVersion.VersionFlag.V202012, builder -> {

        }
      );

    final var builder =
      SchemaValidatorsConfig.builder();
    final var config =
      builder.build();
    final var schema =
      jsonSchemaFactory.getSchema(
        SchemaLocation.of(
          schemaFile.toAbsolutePath()
            .toUri()
            .toString()
        ), config
      );

    final var text =
      Files.readString(sourceFile, StandardCharsets.UTF_8);

    final var assertions =
      schema.validate(
        text, InputFormat.JSON, executionContext -> {
          final var execConfig = executionContext.getExecutionConfig();
          execConfig.setFormatAssertionsEnabled(true);
          execConfig.setDebugEnabled(true);
        });

    for (final var assertion : assertions) {
      System.err.printf(
        "error: (%s): %s: %s%n",
        assertion.getSchemaLocation(),
        assertion.getCode(),
        assertion.getError()
      );
    }
  }
}
