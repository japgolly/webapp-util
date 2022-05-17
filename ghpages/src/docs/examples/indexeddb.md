{% laika.title = IndexedDB %}

IndexedDb Example
=================

Below will demonstrate a few different ways of using the IndexedDB API.
If you're impatient feel free to jump straight to the [Usage] section.

## Models

For our demo, we start with a few model classes.

@:sourceFile(IDBExampleModels.scala)

## Protocols

Now we'll define our IndexedDB protocols and demonstrate some nice features like
easy-to-use data compression and encryption.

@:sourceFile(IDBExampleProtocols.scala)

## Usage

Here we get to actual usage.

@:sourceFile(IDBExample.scala)

## Testing

Here's a quick test that demonstrates we can save and load a `Person`.

To test your `IndexedDb` code, you would typically:

1. Write your code in such a way that it asks for an `IndexedDb` instance as a normal function argument
2. Create a `FakeIndexedDb()` instance
3. Simply provide the `FakeIndexedDb()` to your main code
4. (Optionally) inspect the DB contents directly by using the `IndexedDb` API as normal

@:sourceFile(IDBExampleTest.scala)
