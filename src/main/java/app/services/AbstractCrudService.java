package app.services;

import app.daos.GenericDAO;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public abstract class AbstractCrudService<E, D, R> implements CrudService<D, R> {
    protected final GenericDAO<E> dao;
    protected final Function<E, D> toDTO;
    protected final Function<R, E> toEntity;
    protected final BiConsumer<R, E> applyUpdate;

    public AbstractCrudService(GenericDAO<E> dao,
                               Function<E, D> toDTO,
                               Function<R, E> toEntity,
                               BiConsumer<R, E> applyUpdate) {
        this.dao = dao;
        this.toDTO = toDTO;
        this.toEntity = toEntity;
        this.applyUpdate = applyUpdate;
    }

    protected void resolveReferences(E entity, R request) {
    }

    protected <T> T optional(GenericDAO<T> refDao, Long id) {
        return id == null ? null : refDao.findByIdOrThrow(id);
    }

    @Override
    public List<D> getAll() {
        return dao.findAll().stream().map(toDTO).toList();
    }

    @Override
    public D getById(Long id) {
        return toDTO.apply(dao.findByIdOrThrow(id));
    }

    @Override
    public D create(R request) {
        E e = toEntity.apply(request);
        resolveReferences(e, request);
        return toDTO.apply(dao.persist(e));
    }

    @Override
    public D update(Long id, R request) {
        E e = dao.findByIdOrThrow(id);
        applyUpdate.accept(request, e);
        resolveReferences(e, request);
        return toDTO.apply(dao.merge(e));
    }

    @Override
    public void delete(Long id) {
        dao.delete(id);
    }
}
