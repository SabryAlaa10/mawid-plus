# app/main.py
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.routers import chat, doctors, appointments

app = FastAPI(title="Mawid+ AI Assistant", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(chat.router, prefix="/api", tags=["Chat"])
app.include_router(doctors.router, prefix="/api", tags=["Doctors"])
app.include_router(appointments.router, prefix="/api", tags=["Appointments"])


@app.get("/health")
def health_check():
    return {"status": "ok", "service": "mawid-assistant"}