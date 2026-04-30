# app/main.py
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.routers import chat, doctors, appointments

app = FastAPI(title="Mawid+ AI Assistant", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://10.0.2.2",           # Android emulator dev
        "http://localhost:5173",      # Vite Web dev
        "http://localhost:3000",      # alternative Web dev
        # Add your production domain here before launch:
        # "https://your-production-domain.com",
    ],
    allow_credentials=True,
    allow_methods=["GET", "POST", "OPTIONS"],
    allow_headers=["Authorization", "Content-Type"],
)

app.include_router(chat.router, prefix="/api", tags=["Chat"])
app.include_router(doctors.router, prefix="/api", tags=["Doctors"])
app.include_router(appointments.router, prefix="/api", tags=["Appointments"])


@app.get("/health")
def health_check():
    return {"status": "ok", "service": "mawid-assistant"}