package app.daos;

import app.exceptions.NotFoundException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.util.ArrayList;
import java.util.List;

public class GenericDAO<T>
{
    private final EntityManagerFactory emf;
    private final Class<T> type;

    public GenericDAO(EntityManagerFactory emf, Class<T> type)
    {
        this.emf = emf;
        this.type = type;
    }

    public T persist(T entity)
    {
        try (EntityManager em = emf.createEntityManager())
        {
            em.getTransaction().begin();
            em.persist(entity);
            em.getTransaction().commit();
            return entity;
        }
    }

    public void persistAll(List<T> entities)
    {
        if (entities.isEmpty())
        {
            return;
        }
        try (EntityManager em = emf.createEntityManager())
        {
            var transaction = em.getTransaction();
            transaction.begin();
            try
            {
                for (T entity : entities)
                {
                    em.persist(entity);
                }
                transaction.commit();
            } catch (RuntimeException exception)
            {
                if (transaction.isActive())
                {
                    transaction.rollback();
                }
                throw exception;
            }
        }
    }

    public T merge(T entity)
    {
        try (EntityManager em = emf.createEntityManager())
        {
            em.getTransaction().begin();
            T merged = em.merge(entity);
            em.getTransaction().commit();
            return merged;
        }
    }

    /**
     * Merges several entities in one transaction (all are saved, or none if something fails).
     * <p>
     * The method has its own type parameter {@code E} instead of the class's {@code T}. The class's
     * {@code T} is the entity type this DAO manages (for example Location), but this method must also
     * accept lists of other or mixed entity types, such as the institutions, companies and housings that
     * {@code SeedService.enrichFromOsm} merges through the location DAO.
     * <p>
     * {@code em.merge} does not make the passed object managed. It copies the state into a managed
     * instance and returns that copy, so the method returns the merged copies. Use them (not the
     * originals) if you need generated ids or further changes after saving.
     *
     * @param entities the entities to merge; may be empty
     * @param <E>      the entity type of the list elements
     * @return the merged, managed copies in the same order, or an empty list if the input was empty
     */
    public <E> List<E> mergeAllEntities(List<E> entities)
    {
        if (entities.isEmpty())
        {
            return List.of();
        }
        try (EntityManager em = emf.createEntityManager())
        {
            var transaction = em.getTransaction();
            transaction.begin();
            try
            {
                List<E> merged = new ArrayList<>();
                for (E entity : entities)
                {
                    // merge returns the managed copy; the passed entity itself stays detached
                    merged.add(em.merge(entity));
                }
                transaction.commit();
                return merged;
            } catch (RuntimeException exception)
            {
                // undo the whole batch if any single merge fails
                if (transaction.isActive())
                {
                    transaction.rollback();
                }
                throw exception;
            }
        }
    }

    public T findByIdOrThrow(Long id)
    {
        if (id == null)
        {
            throw new IllegalArgumentException(type.getSimpleName() + " id is required");
        }
        try (EntityManager em = emf.createEntityManager())
        {
            T entity = em.find(type, id);
            if (entity == null)
            {
                throw new NotFoundException(type.getSimpleName() + " " + id + " not found");
            }
            return entity;
        }
    }

    public List<T> findAll()
    {
        try (EntityManager em = emf.createEntityManager())
        {
            return em.createQuery("SELECT e FROM " + type.getSimpleName() + " e", type).getResultList();
        }
    }

    public List<T> findAllWithLocation()
    {
        try (EntityManager em = emf.createEntityManager())
        {
            return em.createQuery("SELECT e FROM " + type.getSimpleName() + " e JOIN FETCH e.location", type)
                    .getResultList();
        }
    }

    public long countWhere(String attributePath, Object value)
    {
        try (EntityManager em = emf.createEntityManager())
        {
            return em.createQuery("SELECT COUNT(e) FROM " + type.getSimpleName() + " e WHERE e." + attributePath + " = :value", Long.class)
                    .setParameter("value", value)
                    .getSingleResult();
        }
    }

    public void delete(Long id)
    {
        if (id == null)
        {
            throw new IllegalArgumentException(type.getSimpleName() + " id is required");
        }
        try (EntityManager em = emf.createEntityManager())
        {
            em.getTransaction().begin();
            T entity = em.find(type, id);
            if (entity == null)
            {
                em.getTransaction().rollback();
                throw new NotFoundException(type.getSimpleName() + " " + id + " not found");
            }
            em.remove(entity);
            em.getTransaction().commit();
        }
    }
}
