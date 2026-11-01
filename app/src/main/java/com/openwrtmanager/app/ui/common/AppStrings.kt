package com.openwrtmanager.app.ui.common

import java.util.Locale

/** Centralized UI translations. English remains the fallback for unsupported locales. */
object AppStrings {
    private val arabic = mapOf(
        "Dashboard" to "لوحة المعلومات",
        "Packages" to "الحزم",
        "Retry" to "إعادة المحاولة",
        "Router overview" to "نظرة عامة على الموجّه",
        "Refresh" to "تحديث",
        "Log out" to "تسجيل الخروج",
        "Loading failed" to "فشل التحميل",
        "Restart the router?" to "إعادة تشغيل الموجّه؟",
        "The network will be briefly interrupted and all devices will reconnect. This action cannot be undone." to "سينقطع الاتصال بالشبكة مؤقتًا وستعيد جميع الأجهزة الاتصال. لا يمكن التراجع عن هذا الإجراء.",
        "Restart" to "إعادة التشغيل",
        "Cancel" to "إلغاء",
        "Load average (1 / 5 / 15 min)" to "متوسط الحمل (1 / 5 / 15 دقيقة)",
        "Memory usage" to "استخدام الذاكرة",
        "Storage (used to check package installation)" to "التخزين (يُستخدم للتحقق من تثبيت الحزم)",
        "Restart router" to "إعادة تشغيل الموجّه",
        "Add static lease" to "إضافة حجز ثابت",
        "Delete static lease" to "حذف الحجز الثابت",
        "Click “Apply changes” after deletion for it to take effect." to "اضغط «تطبيق التغييرات» بعد الحذف حتى يصبح ساريًا.",
        "Unsaved changes" to "تغييرات غير محفوظة",
        "Discard" to "تجاهل",
        "Apply changes" to "تطبيق التغييرات",
        "Start:" to "البداية:",
        "Count:" to "العدد:",
        "Lease:" to "مدة الحجز:",
        "Default" to "افتراضي",
        "Enable DHCP" to "تفعيل DHCP",
        "Delete" to "حذف",
        "Add" to "إضافة",
        "Device name" to "اسم الجهاز",
        "MAC address" to "عنوان MAC",
        "IP address" to "عنوان IP",
        "Firewall" to "جدار الحماية",
        "Add rule" to "إضافة قاعدة",
        "Delete rule" to "حذف القاعدة",
        "Add firewall rule" to "إضافة قاعدة جدار حماية",
        "Rule name" to "اسم القاعدة",
        "Source zone (e.g. wan)" to "المنطقة المصدر (مثل wan)",
        "Destination zone (e.g. lan)" to "المنطقة الوجهة (مثل lan)",
        "Destination port (optional)" to "منفذ الوجهة (اختياري)",
        "Protocol (tcp/udp)" to "البروتوكول (tcp/udp)",
        "Action (ACCEPT/REJECT/DROP)" to "الإجراء (ACCEPT/REJECT/DROP)",
        "Connect to your OpenWrt router" to "الاتصال بموجّه OpenWrt الخاص بك",
        "Sign in with your LuCI administrator account" to "سجّل الدخول بحساب مسؤول LuCI الخاص بك",
        "Name (optional, e.g. Home router)" to "الاسم (اختياري، مثل: موجّه المنزل)",
        "Router address" to "عنوان الموجّه",
        "Username" to "اسم المستخدم",
        "Password" to "كلمة المرور",
        "Sign in" to "تسجيل الدخول",
        "Enter the router address and username" to "أدخل عنوان الموجّه واسم المستخدم",
        "Saved connections" to "الاتصالات المحفوظة",
        "Connection failed: " to "فشل الاتصال: ",
        "Delete" to "حذف",
        "Fill form" to "ملء النموذج",
        "Running…" to "جارٍ التنفيذ…",
        "Close" to "إغلاق",
        "Package management" to "إدارة الحزم",
        "Update package index (opkg update)" to "تحديث فهرس الحزم (opkg update)",
        "Installed" to "مثبّتة",
        "Upgradable" to "قابلة للتحديث",
        "Search and install" to "البحث والتثبيت",
        "Uninstall" to "إلغاء التثبيت",
        "Upgrade all" to "تحديث الكل",
        "Upgrade" to "تحديث",
        "Search packages" to "البحث عن الحزم",
        "Search" to "بحث",
        "Install" to "تثبيت",
        "Loading package details…" to "جارٍ تحميل تفاصيل الحزمة…",
        "No data" to "لا توجد بيانات",
        "Version" to "الإصدار",
        "Package name" to "اسم الحزمة",
        "Description" to "الوصف",
        "Back" to "رجوع",
        "Open in browser" to "فتح في المتصفح",
        "Plugins" to "الإضافات",
        "DHCP" to "DHCP",
        "Loading failed" to "فشل التحميل"
    )

    fun text(value: String): String {
        if (Locale.getDefault().language != "ar") return value
        arabic[value]?.let { return it }
        return when {
            value.startsWith("Delete static lease “") -> {
                val item = value.removePrefix("Delete static lease “").removeSuffix("”？")
                "حذف الحجز الثابت «$item»؟"
            }
            value.startsWith("Delete rule “") -> {
                val item = value.removePrefix("Delete rule “").removeSuffix("”？")
                "حذف القاعدة «$item»؟"
            }
            value.startsWith("Install ") -> "تثبيت ${value.removePrefix("Install ").removeSuffix("？")}؟"
            value.startsWith("Uninstall ") -> "إلغاء تثبيت ${value.removePrefix("Uninstall ").removeSuffix("？")}؟"
            value.startsWith("Version: ") -> "الإصدار: ${value.removePrefix("Version: ")}"
            value.startsWith("Upgradable (") -> "قابلة للتحديث ${value.substringAfter("(").let { "($it" }}"
            value.startsWith("Connection failed: ") -> "فشل الاتصال: ${value.removePrefix("Connection failed: ")}"
            value.startsWith("Input: ") -> value.replace("Input:", "الدخول:").replace("Output:", "الخروج:").replace("Forward:", "التمرير:").replace(" · NAT", " · NAT")
            else -> value
        }
    }
}
