package org.carrot2.core.parameter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

/**
 * 
 */
public class AttributeBinderTest
{
    @Bindable
    public static class TestImpl
    {
        @Attribute(key = "inField", bindingDirection = BindingDirection.IN)
        protected int inField = 5;

        @Attribute(key = "inoutField", bindingDirection = BindingDirection.INOUT)
        protected int inoutField = 5;

        @Attribute(key = "outField", bindingDirection = BindingDirection.OUT)
        protected int outField = 5;

        @Attribute(bindingDirection = BindingDirection.INOUT)
        protected int defaultKeyField = 5;
    }

    @Bindable
    public static class TestImplSubclass extends TestImpl
    {
        @Attribute(key = "subclassInField", bindingDirection = BindingDirection.IN)
        private int subclassInField = 5;

        @Attribute(key = "subclassOutField", bindingDirection = BindingDirection.OUT)
        private int subclassOutField = 5;
    }

    @Bindable(prefix = "prefix")
    public static class TestImpl2
    {
        @Attribute(bindingDirection = BindingDirection.IN)
        private int inField = 5;

        private TestImpl referenced = new TestImpl();
    }

    @Bindable(prefix = "circular1")
    public static class CircularReference1
    {
        @Attribute(bindingDirection = BindingDirection.IN)
        private int inField = 5;
        
        private CircularReference2 ref;
    }

    @Bindable(prefix = "circular2")
    public static class CircularReference2
    {
        @Attribute(bindingDirection = BindingDirection.IN)
        private int inField = 5;
        
        @SuppressWarnings("unused")
        private CircularReference1 ref;
    }
    
    public static class NotBindable
    {
        @Attribute(key = "test", bindingDirection = BindingDirection.IN)
        private int test = 20;
    }

    @Test
    public void testInAttributes() throws InstantiationException
    {
        final Map<String, Object> params = new HashMap<String, Object>();
        params.put("inField", 10);
        params.put("outField", 10);
        params.put("inoutField", 7);

        TestImpl instance = new TestImpl();
        AttributeBinder.bind(instance, params, BindingDirection.IN);
        assertEquals(10, instance.inField);
        assertEquals(5, instance.outField);
        assertEquals(7, instance.inoutField);
    }

    @Test
    public void testInAttributesForSubclass() throws InstantiationException
    {
        final Map<String, Object> params = new HashMap<String, Object>();
        params.put("inField", 10);
        params.put("outField", 10);
        params.put("inoutField", 7);
        params.put("subclassInField", 10);
        params.put("subclassOutField", 10);

        TestImplSubclass instance = new TestImplSubclass();
        AttributeBinder.bind(instance, params, BindingDirection.IN);
        assertEquals(10, instance.inField);
        assertEquals(7, instance.inoutField);
        assertEquals(5, instance.outField);
        assertEquals(10, instance.subclassInField);
        assertEquals(5, instance.subclassOutField);
        
        AttributeBinder.bind(instance, params, BindingDirection.OUT);
        assertEquals(5, params.get("outField"));
    }

    @Test
    public void testOutAttributes() throws InstantiationException
    {
        final Map<String, Object> params = new HashMap<String, Object>();
        params.put("inField", 10);
        params.put("outField", 10);

        TestImpl instance = new TestImpl();
        AttributeBinder.bind(instance, params, BindingDirection.OUT);
        assertEquals(new Integer(5), params.get("outField"));
        assertEquals(new Integer(10), params.get("inField"));
        assertEquals(new Integer(5), params.get("inoutField"));
    }

    @Test
    public void testOutAttributesForSubclass() throws InstantiationException
    {
        final Map<String, Object> params = new HashMap<String, Object>();
        params.put("inField", 10);
        params.put("outField", 10);
        params.put("inoutField", 7);
        params.put("subclassInField", 10);
        params.put("subclassOutField", 10);

        TestImplSubclass instance = new TestImplSubclass();
        AttributeBinder.bind(instance, params, BindingDirection.OUT);
        assertEquals(5, params.get("outField"));
        assertEquals(10, params.get("inField"));
        assertEquals(5, params.get("inoutField"));
        assertEquals(5, params.get("subclassOutField"));
        assertEquals(10, params.get("subclassInField"));
    }

    @Test
    public void testDefaultKey() throws InstantiationException
    {
        final Map<String, Object> params = new HashMap<String, Object>();
        final String fieldName = TestImpl.class.getName() + ".defaultKeyField";
        final TestImpl instance = new TestImpl();

        AttributeBinder.bind(instance, params, BindingDirection.INOUT);
        assertEquals(new Integer(5), params.get(fieldName));

        params.put(fieldName, 10);
        AttributeBinder.bind(instance, params, BindingDirection.INOUT);
        assertEquals(new Integer(10), params.get(fieldName));
    }

    @Test
    public void testClassPrefix() throws InstantiationException
    {
        final Map<String, Object> params = new HashMap<String, Object>();
        params.put("prefix.inField", 10);

        TestImpl2 instance = new TestImpl2();
        AttributeBinder.bind(instance, params, BindingDirection.IN);
        assertEquals(10, instance.inField);
    }

    @Test
    public void testReferenced() throws InstantiationException
    {
        final Map<String, Object> params = new HashMap<String, Object>();

        TestImpl2 instance = new TestImpl2();
        AttributeBinder.bind(instance, params, BindingDirection.INOUT);

        assertEquals(new Integer(5), params.get("prefix.inField"));
        assertEquals(new Integer(5), params.get("inField"));

        params.put("prefix.inField", 10);
        params.put("inField", 10);
        AttributeBinder.bind(instance, params, BindingDirection.INOUT);
        assertEquals(10, instance.inField);
        assertEquals(10, instance.referenced.inField);
    }
    
    @Test
    public void testCircularReferences() throws InstantiationException
    {
        final Map<String, Object> params = new HashMap<String, Object>();

        CircularReference1 instance = new CircularReference1();
        instance.ref = new CircularReference2();
        instance.ref.ref = instance;
        params.put("circular1.inField", 7);
        params.put("circular2.inField", 8);
        AttributeBinder.bind(instance, params, BindingDirection.IN);
        assertEquals(7, instance.inField);
        assertEquals(8, instance.ref.inField);
    }

    @Test
    public void testNotBindableBindingAttempt() throws InstantiationException
    {
        NotBindable instance = new NotBindable();

        final Map<String, Object> params = new HashMap<String, Object>();
        params.put("test", 20);

        try
        {
            AttributeBinder.bind(instance, params, BindingDirection.IN);
            fail();
        }
        catch (RuntimeException e)
        {
            // expected
        }

        assertEquals("Field value not changed", 20, instance.test);
    }
}
