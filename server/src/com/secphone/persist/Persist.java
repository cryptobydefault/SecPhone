package com.secphone.persist;

import java.util.*;
import org.hibernate.*;
import org.hibernate.cfg.*;

/**
 * Helper file for persisting objects with Hibernate.  Yes, Java Data Objects
 * (such as Account and Message) should really subclass this...
 */
public class Persist {
	static SessionFactory sessionFactory = null;
	
	static {
		sessionFactory = new Configuration()
			.configure()
			.setProperty(Environment.HBM2DDL_AUTO, "update")
			.buildSessionFactory();
	}
	
	/**
	 * Empty constructor.  Note that a new SessionFactory is created via the "static"
	 * method when creating new Persist instances.
	 */
	protected Persist() { }
	
	protected Session getSession() {
		return sessionFactory.openSession();
	}
	
	/**
	 * @param query name of query, as defined in .hbm.xml file
	 * @param params map of key/value pairs passed to query
	 * @param isList should return a list (as opposed to a single item)
	 * @param max if returning a list, what is max returned records (0 for no max)
	 * @return list if isList is set to true, otherwise object relevant to query
	 */
	protected Object doNamedQuery(String query, Map<String, Object> params, boolean isList, int max) {
		Session session = sessionFactory.openSession();
		Query q = session.getNamedQuery(query);
		Iterator it = params.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			Object val = params.get(key);
			
			if (val instanceof String) {
				q.setString(key, (String) val);
			} else if (val instanceof Long) {
				q.setLong(key, ((Long) val).longValue());
			} else {
				q.setParameter(key, val);
			}
		}
		
		if (max != 0) q.setMaxResults(max);
		
		Object ret = null;
		if (isList) ret = q.list();
		else ret = q.uniqueResult();
		
		session.close();
		return ret;
	}
	
	protected void update(Object o) 
	throws PersistException {
		Session s = sessionFactory.openSession();
		Transaction tx = null;

		try {
			tx = s.beginTransaction();
			s.update(o);
			tx.commit();
		} catch(Exception e) { 
			System.out.println("error: " + e); 
			if (tx != null)
				tx.rollback();
			throw new PersistException(e.getMessage());
		} finally {
			s.close();
		}
	}
	
	protected void persist(Object o) 
	throws PersistException {
		Session s = sessionFactory.openSession();
		Transaction tx = null;

		try {
			tx = s.beginTransaction();
			s.persist(o);
			tx.commit();
		} catch(Exception e) { 
			System.out.println("error: " + e); 
			if (tx != null)
				tx.rollback();
			throw new PersistException(e.getMessage());
		} finally {
			s.close();
		}
	}

}
