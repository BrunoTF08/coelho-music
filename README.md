# Coelho Music

Reprodutor Android nativo de músicas locais. Kotlin, Compose Material 3, MVVM, Hilt, Media3, Room, DataStore e Coil. Android 8.0+ (API 26), compilação e alvo API 36.

## Download e instalação

[Baixar Coelho Music 1.0.1 — APK release para Android](https://github.com/BrunoTF08/coelho-music/releases/download/v1.0.1/Coelho-Music-1.0.1-release.apk)

Veja os detalhes na [release v1.0.1](https://github.com/BrunoTF08/coelho-music/releases/tag/v1.0.1). No Android 8.0 ou superior, abra o arquivo e autorize a instalação pelo aplicativo utilizado para baixá-lo, caso o Android solicite. Ao iniciar o Coelho Music, permita acesso às músicas locais.

A versão 1.0.1 é uma compilação release com chave própria, sem permissão de rede ou microfone. Leia a [revisão de segurança](SECURITY_REVIEW.md) e as [instruções de assinatura](SIGNING.md). Para migrar da versão debug antiga, será necessário desinstalá-la, perdendo seus dados locais; os arquivos de música externos permanecem no dispositivo.

## Abrir e compilar

Configure sua chave conforme SIGNING.md antes de compilar uma release. Abra esta pasta no Android Studio e selecione um JDK 17 em **Gradle JDK**. Configure o caminho do SDK em `local.properties` (arquivo local, não versionado). As dependências são fixadas em `app/build.gradle.kts`; o wrapper usa Gradle 8.13 e AGP 8.11.1.

No ambiente preparado durante a implementação, execute:

```powershell
./build.ps1 -Release -Checks
```

Em outra máquina com Java e SDK configurados:

```powershell
./gradlew.bat :app:assembleRelease :app:testReleaseUnitTest :app:lintRelease
```

APK: `app/build/outputs/apk/release/app-release.apk`. A primeira compilação precisa de internet para as ferramentas e dependências. O aplicativo instalado funciona offline e não declara permissão de internet.

## Estrutura

```text
app/src/main/java/com/coelho/music/
  data/local/          Room: favoritos, histórico, playlists e ordem
  data/repository/     MediaStore e preferências DataStore
  domain/model/        Música, normalização da pesquisa e tempo
  di/                  Módulo Hilt
  player/controller/   MediaController e estado observável da reprodução
  player/service/      MediaSessionService, ExoPlayer e efeitos Android
  ui/components/       Capas, linhas, botões e miniplayer
  ui/navigation/       Navegação, permissões e ciclo de vida
  ui/screens/          Abertura, biblioteca, playlists, player e ajustes
  ui/theme/            Paleta e temas Material 3
```

## Recursos implementados

- Abertura com identidade própria, logotipo vetorial de coelho com fones e entrada animada.
- Permissão READ_MEDIA_AUDIO no Android 13+, READ_EXTERNAL_STORAGE até API 32. Sem permissão de microfone; visualizador removido na revisão 1.0.1.
- Biblioteca MediaStore em dispatcher IO, atualização manual e observação de alterações, estados vazios, de erro e carregamento; pesquisa sem distinção de maiúsculas ou acentos em títulos, artistas e álbuns. Playlists correspondentes aparecem na pesquisa e abrem a área de playlists.
- Navegação por artistas, álbuns, favoritas, músicas recentes e todas as músicas; listas lazy e capas via Coil.
- Reprodução pelo Media3 em serviço independente da atividade: foco de áudio, pausa por desconexão, sessão de mídia, notificação, comandos Bluetooth e tela de bloqueio.
- Player completo: seek, anterior/próxima, shuffle, repeat, volume da sessão, fila com adição, remoção, troca de música e reordenação. Gradiente calculado por Palette a partir da capa; fallback sem imagens fictícias.
- Fila, posição, repetição e aleatório salvos localmente. Ao reabrir, a fila é restaurada em pausa; a posição é gravada a cada cinco segundos durante reprodução e nas mudanças relevantes. Forçar a parada do app pelo sistema encerra seu serviço.
- Playlists Room: criar, renomear, excluir com confirmação, adicionar/remover/reordenar, capa personalizada com URI persistente. Favoritos e histórico persistentes. Arquivos ausentes não são incluídos nas listas reproduzíveis; entradas da playlist permanecem guardadas caso retornem.
- Equalizador real por sessão, com bandas e limites consultados no dispositivo; perfis Rock, Pop, Eletrônica, Graves e Voz aplicam níveis reais, além de ajustes personalizados persistentes. Indicação quando indisponível.
- Visualizador removido na revisão de segurança: o app não captura áudio. Equalizador real preservado.
- Letras locais: botão **Abrir letra**, selecione arquivo de texto ou LRC. Exibição textual, sem sincronização; o arquivo é escolhido pelo usuário, sem serviço online de letras.
- Tema claro/escuro, três destaques, filtro por duração e preferências DataStore. Qualidade original dos arquivos, sem opção artificial de qualidade de streaming. Atalho para notificações nas configurações do sistema.

## Privacidade

Sem conta, analytics, anúncios, servidores ou permissão de internet. Não há captura de áudio. Capas personalizadas e letras usam o seletor de documentos; não é solicitado acesso irrestrito a arquivos. Backup automático desabilitado.

## Verificação no dispositivo

Os testes unitários cobrem busca com acentos, formatação de duração e rejeição de URIs externas/malformadas na política de reprodução. O lint e a compilação verificam o projeto; não substituem os cenários abaixo. Não havia dispositivo conectado nem AVD configurado durante a implementação.

1. Android 8–12: conceder/negar armazenamento; Android 13+: conceder/negar acesso a músicas, inclusive negação permanente e posterior liberação nas configurações.
2. Copiar arquivos próprios ou com licença adequada em MP3, FLAC, WAV e AAC para Music. Abrir o aplicativo, pesquisar e reproduzir; suporte de codecs depende do dispositivo.
3. Reproduzir, trocar de tela, bloquear por alguns minutos, verificar notificação e tela de bloqueio; retirar fones e testar foco com outro aplicativo e chamadas; testar comandos Bluetooth.
4. Seek, shuffle, três estados de repetição, fim de fila, remoção/reordenação e troca de faixa. Remover ou corromper um arquivo e verificar a mensagem de erro; selecionar outra música para continuar.
5. Criar/renomear/reordenar playlist, selecionar capa, favoritar, fechar e reabrir; conferir fila e posição restauradas em pausa.
6. Aplicar efeitos em dispositivo que os ofereça; testar caso indisponível. Verificar que não existe solicitação de microfone, visualizador ou permissão de rede.
7. Testar biblioteca com milhares de faixas, telas pequenas/grandes e fontes ampliadas. Verificar acessibilidade, uso de memória e bateria em aparelho físico.

## Referências de implementação

- [Media3: reprodução em segundo plano](https://developer.android.com/media/media3/session/background-playback)
- [MediaStore: arquivos de áudio](https://developer.android.com/training/data-storage/shared/media)
- [Audio effects](https://developer.android.com/reference/android/media/audiofx/Equalizer)
- [Android Gradle Plugin](https://developer.android.com/build/releases/about-agp)

As bibliotecas usam versões estáveis fixas compatíveis com a configuração escolhida, sem resolução dinâmica de versões.
