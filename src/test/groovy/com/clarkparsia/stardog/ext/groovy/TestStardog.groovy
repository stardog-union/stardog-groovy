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
package com.clarkparsia.stardog.ext.groovy;

import static org.junit.Assert.*;

import com.clarkparsia.stardog.api.Query
import org.junit.Before;
import org.junit.Test;
import org.openrdf.query.TupleQueryResult

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
		stardog = new Stardog([embedded:true, createIfNotPresent:true, home:"/opt/stardog", to:"testgroovy", username:"admin", password:"admin"])
		stardog.insert([["urn:test1", "urn:test:predicate", "hello world"],
					   ["urn:test2", "urn:test:predicate", "hello world2"]])
	}

	/**
	 * Test method for {@link com.clarkparsia.stardog.ext.groovy.Stardog#withConnection(groovy.lang.Closure)}.
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
				Query query = con.query(queryString);
				result = query.executeSelect();
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
	 * Test method for {@link com.clarkparsia.stardog.ext.groovy.Stardog#query(java.lang.String, groovy.lang.Closure)}.
	 */
	@Test
	public void testQuery() {
		assertNotNull(stardog)
		def list = []
		stardog.query("select ?x ?y ?z WHERE { ?x ?y ?z } LIMIT 2", { list << it } )
		assertTrue(list.size == 2)
	}

	@Test
	public void testInsertRemove() {
		assertNotNull(stardog)
		stardog.insert([["urn:test3", "urn:test:predicate", "hello world"],
			["urn:test4", "urn:test:predicate", "hello world2"]])
		stardog.remove(["urn:test3", "urn:test:predicate", "hello world"])
		stardog.remove(["urn:test4", "urn:test:predicate", "hello world2"])
	}
}
