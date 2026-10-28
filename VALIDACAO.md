# ValidaÃ§Ã£o da release 1.0.1 â€” 27/10/2026

O relatÃ³rio completo estÃ¡ em [SECURITY_REVIEW.md](SECURITY_REVIEW.md), incluindo riscos residuais.

| VerificaÃ§Ã£o | Resultado |
| --- | --- |
| `:app:assembleRelease` | CompilaÃ§Ã£o concluÃ­da com credenciais prÃ³prias |
| `:app:testReleaseUnitTest` | 4 testes, 0 falhas, 0 erros |
| `:app:lintRelease` | 0 erros, 19 avisos |
| VerificaÃ§Ã£o Gradle | Build normal com hashes SHA-256 ativos |
| `apksigner verify` | Assinaturas v2 e v3 vÃ¡lidas; certificado prÃ³prio RSA 3072 |
| Certificado do APK | Coincide com `security/release-certificate.pem` |
| `zipalign -c -P 16 -v 4` | Alinhamento ZIP validado |
| Segmentos ELF de 64 bits | Alinhamento de pelo menos 16 KB |
| Bibliotecas nativas | 8 entradas, hashes coincidem com AARs AndroidX |
| PermissÃµes reais no APK | 6 permissÃµes Android justificadas e 1 proteÃ§Ã£o signature AndroidX |
| Rede e microfone | INTERNET, ACCESS_NETWORK_STATE e RECORD_AUDIO ausentes |
| Componentes | ServiÃ§o privado; componentes exportados adicionais protegidos pelo sistema |
| OSV | 377 mÃ³dulos/versÃµes consultados; 0 correspondÃªncias em 198 mÃ³dulos do runtime; 1 advisory residual no plugin Kotlin de build |
| Microsoft Defender | AtualizaÃ§Ã£o de assinaturas concluÃ­da; varredura release sem ameaÃ§as reportadas |
| Aparelho/AVD/Play Protect | NÃ£o disponÃ­veis para teste |

Os 19 avisos do lint incluem atualizaÃ§Ãµes de versÃµes, recomendaÃ§Ãµes de KTX/KSP e do compilador; nenhum baseline foi usado para esconder erros. NÃ£o sÃ£o equivalentes a resultados de antivÃ­rus.

APK: `app/build/outputs/apk/release/app-release.apk`. A cÃ³pia para distribuiÃ§Ã£o Ã© `dist/Coelho-Music-1.0.1-release.apk`.

O hash do APK exato consta em [apk-audit.json](security/evidence/apk-audit.json) e `dist/SHA256SUMS.txt`. Assinaturas e fingerprint constam em [release-signature.txt](security/evidence/release-signature.txt). Logs de build, consulta OSV, manifesto extraÃ­do, inspeÃ§Ã£o DEX e scans estÃ£o em `security/evidence/`.

O relatÃ³rio OSV continua apontando **GHSA-r937-wjx7-w2jp** no plugin Kotlin (ambiente de compilaÃ§Ã£o). Cache Gradle e compilaÃ§Ã£o incremental desativados reduzem a exposiÃ§Ã£o, mas nÃ£o comprovam eliminar a vulnerabilidade. Esse plugin nÃ£o integra o runtime Android. Adotar uma versÃ£o estÃ¡vel corrigida e compatÃ­vel permanece pendente.

NÃ£o foram executados testes comportamentais Android, Bluetooth, reproduÃ§Ã£o em segundo plano, instalaÃ§Ã£o nem Play Protect. Os testes unitÃ¡rios cobrem normalizaÃ§Ã£o de busca, duraÃ§Ã£o e polÃ­tica restritiva de URIs. Defender sem detecÃ§Ãµes nÃ£o comprova ausÃªncia de malware ou aceitaÃ§Ã£o em outro antivÃ­rus. A causa do alerta original permanece desconhecida.

## Identificação do artefato distribuído

SHA-256 do APK:
```text
b052ba1d24a3a3986201af68700be06fa09d39b2e5ebdf4212cef07b34e88db0
```

SHA-256 do certificado próprio:
```text
26529fcbc7c756bc03db1e3710f8162978a44401b2835c1efb920dc451f10008
```
