package org.javers.spring.auditable.aspect.springdata;

import org.javers.core.Javers;
import org.javers.spring.auditable.AuthorProvider;
import org.javers.spring.auditable.CommitPropertiesProvider;
import org.springframework.data.repository.core.RepositoryMetadata;

import java.util.Map;

import static org.javers.repository.jql.InstanceIdDTO.instanceId;

/**
 * Created by gessnerfl on 22.02.15.
 */
class OnDeleteAuditChangeHandler extends AbstractAuditChangeHandler {
    OnDeleteAuditChangeHandler(Javers javers, AuthorProvider authorProvider, CommitPropertiesProvider commitPropertiesProvider) {
        super(javers, authorProvider, commitPropertiesProvider);
    }

    @Override
    public void handle(RepositoryMetadata repositoryMetadata, Object domainObject) {
        Map<String, String> props = commitPropertiesProvider.provide();
        String author = authorProvider.provide();

        if (isIdClass(repositoryMetadata, domainObject)) {
            javers.commitShallowDeleteById(author, instanceId(domainObject, repositoryMetadata.getDomainType()), props);
        } else if (isDomainClass(repositoryMetadata, domainObject)) {
            javers.commitShallowDelete(author, domainObject, props);
        } else {
            throw new IllegalArgumentException("Domain object or object id expected");
        }
    }
}
