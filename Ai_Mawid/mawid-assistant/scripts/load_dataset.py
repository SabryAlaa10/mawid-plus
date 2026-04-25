# -*- coding: utf-8 -*-
"""
سكربت تحميل قاعدة المعرفة الطبية إلى Supabase مع Embeddings
=============================================================

الاستخدام:
  1. ثبّت المتطلبات: pip install -r requirements.txt
  2. أنشئ ملف .env بالبيانات المطلوبة
  3. شغّل SQL إنشاء جدول kb_chunks في Supabase (موجود أسفل)
  4. شغّل: python scripts/load_dataset.py

المتطلبات في .env:
  SUPABASE_URL=https://ccvhcytfozocejocmbqf.supabase.co
  SUPABASE_SERVICE_ROLE_KEY=eyJhbGciOi...
"""

import argparse
import os
import sys
import time

# أضف مجلد المشروع للـ path
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from dotenv import load_dotenv
from supabase import create_client, Client
from sentence_transformers import SentenceTransformer

from app.data.medical_kb.medical_knowledge_ar import MEDICAL_KNOWLEDGE

load_dotenv()

# ────────────────────────────────────────────────────────
# إعداد الاتصالات
# ────────────────────────────────────────────────────────
SUPABASE_URL = os.getenv("SUPABASE_URL")
SUPABASE_KEY = os.getenv("SUPABASE_SERVICE_ROLE_KEY")

if not SUPABASE_URL or not SUPABASE_KEY:
    print("خطأ: تأكد من وجود SUPABASE_URL و SUPABASE_SERVICE_ROLE_KEY في ملف .env")
    sys.exit(1)

supabase: Client = create_client(SUPABASE_URL, SUPABASE_KEY)

# تحميل نموذج Embedding (يعمل محلياً — بدون API)
# يجب أن يبقى مطابقاً لـ app/services/rag.py (EMBEDDING_MODEL_NAME) — vector(384)
print("جاري تحميل نموذج الـ Embedding متعدد اللغات (أول مرة قد ينزّل ~200MB)...")
model = SentenceTransformer("sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2")
print("تم تحميل النموذج. إن كانت kb_chunks مُمبّاةً بنموذج قديم فأعِد تشغيل السكربت لإعادة توليد الوسوم.\n")


# ────────────────────────────────────────────────────────
# SQL لإنشاء الجدول (شغّله مرة واحدة في Supabase SQL Editor)
# ────────────────────────────────────────────────────────
CREATE_TABLE_SQL = """
-- تفعيل pgvector
CREATE EXTENSION IF NOT EXISTS vector WITH SCHEMA extensions;

-- جدول أجزاء المعرفة الطبية
CREATE TABLE IF NOT EXISTS public.kb_chunks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content TEXT NOT NULL,
    category TEXT NOT NULL DEFAULT '',
    specialty TEXT,
    severity TEXT DEFAULT 'متوسط',
    metadata JSONB DEFAULT '{}',
    embedding vector(384),
    created_at TIMESTAMPTZ DEFAULT now()
);

-- فهرس للبحث السريع
CREATE INDEX IF NOT EXISTS idx_kb_embedding
    ON public.kb_chunks
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 10);

-- دالة البحث بالتشابه
CREATE OR REPLACE FUNCTION match_kb_chunks(
    query_embedding vector(384),
    match_threshold FLOAT DEFAULT 0.5,
    match_count INT DEFAULT 3
)
RETURNS TABLE (
    id UUID, content TEXT, category TEXT,
    specialty TEXT, metadata JSONB, similarity FLOAT
)
LANGUAGE sql STABLE AS $$
    SELECT
        id, content, category, specialty, metadata,
        1 - (embedding <=> query_embedding) AS similarity
    FROM public.kb_chunks
    WHERE 1 - (embedding <=> query_embedding) > match_threshold
    ORDER BY embedding <=> query_embedding
    LIMIT match_count;
$$;
"""


def clear_kb_chunks() -> None:
    """حذف كل الصفوف لإعادة توليد embeddings بنفس نموذج rag.py (تجنب تكرار/خلط فضاءات)."""
    try:
        r = (
            supabase.table("kb_chunks")
            .delete()
            .gte("created_at", "1970-01-01T00:00:00+00:00")
            .execute()
        )
        n = len(r.data) if r.data else 0
        print(f"تم تفريغ kb_chunks (حذف {n} صفاً سجِل).")
    except Exception as e:
        print(f"تنبيه: تعذر تفريغ الجدول تلقائياً: {e}")
        print("نفّذ يدوياً في SQL Editor:  TRUNCATE public.kb_chunks RESTART IDENTITY;")
        raise


def seed_knowledge() -> None:
    """تحميل كل البيانات الطبية مع embeddings إلى Supabase"""

    total = len(MEDICAL_KNOWLEDGE)
    success = 0
    errors = 0

    print(f"بدء تحميل {total} عنصر طبي...\n")
    print("-" * 60)

    for i, item in enumerate(MEDICAL_KNOWLEDGE, 1):
        try:
            embedding = model.encode(item["content"]).tolist()

            row = {
                "content": item["content"],
                "category": item["category"],
                "specialty": item.get("specialty"),
                "severity": item.get("severity", "متوسط"),
                "embedding": embedding,
                "metadata": {
                    "category": item["category"],
                    "specialty": item.get("specialty"),
                    "severity": item.get("severity", "متوسط"),
                },
            }

            supabase.table("kb_chunks").insert(row).execute()
            success += 1

            label = item["content"][:50].replace("\n", " ")
            print(f"  [{i:3d}/{total}] {item['category']:20s} | {label}...")

        except Exception as e:
            errors += 1
            print(f"  [{i:3d}/{total}] خطأ: {e}")

        if i % 20 == 0:
            time.sleep(0.5)

    print("-" * 60)
    print(f"\nالنتيجة: {success} نجح | {errors} فشل | من أصل {total}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description="تحميل MEDICAL_KNOWLEDGE إلى Supabase مع embeddings (مطابقة راغ rag.py)"
    )
    parser.add_argument(
        "-y",
        "--yes",
        action="store_true",
        help="تجاوز السؤال التفاعلي",
    )
    parser.add_argument(
        "--replace",
        action="store_true",
        help="تفريغ kb_chunks ثم إعادة الإدراج (مطلوب بعد تغيير نموذج الـ embedding)",
    )
    args = parser.parse_args()
    do_replace = args.replace

    print("=" * 60)
    print("  تحميل قاعدة المعرفة الطبية — موعد+")
    print("=" * 60)
    print()
    print("تنبيه: تأكد من تشغيل SQL إنشاء الجدول أولاً.")
    print("SQL موجود في المتغير CREATE_TABLE_SQL في هذا الملف.")
    print()

    if not args.yes:
        answer = input("هل تريد المتابعة؟ (y/n): ").strip().lower()
        if answer not in ("y", "yes", "نعم"):
            print("تم الإلغاء.")
            sys.exit(0)
    if do_replace:
        print("وضع --replace: تفريغ kb_chunks ثم إعادة الإدراج.\n")
        clear_kb_chunks()
    else:
        print("تنبيه: بدون --replace قد تتكرر الصفوف. لإعادة الـ embeddings استخدم: -y --replace\n")

    seed_knowledge()