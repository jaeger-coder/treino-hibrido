# Treino Híbrido — Android wrapper

APK wrapper do Web App do Treino Híbrido.

## O que já está implementado
- URL padrão apontando para o deployment atual do Apps Script.
- Botão de configurações no canto superior direito para trocar e salvar a URL.
- Restaurar URL padrão.
- WebView com DOM storage, cookies e cache persistente.
- Cache nativo dos campos digitados, sobrevivendo ao fechamento do app.
- Injeção que tenta repor séries já salvas no Sheets usando o `STATE.data.logs` carregado pelo próprio Web App.
- Ícone roxo do Treino Híbrido.

## Build
Abra a pasta no Android Studio e gere `Build > Build APK(s)`. O projeto usa compileSdk 35, minSdk 26 e Java 17.

URL padrão:
https://script.google.com/macros/s/AKfycbw_6aLH5qbvE6zWMpbAj2BgISczuzo07RYvU-E06nD9iUpZH5jTaJMet1617v_ZxRh5OA/exec
