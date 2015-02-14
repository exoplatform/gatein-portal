package org.exoplatform.services.organization.idm;

import org.picketlink.idm.impl.api.SimpleAttribute;
import org.picketlink.idm.impl.tree.Fqn;
import org.picketlink.idm.impl.tree.Node;
import org.picketlink.idm.impl.types.SimpleIdentityObject;
import org.picketlink.idm.impl.types.SimpleIdentityObjectRelationship;
import org.picketlink.idm.impl.types.SimpleIdentityObjectRelationshipType;
import org.picketlink.idm.impl.types.SimpleIdentityObjectType;
import org.picketlink.idm.spi.cache.IdentityObjectRelationshipNameSearch;
import org.picketlink.idm.spi.cache.IdentityObjectRelationshipSearch;
import org.picketlink.idm.spi.cache.IdentityObjectSearch;
import org.picketlink.idm.spi.model.IdentityObject;
import org.picketlink.idm.spi.model.IdentityObjectAttribute;
import org.picketlink.idm.spi.model.IdentityObjectRelationship;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * Cache provider implementation based on Infinispan and its tree cache API
 * Override org.picketlink.idm.impl.cache.InfinispanIdentityStoreCacheProviderImpl
 * for https://issues.jboss.org/browse/PLINK-645
 * TODO: remove when PicketLink IDM 1.4.6 is available
 */
public class InfinispanIdentityStoreCacheProviderImpl extends org.picketlink.idm.impl.cache.InfinispanIdentityStoreCacheProviderImpl
{
   private static Logger log = Logger.getLogger(InfinispanIdentityStoreCacheProviderImpl.class.getName());

   private Fqn getFqn(String ns, String node, int hash)
   {
      return Fqn.fromString(getNamespacedFqn(ns) + "/" + node + "/" + hash);
   }

   @Override
   public Collection<IdentityObject> getIdentityObjectSearch(String ns, IdentityObjectSearch search)
   {
      Fqn nodeFqn = getFqn(ns, NODE_IO_SEARCH, search.hashCode());

      Node node = getNode(nodeFqn);

      if (node != null)
      {
         Collection<IdentityObject> results = (Collection<IdentityObject>)node.get(NODE_OBJECT_KEY);

         if (log.isLoggable(Level.FINER) && results != null)
         {
            log.finer(this.toString() + "IdentityObject search found in cache: results.size()=" + results.size()
                  + ";namespace=" + ns);
         }

         return safeCopyIO(results);
      }

      return null;
   }

   @Override
   public Set<IdentityObjectRelationship> getIdentityObjectRelationshipSearch(String ns, IdentityObjectRelationshipSearch search)
   {
      Fqn nodeFqn = getFqn(ns, NODE_IO_REL_SEARCH, search.hashCode());

      Node node = getNode(nodeFqn);

      if (node != null)
      {
         Set<IdentityObjectRelationship> results = (Set<IdentityObjectRelationship>)node.get(NODE_OBJECT_KEY);

         if (log.isLoggable(Level.FINER) && results != null)
         {
            log.finer(this.toString() + "IdentityObjectRelationship search found in cache: results.size()=" + results.size()
                  + ";namespace=" + ns);
         }

         return safeCopyIOR(results);
      }

      return null;
   }

   @Override
   public void putIdentityObjectRelationshipNameSearch(String ns, IdentityObjectRelationshipNameSearch search, Set<String> results)
   {
      Fqn nodeFqn = getFqn(ns, NODE_IO_REL_NAME_SEARCH, search.hashCode());

      Node ioNode = addNode(nodeFqn);

      if (ioNode != null)
      {
         ioNode.put(NODE_OBJECT_KEY, new HashSet<String>(results));

         if (log.isLoggable(Level.FINER))
         {
            log.finer(this.toString() + "IdentityObjectRelationshipName search stored in cache: results.size()=" + results.size()
                  + ";namespace=" + ns);
         }
      }
   }

   @Override
   public Set<String> getIdentityObjectRelationshipNameSearch(String ns, IdentityObjectRelationshipNameSearch search)
   {
      Fqn nodeFqn = getFqn(ns, NODE_IO_REL_NAME_SEARCH, search.hashCode());

      Node node = getNode(nodeFqn);

      if (node != null)
      {
         Set<String> results = (Set<String>)node.get(NODE_OBJECT_KEY);

         if (log.isLoggable(Level.FINER) && results != null)
         {
            log.finer(this.toString() + "IdentityObjectRelationshipName search found in cache: results.size()=" + results.size()
                  + ";namespace=" + ns);
         }

         return new HashSet<String>(results);
      }

      return null;
   }

   @Override
   public void putProperties(String ns, IdentityObjectRelationship relationship, Map<String, String> properties)
   {
      Fqn nodeFqn = getFqn(ns, NODE_REL_PROPS, decode(relationship));

      Node ioNode = addNode(nodeFqn);

      if (ioNode != null)
      {
         ioNode.put(NODE_OBJECT_KEY, new HashMap<String, String>(properties));

         if (log.isLoggable(Level.FINER))
         {
            log.finer(this.toString() + "IdentityObjectRelationship properties stored in cache: relationship="
                  + relationship + "; properties.size()=" + properties.size() + ";namespace=" + ns);
         }
      }
   }

   private String decode(IdentityObjectRelationship r)
   {
      return r.getFromIdentityObject().getIdentityType().getName() +
            r.getFromIdentityObject().getName() +
            r.getToIdentityObject().getIdentityType().getName() +
            r.getToIdentityObject().getName() +
            r.getType().getName();
   }

   @Override
   public void putProperties(String ns, String name, Map<String, String> properties)
   {
      Fqn nodeFqn = getFqn(ns, NODE_REL_NAME_PROPS, name);

      Node ioNode = addNode(nodeFqn);

      if (ioNode != null)
      {
         ioNode.put(NODE_OBJECT_KEY, new HashMap<String, String>(properties));

         if (log.isLoggable(Level.FINER))
         {
            log.finer(this.toString() + "IdentityObjectRelationshipName properties stored in cache: name="
                  + name + "; properties.size()=" + properties.size() + ";namespace=" + ns);
         }
      }
   }

   @Override
   public Map<String, String> getProperties(String ns, String name)
   {
      Fqn nodeFqn = getFqn(ns, NODE_REL_NAME_PROPS, name);

      Node node = getNode(nodeFqn);

      if (node != null)
      {
         Map<String, String> props = (Map<String, String>)node.get(NODE_OBJECT_KEY);

         if (log.isLoggable(Level.FINER) && props != null)
         {
            log.finer(this.toString() + "IdentityObjectRelationshipName properties found in cache: properties.size()=" + props.size() +
                  "; name=" + name + ";namespace=" + ns);
         }

         return new HashMap<String, String>(props);
      }

      return null;
   }

   @Override
   public Map<String, IdentityObjectAttribute> getIdentityObjectAttributes(String ns, IdentityObject io)
   {
      Fqn nodeFqn = getFqn(ns, NODE_IO_ATTRIBUTES, io.getIdentityType().getName() + io.getName());

      Node node = getNode(nodeFqn);

      if (node != null)
      {
         Map<String, IdentityObjectAttribute> props = (Map<String, IdentityObjectAttribute>)node.get(NODE_OBJECT_KEY);

         if (log.isLoggable(Level.FINER) && props != null)
         {
            log.finer(this.toString() + "IIdentityObject attributes found in cache: attributes.size()=" + props.size() +
                  "; io=" + io + ";namespace=" + ns);
         }

         return safeCopyAttr(props);
      }

      return null;
   }

   private List<IdentityObject> safeCopyIO(Collection<IdentityObject> res)
   {
      List<IdentityObject> nr = new LinkedList<IdentityObject>();

      for (IdentityObject io : res)
      {
         nr.add(new SimpleIdentityObject(io.getName(),
               new SimpleIdentityObjectType(io.getIdentityType().getName())));
      }

      return nr;
   }

   private Set<IdentityObjectRelationship> safeCopyIOR(Set<IdentityObjectRelationship> res)
   {
      Set<IdentityObjectRelationship> nr = new HashSet<IdentityObjectRelationship>();

      for (IdentityObjectRelationship ior : res)
      {
         IdentityObject from = new SimpleIdentityObject(ior.getFromIdentityObject().getName(),
               new SimpleIdentityObjectType(ior.getFromIdentityObject().getIdentityType().getName()));
         IdentityObject to = new SimpleIdentityObject(ior.getToIdentityObject().getName(),
               new SimpleIdentityObjectType(ior.getToIdentityObject().getIdentityType().getName()));

         nr.add(new SimpleIdentityObjectRelationship(from, to, ior.getName(), new SimpleIdentityObjectRelationshipType(ior.getType().getName())));
      }

      return nr;
   }

   private Map<String, IdentityObjectAttribute> safeCopyAttr(Map<String, IdentityObjectAttribute> res)
   {
      Map<String, IdentityObjectAttribute> nr = new HashMap<String, IdentityObjectAttribute>();

      for (IdentityObjectAttribute attr : res.values())
      {
         nr.put(attr.getName(), new SimpleAttribute(attr));
      }

      return nr;
   }
}
