# Assinatura de distribuição

O APK release usa chave **própria do projeto**, gerada localmente com o `keytool` do JDK 17, RSA 3072 e SHA256withRSA. O certificado é autoassinado, prática normal de assinatura Android. Não há certificado comprado, emprestado ou chave de terceiro. O nome `CN=Coelho Music` identifica o projeto; não é comprovação de identidade por uma autoridade certificadora.

Nesta máquina, a chave privada está em:

```text
C:\Users\BRUNO\.codex\signing\coelho-music\coelho-music-release.p12
```

As senhas aleatórias estão em `signing.properties`, ignorado pelo Git, com ACL restrita ao usuário atual. A pasta da chave também tem herança de permissões removida. O certificado **público**, que não permite assinar aplicativos, está em `security/release-certificate.pem`.

Faça uma cópia de segurança protegida da chave privada **e** das credenciais. Perder a chave impede atualizar instalações assinadas por ela. Não publique esses arquivos nem os anexe a releases, issues ou chats. Não execute o gerador novamente para uma aplicação já distribuída com essa identidade.

## Compilar

Nesta máquina as credenciais já estão configuradas:

```powershell
./build.ps1 -Release -Checks
```

Em outro ambiente, configure um JDK 17 e o SDK. Recupere sua chave de backup e preencha `signing.properties` conforme `signing.properties.example`. Alternativamente, use variáveis de ambiente `COELHO_STORE_FILE`, `COELHO_STORE_PASSWORD`, `COELHO_KEY_ALIAS` e `COELHO_KEY_PASSWORD` em armazenamento seguro do CI. Não coloque senhas na linha de comando.

Para um projeto novo, ainda sem identidade de distribuição, `./scripts/New-ReleaseSigning.ps1` cria uma chave e credenciais. O script recusa sobrescrever uma chave/configuração existente.

A compilação release falha se faltarem credenciais. Não utiliza assinatura debug como fallback. O tipo release define `debuggable=false`, sem minificação, ofuscação ou empacotador, e ativa assinaturas APK v2/v3. O projeto requer API 26+, portanto não precisa da assinatura JAR v1.

## Migração do APK antigo

A versão 1.0.0 foi assinada com a chave de debug; a release 1.0.1 usa outra identidade. O Android normalmente recusará uma atualização por cima da versão antiga. Será necessário desinstalar o APK de debug antes de instalar esta release, removendo playlists, favoritos e preferências locais. Os arquivos de música externos não são removidos pelo aplicativo. Não há tentativa de contornar a verificação de assinaturas do Android.

## Integridade

`apksigner verify --verbose --print-certs` verifica as assinaturas e informa a impressão digital pública. `SHA256SUMS.txt` identifica o APK exato analisado. Uma assinatura válida e um hash coincidente comprovam identidade/integridade do artefato, não ausência de malware ou aprovação no Play Protect.
