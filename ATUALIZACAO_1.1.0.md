# Coelho Music 1.1.0 — biblioteca recente e gestos

## Arquitetura e preservação

O projeto existente foi atualizado em Kotlin/Compose, mantendo MVVM, Hilt, Media3, Room e DataStore. `LibraryRepository` consulta o MediaStore; `MusicViewModel` coordena biblioteca/preferências; `PlaybackController` observa a sessão; `PlaybackService` continua proprietário da única instância ExoPlayer.

**O manifesto, o serviço de reprodução, a versão/esquema Room, as entidades, os DAOs e a chave release não foram alterados.** Playlists, favoritos e preferências da instalação release permanecem nos mesmos bancos/arquivos. Não houve nova dependência, permissão, serviço de monitoramento ou recriação do projeto. A identidade visual escura/roxa foi preservada, assim como tema claro e destaques opcionais existentes.

## Implementado

### Biblioteca

- Seção **Adicionadas recentemente** no topo do início, com até dez cartões: capa, título, artista e duração. **Ver todas** abre a lista completa. Tocar inicia a música usando a sequência ordenada como fila.
- Ordenação por `DATE_ADDED` decrescente, com ID decrescente como desempate determinístico. A data é a inclusão no índice MediaStore, não a criação original do arquivo. O filtro de duração existente também se aplica às recentes.
- `ContentObserver` registra alterações de mídia enquanto o app está em primeiro plano. Notificações são agrupadas por 350 ms. Quando o provedor informa o ID, só os IDs afetados são consultados; notificações de coleção, mais de 200 IDs ou retorno ao app fazem leitura completa. Não existe polling de biblioteca.
- IDs alterados são acumulados antes da conflation: eventos consecutivos não descartam músicas. Se uma mudança chega durante uma leitura, há nova passagem após ela. Consultas são serializadas, com fechamento de cursor, verificação de cancelamento e preservação das referências de itens inalterados.
- Arquivos pendentes ou na lixeira são ignorados onde a API oferece esses campos. Ao se tornarem disponíveis, uma notificação do MediaStore os inclui; se o app estava fechado/em segundo plano, a retomada reconcilia a biblioteca. O app não força a indexação de caminhos aos quais não tem acesso.
- Inclui também áudios válidos sem `IS_MUSIC` preenchido por alguns downloaders, excluindo alarmes/toques/notificações nessa alternativa. Não baixa arquivos nem amplia permissões.
- Estados de carregamento, falha com tentativa novamente, biblioteca vazia, ausência de músicas recentes e permissão negada. Mensagem: “Suas novas músicas aparecerão aqui.”

### Player e fila

- Arrastar a capa para a esquerda avança; direita volta à faixa anterior. As capas vizinhas acompanham o dedo. A troca só é solicitada após atingir **25% da largura**; movimentos incompletos/cancelados retornam à origem e não enviam comando de troca.
- Spring usa a velocidade medida no gesto. A velocidade não transforma um movimento curto em troca. Nos extremos da fila há resistência e retorno.
- Anterior/próxima são os índices reais do Media3, incluindo shuffle e repetição. Antes de enviar o comando, o controlador confere a faixa atual e o destino esperado; mudanças por Bluetooth/notificação ou edição da fila invalidam uma previsão antiga.
- Mudanças externas atualizam as capas por `AnimatedContent`; o estado Media3 é autoritativo. Troca externa, cancelamento, mudança de tamanho/rotação ou saída da tela cancelam a animação antiga. Há retorno de segurança se o comando não receber atualização de faixa.
- Arrastar a capa **64 dp para cima** abre a fila; **64 dp para baixo** minimiza. A área de reconhecimento fica na capa para não interceptar sliders, botões e rolagem do restante do player. A capa oferece também ações de acessibilidade.
- A fila agora é um bottom sheet, com **A seguir** na ordem real de execução e **Fila completa**. Permite selecionar, mover e remover músicas, incluindo entradas repetidas.
- O botão anterior existente mantém sua semântica Media3 de voltar/reiniciar. O gesto à direita usa explicitamente a faixa anterior.

### Miniplayer e animações

- Mantém capa, título, artista, reproduzir/pausar, próxima e progresso discreto. Toque ou arraste de pelo menos **32 dp para cima** expande.
- Transições verticais suaves na expansão/redução, entrada/saída da navegação inferior, animação de favoritos e cor de gradiente interpolada a partir da capa real. O miniplayer reaparece com o estado da mesma sessão.
- Usa APIs Compose já presentes: `Animatable`, `spring`, `AnimatedContent`, `AnimatedVisibility`, `animateContentSize`, listas lazy e `animateItem`. As animações Compose utilizam a escala de duração do sistema; não há motor de animação independente nem loop visual permanente. Escala zero/redução de movimento precisa ser validada no Android real.

### Desempenho

- O controlador deixou de reconstruir/copiar toda a fila a cada 500 ms. A fila é atualizada por eventos de timeline/metadados. Posição/duração têm fluxo separado e são coletadas apenas pelos controles de progresso; a consulta de posição para a UI é suspensa em segundo plano.
- Pesquisa/filtragem e ordenação de recentes fora da thread principal, com cancelamento dos resultados antigos. Busca normalizada é pré-calculada por música e usa Regex compartilhada. Chaves de ordenação evitam normalização repetida no comparador.
- `Song` tem contrato imutável e itens inalterados são reutilizados. Listas têm chaves estáveis; o cache singleton Coil foi mantido e capas usam `AsyncImage` sem subcomposição por linha.
- Operações Room continuam suspensas/assíncronas. Remoções do MediaStore retiram músicas da biblioteca disponível, sem apagar playlists/favoritos do banco. Arquivos inacessíveis continuam produzindo o tratamento de erro existente na reprodução.

## Arquivos criados ou alterados

| Área | Arquivos |
| --- | --- |
| Biblioteca e regras | `data/repository/LibraryRepository.kt`, `domain/model/Song.kt`, **novo** `domain/model/LibraryRules.kt`, `ui/MusicViewModel.kt` |
| Sessão/estado | `player/controller/PlaybackController.kt` |
| Gestos e controles | **novos** `ui/components/ArtworkGestures.kt`, `PlaybackWidgets.kt`, `QueueSheet.kt`; `ui/components/Components.kt` |
| Telas e navegação | **novo** `ui/screens/RecentSongsScreen.kt`; `LibraryScreen.kt`, `PlayerScreen.kt`, `SettingsScreen.kt`, `ui/navigation/CoelhoApp.kt` |
| Testes | **novos** `LibraryUpdateTest.kt` e `SwipeRulesTest.kt`; testes anteriores preservados |
| Build | `app/build.gradle.kts`: versão 1.1.0/código 3; pacote debug separado |
| Evidências | `scripts/audit_apk.py`: diretório de saída opcional; `docs/updates/1.1.0/`, README e este relatório |

Os caminhos Kotlin são relativos a `app/src/main/java/com/coelho/music/`; testes a `app/src/test/java/com/coelho/music/domain/model/`.

## Verificações realizadas

- Compilação debug e release, testes unitários release e lint em ambas as variantes, com verificação SHA-256 de dependências ativa. Nenhuma biblioteca funcional foi substituída nesta atualização.
- **11 testes unitários, 0 falhas:** pesquisa/duração anteriores, política de URI anterior, ordem/desempate de recentes, preview dos dez mais novos em conjunto de 2.000 registros, reutilização de objetos, delta de inclusão/alteração/remoção, limites horizontal/vertical, fila sem vizinho e gesto incompleto.
- Lint: **0 erros, 19 avisos** em cada variante. São principalmente recomendações de versões/KTX/KSP já conhecidas; não foram suprimidos por baseline.
- Inspeção do APK release pelo script SDK: permissões esperadas, serviço privado, ausência de debug, certificado próprio v2/v3 igual ao da release 1.0.1, alinhamento e procedência de bibliotecas nativas. Os detalhes estão em `docs/updates/1.1.0/security/`.
- Comparação Git: nenhum diff no manifesto, serviço, entidades/DAOs ou esquema Room. Isso comprova preservação estrutural, não substitui um teste de atualização com dados em aparelho.

**Não realizados:** instalação/execução Android, download real por navegador e espera da indexação, injeção de gestos na tela, Bluetooth, foco de áudio, lock screen, medição de FPS/memória/bateria e atualização de uma instalação com dados existentes. ADB não listou dispositivo e não há AVD configurado. Os testes de gestos acima validam a política pura de decisão, não o caminho completo do toque ao áudio.

## Instalação e testes pendentes

**Release:** `Coelho-Music-1.1.0-release.apk`, pacote `com.coelho.music`, mesma chave da 1.0.1. Foi configurada para atualizar essa release preservando dados; não desinstale a versão principal para atualizar. A atualização efetiva ainda deve ser validada no aparelho.

**Debug:** `Coelho-Music-1.1.0-debug.apk`, pacote `com.coelho.music.debug`. Instala ao lado da principal, com dados separados, para testes sem substituir a release. Não contém a chave privada release.

Roteiro de aceite em aparelho:

1. Criar playlist/favoritos e alterar preferências na 1.0.1; atualizar com a release 1.1.0 e conferir preservação.
2. Baixar áudio próprio/licenciado pelo navegador, aguardar indexação e observar a seção recente. Repetir com o app em segundo plano; voltar e conferir. Validar negação/revogação/liberação de permissão.
3. Conferir a ordem `DATE_ADDED`, limite dez, duração, filtros, lista completa e seleção direta. Remover um arquivo e atualizar.
4. Testar esquerda/direita completos e curtos, gesto cancelado, primeira/última faixa, repetição, shuffle, mudança de fila e rotação durante arraste.
5. Alterar a música pela notificação/Bluetooth durante o gesto e conferir capa, faixa e progresso. Verificar que não há troca extra atrasada.
6. Deslizar capa para cima/baixo, selecionar/reordenar/remover na fila e expandir pelo miniplayer. Confirmar áudio contínuo e sliders/botões independentes dos gestos.
7. Testar escala de animação zero, fonte ampliada, tela pequena/grande, biblioteca com milhares de faixas e uso de memória/bateria. Não há números de desempenho medidos nesta entrega.

## Problemas tratados e próximos passos

Foram tratados: custo da cópia periódica da fila, normalização repetida na pesquisa, possível perda de notificações agrupadas e divergência entre capa prevista e ordem shuffle. Como proteção adicional, mudanças externas invalidam o gesto antigo e a animação é cancelada ao mudar o tamanho da capa.

Prioridades futuras: testes Compose instrumentados com dispositivo/AVD, benchmark de listas/capas e persistência em atualização real, ajustes de limiares a partir de uso em aparelho e monitoramento da correção estável do aviso do plugin Kotlin já documentado na [revisão de segurança anterior](SECURITY_REVIEW.md). Esse risco residual do ambiente de build não foi resolvido nem ocultado por esta atualização. Não se afirma ausência de malware ou aprovação no Play Protect.
