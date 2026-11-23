package app.populators;

import app.security.entities.Role;
import app.security.entities.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

/**
 * Creates the admin user. Called at startup by {@link StartupPopulator}; does nothing when the password is null
 * and nothing when the admin already exists.
 */
public final class AdminPopulator
{
    private AdminPopulator()
    {
    }

    public static void populate(EntityManagerFactory emf)
    {
        populate(emf, System.getenv("ADMIN_PASSWORD"));
    }

    public static void populate(EntityManagerFactory emf, String password)
    {
        if (password == null)
        {
            return;
        }
        if (password.isBlank())
        {
            throw new IllegalArgumentException("ADMIN_PASSWORD must not be blank");
        }

        try (EntityManager em = emf.createEntityManager())
        {
            em.getTransaction().begin();
            Role adminRole = em.find(Role.class, "admin");
            if (adminRole == null)
            {
                adminRole = new Role("admin");
                em.persist(adminRole);
            }
            User admin = em.find(User.class, "admin");
            if (admin == null)
            {
                admin = new User("admin", password);
                admin.addRole(adminRole);
                em.persist(admin);
            }
            else if (!admin.getRoles().contains(adminRole))
            {
                admin.addRole(adminRole);
            }
            em.getTransaction().commit();
        }
    }
}
