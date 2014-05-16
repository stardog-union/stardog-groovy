/*
 * Copyright (c) the original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.complexible.stardog.ext.groovy

import static org.junit.Assert.*;

import com.complexible.stardog.api.SelectQuery

import org.junit.Before;
import org.junit.Test;
import org.openrdf.query.TupleQueryResult
import org.openrdf.model.impl.CalendarLiteralImpl;

import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * @author Al Baker
 * @author Clark & Parsia
 *
 */
class TestStardog {

	def stardog

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		stardog = new Stardog([url: "snarl://localhost:5820/", to:"testdb", username:"admin", password:"admin"])
		stardog.insert([["urn:test1", "urn:test:predicate", "hello world"],
			["urn:test2", "urn:test:predicate", "hello world2"]])
	}

	/**
	 * Test method for {@link com.complexible.stardog.ext.groovy.Stardog#withConnection(groovy.lang.Closure)}.
	 */
	@Test
	public void testWithConnection() {
		assertNotNull(stardog)
		stardog.withConnection { con ->
			def queryString = """
SELECT ?s ?p ?o
{ 
  ?s ?p ?o
}
			"""
			TupleQueryResult result = null;
			try {
				SelectQuery query = con.select(queryString);
				result = query.execute();
				while (result.hasNext()) {
					println result.next();
				}

				result.close();


			} catch (Exception e) {
				println "Caught exception ${e}"
			}
		}
	}

	/**
	 * Test method for {@link com.complexible.stardog.ext.groovy.Stardog#query(java.lang.String, groovy.lang.Closure)}.
	 */
	@Test
	public void testQuery() {
		assertNotNull(stardog)
		def list = []
		stardog.query("select ?x ?y ?z WHERE { ?x ?y ?z } LIMIT 2", { list << it } )
		assertTrue(list.size == 2)
	}
	
	/**
	 * Test method for {@link com.complexible.stardog.ext.groovy.Stardog#query(java.lang.String, groovy.lang.Closure)}.
	 */
	@Test
	public void testUpdate() {
		assertNotNull(stardog)
		stardog.update("DELETE { ?a ?b \"hello world2\" } INSERT { ?a ?b \"aloha world2\" } WHERE { ?a ?b \"hello world2\" }")
		
		def list = []
		stardog.query("SELECT ?x ?y ?z WHERE { ?x ?y \"aloha world2\" } LIMIT 2", { list << it } )
		assertTrue(list.size == 1)
		
		stardog.update("DELETE { ?a ?b \"aloha world2\" } INSERT { ?a ?b \"hello world2\" } WHERE { ?a ?b \"aloha world2\" }")
	}
	
	@Test
	public void testEach() {
		assertNotNull(stardog)
		def a
		def b
		def c
		stardog.each("select ?x ?y ?z WHERE { ?x ?y ?z } LIMIT 2", {
			a = x
			b = y
			c = z

		} )
		assertNotNull(a)
		assertNotNull(b)
		assertNotNull(c)
	}

	@Test
	public void testInsertRemove() {
		assertNotNull(stardog)
		stardog.insert([["urn:test3", "urn:test:predicate", "hello world"],
			["urn:test4", "urn:test:predicate", "hello world2"]])
		stardog.remove(["urn:test3", "urn:test:predicate", "hello world"])
		stardog.remove(["urn:test4", "urn:test:predicate", "hello world2"])
	}

	@Test
	public void testInsertRemove2() {
		assertNotNull(stardog)
		stardog.insert(["urn:test3", "urn:test:predicate", "hello world"])
		stardog.remove(["urn:test3", "urn:test:predicate", "hello world"])
		stardog.insert(["urn:test3", "urn:test:predicate", new URI("http://test")])
		stardog.remove(["urn:test3", "urn:test:predicate", new URI("http://test")])
		def date = new Date()
	
		GregorianCalendar gc = new GregorianCalendar();
		gc.setTime(date)
	//	gc.setTimeInMillis(time.getTime());
		DatatypeFactory df = DatatypeFactory.newInstance();
		XMLGregorianCalendar xc = df.newXMLGregorianCalendar (gc);
		def d = new CalendarLiteralImpl(xc)

		stardog.insert(["urn:test3", "urn:test:predicate", d])

	}
	
	@Test
	public void testInsertRemove3() {
		assertNotNull(stardog)
		stardog.insert([["urn:test5", "urn:test:predicate", "hello world"],
						["urn:test5", "urn:test:predicate2", "hello world3"],
						["urn:test6", "urn:test:predicate", "hello world2"]])
		stardog.remove(["urn:test5"])
		stardog.remove(["urn:test6", "urn:test:predicate"])
	}

	@Test
	public void testURIInsertRemove() {
		assertNotNull(stardog)
		def list = []
		stardog.insert([["urn:test1", "urn:test:predicate", "hello world"],
			["urn:test2", "urn:test:predicate", new java.net.URI("http://www.complexible.com")]])
		stardog.query("select ?s ?p {?s ?p <http://www.complexible.com> }", { list << it })
		assertEquals(list.size(), 1)
		list.clear()
		stardog.remove(["urn:test2", "urn:test:predicate", new java.net.URI("http://www.complexible.com")])
		stardog.query("select ?s ?p {?s ?p <http://www.complexible.com> }", { list << it })
		assertEquals(list.size(), 0)
		list.clear()
	}
}
