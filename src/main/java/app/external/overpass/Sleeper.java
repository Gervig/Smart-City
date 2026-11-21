package app.external.overpass;

@FunctionalInterface
public interface Sleeper
{
    void sleep(long millis) throws InterruptedException;
}
