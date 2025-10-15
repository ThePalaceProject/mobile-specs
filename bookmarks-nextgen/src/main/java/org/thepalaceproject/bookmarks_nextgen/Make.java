package org.thepalaceproject.bookmarks_nextgen;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public final class Make
{
  private Make()
  {

  }

  private static final ObjectMapper MAPPER =
    new ObjectMapper();

  public static void main(
    final String[] args)
    throws Exception
  {
    final var sourceFile =
      Paths.get("README.md.in");
    final var schemaFile =
      Paths.get("bookmarks.json.schema");

    final ObjectNode schemaObject =
      (ObjectNode) MAPPER.readTree(schemaFile.toFile());

    final var lines =
      Files.readAllLines(sourceFile, StandardCharsets.UTF_8);

    for (final var line : lines) {
      if ("%schema".equals(line.trim())) {
        emitSchema(schemaObject);
        continue;
      }
      if (line.startsWith("%schema-object")) {
        emitJsonSchemaObject(schemaObject, line);
        continue;
      }

      System.out.println(line);
    }
  }

  private static void emitSchema(
    final ObjectNode schemaObject)
    throws JsonProcessingException
  {
    System.out.println("```json");
    System.out.println(
      MAPPER.writerWithDefaultPrettyPrinter()
        .writeValueAsString(schemaObject)
    );
    System.out.println("```");
  }

  private static void emitJsonSchemaObject(
    final ObjectNode schemaObject,
    final String line)
    throws JsonProcessingException
  {
    final var segments =
      List.of(line.split("\\s+"));
    final var objectName =
      segments.get(1);
    final var defs =
      schemaObject.get("$defs");
    final var object =
      defs.get(objectName);

    System.out.println("```json");
    System.out.println(
      MAPPER.writerWithDefaultPrettyPrinter()
        .writeValueAsString(object)
    );
    System.out.println("```");
  }
}