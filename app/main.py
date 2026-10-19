from fastapi import FastAPI

from app.routers import health

# Create FastAPI instance
app = FastAPI(title="Smart City API")

# Add routers here!
app.include_router(health.router)
