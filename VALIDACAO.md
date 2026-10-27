# Validação da entrega — 27/10/2026

Comando executado com sucesso: `./build.ps1 -Checks`.

| Verificação | Resultado |
| --- | --- |
| `:app:assembleDebug` | BUILD SUCCESSFUL |
| `:app:testDebugUnitTest` | 2 testes, 0 falhas, 0 erros |
| `:app:lintDebug` | 0 erros, 25 avisos |
| `apksigner verify --verbose` | Assinatura APK v2 válida, 1 signatário |
| ADB | Nenhum dispositivo conectado |
| Emulador | Nenhum AVD configurado |

Os avisos do lint incluem versões mais novas disponíveis, sugestões de KTX, recomendação de KSP em vez de kapt e serviço exportado. As versões permanecem fixadas no conjunto estável que foi compilado. O serviço de sessão é exportado para permitir descoberta por controladores de mídia, conforme a integração Media3. Não foi aplicado baseline para esconder erros.

APK: `app/build/outputs/apk/debug/app-debug.apk`.

SHA-256:
```text
7324F7A70B00730676ADCAAF061DEDB13B9C573BF7B0B6F4252484636A4509B3
```

Não foi possível executar a interface, ouvir áudio ou confirmar comportamento de Bluetooth, tela de bloqueio, permissões, persistência entre execuções e efeitos em hardware. Esses testes permanecem necessários; o roteiro está no README. A compilação e os testes unitários não comprovam esses comportamentos.

Os recursos foram implementados em código nativo e conectados às fontes reais do Android. Não há músicas demonstrativas, animação de frequência falsa ou equalizador simulado. Letras são importadas manualmente e exibidas como texto; não há sincronização LRC. A restauração de fila inicia pausada.
