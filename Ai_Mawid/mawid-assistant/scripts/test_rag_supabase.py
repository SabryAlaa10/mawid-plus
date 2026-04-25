#!/usr/bin/env python3
"""
تحقق أن استعلام RAG (نفس نموذج rag.py) يردّ مقاطع من kb_chunks بعد seed.
التشغيل من مجلد mawid-assistant:
  python scripts/test_rag_supabase.py
"""
from __future__ import annotations

import os
import sys
from pathlib import Path

_ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(_ROOT))
os.chdir(_ROOT)

from dotenv import load_dotenv  # noqa: E402

load_dotenv(_ROOT / ".env")


def main() -> int:
    if not (os.getenv("SUPABASE_URL") and os.getenv("SUPABASE_SERVICE_ROLE_KEY")):
        print("[FAIL] SUPABASE_URL / SUPABASE_SERVICE_ROLE_KEY مفقود")
        return 1

    from app.services import rag  # noqa: E402
    from app.services.rag import EMBEDDING_MODEL_NAME, build_rag_context  # noqa: E402

    print("=== RAG + Supabase ===")
    print(f"EMBEDDING_MODEL_NAME: {EMBEDDING_MODEL_NAME}\n")

    q = "ألم في الصدر وخفقان خفيف"
    ctx = build_rag_context(q, history=None)
    if "لم يتم العثور" in ctx:
        print(f"[FAIL] build_rag_context: {ctx!r}\n  تأكد من تشغيل load_dataset.py -y --replace")
        return 1
    print(f"[OK] build_rag_context (طول {len(ctx)} حرف):")
    print(ctx[:500] + ("…" if len(ctx) > 500 else ""))
    print()

    # استعلام قصير جداً قد يقلّ تحت عتبة match — نستخدم عبارة أطول مثل الاستعلامات الفعلية
    rows = rag.search_medical_knowledge("تعب وخفقان في القلب وألم صدري", limit=2)
    if not rows:
        print("[FAIL] search_medical_knowledge لم يردّ أي صف (تحقق من match_kb_chunks والعتبة)")
        return 1
    print(f"[OK] search_medical_knowledge: {len(rows)} نتائج (أول category={rows[0].get('category')!r})")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
