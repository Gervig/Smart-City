package app.services;

import java.util.List;

public interface CrudService<D, R> {
    List<D> getAll();
    D getById(Long id);
    D create(R request);
    D update(Long id, R request);
    void delete(Long id);
}
