Stardog Groovy
==========

Licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)  
_Current Version **2.1.3**_ 

Stardog Groovy - Groovy language bindings to use to develop apps with the [Stardog Graph / RDF Database](http://stardog.com).  

![Stardog](http://docs.stardog.com/img/sd.png)   

## What is it? ##

This bindings provides a set of idiomatic Groovy APIs for interacting with the Stardog database, similar to the Stardog Spring project - an easy to use method for creating connection pools, and the ability run queries over them.

To run the queries, Stardog Groovy uses standard Groovy patterns, such as passing in a closure to iterate over result sets.  In many ways, it is similar to Groovy SPARQL, the SQL bindings in Groovy, etc.

Significant changes in 2.1.3:

*    Installation now available via Maven Central and "com.complexible.stardog:stardog-groovy:2.1.3" dependency
*    No longer a dependency on Spring, i.e. the Stardog-Spring DataSource can no longer be passed as a constructor.  The Stardog Groovy class performs all the same operations.


## Examples ##

Create a new embedded database in one line

	def stardog = new Stardog([home:"/opt/stardog", to:"testgroovy", username:"admin", password:"admin"])

Collect query results via a closure

	def list = []
	stardog.query("select ?x ?y ?z WHERE { ?x ?y ?z } LIMIT 2", { list << it } )
	// list has the two Sesame BindingSet's added to it, ie TupleQueryResult.next called per each run on the closure

Collect query results via projected result values

    stardog.each("select ?x ?y ?z WHERE { ?x ?y ?z } LIMIT 2", {
       println x // whatever x is bound to in the result set
       println y // ..
       println z // 
    }

Like query, this is executed over each TupleQueryResult

Insert multidimensional arrays, single triples also works

	stardog.insert([ ["urn:test3", "urn:test:predicate", "hello world"], ["urn:test4", "urn:test:predicate", "hello world2"] ])

Remove triples via a simple groovy list

	stardog.remove(["urn:test3", "urn:test:predicate", "hello world"])

## Development ##

To get started, just clone the project. You'll need a local copy of Stardog to be able to run the build. For more information on starting the Stardog DB service and how it works, go to [Stardog's documentation](http://stardog.com/docs/), where you'll find everything you need to get up and running with Stardog.

Go to [http://stardog.com](http://stardog.com), download and install the database. If you want to use Stardog Spring, pick up the latest jar and pom and install in your local Maven repo.  

Stardog-groovy does have a dependency on stardog-spring.  For 1.x, this requires a build of stardog-spring and placing the stardog-spring.jar file in stardog-1.x/lib.  For 2.x, Stardog Spring is available with a pom file, and the Stardog Spring installs the jar in your local .m2 folder.

Once you have the local project, start up a local Stardog and create a testdb with "stardog-admin db create -n testdb".

You can then build the project

    gradle build    # validate all the test pass
    gradle install  # install jar into local m2

That will run all the JUnit tests and create the jar in build/libs.  The test does use a running Stardog, started out of band from JUnit.

As of Stardog-Groovy 2.1.2, the jar is now built with Groovy 2.2.2 as a dependency

## NOTE ##

This framework is in continuous development, please check the [issues](https://github.com/clarkparsia/stardog-groovy/issues) page. You're welcome to contribute.

## License

Copyright 2012-2014 Clark & Parsia, Al Baker

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

* [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)  

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.


