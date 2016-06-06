package com.ca.directory.jxplorer.tree;

import com.ca.commons.cbutil.CBIntText;
import com.ca.commons.naming.DN;
import com.ca.commons.naming.RDN;

import javax.naming.InvalidNameException;
import javax.swing.tree.*;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

//import javax.naming.directory.*;
/**
 *    The intention behind this class is to keep DefaultTreeModel largely
 *    unchanged, but add some utility ftns to make the task of translating
 *    between DNs and objects in the tree (SmartNodes and TreePaths).<p>
 *
 *
 */
 
public class SmartModel extends DefaultTreeModel
{
    private static Logger log = Logger.getLogger(SmartModel.class.getName());

    public SmartModel(TreeNode root) { super(root); }
    
    public SmartModel(TreeNode root, boolean asksAllowsChildren) { super(root, asksAllowsChildren); }

    public void nodeChanged(javax.swing.tree.TreeNode treeNode)
    {
        super.nodeChanged(treeNode);
    }


    /**
     *    A conversion function.  It takes a path and
     *    returns it as the corresponding DN.
     *    @param path a tree path to determine the DN for.
     *    @return the corresponding Distinguished Name 
     */
     
    public DN getDNForPath(TreePath path) 
    {
        if (path==null) return null;
        
        DN newDN = new DN();
        
        // there *has* to be a better way to cast arrays.  I wonder what it is.
        Object[] cobbleStones = path.getPath();
        
        SmartNode myRoot = (SmartNode)cobbleStones[0];
        
        try
        {
            for (int i=0; i<cobbleStones.length; i++)
            {
                SmartNode sn = ((SmartNode) cobbleStones[i]);
                
                RDN rdn = sn.getRDN();
                if (rdn.isEmpty() == false)   
                {             
                    newDN.addChildRDN(rdn);
                }    
            }    
        }
        catch (InvalidNameException e)
        {
            log.log(Level.WARNING, "ERROR: getDNForPath(TreePath path) can't parse " + path.toString() + "\n   ", e);
            return null;
        }
           
        return newDN; 
    }

    /** A conversion function - returns the current Node as a DN
     *
     */
     
    public DN getDNForNode(TreeNode node)
    {
        return getDNForPath(getPathForNode(node));
    } 

    /**
     *    A conversion function.  It takes an array
     *    of tree nodes representing a path and
     *    returns it as the corresponding DN.
     *    @param path an array of tree nodes to convert to a DN.
     *    @return the corresponding Distinguished Name 
     */
     
    public DN getDNForPath(TreeNode[] path)
    {
        return getDNForPath(new TreePath(path));        
    }

    /**
     *    A conversion function - gets the path to given node.
     */
     
    public TreePath getPathForNode(TreeNode node)
    {
        TreeNode[] nodePath = getPathToRoot(node);
        if (nodePath == null) { return null; }
        return new TreePath(nodePath);
    }
    
    /**
     *    A conversion function - gets a path for a particular DN
     */
     
    public TreePath getPathForDN(DN nodeDN)
    {
        SmartNode node = getNodeForDN(nodeDN);
        if (node == null) { return null; }
        return getPathForNode(node);
    }
    
    /**
     *    A conversion function.
     *    Returns the existing smart node corresponding to a DN.
     *    Returns null if the DN does not currently exist in
     *    the client tree, even if it is a valid DN on the server.
     *    (We're only searching the GUI Tree here.)
     *
     *    @param nodeDN the full DN of the node to be found
     *    @return the final node in the tree corresponding to the lowest
     *             level RDN of the DN.
     */
     
    public SmartNode getNodeForDN(DN nodeDN)
    {
        if ((nodeDN==null)) return null;
    
        if (nodeDN.size()==0) return (SmartNode)getRoot();
    
        SmartNode child = null;
        SmartNode parent = (SmartNode)getRoot();
        
        // for each tree level, grab the current node pointer (parent)

        for (int i=0;i< nodeDN.size();i++)
        {
            RDN rdn = nodeDN.getRDN(i);

            Enumeration children = parent.children();
            parent = null;
            
            // search children trying to find matching RDN
            while (children.hasMoreElements())
            {
                child = (SmartNode)children.nextElement();
                
                if (child.rdnEquals(rdn))
                {
                    parent = child; // reset parent pointer to current node, prior to next cycle
                    break;
                }
            }

            if (parent == null) // i.e. couldn't find sub-node so...
            {
                return null;    // ... fail
            }     
        }
        
        // if we're still here, we should now have the final node.
        return parent;            
    }

    

    /**
     *  A rare condition occurs when a user tries to copy a 
     *  node over an existing node with the same name.  This
     *  ftn. emulates the behaviour of many tree editing systems
     *  by allowing the copy, but prepending 'Copy ' or 'Copy (n) of'
     *  to the name.  This ftn searches the existing set of
     *  siblings to find a unique name, whether 'Copy ', or
     *  'Copy (n) of' where n is a unique number.
     *
     *   @param activeDN the DN of the node into which the 
     *                   new node is being copied.
     *   @param copyDN   the DN of the node being copied.
     */
    
    // - and who would have thought such a simple task could get so nickity -
     
    protected String getUniqueCopyRDN(DN activeDN, DN copyDN)
    {
        RDN testRDN =   copyDN.getLowestRDN();
        String testValue = copyDN.getLowestRDN().getRawVal();
        String testClass = copyDN.getLowestRDN().getAttID();

        // XXX translation of this is ugly!  Can't see how to improve it off hand however.
        String copyPrefix = testClass + "=" + CBIntText.get("Copy");
        String copyPrefix2 = CBIntText.get("of");
        
        boolean originalExists = false;
        
        int copyNumber = 0;

        SmartNode parent = getNodeForDN(activeDN);
        
        if (parent != null)  // can't think why it *would* equal null, mind you...
        {
            Enumeration children = parent.children();

            /**
             *    Loop through all children, doing two things;
             *    a) check if there is already a child with the name
             *       we're going to add (i.e. we need to do find a new name)
             *    b) check if there are already copies (i.e we need to
             *       produce a unique "Copy (512) of ..." name)
             */
       
            SmartNode child;
            while (children.hasMoreElements())
            {
                child = (SmartNode)children.nextElement();
                RDN childRDN = child.getRDN();
                String childRDNString = childRDN.toString();
                if ((!originalExists ) && childRDN.equals(testRDN))   // test if there is already a doo-dad of this name
                {
                    originalExists = true;    // ... there is, we have work to do!
                }
                
                if (childRDNString.startsWith(copyPrefix)==true) // we have a contender
                {
                    if (childRDNString.endsWith(testValue)) // we've found an *existing*
                    {                                 // copy, and now need to get a
                                                      // unique ID number for it...
                        int startpos = copyPrefix.length() + 1;
                        if (childRDNString.charAt(startpos)=='(')
                        {
                            int endpos = childRDNString.indexOf(')', startpos);
                            if (endpos != -1)
                            {
                                String childCopyNumberText = childRDNString.substring(startpos+1,endpos);
                                try
                                {
                                    int childCopyNumber = Integer.parseInt(childCopyNumberText);
                                    if (childCopyNumber >= copyNumber)      // make sure copyNumber always
                                        copyNumber = childCopyNumber + 1;   // one greater than largest existing
                                }
                                catch (NumberFormatException e) // if it wasn't a number between the brackets...
                                {
                                    if (copyNumber == 0) copyNumber = 2;  
                                }                                        
                            }
                            else
                            {
                                if (copyNumber == 0) copyNumber = 2;  
                            }                                    
                        }                              
                        else  // the copy found is unnumbered, so set out copyNumber
                        {     // to '2' if it hasn't already been set.
                            if (copyNumber == 0) copyNumber = 2;  
                        }
                    }
                }
            }
            
            String returnValue;
            
            if (originalExists) 
            {
                if (copyNumber != 0)        // produce numbered 'copy (x) of' version
                    returnValue = copyPrefix + " (" + copyNumber + ") " + copyPrefix2 + " " + testValue;
                else                        // first copy is unnumbered.
                    returnValue = copyPrefix + " " + copyPrefix2 + " " + testValue;
            }    
            else
                returnValue = testRDN.toString();
            
            return returnValue;    
        }
        return testRDN.toString(); // only if parent is null...not sure how this could happen :-)
    }
    


    /**
    *   When we change the name of a node to one that already exists, the tree will
     * actually have two nodes with the same name.  This checks to see if this has
     * occurred so we can catch the problem before bothering the directory.
    *
    *   @param nodeDN the full DN of the node to be found
    *   @return true if the node exists, false otherwise (or if there is an error).
    *   .
    */

    public boolean checkForAnotherNodeWithSameRDN(DN nodeDN)
    {
        if ((nodeDN==null)) return false;

        if (nodeDN.size()==0) return false;

        RDN nodeRDN = nodeDN.getLowestRDN();
        SmartNode parent = getNodeForDN(nodeDN.getParent());
        SmartNode child = null;

        Enumeration children = parent.children();

        int matchCount = 0;

        while (children.hasMoreElements())      //TE: iterate thru the children and see if the rdn matches.
        {
            child = (SmartNode)children.nextElement();

            if (child.rdnEquals(nodeRDN))
            {
                //return true;

                matchCount++;    // WTF???

                if(matchCount==2)
                    return true;
                
            }
        }

        return false;
    }

    /**
     * Does a node corresponding to the DN exist in the tree...
     * @param nodeDN
     * @return
     */
    
    public boolean exists(DN nodeDN)
    {
        if ((nodeDN==null)) return false;

        if (nodeDN.size()==0) return false;

        return getNodeForDN(nodeDN)!=null;
    }

}