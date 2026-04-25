#!/usr/bin/env python3
"""
تحقق سريع من إعداد موديل المساعد دون عرض مفاتيح أو أسرار.
الاستخدام (من مجلد mawid-assistant):
  python scripts/verify_assistant_stack.py
لاختبار /auth/v1/user بتوكن مستخدم فعلي (مثلاً من التطبيق إن وُفّر فقط مؤقتاً):
  set ASSISTANT_TEST_ACCESS_TOKEN=eyJ...   (PowerShell: $env:ASSISTANT_TEST_ACCESS_TOKEN="...")
  python scripts/verify_assistant_stack.py
"""
from __future__ import annotations

import base64
import json
import os
import sys
from pathlib import Path

# مجلد المشروع = أب لـ scripts/
_ROOT = Path(__file__).resolve().parent.parent
if str(_ROOT) not in sys.path:
    sys.path.insert(0, str(_ROOT))

# تحميل .env من mawid-assistant
os.chdir(_ROOT)
from dotenv import load_dotenv  # noqa: E402

load_dotenv(_ROOT / ".env")


def _b64url_payload_part(jwt: str) -> dict | None:
    try:
        parts = jwt.strip().split(".")
        if len(parts) < 2:
            return None
        payload = parts[1] + "=="[: (4 - len(parts[1]) % 4) % 4]
        raw = base64.urlsafe_b64decode(payload.encode())
        return json.loads(raw.decode())
    except Exception:
        return None


def _mask(s: str | None, keep: int = 8) -> str:
    if not s:
        return "(فارغ)"
    s = s.strip()
    if len(s) <= keep * 2:
        return "***"
    return s[:keep] + "..." + s[-keep:]


def main() -> int:
    supabase_url = (os.getenv("SUPABASE_URL") or "").strip()
    api_key = (os.getenv("SUPABASE_SERVICE_ROLE_KEY") or os.getenv("SUPABASE_ANON_KEY") or "").strip()
    jwt_secret = (os.getenv("SUPABASE_JWT_SECRET") or "").strip()

    print("=== mawid-assistant -- التحقق من الإعداد ===\n")

    if not supabase_url:
        print("[FAIL] SUPABASE_URL مفقود في .env")
        return 1
    print(f"[OK] SUPABASE_URL: {_mask(supabase_url, 12)}")

    if not api_key:
        print("[FAIL] لا يوجد SUPABASE_SERVICE_ROLE_KEY ولا SUPABASE_ANON_KEY")
        return 1
    print(f"[OK] مفتاح API (للتحقق عبر /auth): {_mask(api_key)}")

    claims = _b64url_payload_part(api_key)
    if claims:
        ref = claims.get("ref")
        role = claims.get("role")
        print(f"  (من jwt المفتاح) ref={ref!r}  role={role!r}")
        if ref and ref not in supabase_url:
            print(
                f"[FAIL] ref المفتاح ({ref}) لا يظهر داخل SUPABASE_URL --",
                "تطبيق أندرويد وملف .env لازم لنفس مشروع Supabase.",
            )
            return 1
        print("[OK] ref المفتاح يتطابق مع host المشروع في الرابط\n")
    else:
        print("  (تعذر قراءة ref من المفتاح -- تابع يدويًا)\n")

    if jwt_secret:
        print("[OK] SUPABASE_JWT_SECRET: معرّف (للاحتياطي المحلي)\n")
    else:
        print("! SUPABASE_JWT_SECRET: غير معرّف (السيرفر يعتمد /auth/v1/user إن وُجدت URL+مفتاح)\n")

    try:
        import httpx
    except ImportError:
        print("ثبّت: pip install httpx")
        return 1

    # السيرفر المحلي
    health_ok = False
    for base in ("http://127.0.0.1:8001", "http://127.0.0.1:8000"):
        try:
            r = httpx.get(f"{base}/health", timeout=3.0)
            if r.status_code == 200:
                print(f"[OK] السيرفر المحلي يستجيب: {base}/health -> {r.json()!r}\n")
                health_ok = True
                break
        except httpx.RequestError as e:
            print(f"  (تعذر {base}/health: {e})")
    if not health_ok:
        print(
            "[FAIL] لا رد من :8001 ولا :8000 -- شغّل Uvicorn من مجلد mawid-assistant مثلاً:",
            "  uvicorn app.main:app --reload --host 0.0.0.0 --port 8001",
        )
        return 1

    # طلب خفيف: انتهاء وهمي يثبت أن /auth/v1/user موجود
    u = f"{supabase_url.rstrip('/')}/auth/v1/user"
    try:
        r = httpx.get(
            u,
            headers={"Authorization": "Bearer x", "apikey": api_key},
            timeout=10.0,
        )
        if r.status_code in (401, 403, 200):
            print(
                f"[OK] Supabase auth يتصل: GET /auth/v1/user -> status={r.status_code} (متوقع 401 بتوكن وهمي)\n"
            )
        else:
            print(f"! GET /auth/v1/user status={r.status_code} (غير مألوف -- راجع المشروع)\n")
    except httpx.RequestError as e:
        print(f"[FAIL] لا يمكن الاتصال بـ Supabase من الجهاز الذي يشغّل السكريبت: {e}\n")
        return 1

    test = (os.getenv("ASSISTANT_TEST_ACCESS_TOKEN") or "").strip()
    if test:
        r = httpx.get(
            u,
            headers={"Authorization": f"Bearer {test}", "apikey": api_key},
            timeout=12.0,
        )
        if r.status_code == 200:
            print(
                "[OK] ASSISTANT_TEST_ACCESS_TOKEN: يعمل مع /auth/v1/user (200) -- المفتاح والمشروع صحيحان.\n"
            )
        elif r.status_code in (401, 403):
            print(
                "[FAIL] التوكن التجريبي مرفوض (401/403) --",
                "منتهي أو غلط مشروع. خذ accessToken جديد من تطبيق بعد تسجيل دخول.\n",
            )
        else:
            print(f"! status={r.status_code} body={r.text[:200]!r}\n")
    else:
        print(
            "INFO: لاختبار accessToken: ضبط متغير ASSISTANT_TEST_ACCESS_TOKEN ثم أعد تشغيل (لا ترفع التوكن علناً)."
        )

    print("=== انتهى ===")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
