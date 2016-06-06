/**
 * Created by IntelliJ IDEA.
 * User: erstr01
 * Date: Mar 19, 2003
 * Time: 11:45:12 AM
 * To change this template use Options | File Templates.
 */
package com.ca.directory.jxplorer.viewer.tableviewer;

import junit.framework.*;
import javax.naming.NamingException;

import com.ca.commons.naming.*;



public class AttributeTableModelTest extends TestCase
{
    /**
     * Set up a test AttributeTableModel with some standard attributes and a multi-valued RDN.
     * @return a test AttributeTableModel
     */

    private AttributeTableModel getStandardTestModel()
    {
        DXEntry entry = new DXEntry(new DN("cn=john+sn=smith,ou=pig botherers,o=pig corp"));

        DXAttribute cn = new DXAttribute("cn", "john");
        cn.add("fred");
        cn.add("nigel");

        DXAttribute sn = new DXAttribute("sn", "smith");
        sn.add("john");
        sn.add("fred");

        DXAttribute objectClass = new DXAttribute("objectClass", "top");
        objectClass.add("person");
        objectClass.add("inetOrgPerson");

        DXAttribute favouriteDrink = new DXAttribute("favouriteDrink", "Midori");
        objectClass.add("Cointreau");
        objectClass.add("Lemon Juice");
        objectClass.add("Mmm... Japanese Slipper...");

        entry.put(cn);
        entry.put(sn);
        entry.put(objectClass);
        entry.put("eyeColour", "purple");
        entry.put("mySocks", "moldy");


        AttributeTableModel model = new AttributeTableModel();
        model.insertAttributes(entry);
        return model;
    }

    public AttributeTableModelTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        return new TestSuite(AttributeTableModelTest.class);
    }

    public static void main (String[] args)
    {
        junit.textui.TestRunner.run(suite());
    }

    public void testGetRDN()
    {
        AttributeTableModel model = getStandardTestModel();
        assertEquals("test getRDN using cn=john+sn=smith", "cn=john+sn=smith", model.getRDN().toString());
    }

    public void testRemoveNamingComponent()
    {
        AttributeTableModel model = getStandardTestModel();
        model.removeNamingComponent(new AttributeNameAndType("sn", true), new AttributeValue(new DXAttribute("sn", "smith"), "smith"));
        assertEquals("test getRDN after removing 'sn=fred' using cn=john", "cn=john", model.getRDN().toString());
        assertEquals(model.namingTypes.length, 1);
        assertEquals(model.namingRawValues.length, 1);
        assertEquals(model.numberNamingValues, 1);
    }

    public void testAddNamingComponent()
    {
        AttributeTableModel model = getStandardTestModel();

        int row = getSpecificAttributeValueRow(model, "eyeColour", "purple");

        assertTrue("Couldn't find eyeColour attribute ", (row != -1));
        AttributeValue val = (AttributeValue)model.attributeValues.get(row);
        AttributeNameAndType type = (AttributeNameAndType)model.attributeTypes.get(row);

        model.addNamingComponent(type, val);
        assertEquals("test getRDN after adding eyeColour=purple ", "cn=john+eyecolour=purple+sn=smith", model.getRDN().toString());

        assertEquals(model.namingTypes.length, 3);
        assertEquals(model.namingRawValues.length, 3);
        assertEquals(model.numberNamingValues, 3);

    }



    private int getSpecificAttributeValueRow(AttributeTableModel model, String id, String value)
    {
        AttributeValue val = null;
        for (int i=0; i<model.attributeValues.size(); i++)
        {
            val = (AttributeValue)model.attributeValues.get(i);
            //System.out.println("searching : " + val.getID().toString() + ":" + val.getStringValue() + " for " + id + ":" + value);
            if (val.getID().equalsIgnoreCase(id) && val.getStringValue().equals(value))
            {
                return i;
            }
        }
        return -1;
    }



    public void testChangeMultiValuesNamingComponentValue()
    {
        AttributeTableModel model = getStandardTestModel();

        // find which row is 'cn=john', and change it...
        int row = getSpecificAttributeValueRow(model, "cn", "john");

        assertTrue("couldn't find test attribute cn=john to rename", (row!=-1));
        model.setValueAt("erick", row, 1);
        assertEquals("erick", model.getValueAt(row, 1).toString());
        assertEquals("renamed rdn from cn=john+sn=smith to cn=erick+sn=smith", "cn=erick+sn=smith", model.getRDN().toString());
    }

    public void testChangeSingleValuedNamingComponentValue()
    {
        AttributeTableModel model = getStandardTestModel();

        model.removeNamingComponent(new AttributeNameAndType("sn", true), new AttributeValue(new DXAttribute("sn", "smith"), "smith"));
        // find which row is 'cn=john', and change it...
        int row = -1;
        for (int i=0; i<model.attributeValues.size(); i++)
        {
            AttributeValue val = (AttributeValue)model.attributeValues.get(i);
            if (val.getID().equals("cn") && val.getStringValue().equals("john"))
            {
                row = i;
                break;
            }
        }
        assertTrue("couldn't find test attribute cn=john to rename", (row!=-1));
        model.setValueAt("erick", row, 1);
        assertEquals("erick", model.getValueAt(row, 1).toString());

        assertEquals("renamed rdn from cn=john to cn=erick", "cn=erick", model.getRDN().toString());
    }



    public void testRemoveRowFromArray() throws NamingException
    {
        int testElement1 = 0;
        String[] test1 = new String[] {"0","1","2","3","4"};
        String[] test1b = new String[] {"1","2","3","4"};
        int testElement2 = 3;
        String[] test2 = new String[] {"0","1","2","3","4"};
        String[] test2b = new String[] {"0","1","2","4"};
        int testElement3 = 4;
        String[] test3 = new String[] {"0","1","2","3","4"};
        String[] test3b = new String[] {"0","1","2","3"};


        // test first element removal
        assertEqualArrays(test1b, AttributeTableModel.removeRowFromArray(test1, testElement1));

        // test mid element removal
        assertEqualArrays(test2b, AttributeTableModel.removeRowFromArray(test2, testElement2));

        // test end element removal
        assertEqualArrays(test3b, AttributeTableModel.removeRowFromArray(test3, testElement3));

        // test lower out-of-bounds error
        assertEqualArrays(test1, AttributeTableModel.removeRowFromArray(test1, -1));

        // test upper    out-of-bounds error
        assertEqualArrays(test1, AttributeTableModel.removeRowFromArray(test1, test1.length));

    }

    private void assertEqualArrays(String[] array1, String[] array2)
    {
        String expectedArray = "expected array is {";
        for (int i=0; i<array1.length;i++)
            expectedArray += "\"" + array1[i] + "\",";
        expectedArray += "]";

        assertEquals(expectedArray + " but lengths aren't the same!", array1.length, array2.length);

        if (array1.length == array2.length)
        {
            for (int i=0; i<array1.length; i++)
                assertEquals(expectedArray + " but element " + i + " is wrong.", array1[i], array2[i]);
        }
    }
}
