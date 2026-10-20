package app.controllers;

import java.util.List;

public interface IController<T, I>
{
    List<T> getAll();
    T getById(I id);
}
