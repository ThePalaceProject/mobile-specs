API
===

Note: We should likely version this API such that the paths are `/v1/bookmarks`
or some equivalent.

## /bookmarks (GET)

Calling `/bookmarks` with a `GET` request will return a `BookmarkList`.

### Parameters

#### BookID

If the `bookId` parameter is included, then only bookmarks with the given
`bookId` are returned.

#### Page

If the `page` parameter is included, bookmarks in the given page are returned.

### Returns

A `200` status code and a body of type `BookmarkList`.

### Example

```
$ curl https://www.example.com/bookmarks
{
  "%schema": "urn:org.thepalaceproject.bookmarks:2.0",
  "bookmarksNext": "https://example.com/bookmarks?page=3",
  "bookmarks": [
    {
      "@version": 1,
      "id": "c20eb00d-1d9a-4bc8-a494-b33593788471",
      "bookId": "urn:uuid:15e0b960-649f-4386-a04e-127dd2e9713b",
       ...
```

## /bookmarks (POST)

Calling `/bookmarks` with a `POST` request, where the body of the request
is a serialized `BookmarkList` value, will create or update any
corresponding bookmarks on the server.

Note: Applications *SHOULD NOT* send bookmarks for which they are not the
creating device. By following this rule, cooperating applications can avoid
having deleted bookmarks accidentally sent to the server again by other peers.

### Example

```
$ cat data.json
{
  "%schema": "urn:org.thepalaceproject.bookmarks:2.0",
  "bookmarks": [
    {
      "@version": 1,
      "id": "c20eb00d-1d9a-4bc8-a494-b33593788471",
      "bookId": "urn:uuid:15e0b960-649f-4386-a04e-127dd2e9713b",
       ...

$ curl -d @data.json -X POST https://www.example.com/bookmarks
```

### Returns

A `200` status code and an empty body.

## /bookmarks (DELETE)

Calling `/bookmarks` with a `DELETE` request, where the body of the request
is an array of bookmark identifiers, will delete bookmarks with those
identifiers on the server.

### Returns

A `200` status code and an empty body.

### Example

```
$ cat data.json
["c20eb00d-1d9a-4bc8-a494-b33593788471",
 "90643058-560b-4319-90a2-18e8de1b71d9",
 "649bf1c5-d41b-4841-95cb-3bb1f60a33df"]
$ curl -d @data.json -X DELETE https://www.example.com/bookmarks
```
