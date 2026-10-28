# Coelho Music — revisão de segurança e distribuição

**Data: 27/10/2026, America/Sao_Paulo. Versão revisada: 1.0.1, código 2.**

## Resultado e limites

Foram revisados todos os arquivos Kotlin próprios, manifesto fonte e mesclado, configuração Gradle, componentes e permissões do APK, árvore de dependências de execução/compilação/testes/plugins, conteúdo ZIP, referências DEX, bibliotecas nativas e assinatura. As correções estão implementadas. A entrega passa a ser uma release assinada com chave própria, sem acesso à internet ou microfone e com reprodução restrita a áudio do MediaStore.

**Não há evidência suficiente para determinar a causa do alerta original nem afirmar que o APK está livre de malware.** O nome do antivírus, texto da detecção e resultado do Play Protect não foram fornecidos. Os achados abaixo não comprovam que algum deles causou o alerta. Não foi alterado o Play Protect, não foram criadas exceções no antivírus nem empregados empacotadores, alterações de identidade destinadas a evitar detecção ou técnicas de ocultação.

Existe um **risco residual nas ferramentas de compilação**, detalhado em F07. Uma análise sem correspondências em uma base de vulnerabilidades e uma varredura local sem detecções não substituem testes Android e análise comportamental.

## Achados e correções

### F01 — distribuição de APK debug — prioridade alta, corrigido

O artefato 1.0.0 tinha `debuggable=true`, certificado Android Debug e atividade Compose Preview exportada, apropriados para desenvolvimento, não para distribuição. Isso amplia a superfície de depuração e expõe componentes desnecessários. Não é, por si só, evidência de vírus.

Em [app/build.gradle.kts:37](app/build.gradle.kts#L37), foi adicionada configuração release explícita: `debuggable=false`, assinatura própria v2/v3 e falha de compilação sem credenciais. Preview/tooling passam a ser dependências exclusivas de debug. Minificação/ofuscação e redução de recursos estão desativadas para manter o binário auditável; não existe packer. O APK preserva nomes de classes do aplicativo.

### F02 — permissão RECORD_AUDIO para recurso opcional — prioridade média, removida

O visualizador anterior usava a API oficial `android.media.audiofx.Visualizer`, mas exigia a permissão sensível de captura de áudio. Essa permissão não é necessária para tocar músicas. O visualizador, seus callbacks, a interface e a solicitação de permissão foram removidos. O manifesto também impede sua reintrodução por bibliotecas. Isso remove um recurso da versão anterior deliberadamente para reduzir permissões; nenhuma visualização falsa foi adicionada.

O equalizador oficial `android.media.audiofx.Equalizer` foi mantido, sem captura de microfone.

### F03 — suporte de rede transitivo desnecessário — prioridade média, corrigido

O manifesto mesclado antigo continha `ACCESS_NETWORK_STATE`, introduzida por biblioteca, embora o manifesto escrito no projeto não a listasse. Coil 2 também trazia OkHttp. **O APK antigo não declarava INTERNET**; portanto, essa descoberta não comprova envio de dados.

Coil foi migrado para 3.3.0, sem módulos `coil-network-*` ou cliente OkHttp. O manifesto remove explicitamente `INTERNET`, `ACCESS_NETWORK_STATE` e `RECORD_AUDIO` de qualquer mesclagem futura e desativa cleartext. As versões são fixas e a árvore final não contém OkHttp. Imagens continuam vindo do MediaStore ou de documentos escolhidos pelo usuário.

### F04 — serviço de reprodução exportado e ausência de política própria para controladores — prioridade média, corrigido

O serviço retornava a sessão para qualquer conexão descoberta. O Media3 possui regras próprias, mas faltava uma restrição explícita adequada ao escopo offline. O serviço passou a `exported=false`. Em [PlaybackService.kt:56](app/src/main/java/com/coelho/music/player/service/PlaybackService.kt#L56), conexões da sessão são limitadas por `ControllerInfo.isTrusted`, propriedade verificada pelo Media3, não por um nome de pacote informado pelo cliente.

Apenas o UID do próprio app pode fornecer músicas. Controladores confiáveis externos recebem comandos de transporte, sem substituir/adicionar a fila. O serviço continua necessário para reprodução em segundo plano, notificações e integração com controles do sistema. Não existe inicialização no boot, alarme, tarefa agendada, serviço oculto ou persistência de execução independente de reprodução.

A descoberta direta por aplicativos terceiros deixa de estar disponível. Comportamento de controles do sistema/Bluetooth em cada versão Android ainda precisa ser validado em aparelho.

### F05 — falta de uma restrição explícita de origem das músicas — prioridade média, corrigido

O ExoPlayer anterior utilizava a fábrica padrão, capaz de trabalhar com diferentes origens. Os caminhos normais do app usavam MediaStore, mas não havia validação formal ao recuperar fila ou receber itens da sessão.

A reprodução agora usa exclusivamente `ContentDataSource`. A política em [LocalMediaPolicy.kt:6](app/src/main/java/com/coelho/music/domain/model/LocalMediaPolicy.kt#L6) aceita somente `content://media/<volume>/audio/media/<id numérico>`, sem query, fragmento, travessia de diretório ou caminhos codificados. A política é aplicada à fila restaurada e aos itens recebidos pela sessão. URLs HTTP/HTTPS, arquivos por caminho e provedores arbitrários são rejeitados. Há testes para os casos aceitos e rejeitados.

### F06 — dependências antigas e vulnerabilidade Guava — prioridade alta, corrigido

A consulta OSV da árvore antiga de execução encontrou **GHSA-xxph-c9ww-hj94 / CVE-2026-102554** em Guava 33.3.1-android. Trata-se de alocação excessiva de memória ao desserializar coleções Java maliciosas. O código próprio do app não utiliza `ObjectInputStream`, e não foi demonstrado um caminho explorável no Coelho Music. Mesmo assim, a dependência foi atualizada para **33.7.2**, versão corrigida pelos mantenedores.

Também foram atualizadas versões de Media3, Room, Compose, Activity, Lifecycle, Navigation, DataStore, Kotlin/Hilt e Coil, preservando API mínima 26 e compile/target 36. Não foram encontrados repositórios JitPack, URLs de artefatos desconhecidos, dependências locais JAR/AAR/SO próprias, bibliotecas de anúncios, analytics ou instaladores.

| Dependência direta | Antes | Revisada |
| --- | --- | --- |
| Compose BOM | 2025.04.01 | 2025.12.00 |
| Activity Compose | 1.10.1 | 1.11.0 |
| Lifecycle Compose | 2.9.0 | 2.9.4 |
| Navigation Compose | 2.9.0 | 2.9.5 |
| Hilt / compilador | 2.56.2 | 2.57.2 |
| Room / compilador | 2.7.1 | 2.8.5 |
| DataStore | 1.1.4 | 1.2.0 |
| Media3 ExoPlayer e Session | 1.6.1 | 1.11.1 |
| Coil Compose | 2.7.0 | 3.3.0, sem módulo de rede |
| Kotlin / plugin Compose / kapt | 2.1.20 | 2.2.20 |
| Coroutines Android | 1.10.2 | 1.10.2 |
| Hilt Navigation Compose | 1.2.0 | 1.2.0 |
| Palette KTX | 1.0.0 | 1.0.0 |
| JUnit, somente testes | 4.13.2 | 4.13.2 |
| AGP / Gradle | 8.11.1 / 8.13 | 8.11.1 / 8.13 |

As versões mantidas ou não mais recentes foram avaliadas dentro do conjunto compatível com JDK 17 e SDK 36. Não se afirma que todas sejam as últimas publicadas. As dependências próprias e transitivas finais estão em [dependencies.json](security/evidence/dependencies.json), com os escopos. A consulta abrangeu **377 combinações módulo/versão**, incluindo **198 módulos no runtime da release**. O OSV não retornou correspondências conhecidas para esses módulos de runtime na data da consulta. Isso não cobre vulnerabilidades não indexadas, componentes sombreados ou falhas ainda desconhecidas.

### F07 — vulnerabilidades na cadeia de ferramentas — alta no componente, parcialmente corrigido

Ao incluir o classpath dos plugins na análise, surgiram 48 IDs de advisories na primeira consulta ampliada. Essas bibliotecas não são todas incluídas no APK. Foram aplicadas versões corrigidas das dependências de build: Guava 33.7.2-jre, Netty 4.1.137.Final, Bouncy Castle 1.85, Commons Compress 1.28.0, jose4j 0.9.6 e JDOM 2.0.6.1. As substituições estão explícitas em [build.gradle.kts:1](build.gradle.kts#L1). A compatibilidade foi verificada pela compilação, testes e lint.

**Permanece GHSA-r937-wjx7-w2jp no kotlin-gradle-plugin 2.2.20:** execução de código por desserialização insegura em metadados do cache de build. A correção indicada no OSV começa em 2.4.20-Beta1; não foi introduzida uma versão beta nem feita migração de toda a cadeia Android para ocultar o achado.

Em [gradle.properties:5](gradle.properties#L5), cache Gradle e compilação incremental Kotlin foram desativados. Não foi configurado cache remoto. São medidas de redução de exposição, **não comprovação de eliminação da vulnerabilidade**. O plugin não faz parte do runtime Android; o risco é para o ambiente de desenvolvimento/build que processe cache não confiável. Não reutilize caches de terceiros. A adoção de uma versão estável corrigida e compatível continua pendente e requer novo teste da cadeia. O achado é preservado integralmente em [osv-after.json](security/evidence/osv-after.json).

### F08 — controle de integridade e chaves de distribuição ausentes — prioridade média, corrigido

Foram adicionados lockfile das configurações da aplicação, metadados SHA-256 de verificação das dependências e checksum oficial da distribuição Gradle no wrapper. A distribuição local Gradle foi comparada ao SHA-256 publicado em services.gradle.org. A compilação normal foi repetida sem a opção de gerar metadados, para verificar a execução com a verificação ativa.

Os checksums das dependências foram inicializados a partir dos artefatos resolvidos via HTTPS de Google Maven, Maven Central e Gradle Plugin Portal. Isso é um registro de confiança inicial e detecção de alterações posteriores; **não é certificação independente do conteúdo** nem verificação GPG de cada fornecedor. Atualizações exigem revisão do diff de versões e hashes, não a regeneração indiscriminada dos metadados.

A chave release foi criada localmente pelo JDK, RSA 3072/SHA256withRSA, sem certificado de terceiros. Chave e senhas não estão no Git; existem regras de exclusão e ACLs restritas. O certificado público pode ser auditado em [release-certificate.pem](security/release-certificate.pem). O processo recusa sobrescrever uma identidade existente e não faz fallback para debug. Consulte [SIGNING.md](SIGNING.md) para localização, backup e migração.

## Permissões e componentes finais

| Permissão | Justificativa / tratamento |
| --- | --- |
| READ_MEDIA_AUDIO | Biblioteca local no Android 13+. Solicitada em contexto. |
| READ_EXTERNAL_STORAGE, maxSdkVersion 32 | Leitura de música em Android 8–12. Não concede escrita. |
| FOREGROUND_SERVICE | Serviço visível de reprodução em segundo plano. |
| FOREGROUND_SERVICE_MEDIA_PLAYBACK | Tipo específico exigido pelo Android para reprodução. |
| WAKE_LOCK | ExoPlayer mantém CPU durante reprodução local e gerencia a liberação. |
| MODIFY_AUDIO_SETTINGS | Equalizador da própria sessão; permissão normal, sem gravação. |
| com.coelho.music.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION | Gerada pelo AndroidX, protectionLevel signature. Protege receivers dinâmicos; não dá acesso a dados ou rede. |

Não há permissões de internet, estado de rede, gravação, câmera, localização, contatos, SMS, instalação de pacotes, acessibilidade, overlay, MANAGE_EXTERNAL_STORAGE, QUERY_ALL_PACKAGES ou boot.

MainActivity é exportada por ser o launcher e não interpreta extras/URIs externas. PlaybackService e o provider AndroidX Startup são privados. O serviço Room de invalidação entre processos foi removido, pois o app usa um único processo. A atividade Preview não entra na release. Os componentes padrão Media3 BluetoothValidationActivity e ProfileInstallReceiver do AndroidX, quando presentes, são protegidos respectivamente por BLUETOOTH_PRIVILEGED e DUMP; não estão abertos a apps comuns. Foram mantidos por sua integração de plataforma/otimização, sem serviços de captura ou rede.

O relatório do manifesto **extraído do próprio APK**, não apenas do arquivo fonte, está em [release-manifest.txt](security/evidence/release-manifest.txt). Um teste automático exige que qualquer componente exportado além do launcher tenha a proteção de plataforma prevista.

## Código, arquivos e comportamentos examinados

- Acesso às músicas por MediaStore/ContentResolver; Room usa consultas parametrizadas, sem SQL construído com nomes de playlist. Favoritos, histórico e preferências permanecem no armazenamento privado. Backup de nuvem/transferência está desabilitado.
- Letras e capas são abertas somente após seleção explícita pelo seletor Android. Letras têm leitura limitada a 100.000 caracteres e são exibidas como texto, sem HTML/JavaScript. Um provedor de documentos escolhido pelo usuário pode implementar seu próprio acesso à nuvem; isso não é tráfego de rede originado pelo processo deste app.
- PendingIntent de mídia é explícito e imutável. Foco de áudio, desconexão de fones e wake lock usam Media3/APIs Android oficiais.
- Não há comando de shell, Runtime.exec, ProcessBuilder, instalador, download de executáveis, DexClassLoader/InMemoryDexClassLoader, scripts embarcados ou chave privada dentro do APK. A revisão estática não encontrou essas chamadas no código Kotlin próprio.
- Os quatro arquivos classes*.dex são multidex normal do Android. Não foram encontrados DEX em assets nem executáveis APK/JAR/EXE/DLL/scripts aninhados. Nomes de classes do aplicativo permanecem legíveis.
- A inspeção DEX encontrou referências a HttpURLConnection, Socket, DownloadManager, WebView e ObjectInputStream em bibliotecas. Foram contextualizadas com dexdump: Media3 DefaultHttpDataSource/SimpleBitmapLoader, utilitários AndroidX/VersionedParcel, coleções Guava e Okio. Isso não demonstra download, navegação ou desserialização no fluxo do app. A origem de áudio é restrita, não há cliente Coil de rede e o manifesto não concede INTERNET. Esses símbolos não foram ocultados/removidos artificialmente para mudar detecção.
- As oito entradas .so correspondem a duas bibliotecas oficiais AndroidX em quatro ABIs: graphics-path e datastore-core. Seus conteúdos coincidem por SHA-256 com as AARs resolvidas. O teste verifica alinhamento ZIP e segmentos ELF de 64 bits para páginas de 16 KB. Não foi realizada auditoria manual de cada instrução nativa das bibliotecas ou do sistema Android.

## Verificações e evidências

O comando `./build.ps1 -Release -Checks` executa assembleRelease, testReleaseUnitTest e lintRelease. Os testes cobrem busca, duração e política de URI; não são testes de malware ou de comportamento em hardware.

Assinaturas v2/v3, certificado, permissões, componentes, conteúdos ZIP/DEX, procedência das bibliotecas nativas e alinhamento são verificados por `scripts/audit_apk.py` usando aapt2, apksigner e zipalign do SDK oficial. `scripts/query_osv.py` envia somente nomes/versões Maven ao OSV: nenhum áudio, código, chave ou APK é enviado.

Microsoft Defender foi atualizado e executado localmente no APK antigo e no release. Os resultados exatos, versões do mecanismo/assinaturas, hash do APK, resultados SDK e logs de build estão em `security/evidence/` e no resumo [VALIDACAO.md](VALIDACAO.md). A opção de scan `-DisableRemediation` impede quarentena automática durante essa análise, **não desativa a varredura, as assinaturas, a proteção do sistema ou o Play Protect**.

Sem dispositivo conectado ou AVD, não foram realizados instalação/execução Android, análise dinâmica de rede, testes Bluetooth, lock screen ou Google Play Protect. Não foi feita submissão do APK a VirusTotal/MobSF externo. É necessário reproduzir a mensagem original no artefato exato identificado por SHA-256 e, se persistir, usar o canal oficial de revisão do fornecedor; não se deve desativar a proteção para instalar.

## Referências primárias

- [Assinatura Android](https://developer.android.com/studio/publish/app-signing)
- [Media3: serviço de reprodução](https://developer.android.com/media/media3/session/background-playback)
- [Identidade confiável de controladores](https://developer.android.com/reference/androidx/media3/session/MediaSession.ControllerInfo)
- [Coil 3: dependências de rede opcionais](https://coil-kt.github.io/coil/upgrading_to_coil3/)
- [Guava 33.7.2: correção dos mantenedores](https://github.com/google/guava/releases/tag/v33.7.2)
- [Guava CVE-2026-102554](https://github.com/advisories/GHSA-xxph-c9ww-hj94)
- [Aviso residual do plugin Kotlin](https://github.com/advisories/GHSA-r937-wjx7-w2jp)
- [Verificação de dependências Gradle](https://docs.gradle.org/8.13/userguide/dependency_verification.html)
