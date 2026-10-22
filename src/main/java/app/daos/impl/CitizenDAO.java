package app.daos.impl;

import app.daos.IDAO;
import app.dtos.CitizenDTO;
import app.entities.Citizen;
import app.exceptions.ApiException;
import app.mappers.Mapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.util.List;

public class CitizenDAO implements IDAO<CitizenDTO, Long>
{
    private static EntityManagerFactory emf;
    private static CitizenDAO instance;

    public static CitizenDAO getInstance(EntityManagerFactory _emf)
    {
        if (instance == null)
        {
            instance = new CitizenDAO();
            emf = _emf;
        }
        return instance;
    }


    @Override
    public CitizenDTO create(CitizenDTO citizenDTO)
    {

        try (EntityManager em = emf.createEntityManager())
        {
            try
            {
                em.getTransaction().begin();

                em.persist(citizenDTO);
                em.getTransaction().commit();
                return citizenDTO;
            } catch (Exception e)
            {
                em.getTransaction().rollback();
                throw new ApiException(401, "Error creating citizen", e);
            }
        }
    }

    @Override
    public CitizenDTO read(Long id)
    {
        try (EntityManager em = emf.createEntityManager())
        {
            Citizen citizen = em.find(Citizen.class, id);
            CitizenDTO dto = new CitizenDTO(citizen);
            return dto;
        }
    }

    @Override
    public List<CitizenDTO> readAll()
    {
        try (EntityManager em = emf.createEntityManager())
        {
            List<Citizen> citizens = em.createQuery("SELECT c FROM Citizen c ORDER BY c.id", Citizen.class).getResultList();
            List<CitizenDTO> citizenDTOS = Mapper.mapList(citizens, CitizenDTO::new);
            return citizenDTOS;
        } catch (Exception e)
        {
            throw new ApiException(401, "Error finding list of Citizen");
        }
    }

    @Override
    public CitizenDTO update(CitizenDTO citizenDTO)
    {
        try (EntityManager em = emf.createEntityManager())
        {
            Citizen citizen = new Citizen(citizenDTO);
            em.getTransaction().begin();
            Citizen result = em.merge(citizen);
            em.getTransaction().commit();
            CitizenDTO updatedCitizenDTO = new CitizenDTO(result);
            return updatedCitizenDTO;
        } catch (Exception e)
        {
            throw new ApiException(500, "Error updating Citizen", e);
        }
    }

    @Override
    public void delete(Long id)
    {
        try (EntityManager em = emf.createEntityManager())
        {
            em.getTransaction().begin();
            Citizen citizen = em.find(Citizen.class, id);
            if (citizen != null) em.remove(citizen);
            em.getTransaction().commit();
        } catch (Exception e)
        {
            throw new ApiException(500, "Error deleting Citizen", e);
        }
    }
}
