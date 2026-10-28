# Validação da release 1.0.1 — 27/10/2026

O relatório completo está em [SECURITY_REVIEW.md](SECURITY_REVIEW.md), incluindo riscos residuais.

| Verificação | Resultado |
| --- | --- |
| `:app:assembleRelease` | Compilação concluída com credenciais próprias |
| `:app:testReleaseUnitTest` | 4 testes, 0 falhas, 0 erros |
| `:app:lintRelease` | 0 erros, 19 avisos |
| Verificação Gradle | Build normal com hashes SHA-256 ativos |
| `apksigner verify` | Assinaturas v2 e v3 válidas; certificado próprio RSA 3072 |
| Certificado do APK | Coincide com `security/release-certificate.pem` |
| `zipalign -c -P 16 -v 4` | Alinhamento ZIP validado |
| Segmentos ELF de 64 bits | Alinhamento de pelo menos 16 KB |
| Bibliotecas nativas | 8 entradas, hashes coincidem com AARs AndroidX |
| Permissões reais no APK | 6 permissões Android justificadas e 1 proteção signature AndroidX |
| Rede e microfone | INTERNET, ACCESS_NETWORK_STATE e RECORD_AUDIO ausentes |
| Componentes | Serviço privado; componentes exportados adicionais protegidos pelo sistema |
| OSV | 377 módulos/versões consultados; 0 correspondências em 198 módulos do runtime; 1 advisory residual no plugin Kotlin de build |
| Microsoft Defender | Atualização de assinaturas concluída; varredura release sem ameaças reportadas |
| Aparelho/AVD/Play Protect | Não disponíveis para teste |

Os 19 avisos do lint incluem atualizações de versões, recomendações de KTX/KSP e do compilador; nenhum baseline foi usado para esconder erros. Não são equivalentes a resultados de antivírus.

APK: `app/build/outputs/apk/release/app-release.apk`. A cópia para distribuição é `dist/Coelho-Music-1.0.1-release.apk`.

O hash do APK exato consta em [apk-audit.json](security/evidence/apk-audit.json) e `dist/SHA256SUMS.txt`. Assinaturas e fingerprint constam em [release-signature.txt](security/evidence/release-signature.txt). Logs de build, consulta OSV, manifesto extraído, inspeção DEX e scans estão em `security/evidence/`.

O relatório OSV continua apontando **GHSA-r937-wjx7-w2jp** no plugin Kotlin (ambiente de compilação). Cache Gradle e compilação incremental desativados reduzem a exposição, mas não comprovam eliminar a vulnerabilidade. Esse plugin não integra o runtime Android. Adotar uma versão estável corrigida e compatível permanece pendente.

Não foram executados testes comportamentais Android, Bluetooth, reprodução em segundo plano, instalação nem Play Protect. Os testes unitários cobrem normalização de busca, duração e política restritiva de URIs. Defender sem detecções não comprova ausência de malware ou aceitação em outro antivírus. A causa do alerta original permanece desconhecida.
