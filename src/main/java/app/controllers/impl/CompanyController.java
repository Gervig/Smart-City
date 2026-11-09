package app.controllers.impl;

import app.daos.impl.CompanyDAO;
import app.entities.Company;

public class CompanyController extends CrudController<Company> {

    public CompanyController(CompanyDAO companyDAO) {
        super(
                companyDAO,
                Company.class,
                "/companies",
                "Company",
                Company::setId
        );
    }
}