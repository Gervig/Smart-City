package app.daos;

import app.exceptions.NotFoundException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.util.List;

public class GenericDAO<T> {
    private final EntityManagerFactory emf;
    private final Class<T> type;

    public GenericDAO(EntityManagerFactory emf, Class<T> type) {
        this.emf = emf;
        this.type = type;
    }

    public T persist(T entity) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.persist(entity);
            em.getTransaction().commit();
            return entity;
        }
    }

    public void persistAll(List<T> entities) {
        if (entities.isEmpty()) {
            return;
        }
        try (EntityManager em = emf.createEntityManager()) {
            var transaction = em.getTransaction();
            transaction.begin();
            try {
                for (T entity : entities) {
                    em.persist(entity);
                }
                transaction.commit();
            } catch (RuntimeException | Error exception) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw exception;
            }
        }
    }

    public T merge(T entity) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            T merged = em.merge(entity);
            em.getTransaction().commit();
            return merged;
        }
    }

    public int mergeAllEntities(List<?> entities) {
        if (entities.isEmpty()) {
            return 0;
        }
        try (EntityManager em = emf.createEntityManager()) {
            var transaction = em.getTransaction();
            transaction.begin();
            try {
                for (Object entity : entities) {
                    em.merge(entity);
                }
                transaction.commit();
                return entities.size();
            } catch (RuntimeException | Error exception) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw exception;
            }
        }
    }

    public T findByIdOrThrow(Long id) {
        if (id == null) {
            throw new IllegalArgumentException(type.getSimpleName() + " id is required");
        }
        try (EntityManager em = emf.createEntityManager()) {
            T entity = em.find(type, id);
            if (entity == null) {
                throw new NotFoundException(type.getSimpleName() + " " + id + " not found");
            }
            return entity;
        }
    }

    public List<T> findAll() {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("SELECT e FROM " + type.getSimpleName() + " e", type).getResultList();
        }
    }

    public List<T> findAllWithLocation() {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("SELECT e FROM " + type.getSimpleName() + " e JOIN FETCH e.location", type)
                    .getResultList();
        }
    }

    public long countWhere(String attributePath, Object value) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("SELECT COUNT(e) FROM " + type.getSimpleName() + " e WHERE e." + attributePath + " = :value", Long.class)
                    .setParameter("value", value)
                    .getSingleResult();
        }
    }

    public void delete(Long id) {
        if (id == null) {
            throw new IllegalArgumentException(type.getSimpleName() + " id is required");
        }
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            T entity = em.find(type, id);
            if (entity == null) {
                em.getTransaction().rollback();
                throw new NotFoundException(type.getSimpleName() + " " + id + " not found");
            }
            em.remove(entity);
            em.getTransaction().commit();
        }
    }
}
