package japgolly.webapputil.examples.indexeddb

import java.util.UUID

// Let's say these are the data types we want to store in IndexedDb...
final case class PersonId(value: UUID)
final case class Person(id: PersonId, name: String, age: Int)
