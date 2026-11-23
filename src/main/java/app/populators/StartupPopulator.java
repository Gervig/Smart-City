package app.populators;

import app.exceptions.ApiException;
import app.security.entities.Role;
import app.security.entities.User;
import app.utils.Utils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs the role, admin and user populators when the application starts. Every step first checks what is already
 * in the database, so the first start fills an empty database and later starts change nothing. This relies on
 * Hibernate's {@code hbm2ddl.auto=validate}: the data survives a restart, so "already there" can be checked.
 * <ol>
 *     <li>Roles from {@link RolePopulator} ({@code user}, {@code admin}): each one only if missing.</li>
 *     <li>The admin from {@link AdminPopulator}: only when {@code ADMIN_PASSWORD} is set, and only if missing.</li>
 *     <li>The test users from {@link UserPopulator}, with the {@code user} role: each one only if missing.</li>
 * </ol>
 */
public final class StartupPopulator
{
    private static final Logger LOGGER = LoggerFactory.getLogger(StartupPopulator.class);

    private StartupPopulator()
    {
    }

    public static void populate(EntityManagerFactory emf, String adminPassword)
    {
        populateRoles(emf);
        if (adminPassword == null)
        {
            LOGGER.warn("ADMIN_PASSWORD is not set: no admin user was created");
        }
        AdminPopulator.populate(emf, adminPassword);
        populateUsers(emf);
    }

    /**
     * Reads ADMIN_PASSWORD from the environment, or else from config.properties. Returns null when neither has it.
     */
    public static String adminPassword()
    {
        String password = System.getenv("ADMIN_PASSWORD");
        if (password != null)
        {
            return password;
        }
        try
        {
            return Utils.getPropertyValue("ADMIN_PASSWORD", "config.properties");
        }
        catch (ApiException exception)
        {
            return null;
        }
    }

    static void populateRoles(EntityManagerFactory emf)
    {
        try (EntityManager em = emf.createEntityManager())
        {
            em.getTransaction().begin();
            for (Role role : RolePopulator.populate())
            {
                if (em.find(Role.class, role.getName()) == null)
                {
                    em.persist(role);
                    LOGGER.info("Created role {}", role.getName());
                }
            }
            em.getTransaction().commit();
        }
    }

    static void populateUsers(EntityManagerFactory emf)
    {
        try (EntityManager em = emf.createEntityManager())
        {
            em.getTransaction().begin();
            Role userRole = em.find(Role.class, "user");
            for (User user : UserPopulator.populate())
            {
                if (em.find(User.class, user.getUsername()) == null)
                {
                    user.addRole(userRole);
                    em.persist(user);
                    LOGGER.info("Created user {}", user.getUsername());
                }
            }
            em.getTransaction().commit();
        }
    }
}
