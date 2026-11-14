package app.populators;

import app.security.entities.Role;
import app.security.entities.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

/**
 * Run manually with {@code AdminPopulator.populate(HibernateConfig.getEntityManagerFactory())} after setting
 * {@code ADMIN_PASSWORD}; this is deliberately not called automatically by Main.
 */
public final class AdminPopulator
{
    private AdminPopulator()
    {
    }

    public static void populate(EntityManagerFactory emf)
    {
        String password = System.getenv("ADMIN_PASSWORD");
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
