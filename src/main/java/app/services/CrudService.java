package app.services;

import app.dtos.PageRequest;
import app.dtos.PageResponse;

import java.util.List;

public interface CrudService<D, R>
{
    List<D> getAll();
    PageResponse<D> getAll(PageRequest request);
    D getById(Long id);
    D create(R request);
    D update(Long id, R request);
    void delete(Long id);
}
