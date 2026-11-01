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
        " Open in browser" to " فتح في المتصفح",
        "Zones" to "المناطق",
        "Rules" to "القواعد",
        "DHCP pools" to "مجموعات DHCP",
        "Static leases" to "الحجوزات الثابتة",
        "Dangerous operation; the network will be briefly interrupted" to "إجراء خطير؛ سينقطع الاتصال بالشبكة مؤقتًا",
        "This plugin does not have a dedicated management screen yet. Open its LuCI web interface in a browser." to "لا تملك هذه الإضافة شاشة إدارة مخصصة بعد. افتح واجهة LuCI الخاصة بها في المتصفح.",
        "Operation failed" to "فشلت العملية",
        " Operation failed" to " فشلت العملية",
        "No installed plugins found\n(packages whose names start with luci-app-)" to "لا توجد إضافات مثبّتة\n(الحزم التي تبدأ أسماؤها بـ luci-app-)",
        " Upgrade all" to " تحديث الكل",
        " Loading package details…" to " جارٍ تحميل تفاصيل الحزمة…",
        "Plugins" to "الإضافات",
        "DHCP" to "DHCP",
        "Network modes" to "أوضاع الشبكة",
        "WDS Bridge" to "جسر WDS",
        "Mesh 802.11s" to "شبكة Mesh 802.11s",
        "Repeater" to "مكرر الإشارة",
        "WDS, Mesh 802.11s, or Repeater/RelayD" to "WDS أو Mesh 802.11s أو Repeater/RelayD",
        "Choose a topology and configure this router" to "اختر نوع الشبكة واضبط هذا الموجّه",
        "A backup of network and wireless configuration is created before changes. Wi-Fi may disconnect during reload." to "سيتم إنشاء نسخة احتياطية من إعدادات الشبكة واللاسلكي قبل التغيير. قد ينقطع اتصال Wi‑Fi أثناء إعادة التحميل.",
        "Upstream SSID" to "اسم شبكة المصدر",
        "Wi-Fi password" to "كلمة مرور Wi‑Fi",
        "Mesh ID" to "معرّف Mesh",
        "Mesh password" to "كلمة مرور Mesh",
        "Upstream password" to "كلمة مرور المصدر",
        "Local SSID" to "اسم الشبكة المحلية",
        "Local password" to "كلمة مرور الشبكة المحلية",
        "Gateway IP" to "عنوان IP للبوابة",
        "Wi-Fi channel (optional)" to "قناة Wi‑Fi (اختياري)",
        "Router LAN IP" to "عنوان LAN للموجّه",
        "Install packages and apply network mode" to "تثبيت الحزم وتطبيق وضع الشبكة",
        "Install required packages" to "تثبيت الحزم المطلوبة",
        "Optional: install the LuCI/OpenWrt packages for this mode before applying configuration." to "اختياري: تثبيت حزم LuCI/OpenWrt الخاصة بهذا الوضع قبل تطبيق الإعدادات.",
        "Apply network mode" to "تطبيق وضع الشبكة",
        "Apply network mode?" to "تطبيق وضع الشبكة؟",
        "This backs up /etc/config/network and wireless, installs the required packages, and reloads Wi-Fi/network. The active connection may be lost." to "سيتم حفظ نسخة من /etc/config/network و wireless، وتثبيت الحزم المطلوبة، ثم إعادة تحميل الشبكة وWi‑Fi. قد ينقطع الاتصال الحالي.",
        "This backs up /etc/config/network and wireless, installs the selected packages, and reloads Wi-Fi/network. The active connection may be lost." to "سيتم حفظ نسخة من /etc/config/network و wireless، وتثبيت الحزم المحددة، ثم إعادة تحميل الشبكة وWi‑Fi. قد ينقطع الاتصال الحالي.",
        "This backs up /etc/config/network and wireless, applies UCI configuration without installing packages, and reloads Wi-Fi/network. The active connection may be lost." to "سيتم حفظ نسخة من /etc/config/network و wireless، وتطبيق إعدادات UCI دون تثبيت حزم، ثم إعادة تحميل الشبكة وWi‑Fi. قد ينقطع الاتصال الحالي.",
        "Apply" to "تطبيق",
        "For two or more fixed routers: the upstream AP must also support and enable WDS." to "لراوترين أو عدة راوترات ثابتة: يجب أن يدعم الراوتر المصدر WDS وأن يكون مفعّلًا عليه.",
        "For a multi-path mesh: every node must use the same Mesh ID, password, channel, and compatible Wi-Fi security." to "لشبكة متعددة المسارات: يجب استخدام معرّف Mesh وكلمة مرور وقناة وأمان Wi‑Fi متوافقة في كل العقد.",
        "For an upstream router you do not control: relayd bridges a client connection to a local access point." to "للاتصال براوتر لا تتحكم بإعداداته: يربط relayd اتصال العميل بنقطة وصول محلية.",
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
            value.startsWith("Version ") -> "الإصدار ${value.removePrefix("Version ")}"
            value.startsWith("Upgradable (") -> "قابلة للتحديث ${value.substringAfter("(").let { "($it" }}"
            value.startsWith("DHCP pools (") -> "مجموعات DHCP ${value.substringAfter("(").let { "($it" }}"
            value.startsWith("Static leases (") -> "الحجوزات الثابتة ${value.substringAfter("(").let { "($it" }}"
            value.startsWith("Zones (") -> "المناطق ${value.substringAfter("(").let { "($it" }}"
            value.startsWith("Rules (") -> "القواعد ${value.substringAfter("(").let { "($it" }}"
            value.startsWith("Start: ") -> value.replace("Start:", "البداية:").replace("Count:", "العدد:").replace("Lease:", "مدة الحجز:")
            value.contains("% used · remaining") -> value.replace("% used · remaining", "% مستخدم · المتبقي")
            value.startsWith("Connection failed: ") -> "فشل الاتصال: ${value.removePrefix("Connection failed: ")}"
            value.startsWith("Input: ") -> value.replace("Input:", "الدخول:").replace("Output:", "الخروج:").replace("Forward:", "التمرير:").replace(" · NAT", " · NAT")
            value.startsWith("Uptime: ") -> "مدة التشغيل: ${value.removePrefix("Uptime: ")}"
            value.startsWith("Removing core packages") -> "إزالة الحزم الأساسية (مثل dropbear أو uhttpd) قد تجعل الموجّه غير قابل للإدارة. تابع بحذر."
            else -> value
        }
    }
}
