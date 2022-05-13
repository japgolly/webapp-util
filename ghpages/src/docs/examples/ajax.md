{% laika.title = AJAX %}

AJAX Example
============

This is an example around making and testing AJAX calls.

## Shared Code

First we create a definition of our AJAX endpoint. This is cross-compiled for both JVM and JS.

@:sourceFile(AjaxExampleShared.scala)

## Backend

@:sourceFile(AjaxExampleJvm.scala)

## Frontend

@:sourceFile(AjaxExampleJs.scala)

## Testing the Frontend

@:sourceFile(AjaxExampleJsTest.scala)
