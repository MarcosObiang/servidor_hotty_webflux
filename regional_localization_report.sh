#!/bin/bash

echo "🌍 LOCALIZACIÓN REGIONAL: DIFERENCIAS POR PAÍS"
echo "==============================================="
echo

echo "📋 NUEVOS ARCHIVOS DE LOCALIZACIÓN CREADOS:"
echo "-------------------------------------------"
find src/main/resources/messages -name "*.properties" | sort | while read file; do
    echo "   📄 $file"
done
echo

echo "🇺🇸 VS 🇬🇧 INGLÉS:"
echo "------------------"
echo "🇺🇸 EN_US:"
grep "message=" src/main/resources/messages/messages_en_us.properties | sed 's/^/   /'
echo
echo "🇬🇧 EN_GB:"  
grep "message=" src/main/resources/messages/messages_en_gb.properties | sed 's/^/   /'
echo

echo "🇪🇸 ESPAÑOL POR PAÍSES:"
echo "----------------------"
echo "🇪🇸 ES_ES (España):"
grep "like.title=" src/main/resources/messages/messages_es_es.properties | sed 's/^/   /'
grep "like.message=" src/main/resources/messages/messages_es_es.properties | sed 's/^/   /'
echo
echo "🇲🇽 ES_MX (México):"
grep "like.title=" src/main/resources/messages/messages_es_mx.properties | sed 's/^/   /'
grep "like.message=" src/main/resources/messages/messages_es_mx.properties | sed 's/^/   /'
echo
echo "🇦🇷 ES_AR (Argentina):"
grep "chat.message=" src/main/resources/messages/messages_es_ar.properties | sed 's/^/   /'
grep "message.message=" src/main/resources/messages/messages_es_ar.properties | sed 's/^/   /'
echo
echo "🇨🇴 ES_CO (Colombia):"
grep "like.message=" src/main/resources/messages/messages_es_co.properties | sed 's/^/   /'
echo

echo "🎯 DIFERENCIAS DESTACADAS:"
echo "-------------------------"
echo "📝 TUTEO vs VOSEO:"
echo "   🇪🇸 España: 'Has recibido'"
echo "   🇲🇽 México: 'Recibiste'"  
echo "   🇦🇷 Argentina: 'Tenés' (voseo)"
echo
echo "📝 TERMINOLOGÍA:"
echo "   🇪🇸 España: 'Me Gusta'"
echo "   🇺🇸 Otros: 'Like'"
echo "   🇨🇴 Colombia: 'me gusta' (minúsculas)"
echo

echo "🔄 SISTEMA DE FALLBACK INTELIGENTE:"
echo "----------------------------------"
echo "1. 🎯 Busca archivo específico (ej: messages_es_mx.properties)"
echo "2. 🔄 Si no existe, busca genérico (ej: messages_es.properties)"
echo "3. 🛟 Si no existe, usa fallback global (EN)"
echo

echo "💡 EJEMPLOS DE USO:"
echo "-----------------"
echo "// Usuario de México"
echo "LocalizationCodes.ES_MX → 'Recibiste un nuevo like!'"
echo ""
echo "// Usuario de Argentina"  
echo "LocalizationCodes.ES_AR → '¡Tenés un nuevo chat!'"
echo ""
echo "// Usuario de España"
echo "LocalizationCodes.ES_ES → 'Nuevo Me Gusta'"
echo

echo "📊 ESTADÍSTICAS ACTUALES:"
echo "------------------------"
echo "   🌍 Locales específicos: $(find src/main/resources/messages -name "*_*.properties" | wc -l)"
echo "   🌐 Locales genéricos: $(find src/main/resources/messages -name "messages_??.properties" | wc -l)"
echo "   📁 Total archivos: $(find src/main/resources/messages -name "*.properties" | wc -l)"
echo

echo "✅ SISTEMA REGIONAL IMPLEMENTADO EXITOSAMENTE!"
echo "=============================================="
