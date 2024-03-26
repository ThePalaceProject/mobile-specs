audiobook-reading-order-ids
===

## Motivation

We consume audiobooks from lots of different distributors. Each audiobook comes with a _manifest_
that describes the chapters within the book and may contain links to the audio files that make
up each chapter. Manifests come in a wide variety of different formats, and our audiobook APIs 
effectively abstract over the different formats in order to provide a uniform API to our 
applications.

One aspect that all manifest formats have in common is that they provide a list of
_reading order items_. These are the distinct, unique objects that make up the actual audio of
the book. The basic intuition is that, if you were to place all of the reading order items 
end-to-end and combine them into a single audio file, you would get the entire audio of the book
in the order in which it is intended to be heard.

In order to create reliable [bookmarks](/bookmarks) that can be shared across platforms, we need
to have a means to unambiguously refer to _reading order items_ within manifests. Some manifest
formats already give us unique identifiers that can be used, whilst other formats do not. For
the formats that do not, we need a reliable, deterministic scheme that can be used to generate
identifiers that can be shared across different implementations.

## Requirements

Reading order identifiers must have the following properties:

* Identifiers must be _unique_ within a given book. No two reading order items can have the same
  identifier.
* Identifiers must be _stable_. Some manifest formats (notably Overdrive) contain information that
  has a short expiration date and must be continuously refreshed during use. If identifiers were
  to be derived from this short-lived content, then it would be impossible to place the identifiers
  inside long-lived data such as bookmarks, as the identifiers would become useless soon after
  creation.
* The generation of identifiers must be _deterministic_. There must be a straightforward,
  deterministic algorithm that can generate identifiers; for a given manifest `m`, the set
  of identifiers generated for `m` must not vary between generations.

## Definition

A _reading order identifier_ should be considered to be an opaque string value in no particular
format. Two identifiers `a` and `b` are equal iff all of the following conditions hold:

* The length of `a` is equal to the length of `b`.
* For identifiers `a` and `b` of length `n`, for each `i` in `[0, n)`, `a[i] = b[i]`.

Identifiers have no defined order; no identifier should be considered to be "less than" or
"greater than" any other identifier.

Identifiers may occasionally look like [URI](https://datatracker.ietf.org/doc/html/rfc3986)
values, but should not be treated as such: There are no guarantees that an identifier is unique
across all possible manifests; identifiers are only guaranteed to be unique within a particular
manifest.

For reasons of sanity, identifiers must be of a non-zero length.

## Generation

The following sections describe the rules that should be used to generate identifiers for different
types of manifests. Applications should try each rule in turn, falling back to the [Fallback](#fallback)
rule if none of the preceding rules apply.

### WebPub Manifests

Manifests in [WebPub manifest](https://github.com/readium/webpub-manifest) format are required to
contain a `readingOrder` array where each object in the array contains a `href` property that
is expected to be unique within that `readingOrder` array.

If the object does _not_ also contain a `templated` property, then the reading order identifier
should be equal to the value of the `href` property. The reason for avoiding the use of `href`
values that are marked as `templated` is that, in the presence of URI templating, the values of
the `href` field may not actually be unique: It's permitted (although unusual and obscure) for
every `templated` `href` value to be the same, and for each reading order item to contain a
`properties` object containing values to be substituted into the `href` in order to produce a
unique URI. This is a level of complexity that we currently do not want to mandate that players
handle correctly, due to the apparent almost nonexistent use of `templated` links.

### Findaway

Manifests in Findaway audiobooks loosely adhere to the WebPub manifest specification except for
the fact that they typically do not provide any kind of `href` value in `readingOrder` arrays
at all. Instead, each `readingOrder` item will contain the proprietary, integer-typed extension
properties `findaway:part` and `findaway:sequence`. These integer-typed values are accepted by
the proprietary AudioEngine player to select chapters. In any given manifest, the combination
of `findaway:part` and `findaway:sequence` values can be trusted to be unique.

The identifier that applications should generate for a reading order item with
`findaway:part = p` and `findaway:sequence = s` is:

```
urn:org.thepalaceproject:findaway:p:s
```

Newer manifests _may_ include `href` values. If the manifest does contain a non-`templated` 
`href` value, that value should be used in favour of this generation scheme (as, in that case, 
the manifest is essentially a valid [WebPub manifest](#webpub-manifests)).

### Overdrive

Manifests from Overdrive are in a completely proprietary JSON format. The manifests contain a
`contentlinks` array that looks similar to:

```
"contentlinks" : [
 {
    "href" : "https://example.com/data?body=tokentokentokentokentokenQwMS5tcDMifX0%3D&s=ess",
    "type" : "text/html",
    "physicalFileLengthInBytes" : 35399262
 },
 {
    "physicalFileLengthInBytes" : 26515957,
    "type" : "text/html",
    "href" : "https://example.com/data?body=tokentokentokentokentokenQwMi5tcDMifX0%3D&s=ess"
 },
```

Each `href` property inside each `contentlinks` element has a value that is a URI that has a 
short expiration date. Therefore, the values of these `href` properties are not _stable_ and
therefore cannot be used to derive stable identifiers. As none of the other properties within
the `contentlinks` elements will uniquely identify that element, the only option applications
have is to refer to elements by integer index.

We therefore number each element of the `contentlinks` array, starting at `0`, and
the identifier that applications should generate for a reading order item with integer index `i`
is equal to that of the [Fallback](#fallback) rule for a reading order item `i`.

### Fallback

If none of the above rules apply for the current manifest, then the application must resort to
generating identifiers based on an integer index into an array of reading order items. For
a reading order item index `i`, starting at `0`, the identifier generated must be:

```
urn:org.thepalaceproject:reading_order_item:i
```

In pseudocode:

```
x : Array[ReadingOrderItem]
o : Array[Identifier]

for i in 0 .. (length x)
  o[i] = Identifier("urn:org.thepalaceproject:reading_order_item:i")
```

## Examples

### WebPub

Given the following `readingOrder` taken from a [WebPub manifest](#webpub-manifests):

```json
{
  "readingOrder": [
    {
      "href": "https://example.com/c0",
      "type": "text/html",
      "title": "Chapter 1"
    },
    {
      "href": "https://example.com/c1",
      "type": "text/html",
      "title": "Chapter 2"
    }
  ]
}
```

The application must generate the identifier `https://example.com/c0` for
the first item and `https://example.com/c1` for the second. This is a standard manifest with
non-`templated` URIs, and therefore the `href` values can be trusted to be unique.

### WebPub Templated

Given the following `readingOrder` taken from a [WebPub manifest](#webpub-manifests):

```json
{
  "readingOrder": [
    {
      "href": "https://example.com/{c}",
      "type": "text/html",
      "templated": "true",
      "title": "Chapter 1",
      "properties": {
        "c": "23"
      }
    },
    {
      "href": "https://example.com/{c}",
      "type": "text/html",
      "templated": "true",
      "title": "Chapter 2",
      "properties": {
        "c": "25"
      }
    }
  ]
}
```

The application must generate the identifier `urn:org.thepalaceproject:reading_order_item:0` for 
the first item and `urn:org.thepalaceproject:reading_order_item:1` for the second. This is due to the
use of `templated` URIs.

### Findaway

Given the following `readingOrder` taken from a Findaway manifest:

```json
{
  "readingOrder": [
    {
      "findaway:part": 1,
      "title": "Part 1 Chapter 2",
      "findaway:sequence": 1,
      "type": "audio/mpeg",
      "duration": 360.672
    },
    {
      "findaway:part": 1,
      "title": "Part 1 Chapter 2",
      "findaway:sequence": 2,
      "type": "audio/mpeg",
      "duration": 360.672
    }
  ]
}
```

The application must generate the identifier `urn:org.thepalaceproject:findaway:1:1` for
the first item and `urn:org.thepalaceproject:findaway:1:2` for the second.

### Overdrive

Given the following Overdrive manifest:

```json
{
  "contentlinks": [
    {
      "href": "https://example.com/data?body=tokentokentokentokentokenQwMS5tcDMifX0%3D&s=ess",
      "type": "text/html",
      "physicalFileLengthInBytes": 35399262
    },
    {
      "physicalFileLengthInBytes": 26515957,
      "type": "text/html",
      "href": "https://example.com/data?body=tokentokentokentokentokenQwMi5tcDMifX0%3D&s=ess"
    }
  ]
}
```

The application must generate the identifier `urn:org.thepalaceproject:reading_order_item:0` for
the first item and `urn:org.thepalaceproject:reading_order_item:1` for the second.
