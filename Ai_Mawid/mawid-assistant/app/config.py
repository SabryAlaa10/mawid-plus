import os
from dotenv import load_dotenv

load_dotenv()


def _s(name: str) -> str | None:
    v = os.getenv(name)
    return (v or "").strip() or None


SUPABASE_URL = _s("SUPABASE_URL")
SUPABASE_SERVICE_ROLE_KEY = _s("SUPABASE_SERVICE_ROLE_KEY")
SUPABASE_ANON_KEY = _s("SUPABASE_ANON_KEY")
SUPABASE_JWT_SECRET = _s("SUPABASE_JWT_SECRET")

# مفتاح apikey لطلبات Auth/REST: يكفي واحد (يفضّل service role على السيرفر)
SUPABASE_API_KEY: str | None = SUPABASE_SERVICE_ROLE_KEY or SUPABASE_ANON_KEY
OPENROUTER_API_KEY = os.getenv("OPENROUTER_API_KEY")
GROQ_API_KEY = os.getenv("GROQ_API_KEY")
