#!/bin/sh -ex

mvn package
java -jar target/bookmarks-nextgen-1.0.0-main.jar make | tee README.md
