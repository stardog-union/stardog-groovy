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

import com.complexible.common.openrdf.model.Graphs
import com.complexible.stardog.api.*
import com.complexible.stardog.reasoning.api.ReasoningType
import com.complexible.stardog.StardogException;

import org.openrdf.model.Resource
import org.openrdf.model.Value
import org.openrdf.model.impl.LiteralImpl
import org.openrdf.model.impl.StatementImpl
import org.openrdf.model.impl.URIImpl
import org.openrdf.model.impl.ValueFactoryImpl
import org.openrdf.query.TupleQueryResult

import groovy.util.logging.*

/**
 * Stardog - Groovy wrapper on top of SNARL for easy access
 * and idiomatic groovy usage of Stardog, such as closure based processing
 *  
 *  Provides simplified abstraction over connection management
 * 
 * @author Al Baker
 * @author Clark & Parsia, LLC
 *
 */
@Slf4j
class Stardog {


	// standard properties, similar to ConnectionConfiguration and Stardog-Spring DataSource
	String url
	String username
	String password
	String reasoning
	boolean createIfNotPresent = false
	String to
	boolean failAtCapacity = false
	boolean growAtCapacity = true
	int maxIdle = 100
	int maxPool = 100
	int minPool = 10
	boolean noExpiration = false
	boolean embedded = false
	String home

	private ConnectionPool pool;

	private ConnectionConfiguration connectionConfig;

	private ConnectionPoolConfig poolConfig;


	public Stardog() { }

	public Stardog(Map props) {
		url = props.url ?: null
		username = props.username ?: null
		password = props.password ?: null
		reasoning = props.reasoning ?: null
		to = props.to ?: null
		failAtCapacity = props.failAtCapacity ?: false
		growAtCapacity = props.growAtCapacity ?: true
		maxIdle = props.maxIdle ?: 100
		maxPool = props.maxPool ?: 100
		minPool = props.minPool ?: 100
		noExpiration = props.noExpiration ?: false

		if (props.home) {
			System.setProperty("stardog.home", props.home)
		}

		initialize()
	}

	void initialize() {

		connectionConfig = ConnectionConfiguration.to(to)
				
		if (reasoning != null) {
			connectionConfig = connectionConfig.reasoning(ReasoningType.valueOf(reasoning))
		}

		if (url != null) {
			connectionConfig = connectionConfig.server(url)
		}

		connectionConfig = connectionConfig.credentials(username, password)

		poolConfig = ConnectionPoolConfig
				.using(connectionConfig)
				.minPool(minPool)
				.maxPool(maxPool)

		pool = poolConfig.create();
	}


	public Connection getConnection() {
		try {
			if (pool == null)
				afterPropertiesSet();
			return pool.obtain();
		} catch (StardogException e) {
			log.error("Error obtaining connection from Stardog pool", e);
			throw new RuntimeException(e);
		}
	}

	public void releaseConnection(Connection connection) {
		try {
			pool.release(connection);
		} catch (StardogException e) {
			log.error("Error releasing connection from Stardog pool", e);
			throw new RuntimeException(e);
		}
	}


	/**
	 * <code>withConnection</code>
	 * @param Closure to execute over the connection
	 */
	public void withConnection(Closure c) {
		Connection con = getConnection()
		c.call(con)
		releaseConnection(con)
	}

	/**
	 * <code>query</code>
	 * @param queryString SPARQL query string
	 * @param closure to execute over the result set
	 */
	public void query(String queryString, Closure c) {
		query(queryString, null, c)
	}
	
	/**
	 * <code>query</code>
	 * @param queryString SPARQL query string
	 * @param args map of string and object to pass bind as input parameters
	 * @param closure to execute over the result set
	 */
	public void query(String queryString, Map args, Closure c) {
		Connection con = getConnection()
		TupleQueryResult result = null
		try {
			SelectQuery query = con.select(queryString)
			
			args?.each {
				query.parameter(it.key, it.value)
			}
			
			result = query.execute()
			while (result.hasNext()) {
				c.call(result.next())
			}

			result.close()

		} catch (Exception e) {
			throw new RuntimeException(e)
		} finally {
			releaseConnection(con)
		}
	}
	
	/**
	 * <code>update</code>
	 * @param updateString SPARQL update string
	 */
	public void update(String queryString) {
		update(queryString, null)
	}
	
	/**
	 * <code>update</code>
	 * @param updateString SPARQL update string
	 * @param args map of string and object to pass bind as input parameters
	 */
	public void update(String queryString, Map args) {
		Connection con = getConnection()
		try {
			def query = con.update(queryString)
			
			args?.each {
				query.parameter(it.key, it.value)
			}
			
			query.execute()
			
		} catch (Exception e) {
			throw new RuntimeException(e)
		} finally {
			releaseConnection(con)
		}
	}
	
	/**
	 * <code>each</code>
	 * iterates over a Binding result
	 * @param queryString
	 * @param closure with each SPARQL query bound into the closure
	 */
	public void each(String queryString, Closure c) {
		Connection con = getConnection()
		TupleQueryResult result = null
		try {
			SelectQuery query = con.select(queryString)
			result = query.execute()
			while (result.hasNext()) {
				def input = result.next().iterator().collectEntries( {
					[ (it.getName()) : (it.getValue()) ]
				})
				//println "Stardog.each(): input = ${input}"
				// binds the Sesame result set as a map into the closure so SPARQL variables
				// become closure native variables, e.g. "x"
				c.delegate = input
				c.call()
			}

			result.close()

		} catch (Exception e) {
			throw new RuntimeException(e)
		} finally {
			releaseConnection(con)
		}
	}

	/**
	 * <code>validTripleList</code>
	 * @param l representing the triple
	 * @return boolean if it is valid list, ie contains s, p, o.
	 */
	boolean validTripleList(List l) {

		if (l == null) { return false }

		if (l.size != 3) { return false }

		def s = l[0]
		def p = l[1]
		def o = l[2]

		if (s && p && o) {
			return true
		} else {
			return false
		}
	}

	/**
	 * <code>insert</code>
	 * Inserts either a single list, or a list of lists of triples
	 * assumes URIImpl(s,p)
	 * assumes LiteralImpl(o) unless a java.net.URI is passed in, in which case it will insert a URIImpl  
	 * @param arr lists
	 */
	public void insert(List arr) {
		Connection con = getConnection()
		Adder adder = null
		try {
			def statements = []
			if (arr.size >= 1) {
				if (arr[0].class == java.util.ArrayList.class) {
					arr.each { arr2 ->
						if (validTripleList(arr2)) {
							def s = arr2[0]
							def p = arr2[1]
							def o = arr2[2]
							if (o.class == java.net.URI.class) {
								statements.add(new StatementImpl(new URIImpl(s), new URIImpl(p), new URIImpl(o.toString())))
							}
							else if (o.class == java.lang.String.class) {
								statements.add(new StatementImpl(new URIImpl(s), new URIImpl(p), new LiteralImpl(o)))
							}
							else {
								statements.add(new StatementImpl(new URIImpl(s), new URIImpl(p), o))
							}
						}
					}
				} else {
					if (validTripleList(arr)) {
						def s = arr[0]
						def p = arr[1]
						def o = arr[2]
						if (o.class == java.net.URI.class) {
							statements.add(new StatementImpl(new URIImpl(s), new URIImpl(p), new URIImpl(o.toString())))
						} else if (o.class == java.lang.String.class) {
							statements.add(new StatementImpl(new URIImpl(s), new URIImpl(p), new LiteralImpl(o)))
						} else {
							statements.add(new StatementImpl(new URIImpl(s), new URIImpl(p), o))
						}
					}
				}
			}
			con.begin()
			con.add().graph(Graphs.newGraph(statements))
			con.commit()

		} catch (Exception e) {
			throw new RuntimeException(e)
		} finally {
			adder = null
			releaseConnection(con)
		}

	}


	/**
	 * <code>remove</code>
	 * @param list of format subject, predicate, object, graph URI
	 */
	public void remove(List args) {
		Connection connection = getConnection()
		def subject = args[0]
		def predicate = args[1]
		def object = args[2]
		def graphUri = args[3]

		URIImpl subjectResource = null
		URIImpl predicateResource = null
		Resource context = null

		if (subject != null) {
			subjectResource = new URIImpl(subject)
		}
		if (predicate != null) {
			predicateResource = new URIImpl(predicate)
		}

		if (graphUri != null) {
			context = ValueFactoryImpl.getInstance().createURI(graphUri)
		}

		Value objectValue = null
		if (object != null) {
			if (object.class == java.net.URI.class) {
				objectValue = new URIImpl(object.toString())
			}
			else {
				objectValue = TypeConverter.asLiteral(object)
			}
		}

		try {
			connection.begin()
			connection.remove().statements(subjectResource, predicateResource, objectValue, context)
			connection.commit()
		} catch (Exception e) {
			throw new RuntimeException(e)
		} finally {
			releaseConnection(connection)
		}
	}


}
