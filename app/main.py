from fastapi import FastAPI

app = FastAPI(title="Smart City API")

@app.get("/health")
def health():
    return {"status": "ok"}