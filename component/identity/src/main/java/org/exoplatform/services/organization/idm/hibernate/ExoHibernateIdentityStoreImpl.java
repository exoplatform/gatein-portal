package org.exoplatform.services.organization.idm.hibernate;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.Session;
import org.picketlink.idm.common.exception.IdentityException;
import org.picketlink.idm.impl.helper.Tools;
import org.picketlink.idm.impl.model.hibernate.HibernateIdentityObject;
import org.picketlink.idm.impl.model.hibernate.HibernateIdentityObjectType;
import org.picketlink.idm.impl.model.hibernate.HibernateRealm;
import org.picketlink.idm.spi.model.IdentityObject;
import org.picketlink.idm.spi.model.IdentityObjectType;
import org.picketlink.idm.spi.search.IdentityObjectSearchCriteria;
import org.picketlink.idm.spi.store.IdentityStoreInvocationContext;

/**
 * This class is a fork of org.picketlink.idm.impl.store.hibernate.HibernateIdentityStoreImpl
 * This class adds a new capability: allow testing on emptiness of an attribute
 */
public class ExoHibernateIdentityStoreImpl extends OriginalHibernateIdentityStoreImpl {
  private static final long serialVersionUID = -5121101788795147411L;

  private static Logger log = Logger.getLogger(OriginalHibernateIdentityStoreImpl.class.getName());

  public ExoHibernateIdentityStoreImpl(String id) {
    super(id);
  }

  public Collection<IdentityObject> findIdentityObject(IdentityStoreInvocationContext ctx, IdentityObjectType identityType, IdentityObjectSearchCriteria criteria) throws IdentityException {
    checkIOType(identityType);

    HibernateIdentityObjectType hibernateType = getHibernateIdentityObjectType(ctx, identityType);
    HibernateRealm realm = getRealm(getHibernateSession(ctx), ctx);

    List<IdentityObject> results;

    Session hibernateSession = getHibernateSession(ctx);

    try {
      StringBuilder hqlBuilderSelect = new StringBuilder("select distinct io from HibernateIdentityObject io");
      Map<String, Object> queryParams = new HashMap<String, Object>();

      StringBuilder hqlBuilderConditions = new StringBuilder(" where io.realm=:realm and io.identityType=:identityType");
      queryParams.put("realm", realm);
      queryParams.put("identityType", hibernateType);

      hqlBuilderConditions.append(" and io.name like :ioName");
      if (criteria != null && criteria.getFilter() != null) {
        queryParams.put("ioName", criteria.getFilter().replaceAll("\\*", "%"));
      } else {
        queryParams.put("ioName", "%");
      }

      if (criteria != null && criteria.isFiltered() && criteria.getValues() != null) {
        int i = 0;
        for (Map.Entry<String, String[]> entry : criteria.getValues().entrySet()) {
          // Resolve attribute name from the store attribute mapping
          String mappedAttributeName = null;
          try {
            mappedAttributeName = resolveAttributeStoreMapping(hibernateType, entry.getKey());
          } catch (IdentityException e) {
            // Nothing
          }

          /** Begin eXo customization**/
          if (entry.getValue() == null || entry.getValue().length == 0) {
            String attrTableJoinName = "attrs" + i;
            String attrParamName = "attr" + i;
            hqlBuilderConditions.append(" and not exists(from io.attributes as " + attrTableJoinName + " where " + attrTableJoinName + ".name like :" + attrParamName + ")");
            queryParams.put(attrParamName, mappedAttributeName);
            /** End eXo customization**/
          } else {
            Set<String> given = new HashSet<String>(Arrays.asList(entry.getValue()));
            for (String attrValue : given) {
              attrValue = attrValue.replaceAll("\\*", "%");

              i++;
              String attrTableJoinName = "attrs" + i;
              String textValuesTableJoinName = "textValues" + i;
              String attrParamName = "attr" + i;
              String textValueParamName = "textValue" + i;

              hqlBuilderSelect.append(" join io.attributes as " + attrTableJoinName);
              hqlBuilderSelect.append(" join " + attrTableJoinName + ".textValues as " + textValuesTableJoinName);
              hqlBuilderConditions.append(" and " + attrTableJoinName + ".name like :" + attrParamName);
              hqlBuilderConditions.append(" and " + textValuesTableJoinName + " like :" + textValueParamName);

              queryParams.put(attrParamName, mappedAttributeName);
              queryParams.put(textValueParamName, attrValue);
            }
          }
        }
      }

      if (criteria != null && criteria.isSorted()) {
        if (criteria.isAscending()) {
          hqlBuilderConditions.append(" order by io.name asc");
        } else {
          hqlBuilderConditions.append(" order by io.name desc");
        }
      }

      Query hibernateQuery = hibernateSession.createQuery(hqlBuilderSelect.toString() + hqlBuilderConditions.toString());

      if (criteria != null && criteria.isPaged()) {
        if (criteria.getMaxResults() > 0) {
          hibernateQuery.setMaxResults(criteria.getMaxResults());
        }
        hibernateQuery.setFirstResult(criteria.getFirstResult());
      }

      // Apply parameters to Hibernate query
      applyQueryParameters(hibernateQuery, queryParams);

      hibernateQuery.setCacheable(true);

      results = (List<IdentityObject>) hibernateQuery.list();
      Hibernate.initialize(results);
    } catch (Exception e) {
      if (log.isLoggable(Level.FINER)) {
        log.log(Level.FINER, "Exception occurred: ", e);
      }

      throw new IdentityException("Cannot find IdentityObjects with type '" + identityType.getName() + "'", e);
    }

    return results;
  }

  protected void filterByAttributesValues(Collection<IdentityObject> objects, Map<String, String[]> attrs) {
    Set<IdentityObject> toRemove = new HashSet<IdentityObject>();

    for (IdentityObject object : objects) {
      Map<String, Collection> presentAttrs = ((HibernateIdentityObject) object).getAttributesAsMap();
      for (Map.Entry<String, String[]> entry : attrs.entrySet()) {
        // Resolve attribute name from the store attribute mapping
        String mappedAttributeName = null;
        try {
          mappedAttributeName = resolveAttributeStoreMapping(object.getIdentityType(), entry.getKey());
        } catch (IdentityException e) {
          // Nothing
        }

        if (mappedAttributeName == null) {
          toRemove.add(object);
          break;
        }

        /** Begin eXo customization**/
        if (entry.getValue() == null || entry.getValue().length == 0 || StringUtils.isEmpty(entry.getValue()[0])) {
          if (presentAttrs.containsKey(mappedAttributeName)) {
            toRemove.add(object);
            break;
          }
          /** End eXo customization**/
        } else if (presentAttrs.containsKey(mappedAttributeName)) {
          Set<String> given = new HashSet<String>(Arrays.asList(entry.getValue()));

          Collection present = presentAttrs.get(mappedAttributeName);

          for (String s : given) {
            String regex = Tools.wildcardToRegex(s);

            boolean matches = false;

            for (Object o : present) {
              if (o.toString().matches(regex)) {
                matches = true;
              }
            }

            if (!matches) {
              toRemove.add(object);
              break;
            }
          }

        } else {
          toRemove.add(object);
          break;
        }
      }
    }

    for (IdentityObject identityObject : toRemove) {
      objects.remove(identityObject);
    }
  }

}
