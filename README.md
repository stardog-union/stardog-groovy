Stardog Groovy
==========

Licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)  
_Current Version **2.0.0**_ 

Stardog Groovy - Groovy language bindings to use to develop apps with the [Stardog Graph / RDF Database](http://stardog.com).  

![Stardog](http://docs.stardog.com/img/sd.png)   

## What is it? ##

This bindings provides a similar set of functionality to the Stardog Spring project - an easy to use method for creating connection pools, and the ability run queries over them.

To run the queries, Stardog Groovy uses standard Groovy patterns, such as passing in a closure to iterate over result sets.  In many ways, it is similar to Groovy SPARQL, the SQL bindings in Groovy, etc.

These bindings also integrate with [Stardog Spring](https://github.com/clarkparsia/stardog-spring) and can be configured with the DataSource, making [Grails](http://grails.org) integration easier.

## Examples ##

Create a new embedded database in one line

	def stardog = new Stardog([embedded:true, createIfNotPresent:true, home:"/opt/stardog", to:"testgroovy", username:"admin", password:"admin"])

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

Go to [http://stardog.com](http://stardog.com), download and install the database. If you want to use Stardog Spring, pick up the latest jar and drop in the stardog/lib folder

Stardog-groovy does have a dependency on stardog-spring.  For 1.x, this requires a build of stardog-spring and placing the stardog-spring.jar file in stardog-1.x/lib.  For future releases of Stardog, ie 2.x, Stardog dependencies will be available in Maven and this step will be eliminated.

Once you have the local project, change the build.gradle file 

    def stardogLocation = "/home/wherever/tools/stardog/stardog-1.1/lib"

You can then build the project

    gradle build

That will run all the JUnit tests and create the jar in build/libs.  The test case does reference stardog.home, so you may want to change TestStardog.groovy

All tests should pass. 

As of Stardog-Groovy 0.2, the jar is now built with Groovy 2.0.6 as a dependency

## NOTE ##

This framework is in continuous development, please check the [issues](https://github.com/clarkparsia/stardog-groovy/issues) page. You're welcome to contribute.

