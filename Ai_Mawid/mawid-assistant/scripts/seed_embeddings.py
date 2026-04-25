# -*- coding: utf-8 -*-
"""
تحميل قاعدة المعرفة الطبية إلى Supabase (kb_chunks) مع Embeddings.

هذا الملف موجود عشان يطابق اسم الخطوة في الدليل (seed_embeddings.py).
التنفيذ الفعلي موجود في `scripts/load_dataset.py` — هنا بنستدعيه بدون تفاعل.
"""

import os
import sys

from dotenv import load_dotenv

# تأكد إن جذر المشروع موجود في PYTHONPATH
PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, PROJECT_ROOT)


def main() -> None:
    load_dotenv(os.path.join(PROJECT_ROOT, ".env"))

    if not os.getenv("SUPABASE_URL") or not os.getenv("SUPABASE_SERVICE_ROLE_KEY"):
        raise SystemExit("Missing SUPABASE_URL or SUPABASE_SERVICE_ROLE_KEY in .env")

    from scripts.load_dataset import seed_knowledge  # import after env + path setup

    seed_knowledge()


if __name__ == "__main__":
    main()