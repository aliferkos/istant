# Fendas — Masmorras Roguelike (Paper 1.21.1+)

Masmorras instantâneas com salas **geradas automaticamente em código** — nada para construir.

## Como compilar
Requer Java 21 e Maven.
```
mvn package
```
O plugin sai em `target/Fendas-1.0.0.jar` → copie para `plugins/` e reinicie.
(No IntelliJ: abra a pasta como projeto Maven → aba Maven → Lifecycle → package.)

Não precisa de nenhum outro plugin (nem WorldEdit).

## Primeiros passos
1. Coloque o jar e reinicie. O plugin cria sozinho o mundo vazio `fendas`.
2. Olhe para um bloco (ex.: um Lodestone ou Respawn Anchor) e use `/fenda altar adicionar`.
3. Pegue uma chave para testar: `/fenda chave`.
4. Clique com a Pedra de Fenda no altar. Quem estiver a até 6 blocos entra junto (máx. 4).
5. Para testar sem chave/altar: `/fenda iniciar`.

## Como funciona uma partida
- Cada **andar** = 3 salas de combate + sala do tesouro, ligadas por corredores.
- Tema sorteado por andar: **Cavernas de Gelo, Ruínas Esquecidas, Abismo Profundo, Forja Infernal**
  (blocos, mobs, chefe e nome dos itens mudam).
- Cada sala sorteia tamanho, altura e estilo: pilares, fossos (lava / neve fofa), ruínas ou misto.
- 3 ondas por sala; na última onda da sala 3 aparece o **chefe do andar**.
- Cada andar tem 1 **Maldição** (com recompensa) e 50% de chance de 1 **Bênção**.
- Na sala do tesouro: **baú** com ouro, esmeraldas e itens de RPG (Comum → Lendário),
  bloco **Lodestone** para descer ao próximo andar (até 3) e **Crying Obsidian** para sair.
- Quem morre não perde nada: vira espectador e volta quando a sala é limpa.
  Se todos caírem, a fenda fecha e todo mundo volta.

## Comandos
| Comando | Quem |
|---|---|
| `/fenda sair` | todos — abandona a fenda |
| `/fenda info` | todos — andar, sala, modificadores |
| `/fenda chave [jogador] [qtd]` | admin |
| `/fenda altar adicionar \| remover \| listar` | admin |
| `/fenda iniciar` | admin — abre fenda sem chave |
| `/fenda recarregar` | admin |

Permissão de admin: `fendas.admin` (OP por padrão).

## Importante no seu servidor
- **Aeternum Seasons**: adicione o mundo `fendas` à lista de mundos ignorados.
- **FrostSurvival (frio/sede)**: exclua o mundo `fendas` (ou deixe ativo de propósito, para dificultar).
- **Multiverse**: não precisa importar o mundo. Se o Multiverse já tiver um mundo chamado `fendas`
  que NÃO foi criado pelo plugin, ele não é apagado — troque o nome em `config.yml`.
- O mundo `fendas` é **apagado e recriado a cada reinício** (só ele, e só se tiver o marcador do plugin).
  Quem estava dentro de uma fenda é devolvido ao local de onde saiu ao entrar de novo.

## Ajustes (config.yml)
Quantidade de mobs, ondas, andares, tempo limite, chance de drop da chave e velocidade de
construção (`blocos-por-tick`: diminua se notar lag ao abrir fendas).
