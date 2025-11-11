package org.thepalaceproject.bookmarks_nextgen;

import java.util.Arrays;

public final class Main
{
  private Main()
  {

  }

  public static void main(
    final String[] args)
    throws Exception
  {
    if (args.length < 1) {
      usage();
      throw new IllegalArgumentException();
    }

    switch (args[0]) {
      case "make" -> {
        Make.main(Arrays.copyOfRange(args, 1, args.length));
      }
      case "validate" -> {
        Validate.main(Arrays.copyOfRange(args, 1, args.length));
      }
      default -> {
        usage();
        throw new IllegalStateException("Unexpected value: " + args[0]);
      }
    }
  }

  private static void usage()
  {
    System.err.println("Usage: make | validate");
  }
}
