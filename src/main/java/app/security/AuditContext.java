package app.security;

import jakarta.persistence.EntityManager;

/**
 * Who is making the current request, for the PostgreSQL audit log (SQL/audit/09_audit_log.sql).
 * <p>
 * The backend always connects as the same database user (smartcity_app), so the trigger cannot tell API users apart
 * by itself. The security filter stores the logged-in username here for the request, and every write transaction in
 * GenericDAO hands it to PostgreSQL with set_config('smartcity.app_user', ..., true). The "true" makes it local to the
 * transaction, so it can never leak into another request that reuses the pooled connection.
 * <p>
 * Javalin handles a request on one thread, so a ThreadLocal holds it; the filter clears it at the start and the end of
 * every request, because the thread is reused for the next one.
 */
public final class AuditContext
{
    private static final ThreadLocal<String> USER = new ThreadLocal<>();

    private AuditContext()
    {
    }

    public static void setUser(String username)
    {
        USER.set(username);
    }

    public static String user()
    {
        return USER.get();
    }

    public static void clear()
    {
        USER.remove();
    }

    /** Call right after a transaction begins: the audit trigger then records the API user of this request */
    public static void tag(EntityManager em)
    {
        String user = USER.get();
        if (user != null)
        {
            em.createNativeQuery("SELECT set_config('smartcity.app_user', ?1, true)")
                    .setParameter(1, user)
                    .getSingleResult();
        }
    }
}
