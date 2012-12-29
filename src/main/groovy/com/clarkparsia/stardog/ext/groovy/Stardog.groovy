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
package com.clarkparsia.stardog.ext.groovy

import groovy.lang.Closure;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.openrdf.model.Graph;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.Value;
import org.openrdf.model.impl.GraphImpl;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.clarkparsia.stardog.StardogDBMS;
import com.clarkparsia.stardog.api.ConnectionConfiguration;
import com.clarkparsia.stardog.api.ConnectionPool;
import com.clarkparsia.stardog.api.ConnectionPoolConfig;
import com.clarkparsia.stardog.reasoning.ReasoningType;
import com.clarkparsia.stardog.security.SecurityUtil;
import com.clarkparsia.stardog.StardogException;
import com.clarkparsia.stardog.api.Adder;
import com.clarkparsia.stardog.api.Connection;
import com.clarkparsia.stardog.api.Getter;
import com.clarkparsia.stardog.api.Query;
import com.clarkparsia.stardog.api.Remover;
import com.clarkparsia.stardog.ext.spring.DataSource;
import com.clarkparsia.stardog.ext.spring.utils.TypeConverter;

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
class Stardog {

	// for fully formed connectionStrings, takes priority if not null
	def connectionString

	// standard properties, similar to ConnectionConfiguration and Stardog-Spring DataSource
	String url
	String username
	String password
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

	public Stardog() { }

	public Stardog(Map props) {
		url = props.url ?: null
		username = props.username ?: null
		password = props.password ?: null
		createIfNotPresent  = props.createIfNotPresent ?: false
		to = props.to ?: null
		failAtCapacity = props.failAtCapacity ?: false
		growAtCapacity = props.growAtCapacity ?: true
		maxIdle = props.maxIdle ?: 100
		maxPool = props.maxPool ?: 100
		minPool = props.minPool ?: 100
		noExpiration = props.noExpiration ?: false
		embedded = props.embedded ?: false

		if (props.home) {
			System.setProperty("stardog.home", props.home)
		}

		initialize()
	}

	void initialize() {
		ConnectionConfiguration connectionConfig;

		ConnectionPoolConfig poolConfig;

		connectionConfig = ConnectionConfiguration.to(to);

		if (url != null) {
			connectionConfig = connectionConfig.url(url);
		}

		if (embedded) {
			StardogDBMS.startEmbeddedServer();
		}

		if (createIfNotPresent) {
			StardogDBMS dbms = StardogDBMS.login(username, password.toCharArray());
			if (dbms.list().contains(to)) {
				dbms.drop(to);
			}
			dbms.createMemory(to);
			dbms.logout();
		}

		connectionConfig = connectionConfig.credentials(username, password);

		poolConfig = ConnectionPoolConfig
				.using(connectionConfig)
				.minPool(minPool)
				.maxPool(maxPool)

		dataSource = new DataSource(connectionConfig, poolConfig)

		dataSource.afterPropertiesSet();
	}

	private DataSource dataSource;

	/**
	 * @return the dataSource
	 */
	public DataSource getDataSource() {
		return dataSource;
	}

	/**
	 * @param dataSource the dataSource to set
	 */
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * <code>withConnection</code>
	 * @param Closure to execute over the connection
	 */
	public void withConnection(Closure c) {
		Connection con = dataSource.getConnection()
		c.call(con)
		dataSource.releaseConnection(con)
	}

	/**
	 * <code>query</code>
	 * @param queryString SPARQL query string
	 * @param closure to execute over the result set
	 */
	public void query(String queryString, Closure c) {
		Connection con = dataSource.getConnection()
		TupleQueryResult result = null;
		try {
			Query query = con.query(queryString);
			result = query.executeSelect();
			while (result.hasNext()) {
				c.call(result.next());
			}

			result.close();


		} catch (Exception e) {
			throw new RuntimeException(e)
		} finally {
			dataSource.releaseConnection(con)
		}
	}
	
	/**
	 * <code>each</code>
	 * iterates over a Binding result
	 * @param queryString
	 * @param closure with each SPARQL query bound into the closure
	 */
	public void each(String queryString, Closure c) {
		Connection con = dataSource.getConnection()
		TupleQueryResult result = null;
		try {
			Query query = con.query(queryString);
			result = query.executeSelect();
			while (result.hasNext()) {
				def bs = result.next()
				def input = result.next().iterator().collectEntries( {
					[ (it.getName()) : (it.getValue()) ]
				})
				// binds the Sesame result set as a map into the closure so SPARQL variables
				// become closure native variables, e.g. "x"
				c.delegate = input
				c.call();
			}

			result.close();


		} catch (Exception e) {
			throw new RuntimeException(e)
		} finally {
			dataSource.releaseConnection(con)
		}
	}
	
	/**
	 * <code>insert</code>
	 * Inserts either a single list, or a list of lists of triples
	 * assumes URIImpl(s,p) and LiteralImpl(o)  
	 * @param arr lists
	 */
	public void insert(List arr) {
		Connection con = dataSource.getConnection()
		Adder adder = null;
		try {
			con.begin();
			adder = con.add();
			Graph graph = new GraphImpl();

			if (arr.size >= 1) {
				if (arr[0].class == java.util.ArrayList.class) {
					arr.each { arr2 ->
						if (arr2.size == 3) {
							def s = arr2[0]
							def p = arr2[1]
							def o = arr2[2]

							graph.add(new URIImpl(s), new URIImpl(p), new LiteralImpl(o));
						}
					}
				} else {
					def s = arr[0]
					def p = arr[1]
					def o = arr[2]
					graph.add(new URIImpl(s), new URIImpl(p), new LiteralImpl(o));
				}
			}
			adder.graph(graph);
			con.commit();

		} catch (StardogException e) {
			throw new RuntimeException(e);
		} finally {
			adder = null;
			dataSource.releaseConnection(con);
		}

	}


	/**
	 * <code>remove</code>
	 * @param list of format subject, predicate, object, graph URI
	 */
	public void remove(List args) {
		Connection connection = dataSource.getConnection();
		def subject = args[0]
		def predicate = args[1]
		def object = args[2]
		def graphUri = args[3]

		URIImpl subjectResource = null;
		URIImpl predicateResource = null;
		Resource context = null;

		if (subject != null) {
			subjectResource = new URIImpl(subject);
		}
		if (predicate != null) {
			predicateResource = new URIImpl(predicate);
		}

		if (graphUri != null) {
			context = ValueFactoryImpl.getInstance().createURI(graphUri);
		}

		Value objectValue = null;
		if (object != null) {
			objectValue = TypeConverter.asLiteral(object);
		}

		try {
			connection.begin();
			connection.remove().statements(subjectResource, predicateResource, objectValue, context);
			connection.commit();
		} catch (StardogException e) {
			throw new RuntimeException(e);
		} finally {
			dataSource.releaseConnection(connection);
		}
	}


}
