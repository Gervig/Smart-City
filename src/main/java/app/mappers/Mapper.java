package app.mappers;

import java.util.List;
import java.util.function.Function;

public class Mapper
{
    public static <S, T> List<T> mapList(List<S> source, Function<S, T> mapper)
    {
        return source.stream()
                .map(mapper)
                .toList();
    }

    public static <S, T> T map(S source, Function<S, T> mapper)
    {
        return mapper.apply(source);
    }
}