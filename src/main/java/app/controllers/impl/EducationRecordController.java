package app.controllers.impl;

import app.daos.impl.EducationRecordDAO;
import app.entities.EducationRecord;

public class EducationRecordController
        extends CrudController<EducationRecord> {

    public EducationRecordController(
            EducationRecordDAO educationRecordDAO) {
        super(
                educationRecordDAO,
                EducationRecord.class,
                "/education-records",
                "Education record",
                EducationRecord::setId
        );
    }
}