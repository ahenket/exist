package org.exist.xquery.test;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.exist.EXistException;
import org.exist.collections.Collection;
import org.exist.collections.IndexInfo;
import org.exist.collections.triggers.TriggerException;
import org.exist.security.PermissionDeniedException;
import org.exist.security.SecurityManager;
import org.exist.security.xacml.AccessContext;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.storage.serializers.Serializer;
import org.exist.storage.txn.TransactionException;
import org.exist.storage.txn.TransactionManager;
import org.exist.storage.txn.Txn;
import org.exist.util.Configuration;
import org.exist.util.LockException;
import org.exist.xquery.CompiledXQuery;
import org.exist.xquery.XQuery;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.value.NodeValue;
import org.exist.xquery.value.Sequence;
import org.xml.sax.SAXException;

public class XQueryUpdateTest extends TestCase {

	public static void main(String[] args) {
        TestRunner.run(XQueryUpdateTest.class);
    }

    protected static String TEST_COLLECTION = DBBroker.ROOT_COLLECTION + "/test";
    
    protected static String TEST_XML = 
        "<?xml version=\"1.0\"?>" +
        "<products/>";
    
    protected final static int ITEMS_TO_APPEND = 100;
    
    private BrokerPool pool;
    
	public void testAppend() {
        DBBroker broker = null;
        try {
        	System.out.println("testAppend() ...\n");
            broker = pool.get(SecurityManager.SYSTEM_USER);
            
            XQuery xquery = broker.getXQueryService();
            String query =
            	"   declare variable $i external;\n" +
            	"	update insert\n" +
            	"		<product id='id{$i}' num='{$i}'>\n" +
            	"			<description>Description {$i}</description>\n" +
            	"			<price>{$i + 1.0}</price>\n" +
            	"			<stock>{$i * 10}</stock>\n" +
            	"		</product>\n" +
            	"	into /products";
            XQueryContext context = xquery.newContext(AccessContext.TEST);
            CompiledXQuery compiled = xquery.compile(context, query);
            for (int i = 0; i < ITEMS_TO_APPEND; i++) {
                context.declareVariable("i", new Integer(i));
                xquery.execute(compiled, null);
            }
            
            Sequence seq = xquery.execute("/products", null, AccessContext.TEST);
            assertEquals(seq.getLength(), 1);
            
            Serializer serializer = broker.getSerializer();
            System.out.println(serializer.serialize((NodeValue) seq.itemAt(0)));
            
            seq = xquery.execute("//product", null, AccessContext.TEST);
            assertEquals(ITEMS_TO_APPEND, seq.getLength());
            
            seq = xquery.execute("//product[price > 0.0]", null, AccessContext.TEST);
            assertEquals(ITEMS_TO_APPEND, seq.getLength());
            System.out.println("testAppend: PASS");
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            pool.release(broker);
        }
	}
    
	public void testAppendAttributes() {
		testAppend();
        DBBroker broker = null;
        try {
        	System.out.println("testAppendAttributes() ...\n");
            broker = pool.get(SecurityManager.SYSTEM_USER);
            
            XQuery xquery = broker.getXQueryService();
            String query =
            	"   declare variable $i external;\n" +
            	"	update insert\n" +
            	"		attribute name { concat('n', $i) }\n" +
            	"	into //product[@num = $i]";
            XQueryContext context = xquery.newContext(AccessContext.TEST);
            CompiledXQuery compiled = xquery.compile(context, query);
            for (int i = 0; i < ITEMS_TO_APPEND; i++) {
                context.declareVariable("i", new Integer(i));
                xquery.execute(compiled, null);
            }
            
            Sequence seq = xquery.execute("/products", null, AccessContext.TEST);
            assertEquals(seq.getLength(), 1);
            
            Serializer serializer = broker.getSerializer();
            System.out.println(serializer.serialize((NodeValue) seq.itemAt(0)));
            
            seq = xquery.execute("//product", null, AccessContext.TEST);
            assertEquals(ITEMS_TO_APPEND, seq.getLength());
            
            seq = xquery.execute("//product[@name = 'n20']", null, AccessContext.TEST);
            assertEquals(1, seq.getLength());
            
            store(broker, "<test attr1='a' attr2='b'>ccc</test>");
            query = "update insert attribute attr1 { 'c' } into /test";
            
            System.out.println("testing duplicate attribute ...");
            seq = xquery.execute("/test", null, AccessContext.TEST);
            assertEquals(1, seq.getLength());
            System.out.println(serializer.serialize((NodeValue) seq.itemAt(0)));
            
            System.out.println("testAppendAttributes: PASS");
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            pool.release(broker);
        }
	}
	
    public void testInsertBefore() {
        DBBroker broker = null;
        try {
            System.out.println("testInsertBefore() ...\n");
            broker = pool.get(SecurityManager.SYSTEM_USER);
            
            String query =
                "   update insert\n" +
                "       <product id='original'>\n" +
                "           <description>Description</description>\n" +
                "           <price>0</price>\n" +
                "           <stock>10</stock>\n" +
                "       </product>\n" +
                "   into /products";
            
            XQuery xquery = broker.getXQueryService();
            xquery.execute(query, null, AccessContext.TEST);
            
            query =
                "   declare variable $i external;\n" +
                "   update insert\n" +
                "       <product id='id{$i}'>\n" +
                "           <description>Description {$i}</description>\n" +
                "           <price>{$i + 1.0}</price>\n" +
                "           <stock>{$i * 10}</stock>\n" +
                "       </product>\n" +
                "   preceding /products/product[1]";
            XQueryContext context = xquery.newContext(AccessContext.TEST);
            CompiledXQuery compiled = xquery.compile(context, query);
            for (int i = 0; i < ITEMS_TO_APPEND; i++) {
                context.declareVariable("i", new Integer(i));
                xquery.execute(compiled, null);
            }
            
            Sequence seq = xquery.execute("/products", null, AccessContext.TEST);
            assertEquals(seq.getLength(), 1);
            
            Serializer serializer = broker.getSerializer();
            System.out.println(serializer.serialize((NodeValue) seq.itemAt(0)));
            
            seq = xquery.execute("//product", null, AccessContext.TEST);
            assertEquals(ITEMS_TO_APPEND + 1, seq.getLength());

            seq = xquery.execute("//product[price > 0.0]", null, AccessContext.TEST);
            assertEquals(ITEMS_TO_APPEND, seq.getLength());
            System.out.println("testInsertBefore: PASS");
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            pool.release(broker);
        }
    }
    
    public void testInsertAfter() {
        DBBroker broker = null;
        try {
            System.out.println("testInsertAfter() ...\n");
            broker = pool.get(SecurityManager.SYSTEM_USER);
            
            String query =
                "   update insert\n" +
                "       <product id='original'>\n" +
                "           <description>Description</description>\n" +
                "           <price>0</price>\n" +
                "           <stock>10</stock>\n" +
                "       </product>\n" +
                "   into /products";
            
            XQuery xquery = broker.getXQueryService();
            xquery.execute(query, null, AccessContext.TEST);
            
            query =
                "   declare variable $i external;\n" +
                "   update insert\n" +
                "       <product id='id{$i}'>\n" +
                "           <description>Description {$i}</description>\n" +
                "           <price>{$i + 1.0}</price>\n" +
                "           <stock>{$i * 10}</stock>\n" +
                "       </product>\n" +
                "   following /products/product[1]";
            XQueryContext context = xquery.newContext(AccessContext.TEST);
            CompiledXQuery compiled = xquery.compile(context, query);
            for (int i = 0; i < ITEMS_TO_APPEND; i++) {
                context.declareVariable("i", new Integer(i));
                xquery.execute(compiled, null);
            }
            
            Sequence seq = xquery.execute("/products", null, AccessContext.TEST);
            assertEquals(seq.getLength(), 1);
            
            Serializer serializer = broker.getSerializer();
            System.out.println(serializer.serialize((NodeValue) seq.itemAt(0)));
            
            seq = xquery.execute("//product", null, AccessContext.TEST);
            assertEquals(ITEMS_TO_APPEND + 1, seq.getLength());

            seq = xquery.execute("//product[price > 0.0]", null, AccessContext.TEST);
            assertEquals(ITEMS_TO_APPEND, seq.getLength());
            System.out.println("testInsertAfter: PASS");
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            pool.release(broker);
        }
    }
    
    public void testUpdate() {
    	testAppend();
        DBBroker broker = null;
        try {
            System.out.println("testUpdate() ...\n");
            broker = pool.get(SecurityManager.SYSTEM_USER);
            
            XQuery xquery = broker.getXQueryService();
            
            String query =
                "for $prod in //product return\n" +
                "	update value $prod/description\n" +
                "	with 'Updated Description'";
            Sequence seq = xquery.execute(query, null, AccessContext.TEST);
            
            seq = xquery.execute("//product[starts-with(description, 'Updated')]", null, AccessContext.TEST);
            assertEquals(seq.getLength(), ITEMS_TO_APPEND);
            
            Serializer serializer = broker.getSerializer();
            System.out.println(serializer.serialize((NodeValue) seq.itemAt(0)));
            
            query =
            	"for $prod in //product return\n" +
                "	update value $prod/stock/text()\n" +
                "	with 400";
            seq = xquery.execute(query, null, AccessContext.TEST);
            
            seq = xquery.execute("//product[stock = 400]", null, AccessContext.TEST);
            assertEquals(seq.getLength(), ITEMS_TO_APPEND);
            
            System.out.println(serializer.serialize((NodeValue) seq.itemAt(0)));
            
            query =
            	"for $prod in //product return\n" +
                "	update value $prod/@num\n" +
                "	with xs:int($prod/@num) * 3";
            seq = xquery.execute(query, null, AccessContext.TEST);
            
            seq = xquery.execute("/products", null, AccessContext.TEST);
            assertEquals(seq.getLength(), 1);
            
            seq = xquery.execute("//product[@num = 3]", null, AccessContext.TEST);
            assertEquals(seq.getLength(), 1);
            
            System.out.println(serializer.serialize((NodeValue) seq.itemAt(0)));
            
            query =
            	"for $prod in //product return\n" +
                "	update value $prod/stock\n" +
                "	with (<local>10</local>,<external>1</external>)";
            seq = xquery.execute(query, null, AccessContext.TEST);
            
            seq = xquery.execute("/products", null, AccessContext.TEST);
            assertEquals(seq.getLength(), 1);
            
            seq = xquery.execute("//product/stock/external[. = 1]", null, AccessContext.TEST);
            assertEquals(seq.getLength(), ITEMS_TO_APPEND);
            
            System.out.println("testUpdate: PASS");
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            pool.release(broker);
        }
    }
    
    public void testRemove() {
    	testAppend();
    	
        DBBroker broker = null;
        try {
        	broker = pool.get(SecurityManager.SYSTEM_USER);
        	
        	XQuery xquery = broker.getXQueryService();
        	
        	String query =
        		"for $prod in //product return\n" +
        		"	update delete $prod\n";
        	Sequence seq = xquery.execute(query, null, AccessContext.TEST);
        	
        	seq = xquery.execute("//product", null, AccessContext.TEST);
        	assertEquals(seq.getLength(), 0);
        	
        	System.out.println("testRemove: PASS");
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            pool.release(broker);
        }
	}
    
    public void testRename() {
    	testAppend();
        DBBroker broker = null;
        try {
            System.out.println("testUpdate() ...\n");
            broker = pool.get(SecurityManager.SYSTEM_USER);
            
            XQuery xquery = broker.getXQueryService();
            
            String query =
            	"for $prod in //product return\n" +
            	"	update rename $prod/description as 'desc'\n";
            Sequence seq = xquery.execute(query, null, AccessContext.TEST);
            
            seq = xquery.execute("//product/desc", null, AccessContext.TEST);
            assertEquals(seq.getLength(), ITEMS_TO_APPEND);
            
            query =
            	"for $prod in //product return\n" +
            	"	update rename $prod/@num as 'count'\n";
            seq = xquery.execute(query, null, AccessContext.TEST);
            
            seq = xquery.execute("//product/@count", null, AccessContext.TEST);
            assertEquals(seq.getLength(), ITEMS_TO_APPEND);
            
            System.out.println("testUpdate: PASS");
        } catch (Exception e) {
        	e.printStackTrace();
            fail(e.getMessage());
        } finally {
            pool.release(broker);
        }
    }
    
    public void testReplace() {
    	testAppend();
        DBBroker broker = null;
        try {
            System.out.println("testReplace() ...\n");
            broker = pool.get(SecurityManager.SYSTEM_USER);
            
            XQuery xquery = broker.getXQueryService();
            
            String query =
            	"for $prod in //product return\n" +
            	"	update replace $prod/description with <desc>An updated description.</desc>\n";
            Sequence seq = xquery.execute(query, null, AccessContext.TEST);
            
            seq = xquery.execute("//product/desc", null, AccessContext.TEST);
            assertEquals(seq.getLength(), ITEMS_TO_APPEND);
            
            query =
            	"for $prod in //product return\n" +
            	"	update replace $prod/@num with '1'\n";
            seq = xquery.execute(query, null, AccessContext.TEST);
            
            seq = xquery.execute("//product/@num", null, AccessContext.TEST);
            assertEquals(seq.getLength(), ITEMS_TO_APPEND);
            
            query =
            	"for $prod in //product return\n" +
            	"	update replace $prod/desc/text() with 'A new update'\n";
            seq = xquery.execute(query, null, AccessContext.TEST);
            
            seq = xquery.execute("//product[starts-with(desc, 'A new')]", null, AccessContext.TEST);
            assertEquals(seq.getLength(), ITEMS_TO_APPEND);
            
            System.out.println("testUpdate: PASS");
        } catch (Exception e) {
        	e.printStackTrace();
            fail(e.getMessage());
        } finally {
            pool.release(broker);
        }
    }
    
    protected void setUp() throws Exception {
        this.pool = startDB();
        DBBroker broker = null;
        try {
            broker = pool.get(SecurityManager.SYSTEM_USER);
            store(broker, TEST_XML);
        } catch (Exception e) {
        	e.printStackTrace();
            fail(e.getMessage());
        }  finally {
            pool.release(broker);
        }
    }

	private void store(DBBroker broker, String data) throws PermissionDeniedException, EXistException, TriggerException, SAXException, LockException, TransactionException {
		TransactionManager mgr = pool.getTransactionManager();
		Txn transaction = mgr.beginTransaction();        
		System.out.println("Transaction started ...");
		
		Collection root = broker.getOrCreateCollection(transaction, TEST_COLLECTION);
		broker.saveCollection(transaction, root);
		
		IndexInfo info = root.validateXMLResource(transaction, broker, "test.xml", data);
		root.store(transaction, broker, info, TEST_XML, false);
   
		mgr.commit(transaction);
	}
    
    protected BrokerPool startDB() {
        String home, file = "conf.xml";
        home = System.getProperty("exist.home");
        if (home == null)
            home = System.getProperty("user.dir");
        try {
            Configuration config = new Configuration(file, home);
            BrokerPool.configure(1, 5, config);
            return BrokerPool.getInstance();
        } catch (Exception e) {
            fail(e.getMessage());
        }
        return null;
    }
    
     protected void tearDown() {
         try {
             BrokerPool.stopAll(false);
         } catch (Exception e) {            
             fail(e.getMessage());
         }
     }
}
