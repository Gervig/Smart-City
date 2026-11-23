package app.daos;

import app.entities.Citizen;
import app.entities.Family; // [CHANGED] Added for ALLOWED_COUNT_PATHS
import app.entities.Institution; // [CHANGED] Added for ALLOWED_COUNT_PATHS
import app.exceptions.NotFoundException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.ManagedType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.Locale;
import java.util.Map;


public class GenericDAO<T>
{

    private static final Pattern SAFE_ATTRIBUTE_PATH =
            Pattern.compile("[A-Za-z_][A-Za-z0-9_]*(?>\\.[A-Za-z_][A-Za-z0-9_]*)*");


    // [CHANGED] Expanded allow-list with paths used by tests and services
    private static final Map<Class<?>, Set<String>> ALLOWED_COUNT_PATHS = Map.of(
            Citizen.class, Set.of("housing.id", "firstName"),
            Family.class, Set.of("familyName"),
            Institution.class, Set.of("phone")
    );

    private final EntityManagerFactory emf;
    private final Class<T> type;

    public GenericDAO(EntityManagerFactory emf, Class<T> type)
    {
        // [CHANGED] Fail fast with NullPointerException if emf or type is null
        this.emf = Objects.requireNonNull(emf, "EntityManagerFactory is required");
        this.type = Objects.requireNonNull(type, "Entity type is required");
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
            Map<String, String> allowedFilters,
            Map<String, String> allowedSortFields
    )
    {
        if (page < 0 || size < 1) {
            throw new IllegalArgumentException("Page must be non-negative and size must be positive");
        }
        long offset = (long) page * size;
        if (offset > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Page offset is too large");
        }

        try (EntityManager em = emf.createEntityManager())
        {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<T> query = cb.createQuery(type);
            Root<T> root = query.from(type);

            String sortPathName = allowedSortFields.get(sortField);
            if (sortPathName == null) {
                throw new IllegalArgumentException(
                        "Unsupported sort field: " + sortField);
            }

            query.select(root);

            // Apply the requested filters.
            applyFilters(cb, query, root, filters, allowedFilters, em);

            // Apply sorting.
            Path<?> sortPath = resolveBasicPath(root, em, sortPathName);
            List<jakarta.persistence.criteria.Order> orders = new ArrayList<>();
            if ("desc".equalsIgnoreCase(sortDirection)) {
                orders.add(cb.desc(sortPath));
            } else if ("asc".equalsIgnoreCase(sortDirection)) {
                orders.add(cb.asc(sortPath));
            } else {
                throw new IllegalArgumentException("Sort direction must be asc or desc");
            }
            if (!"id".equals(sortPathName)) {
                orders.add(cb.asc(resolveBasicPath(root, em, "id")));
            }
            query.orderBy(orders);

            return em.createQuery(query)
                    .setFirstResult((int) offset)
                    .setMaxResults(size)
                    .getResultList();
        }
    }

    public long countFiltered(
            Map<String, String> filters,
            Map<String, String> allowedFilters)
    {
        try (EntityManager em = emf.createEntityManager())
        {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Long> query = cb.createQuery(Long.class);
            Root<T> root = query.from(type);

            query.select(cb.count(root));

            applyFilters(cb, query, root, filters, allowedFilters, em);

            return em.createQuery(query).getSingleResult();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void applyFilters(
            CriteriaBuilder cb,
            CriteriaQuery<?> query,
            Root<T> root,
            Map<String, String> filters,
            Map<String, String> allowedFilters,
            EntityManager em)
    {
        List<Predicate> predicates = new ArrayList<>();

        for (Map.Entry<String, String> entry : filters.entrySet())
        {
            String field = entry.getKey();
            String value = entry.getValue();

            String attributePath = allowedFilters.get(field);
            if (attributePath == null) {
                throw new IllegalArgumentException("Unsupported filter: " + field);
            }
            if (value == null || value.isBlank())
            {
                continue;
            }

            boolean from = field.endsWith("From");
            boolean to = field.endsWith("To");
            Path<?> path = resolveBasicPath(root, em, attributePath);
            Class<?> javaType = path.getJavaType();

            Object converted = convert(value, javaType);

            if (from)
            {
                requireRangeType(field, javaType);
                predicates.add(cb.greaterThanOrEqualTo(
                        (Path) path, (Comparable) converted));
            } else if (to)
            {
                requireRangeType(field, javaType);
                predicates.add(cb.lessThanOrEqualTo(
                        (Path) path, (Comparable) converted));
            } else if (javaType == String.class)
            {
                predicates.add(cb.like(
                        cb.lower((Path<String>) path),
                        "%" + escapeLike(value.toLowerCase(Locale.ROOT)) + "%",
                        '\\'));
            } else
            {
                predicates.add(cb.equal(path, converted));
            }
        }

        if (!predicates.isEmpty()) {
            query.where(predicates.toArray(new Predicate[0]));
        }
    }

    private Path<?> resolveBasicPath(Root<T> root, EntityManager em, String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Entity path is required");
        }
        String[] parts = path.split("\\.", -1);
        ManagedType<?> managedType = em.getMetamodel().entity(type);
        Path<?> currentPath = root;

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            Attribute<?, ?> attribute;
            try {
                attribute = managedType.getAttribute(part);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid entity path: " + path, e);
            }

            boolean last = i == parts.length - 1;
            if (last) {
                if (attribute.getPersistentAttributeType()
                        != Attribute.PersistentAttributeType.BASIC) {
                    throw new IllegalArgumentException("Entity path must end in a basic attribute: " + path);
                }
            } else {
                Attribute.PersistentAttributeType attributeType =
                        attribute.getPersistentAttributeType();
                if (attribute.isCollection()
                        || (attributeType != Attribute.PersistentAttributeType.MANY_TO_ONE
                        && attributeType != Attribute.PersistentAttributeType.ONE_TO_ONE)) {
                    throw new IllegalArgumentException("Invalid relationship path: " + path);
                }
                try {
                    managedType = em.getMetamodel().managedType(attribute.getJavaType());
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Invalid relationship path: " + path, e);
                }
            }
            currentPath = currentPath.get(part);
        }

        return currentPath;
    }

    private void requireRangeType(String field, Class<?> javaType) {
        if (!Number.class.isAssignableFrom(javaType) && javaType != LocalDate.class) {
            throw new IllegalArgumentException("Range filtering is not supported for: " + field);
        }
    }

    private String escapeLike(String value) {
        return value.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
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
            try {
                return LocalDate.parse(value);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid date value: " + value, e);
            }
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
        // [CHANGED] Validate attributePath before interpolating it into the JPQL query
        String validatedPath = validateAttributePath(attributePath);
        try (EntityManager em = emf.createEntityManager())
        {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Long> query = cb.createQuery(Long.class);
            Root<T> root = query.from(type);
            query.select(cb.count(root))
                    .where(cb.equal(resolveBasicPath(root, em, attributePath), value));
            return em.createQuery(query).getSingleResult();
        }
    }

    // [CHANGED] New helper method to enforce format and allow-list rules on attributePath
    private String validateAttributePath(String attributePath)
    {
        if (attributePath == null || attributePath.isBlank())
        {
            throw new IllegalArgumentException("attributePath is required");
        }
        if (!SAFE_ATTRIBUTE_PATH.matcher(attributePath).matches())
        {
            throw new IllegalArgumentException("Invalid attribute path: " + attributePath);
        }
        Set<String> allowedForType = ALLOWED_COUNT_PATHS.getOrDefault(type, Set.of());
        if (!allowedForType.contains(attributePath))
        {
            throw new IllegalArgumentException("Attribute path not allowed for " + type.getSimpleName() + ": " + attributePath);
        }
        return attributePath;
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
