package org.exoplatform.services.organization.idm;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.gatein.portal.idm.impl.repository.ExoFallbackIdentityStoreRepository;
import org.gatein.portal.idm.impl.store.hibernate.ExoHibernateIdentityStoreImpl;
import org.picketlink.idm.api.IdentitySearchCriteria;
import org.picketlink.idm.api.SortOrder;
import org.picketlink.idm.impl.api.IdentitySearchCriteriaImpl;
import org.picketlink.idm.spi.model.IdentityObject;
import org.picketlink.idm.spi.model.IdentityObjectType;
import org.picketlink.idm.spi.search.IdentityObjectSearchCriteria;
import org.picketlink.idm.spi.store.IdentityStore;
import org.picketlink.idm.spi.store.IdentityStoreInvocationContext;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class IdentityStoreUserListAccess implements ListAccess<String>, Serializable {

    private static final Log LOG = ExoLogger.getLogger(IdentityStoreUserListAccess.class);
    private final ExoFallbackIdentityStoreRepository exoFallbackISRepository ;
    private IdentityStore identityStore ;
    private IdentityObjectType identityObjectType;
    private IdentityStoreInvocationContext identityStoreInvocationContext;

    public IdentityStoreUserListAccess(ExoFallbackIdentityStoreRepository exoFallbackISRepository, IdentityStore identityStore, IdentityObjectType identityObjectType, IdentityStoreInvocationContext identityStoreInvocationContext) {
        this.exoFallbackISRepository = exoFallbackISRepository;
        this.identityStore = identityStore;
        this.identityObjectType = identityObjectType;
        this.identityStoreInvocationContext = identityStoreInvocationContext;
    }

    @Override
    public String[] load(int index, int length) throws Exception {
        IdentitySearchCriteriaImpl searchCriteria = new IdentitySearchCriteriaImpl();

        searchCriteria.page(index,length);
        searchCriteria.sort(SortOrder.ASCENDING);
        Collection<IdentityObject> usersCollection = identityStore.findIdentityObject(identityStoreInvocationContext, identityObjectType, convertSearchControls(searchCriteria));;
        if(identityStore.getClass().getName().equals(ExoHibernateIdentityStoreImpl.class.getName())){
            usersCollection =  usersCollection.stream().filter(IdentityObject -> {
                try {
                    return isFirstlyCreatedIn(exoFallbackISRepository, identityStoreInvocationContext, identityStore, IdentityObject);
                } catch (Exception e) {
                    LOG.error("Error when trying to load users from store "+identityStore.getId(),e);
                }
                return false;
            }).collect(Collectors.toList());
        }
        List<String> usersList = usersCollection.stream().map(IdentityObject::getName).collect(Collectors.toList());

        return  usersList.toArray(new String[length]);
    }

    @Override
    public int getSize() throws Exception {
        return  identityStore.getIdentityObjectsCount(identityStoreInvocationContext,identityObjectType);
    }

    private IdentityObjectSearchCriteria convertSearchControls(IdentitySearchCriteria criteria)
    {
        if (criteria == null)
        {
            return null;
        }

        if (criteria instanceof IdentityObjectSearchCriteria)
        {
            return (IdentityObjectSearchCriteria)criteria;
        }
        else
        {
            throw new IllegalArgumentException("Not supported IdentitySearchCriteria implementation: " + criteria.getClass());
        }
    }

    private boolean isFirstlyCreatedIn(ExoFallbackIdentityStoreRepository idmRepo, IdentityStoreInvocationContext identityStoreInvocationContext, IdentityStore identityStore, IdentityObject identityObject) throws Exception{
        return idmRepo.isFirstlyCreatedIn(identityStoreInvocationContext,identityStore,identityObject);
    }
}
