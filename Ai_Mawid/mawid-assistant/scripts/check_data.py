# -*- coding: utf-8 -*-
"""
سكربت للتحقق من البيانات المحملة في Supabase
"""

import os
import sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from dotenv import load_dotenv
from supabase import create_client, Client

load_dotenv()

SUPABASE_URL = os.getenv("SUPABASE_URL")
SUPABASE_KEY = os.getenv("SUPABASE_SERVICE_ROLE_KEY")

if not SUPABASE_URL or not SUPABASE_KEY:
    print("خطأ: تأكد من وجود SUPABASE_URL و SUPABASE_SERVICE_ROLE_KEY في ملف .env")
    sys.exit(1)

supabase: Client = create_client(SUPABASE_URL, SUPABASE_KEY)

def check_data():
    """التحقق من البيانات المحملة"""

    print("=" * 60)
    print("  التحقق من البيانات في Supabase")
    print("=" * 60)

    # عد السجلات
    try:
        result = supabase.table("kb_chunks").select("id", count="exact").execute()
        total_count = result.count
        print(f"\n📊 إجمالي السجلات: {total_count}")

        if total_count == 0:
            print("❌ لا توجد بيانات محملة!")
            return

        # أظهر أمثلة
        print("\n📋 أمثلة من البيانات:")
        print("-" * 60)

        samples = supabase.table("kb_chunks").select("category, specialty, severity, content").limit(5).execute()

        for i, item in enumerate(samples.data, 1):
            print(f"{i}. {item['category']} | {item['specialty'] or 'عام'} | {item['severity']}")
            print(f"   {item['content'][:80]}...")
            print()

        # إحصائيات حسب التخصص
        print("📈 إحصائيات حسب التخصص:")
        print("-" * 60)

        # استعلام للإحصائيات
        stats = supabase.table("kb_chunks").select("specialty", count="exact").execute()
        specialty_counts = {}
        for item in stats.data:
            spec = item['specialty'] or 'عام'
            specialty_counts[spec] = specialty_counts.get(spec, 0) + 1

        for spec, count in sorted(specialty_counts.items(), key=lambda x: x[1], reverse=True)[:10]:
            print(f"  {spec}: {count}")

        print(f"\n✅ البيانات محملة بنجاح! ({total_count} عنصر طبي جاهز للاستخدام)")

    except Exception as e:
        print(f"❌ خطأ في الاتصال: {e}")

if __name__ == "__main__":
    check_data()