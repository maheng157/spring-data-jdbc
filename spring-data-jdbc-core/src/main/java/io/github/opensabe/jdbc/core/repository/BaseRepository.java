package io.github.opensabe.jdbc.core.repository;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

/**
 * @author heng.ma
 */
@NoRepositoryBean
public interface BaseRepository<T, ID> extends
        InsertRepository<T>,
        UpdateRepository<T>,
        DeleteRepository<T, ID>,
        CountExecutor<T>,
        ListByLimitExecutor<T>,
        OptionalExecutor<T, ID>,
        PageAndSortingExecutor<T>,
        ListPageAndSortingExecutor<T>,
        ListQueryExecutor<T, ID>,
        ExistsExecutor<T, ID>,
        AssignmentTableQueryRepository<T, ID>,
        Repository<T, ID>
{
}
