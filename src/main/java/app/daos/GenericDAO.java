package app.daos;

import app.exceptions.NotFoundException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.Attribute;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


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
                // undo the whole batch if any single merge fails
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

    public List<T> findPage(
            int page,
            int size,
            String sortField,
            String sortDirection,
            Map<String, String> filters,
            Set<String> allowedFilters
    )
    {
        try (EntityManager em = emf.createEntityManager())
        {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<T> query = cb.createQuery(type);
            Root<T> root = query.from(type);

            // Only allow sorting by basic entity fields.
            Set<String> sortableFields = em.getMetamodel()
                    .entity(type)
                    .getSingularAttributes()
                    .stream()
                    .filter(a -> a.getPersistentAttributeType()
                            == Attribute.PersistentAttributeType.BASIC)
                    .map(Attribute::getName)
                    .collect(Collectors.toSet());

            if (!sortableFields.contains(sortField))
            {
                throw new IllegalArgumentException(
                        "Unsupported sort field: " + sortField);
            }

            query.select(root);

            // Apply the requested filters.
            applyFilters(cb, query, root, filters, allowedFilters);

            // Apply sorting.
            Path<?> sortPath = root.get(sortField);

            if ("desc".equalsIgnoreCase(sortDirection))
            {
                query.orderBy(cb.desc(sortPath));
            } else
            {
                query.orderBy(cb.asc(sortPath));
            }

            return em.createQuery(query)
                    .setFirstResult(page * size)
                    .setMaxResults(size)
                    .getResultList();
        }
    }

    public long countFiltered(
            Map<String, String> filters,
            Set<String> allowedFilters)
    {
        try (EntityManager em = emf.createEntityManager())
        {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Long> query = cb.createQuery(Long.class);
            Root<T> root = query.from(type);

            query.select(cb.count(root));

            applyFilters(cb, query, root, filters, allowedFilters);

            return em.createQuery(query).getSingleResult();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void applyFilters(
            CriteriaBuilder cb,
            CriteriaQuery<?> query,
            Root<T> root,
            Map<String, String> filters,
            Set<String> allowedFilters)
    {
        List<Predicate> predicates = new ArrayList<>();

        for (Map.Entry<String, String> entry : filters.entrySet())
        {
            String field = entry.getKey();
            String value = entry.getValue();

            if (value == null || value.isBlank())
            {
                continue;
            }

            if (!allowedFilters.contains(field))
            {
                throw new IllegalArgumentException(
                        "Unsupported filter: " + field);
            }

            boolean from = field.endsWith("From");
            boolean to = field.endsWith("To");

            String attribute = from
                    ? field.substring(0, field.length() - 4)
                    : to
                    ? field.substring(0, field.length() - 2)
                    : field;

            Path<?> path = root.get(attribute);
            Class<?> javaType = path.getJavaType();

            Object converted = convert(value, javaType);

            if (from)
            {
                predicates.add(cb.greaterThanOrEqualTo(
                        (Path) path, (Comparable) converted));
            }
            else if (to)
            {
                predicates.add(cb.lessThanOrEqualTo(
                        (Path) path, (Comparable) converted));
            }
            else if (javaType == String.class)
            {
                predicates.add(cb.like(
                        cb.lower((Path<String>) path),
                        "%" + value.toLowerCase(Locale.ROOT) + "%"));
            }
            else
            {
                predicates.add(cb.equal(path, converted));
            }
        }

        query.where(predicates.toArray(new Predicate[0]));
    }


    private Object convert(String value, Class<?> type)
    {
        if (type == String.class)
        {
            return value;
        }
        if (type == Long.class || type == long.class)
        {
            return Long.valueOf(value);
        }
        if (type == Integer.class || type == int.class)
        {
            return Integer.valueOf(value);
        }
        if (type == Double.class || type == double.class)
        {
            return Double.valueOf(value);
        }
        if (type == Boolean.class || type == boolean.class)
        {
            if (!value.equalsIgnoreCase("true")
                    && !value.equalsIgnoreCase("false"))
            {
                throw new IllegalArgumentException(
                        "Invalid boolean value: " + value);
            }
            return Boolean.valueOf(value);
        }
        if (type == LocalDate.class)
        {
            return LocalDate.parse(value);
        }
        if (type == BigDecimal.class)
        {
            return new BigDecimal(value);
        }

        throw new IllegalArgumentException(
                "Unsupported filter type: " + type.getSimpleName());
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
