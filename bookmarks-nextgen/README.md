bookmarks-nextgen
==

## Motivation

At the time of writing (October 2025), the Palace client applications and
server are using an incredibly complicated bookmark system with poorly-defined
formats and an API that is not fit for any purpose.

This specification attempts to define a new API and formats for a bookmark
system that matches the needs of the applications as they currently stand.

New features are mostly out-of-scope; the purpose of this specification is to
provide a system that largely does what the existing system already does, but
without the years of technical debt and design flaws that are hindering
further development. The intention is also to define the specification in a 
manner that allows for future expansion.

## Design

The formats and APIs defined here are _strict_ and _versioned_ in order to
allow for safe evolution. The existing system that we are replacing
is _unversioned_ and _permissive_, which has resulted in each of the 
applications speaking their own dialect (meaning disjoint subsets of information 
that each considers _mandatory_), and no safe way to evolve the formats without
breaking clients.

We carefully distinguish _mandatory_ information from _useful but optional_
information. For example, bookmark [locators](#locator) are _mandatory_
because, without a locator, a bookmark can't function as a bookmark. However,
the [creation date](#metadata) of a bookmark is _optional_ information; it is
useful for display to the user, but a bookmark without a creation date can
still function as a bookmark.

Applications are permitted to behave differently and make decisions based
on _mandatory_ information in a bookmark, and *MUST NOT* behave differently
or make decisions based on _optional_ information. History has shown that,
in the absence of strict rules around behavior, applications quickly become
implicitly dependent on their own _optional_ information, and break when
bookmarks produced by other applications do not contain that supposedly
_optional_ information.

Additionally, schema definitions, once published, are immutable and *MUST NOT* 
be changed. If, for example, the application sprouts new functionality that 
requires more mandatory information to be placed into a [locator](#locator), 
a new _version_ of the given locator type should be added to the schema with 
an incremented version number, and the old version(s) *MUST* be left unchanged.

## Bookmarks

A _bookmark_ consists of:

  * A mandatory [unique identifier](#identifier).
  * A mandatory [book identifier](#book-identifier).
  * A mandatory [device identifier](#device-identifier).
  * A mandatory [locator](#locator).
  * A set of optional [descriptive metadata](#metadata).

An example bookmark is as follows:

```json
{
  "id": "c20eb00d-1d9a-4bc8-a494-b33593788471",
  "bookId": "urn:uuid:15e0b960-649f-4386-a04e-127dd2e9713b",
  "device": {
    "deviceId": "c108e1b1-218c-4b5c-a4d3-88b58d5ef49f",
    "deviceName": "Alice's Lenovo Yoga Book"
  },
  "locator": {
    "@type": "LocatorHrefProgression",
    "@version": 1,
    "href": "page2.xhtml",
    "progressWithinChapter": 0.58
  },
  "metadata": {
    "creationTime": "2025-10-15T16:30:04+00:00",
    "chapterTitle": "How To Read This Book"
  }
}
```

### Identifier

Each _bookmark_ is assigned a _unique identifier_ upon creation. The
_unique identifier_ takes the form of an 
[RFC 9562 UUID](https://www.rfc-editor.org/rfc/rfc9562.html).

```json
{
  "description" : "A unique identifier for a bookmark.",
  "type" : "string",
  "format" : "uuid"
}
```

If two _bookmarks_ have the same _unique identifier_ then, regardless of their
other contents, they are to be treated as _the same bookmark_. It follows that
the _unique identifier_ of a bookmark cannot be changed; if the identifier is
changed, the result is a new bookmark.

#### Rationale

In the system we are replacing, bookmarks do not have identifiers. Applications,
therefore, have to hash the entire contents of the bookmarks if they want to
provide any kind of stable "handle" to allow for modifying and/or deleting
bookmarks.

Additionally, bookmarks that have been received on a device from the server 
have to be laboriously compared field-by-field to determine if they are "the
same" as any local bookmarks.

### Book Identifier

Each _bookmark_ has an associated _book identifier_. This is a value that
identifies the book with which the bookmark is associated, and can effectively
have any format. In practice, this book identifier will be the `id` value
that appears in the OPDS feed entry for the book.

### Device Identifier

Each _bookmark_ is assigned a _device identifier_ upon creation. The
_device identifier_ is an object consisting of a UUID value unique to the
device, and a human-readable _device name_.

```json
{
  "description" : "A unique identifier for a device.",
  "type" : "object",
  "properties" : {
    "deviceId" : {
      "description" : "A unique identifier for a device.",
      "type" : "string",
      "format" : "uuid"
    },
    "deviceName" : {
      "description" : "A human-readable name for the device.",
      "type" : "string"
    }
  },
  "additionalProperties" : false,
  "required" : [ "deviceId", "deviceName" ]
}
```

An example device identifier is as follows:

```json
{
  "deviceId": "c108e1b1-218c-4b5c-a4d3-88b58d5ef49f",
  "deviceName": "Alice's Lenovo Yoga Book"
}
```

Note that the _device identifier_ records the device that _created_ the
bookmark. If, for example, the Palace Circulation manager receives a bookmark
from a device and inserts new metadata into it, then it *MUST* leave the
_device identifier_ untouched.

Applications *SHOULD* create a persistent _device identifier_ on installation
and re-use it for as long as possible. Applications *SHOULD* attempt to come
up with a useful friendly device name (some platforms provide APIs for this
information directly), and *MAY* allow users to set their own device names.

#### Rationale

The intended use for the _device identifier_ is to allow users to be told
which device created which bookmark. For example, a user might start reading
a book on one device `X`, and then later switch to device `Y` to continue
reading. The user can look in their list of bookmarks and see that a bookmark 
exists for a particular time period that was created by device `X`. They can
then select this bookmark and continue from where they left off on device `X`.

### Locator

A _locator_ describes a position within a book. Locators are _typed_
and _versioned_. When examining a locator within a bookmark, applications
*MUST* inspect the `@type` and `@version` properties of the locator object
in order to (unsurprisingly) determine the type and format version prior to
further use.

#### LocatorHrefProgression1

A `LocatorHrefProgression1` object describes a position in a book in terms of
a chapter `href` and a real-valued `progressWithinChapter` offset from the
start of the chapter. The `progressWithinChapter` offset must be in the range
`[0, 1]` where `0` means "at the exact start of the chapter" and `1` means
"at the exact end of the chapter".

```json
{
  "type" : "object",
  "properties" : {
    "@type" : {
      "description" : "The type of locator.",
      "type" : "string",
      "pattern" : "LocatorHrefProgression"
    },
    "@version" : {
      "description" : "The version of the locator format",
      "type" : "number",
      "minimum" : 1,
      "exclusiveMaximum" : 2
    },
    "href" : {
      "description" : "The unique identifier for a chapter.",
      "type" : "string"
    },
    "progressWithinChapter" : {
      "description" : "The progress within a chapter.",
      "type" : "number",
      "minimum" : 0.0,
      "maximum" : 1.0
    }
  },
  "additionalProperties" : false,
  "required" : [ "@type", "@version", "href", "progressWithinChapter" ]
}
```

An example locator is as follows:

```json
{
  "@type": "LocatorHrefProgression",
  "@version": 1,
  "href": "/xyz.html",
  "progressWithinChapter": 0.666
}
```

#### LocatorAudioBookTime1

A `LocatorAudioBookTime1` object describes a position in a book in terms of
a `readingOrderItem` URI and a `readingOrderItemOffsetMilliseconds` value
representing the offset in milliseconds from the start of the reading order
item.

The `readingOrderItem` *MUST* be a URI conforming to the
[audiobook-reading-order-ids](https://github.com/ThePalaceProject/mobile-specs/tree/main/audiobook-reading-order-ids)
specification.

```json
{
  "type" : "object",
  "properties" : {
    "@type" : {
      "description" : "The type of locator",
      "type" : "string",
      "pattern" : "LocatorAudioBookTime"
    },
    "@version" : {
      "description" : "The version of the locator format",
      "type" : "number",
      "minimum" : 1,
      "exclusiveMaximum" : 2
    },
    "readingOrderItem" : {
      "description" : "The reading order item within the book.",
      "type" : "string",
      "format" : "uri"
    },
    "readingOrderItemOffsetMilliseconds" : {
      "description" : "The offset from the start of the reading order item within the book.",
      "type" : "number",
      "minimum" : 0
    }
  },
  "additionalProperties" : false,
  "required" : [ "@type", "@version", "readingOrderItem", "readingOrderItemOffsetMilliseconds" ]
}
```

An example locator is as follows:

```json
{
  "@type": "LocatorAudioBookTime",
  "@version": 1,
  "readingOrderItem" : "urn:org.thepalaceproject:readingOrderItem:23",
  "readingOrderItemOffsetMilliseconds" : 25000
}
```

#### LocatorIntegerPage1

A `LocatorIntegerPage1` object describes a position in a book in terms of a
single integer page value.

```json
{
  "type" : "object",
  "properties" : {
    "@type" : {
      "description" : "The type of locator",
      "type" : "string",
      "pattern" : "LocatorIntegerPage"
    },
    "@version" : {
      "description" : "The version of the locator format",
      "type" : "number",
      "minimum" : 1,
      "exclusiveMaximum" : 2
    },
    "page" : {
      "description" : "The page number within the book.",
      "type" : "number"
    }
  },
  "additionalProperties" : false,
  "required" : [ "@type", "@version", "page" ]
}
```

An example locator is as follows:

```json
{
  "@type": "LocatorPage",
  "@version": 1,
  "page": 23
}
```

#### Rationale

Different types of books have different requirements on position information. 
For example, audiobooks tend to have their positions expressed in terms of an 
audio file name and an offset in milliseconds. In contrast, PDF files tend to 
work solely in terms of integer page numbers. Reading systems for individual 
formats tend to experiment with different ways to express position information 
over time, and therefore _locators_ need a degree of extensibility that the
rest of the bookmark does not. We therefore require all locators to carry an
explicit type, and an explicit version number to indicate against which schema
structures they are expected to validate. In our experience, locator types
come and go fairly frequently, so the extra complexity of being able to
separately version them from the schema itself is justified.

### Metadata

Each _bookmark_ can have _optional metadata_. A few properties in the _metadata_
object are defined, but applications are largely free to place whatever
properties they want into the object. However, applications *MUST* continue
to function correctly if the _metadata_ object is entirely removed from the
bookmark.

#### CreationTime

The `creationTime` property, if present, provides a full 
[ISO 8601](https://en.wikipedia.org/wiki/ISO_8601) timestamp indicating the
time and date on which the bookmark was created. Applications *MUST* include
full time zone information and *SHOULD* create bookmarks in the `UTC` time
zone. Applications *SHOULD* _display_ bookmark times in the device's current
time zone.

```json
{
  "metadata": {
    "creationTime": "2025-10-15T17:48:33+00:00"
  }
}
```

#### ChapterTitle

The `chapterTitle` property, if present, provides the title of the chapter
in which the bookmark appears.

```json
{
  "metadata": {
    "chapterTitle": "How To Read This Book"
  }
}
```

#### BookTitle

The `bookTitle` property, if present, provides the title of the book
in which the bookmark appears.

```json
{
  "metadata": {
    "bookTitle": "Elementary Molecular Assembly For The Oblivious (Volume III)"
  }
}
```


## Bookmark Lists

A _bookmark list_ is a document that provides a list of _bookmarks_.

The `%schema` property *MUST* be present and *MUST* be set to a value of
`urn:org.thepalaceproject.bookmarks:2.0`.

The `bookmarks` property contains a possibly-empty list of [bookmarks](#bookmarks).

The `bookmarksNext` property allows the producer of the list to support
pagination; if a `bookmarksNext` property is present, the URI will return
the next page of bookmarks.

```json
{
  "%schema": "urn:org.thepalaceproject.bookmarks:2.0",
  "bookmarksNext": "https://example.com/bookmarks?page=3",
  "bookmarks": [
    {
      "id": "c20eb00d-1d9a-4bc8-a494-b33593788471",
      "bookId": "urn:uuid:15e0b960-649f-4386-a04e-127dd2e9713b",
      "device": {
        "deviceId": "c108e1b1-218c-4b5c-a4d3-88b58d5ef49f",
        "deviceName": "Alice's Lenovo Yoga Book"
      },
      "locator": {
        "@type": "LocatorHrefProgression",
        "@version": 1,
        "href": "page2.xhtml",
        "progressWithinChapter": 0.58
      },
      "metadata": {
        "creationTime": "2025-10-15T16:30:04+00:00",
        "chapterTitle": "How To Read This Book"
      }
    },
    {
      "id": "1c3228b8-21b7-47c7-b818-a23603e64f67",
      "bookId": "urn:uuid:15e0b960-649f-4386-a04e-127dd2e9713b",
      "device": {
        "deviceId": "c108e1b1-218c-4b5c-a4d3-88b58d5ef49f",
        "deviceName": "Alice's Lenovo Yoga Book"
      },
      "locator": {
        "@type": "LocatorHrefProgression",
        "@version": 1,
        "href": "page2.xhtml",
        "progressWithinChapter": 0.62
      },
      "metadata": {
        "creationTime": "2025-10-15T16:33:04+00:00",
        "chapterTitle": "How To Read This Book"
      }
    }
  ]
}
```

## Schema

The full bookmark [schema](bookmarks.json.schema) is as follows:

```json
{
  "$schema" : "https://json-schema.org/draft/2020-12/schema",
  "$id" : "urn:org.thepalaceproject.bookmarks:2.0",
  "title" : "Palace Project Bookmarks 2.0",
  "description" : "A schema and API for electronic publication bookmarks",
  "oneOf" : [ {
    "$ref" : "#/$defs/Bookmark"
  }, {
    "$ref" : "#/$defs/BookmarkList"
  } ],
  "$defs" : {
    "BookmarkList" : {
      "type" : "object",
      "properties" : {
        "%schema" : {
          "description" : "The schema identifier for the document.",
          "type" : "string",
          "pattern" : "urn:org.thepalaceproject.bookmarks:2.0"
        },
        "bookmarks" : {
          "description" : "The set of applicable bookmarks.",
          "type" : "array",
          "properties" : {
            "$ref" : "#/$defs/Bookmark"
          }
        },
        "bookmarksNext" : {
          "description" : "A link to the next page of bookmarks.",
          "type" : "string",
          "format" : "uri"
        }
      },
      "required" : [ "%schema", "bookmarks" ]
    },
    "Bookmark" : {
      "type" : "object",
      "properties" : {
        "device" : {
          "$ref" : "#/$defs/DeviceIdentifier"
        },
        "id" : {
          "description" : "A unique identifier for a bookmark.",
          "type" : "string",
          "format" : "uuid"
        },
        "bookId" : {
          "description" : "A unique identifier for a book.",
          "type" : "string"
        },
        "locator" : {
          "$ref" : "#/$defs/Locator"
        },
        "metadata" : {
          "$ref" : "#/$defs/Metadata"
        }
      },
      "additionalProperties" : false,
      "required" : [ "device", "id", "bookId", "locator" ]
    },
    "Metadata" : {
      "description" : "Optional metadata for a bookmark.",
      "type" : "object",
      "additionalProperties" : true,
      "properties" : {
        "creationTime" : {
          "description" : "The creation time of a bookmark.",
          "type" : "string",
          "pattern" : "^(?:[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}(?:\\\\.[0-9]+)?(?:Z|[+-][0-9]{2}:[0-9]{2}))$"
        },
        "chapterTitle" : {
          "description" : "The title of the chapter in which the bookmark appears.",
          "type" : "string"
        },
        "bookTitle" : {
          "description" : "The title of the book in which the bookmark appears.",
          "type" : "string"
        }
      }
    },
    "DeviceIdentifier" : {
      "description" : "A unique identifier for a device.",
      "type" : "object",
      "properties" : {
        "deviceId" : {
          "description" : "A unique identifier for a device.",
          "type" : "string",
          "format" : "uuid"
        },
        "deviceName" : {
          "description" : "A human-readable name for the device.",
          "type" : "string"
        }
      },
      "additionalProperties" : false,
      "required" : [ "deviceId", "deviceName" ]
    },
    "BookmarkIdentifier" : {
      "description" : "A unique identifier for a bookmark.",
      "type" : "string",
      "format" : "uuid"
    },
    "Locator" : {
      "oneOf" : [ {
        "$ref" : "#/$defs/LocatorHrefProgression1"
      }, {
        "$ref" : "#/$defs/LocatorAudioBookTime1"
      }, {
        "$ref" : "#/$defs/LocatorIntegerPage1"
      } ]
    },
    "LocatorHrefProgression1" : {
      "type" : "object",
      "properties" : {
        "@type" : {
          "description" : "The type of locator.",
          "type" : "string",
          "pattern" : "LocatorHrefProgression"
        },
        "@version" : {
          "description" : "The version of the locator format",
          "type" : "number",
          "minimum" : 1,
          "exclusiveMaximum" : 2
        },
        "href" : {
          "description" : "The unique identifier for a chapter.",
          "type" : "string"
        },
        "progressWithinChapter" : {
          "description" : "The progress within a chapter.",
          "type" : "number",
          "minimum" : 0.0,
          "maximum" : 1.0
        }
      },
      "additionalProperties" : false,
      "required" : [ "@type", "@version", "href", "progressWithinChapter" ]
    },
    "LocatorAudioBookTime1" : {
      "type" : "object",
      "properties" : {
        "@type" : {
          "description" : "The type of locator",
          "type" : "string",
          "pattern" : "LocatorAudioBookTime"
        },
        "@version" : {
          "description" : "The version of the locator format",
          "type" : "number",
          "minimum" : 1,
          "exclusiveMaximum" : 2
        },
        "readingOrderItem" : {
          "description" : "The reading order item within the book.",
          "type" : "string",
          "format" : "uri"
        },
        "readingOrderItemOffsetMilliseconds" : {
          "description" : "The offset from the start of the reading order item within the book.",
          "type" : "number",
          "minimum" : 0
        }
      },
      "additionalProperties" : false,
      "required" : [ "@type", "@version", "readingOrderItem", "readingOrderItemOffsetMilliseconds" ]
    },
    "LocatorIntegerPage1" : {
      "type" : "object",
      "properties" : {
        "@type" : {
          "description" : "The type of locator",
          "type" : "string",
          "pattern" : "LocatorIntegerPage"
        },
        "@version" : {
          "description" : "The version of the locator format",
          "type" : "number",
          "minimum" : 1,
          "exclusiveMaximum" : 2
        },
        "page" : {
          "description" : "The page number within the book.",
          "type" : "number"
        }
      },
      "additionalProperties" : false,
      "required" : [ "@type", "@version", "page" ]
    }
  }
}
```
