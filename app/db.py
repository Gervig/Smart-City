from app.config import settings

from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, DeclarativeBase, Session
from collections.abc import Generator

# an engine, which the Session will use for connection
# it owns the connection pool
engine = create_engine(settings.database_url, pool_pre_ping=True)


# configured session
# creates a new short lived session each time it's called like: session = SessionLocal()
SessionLocal = sessionmaker(bind=engine)


# base class that all models will inherit from
# it collects all table definitions (Base.metadata), used by Alembic
class Base(DeclarativeBase):
    pass


def get_db() -> Generator[Session, None, None]:
    """
    Handles creating and closing a new SessionLocal instance, 
    or DB session, for every request.
    """
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
