package app.daos.impl;

import app.daos.IDAO;
import app.dtos.InstitutionDTO;
import app.entities.Institution;
import app.exceptions.ApiException;
import app.mappers.Mapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.util.List;

public class InstitutionDAO implements IDAO<InstitutionDTO, Long>
{
    private static EntityManagerFactory emf;
    private static InstitutionDAO instance;

    public static InstitutionDAO getInstance(EntityManagerFactory _emf)
    {
        if (instance == null)
        {
            instance = new InstitutionDAO();
            emf = _emf;
        }
        return instance;
    }


    @Override
    public InstitutionDTO create(InstitutionDTO InstitutionDTO)
    {

        try (EntityManager em = emf.createEntityManager())
        {
            try
            {
                em.getTransaction().begin();

                em.persist(InstitutionDTO);
                em.getTransaction().commit();
                return InstitutionDTO;
            } catch (Exception e)
            {
                em.getTransaction().rollback();
                throw new ApiException(401, "Error creating institution", e);
            }
        }
    }

    @Override
    public InstitutionDTO read(Long id)
    {
        try (EntityManager em = emf.createEntityManager())
        {
            Institution institution = em.find(Institution.class, id);
            InstitutionDTO dto = new InstitutionDTO(institution);
            return dto;
        }
    }

    @Override
    public List<InstitutionDTO> readAll()
    {
        try (EntityManager em = emf.createEntityManager())
        {
            List<Institution> institutions = em.createQuery("SELECT i FROM Institution i ORDER BY i.id", Institution.class).getResultList();
            List<InstitutionDTO> InstitutionDTOS = Mapper.mapList(institutions, InstitutionDTO::new);
            return InstitutionDTOS;
        } catch (Exception e)
        {
            throw new ApiException(401, "Error finding list of institution");
        }
    }

    @Override
    public InstitutionDTO update(InstitutionDTO institutionDTO)
    {
        try (EntityManager em = emf.createEntityManager())
        {
            Institution institution = new Institution(institutionDTO);
            em.getTransaction().begin();
            Institution result = em.merge(institution);
            em.getTransaction().commit();
            InstitutionDTO updatedInstitutionDTO = new InstitutionDTO(result);
            return updatedInstitutionDTO;
        } catch (Exception e)
        {
            throw new ApiException(500, "Error updating institution", e);
        }
    }

    @Override
    public void delete(Long id)
    {
        try (EntityManager em = emf.createEntityManager())
        {
            em.getTransaction().begin();
            Institution institution = em.find(Institution.class, id);
            if (institution != null) em.remove(institution);
            em.getTransaction().commit();
        } catch (Exception e)
        {
            throw new ApiException(500, "Error deleting institution", e);
        }
    }
}
