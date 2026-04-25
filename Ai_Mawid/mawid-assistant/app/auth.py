# app/auth.py
"""التحقق من المستخدم: أولاً عبر Supabase Auth API (لا يتطلب مطابقة JWT Secret يدوياً)."""
import logging
from fastapi import HTTPException, Header
import httpx
from jose import jwt, JWTError
from jose.jwt import ExpiredSignatureError

from app.config import SUPABASE_URL, SUPABASE_API_KEY, SUPABASE_JWT_SECRET

logger = logging.getLogger(__name__)

if not ((SUPABASE_URL and SUPABASE_API_KEY) or SUPABASE_JWT_SECRET):
    raise RuntimeError(
        "مطلوب إعدادات التحقق: إما (SUPABASE_URL + SUPABASE_SERVICE_ROLE_KEY أو "
        "SUPABASE_ANON_KEY) للتحقق عبر /auth/v1/user، أو SUPABASE_JWT_SECRET للتحقق المحلي."
    )


def _user_from_gotrue_user(data: dict) -> dict:
    uid = data.get("id")
    if not uid:
        raise HTTPException(401, "توكن غير صالح")
    app_meta = data.get("app_metadata") or {}
    return {
        "id": str(uid),
        "email": data.get("email"),
        "role": app_meta.get("role") or data.get("role"),
    }


async def _verify_via_supabase_user_endpoint(token: str) -> dict | None:
    """200 → المستخدم. 401/403 → مرفوض. غيرها أو خطأ اتصال → None (للاحتياطي بـ JWT)."""
    if not (SUPABASE_URL and SUPABASE_API_KEY):
        return None
    url = f"{SUPABASE_URL.rstrip('/')}/auth/v1/user"
    try:
        async with httpx.AsyncClient(timeout=12.0) as client:
            r = await client.get(
                url,
                headers={
                    "Authorization": f"Bearer {token}",
                    "apikey": SUPABASE_API_KEY,
                },
            )
    except httpx.RequestError as e:
        logger.warning("Supabase /auth/v1/user unreachable: %s", e)
        return None

    if r.status_code == 200:
        return _user_from_gotrue_user(r.json())
    if r.status_code in (401, 403, 404):
        logger.warning("auth: Supabase /auth/v1/user rejected (status=%s)", r.status_code)
        raise HTTPException(401, "جلسة منتهية، سجل دخول مجدداً")
    logger.warning("Supabase /auth/v1/user status=%s body=%s", r.status_code, r.text[:500])
    return None


def _verify_via_jwt_local(token: str) -> dict:
    if not SUPABASE_JWT_SECRET:
        raise HTTPException(
            503,
            "التحقق من الجلسة غير متاح مؤقتاً (تحقق من اتصال السيرفر بـ Supabase).",
        )
    try:
        unverified = jwt.get_unverified_claims(token)
        sub_preview = (unverified.get("sub") or "")[:8]
    except Exception:
        sub_preview = "?"
    try:
        payload = jwt.decode(
            token,
            SUPABASE_JWT_SECRET,
            algorithms=["HS256"],
            options={"verify_aud": False},
        )
    except ExpiredSignatureError:
        logger.warning("auth: local JWT verify failed: expired (sub…=%s)", sub_preview)
        raise HTTPException(401, "جلسة منتهية، سجل دخول مجدداً") from None
    except JWTError as e:
        logger.warning("auth: local JWT verify failed: %s (sub…=%s)", type(e).__name__, sub_preview)
        raise HTTPException(401, "جلسة منتهية، سجل دخول مجدداً") from None
    uid = payload.get("sub")
    if not uid:
        raise HTTPException(401, "توكن غير صالح")
    return {
        "id": uid,
        "email": payload.get("email"),
        "role": payload.get("role"),
    }


async def get_current_user(authorization: str = Header(...)) -> dict:
    if not authorization.startswith("Bearer "):
        raise HTTPException(401, "غير مصرح")
    token = authorization.replace("Bearer ", "").strip()
    if not token:
        raise HTTPException(401, "غير مصرح")

    # 1) نفس مفتاح المشروع في لوحة Supabase — يعمل حتى لو اختلف Legacy JWT secret عن نسخة .env
    user = await _verify_via_supabase_user_endpoint(token)
    if user is not None:
        return user

    # 2) استثناء: اختبار محلي / انقطاع → تحقق بالتوقيع إن وُجد السر
    return _verify_via_jwt_local(token)
