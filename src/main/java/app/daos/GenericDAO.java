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

    public T merge(T entity) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            T merged = em.merge(entity);
            em.getTransaction().commit();
            return merged;
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
