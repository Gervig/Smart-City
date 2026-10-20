from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import text
from sqlalchemy.exc import SQLAlchemyError
from sqlalchemy.orm import Session

from app.db import get_db

router = APIRouter()

@router.get("/health", summary="Check API status")
def health(db: Session = Depends(get_db)):
    """
    Health check endpoint to verify the API status.
    """
    return {"api": "ok"}

@router.get("/health/db", summary="Check database status")
def health_db(db: Session = Depends(get_db)):
    """
    Health check endpoint to verify the database status.
    """
    try:
        # Check if the database is reachable and responding
        # Execute a simple query to verify the connection
        if db.execute(text("SELECT 1")).scalar() != 1:
            raise HTTPException(status_code=503, detail="Unexpected database response")
    except SQLAlchemyError:
        raise HTTPException(status_code=503, detail="Database unavailable")

    return {"database": "ok"}